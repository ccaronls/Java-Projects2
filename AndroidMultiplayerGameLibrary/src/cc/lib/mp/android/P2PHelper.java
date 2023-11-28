package cc.lib.mp.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import cc.lib.android.CCActivityBase;
import cc.lib.net.AGameClient;
import cc.lib.net.AGameServer;

@SuppressLint("MissingPermission")
abstract class P2PHelper extends BroadcastReceiver implements
        WifiP2pManager.PeerListListener,
        WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.GroupInfoListener,
        Application.ActivityLifecycleCallbacks {

    static final String TAG = "P2PGame" + P2PHelper.class.getSimpleName();

    private final CCActivityBase activity;
    private final WifiP2pManager p2pMgr;
    private final IntentFilter p2pFilter;
    private final WifiP2pManager.Channel channel;
    private final List<WifiP2pDevice> peers = new ArrayList<>();
    private AGameServer server;
    private AGameClient client;
    private boolean registered = false;

    public static boolean isP2PAvailable(Context context) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            return false;
        }
        WifiP2pManager p2pMgr = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (p2pMgr == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isP2pSupported()) {
                return false;
            }
        }
        return true;
    }

    public P2PHelper(CCActivityBase activity) {
        if (!isP2PAvailable(activity))
            throw new IllegalArgumentException("P2P Not supported");
        this.activity =activity;
        p2pFilter = new IntentFilter();
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        //p2pFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pMgr = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pMgr.initialize(activity, activity.getMainLooper(), null);
    }

    protected abstract void onP2PEnabled(boolean enabled);

    @Override
    public final void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                onP2PEnabled(true);
                if (server != null) {
                    p2pMgr.createGroup(channel, new MyActionListener("createGroup(server)") {
                        @Override
                        public void onSuccess() {
                            p2pMgr.requestGroupInfo(channel, P2PHelper.this);
                        }
                    });
                }
            } else {
                onP2PEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // The peer list has changed! We should probably do something about
            // that.
            p2pMgr.requestPeers(channel, this);

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            // Connection state changed! We should probably do something about
            // that.
            p2pMgr.requestPeers(channel, this);
            Log.d(TAG, "P2P peers changed");

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // We are connected with the other device, request connection
                // info to find group owner IP
                p2pMgr.requestConnectionInfo(channel, this);
            } else {
                // its a disconnect

            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            //DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
              //      .findFragmentById(R.id.frag_list);
            WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            if (device != null) {
                Log.d(TAG, "Device Changed: " + device.deviceName);
                onThisDeviceUpdated(device);
            }
        }
    }

    protected abstract void onThisDeviceUpdated(WifiP2pDevice device);

    public final void start(AGameServer server) {
        if (client != null || this.server != null)
            throw new IllegalArgumentException("Already started");
        this.server = server;
        setRegistered(true);
        activity.getApplication().registerActivityLifecycleCallbacks(this);
    }

    private synchronized void setRegistered(boolean enable) {
        if (enable == registered)
            return;
        if (enable) {
            activity.registerReceiver(this, p2pFilter);
            registered = true;
        } else {
            activity.unregisterReceiver(this);
            registered = false;
        }
    }

    @Override
    public final void onGroupInfoAvailable(WifiP2pGroup group) {
        if (group != null) {
            Log.i(TAG, "Group Info: "
                    + "\nInterface: " + group.getInterface()
                    + "\nNetwork Name: " + group.getNetworkName()
                    + "\nOwner: " + group.getOwner().deviceName
                    + "\npassPhrase: " + group.getPassphrase()
            );
        }
    }

    public final void start(AGameClient client) {
        if (this.client != null || server != null)
            throw new IllegalArgumentException("Already started");
        this.client = client;
        setRegistered(true);
        activity.getApplication().registerActivityLifecycleCallbacks(this);
        p2pMgr.discoverPeers(channel, new MyActionListener("start(client)"));
    }

    public final void stop() {
        if (server != null) {
            p2pMgr.removeGroup(channel, new MyActionListener("removeGroup"));
        }
        client = null;
        server = null;
        p2pMgr.stopPeerDiscovery(channel, new MyActionListener("stopPeerDiscovery"));
        setRegistered(false);
        activity.getApplication().unregisterActivityLifecycleCallbacks(this);
    }

    private class MyActionListener implements WifiP2pManager.ActionListener {

        final String action;

        MyActionListener(String action) {
            this.action = action;
        }

        @Override
        public void onSuccess() {
            Log.d(TAG, action + " Success");
        }

        @Override
        public void onFailure(int reason) {
            String msg = action + " Failure "+  getFailureReasonString(reason);
            Log.e(TAG, msg);
            activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_LONG).show());
        }
    }

    protected abstract void onPeersList(List<WifiP2pDevice> peers);

    @Override
    public final void onPeersAvailable(WifiP2pDeviceList peerList) {
        List<WifiP2pDevice> refreshedPeers = new ArrayList<>(peerList.getDeviceList());
        if (!refreshedPeers.equals(peers)) {
            synchronized (peers) {
                peers.clear();
                peers.addAll(refreshedPeers);
            }

            // If an AdapterView is backed by this data, notify it
            // of the change. For instance, if you have a ListView of
            // available peers, trigger an update.
            //((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

            // Perform any other updates needed based on the new list of
            // peers connected to the Wi-Fi P2P network.
            p2pMgr.requestGroupInfo(channel, this);

            onPeersList(peers);
        }

        if (peers.size() == 0) {
            Log.d(TAG, "No devices found");
        }
    }

    public final void connect(WifiP2pDevice device) throws Exception {

        try {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            config.groupOwnerIntent = 0;

            p2pMgr.connect(channel, config, new MyActionListener("connect(" + device.deviceName + ")") {
                @Override
                public void onSuccess() {
                    super.onSuccess();
                    p2pMgr.requestConnectionInfo(channel, P2PHelper.this);
                }
            });
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    public final void setDeviceName(String name) {
        try {
            final Method m = p2pMgr.getClass().getMethod(
                    "setDeviceName",
                    new Class[] { WifiP2pManager.Channel.class, String.class,
                            WifiP2pManager.ActionListener.class });

            m.invoke(p2pMgr, channel, name, new MyActionListener("setDeviceName(" + name + ")"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "onConnectionInfoAvailable: " + info);

        // After the group negotiation, we can determine the group owner
        // (server).
        if (info != null && info.groupOwnerAddress != null && info.groupFormed) {
            String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a group owner thread and accepting
            // incoming connections.
            onGroupFormed(info.groupOwnerAddress, groupOwnerAddress);
        }
    }

    protected abstract void onGroupFormed(InetAddress addr, String ipAddress);

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {
        Log.i(TAG, "activity started");
        setRegistered(true);
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.w(TAG, "activity stopped");
        setRegistered(false);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    private String getFailureReasonString(int reason) {
        switch (reason) {
            case WifiP2pManager.ERROR:
                return activity.getString(cc.lib.mp.android.R.string.wifi_failure_reason_error);
            case WifiP2pManager.BUSY:
                return activity.getString(cc.lib.mp.android.R.string.wifi_failure_reason_busy);
            case WifiP2pManager.P2P_UNSUPPORTED:
                return activity.getString(cc.lib.mp.android.R.string.wifi_failure_reason_p2p_unsupported);
        }
        return activity.getString(cc.lib.mp.android.R.string.wifi_failure_reason_unknown);
    }

    public static String statusToString(int status, Context activity) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return activity.getString(cc.lib.mp.android.R.string.wifi_conn_status_available);
            case WifiP2pDevice.CONNECTED:
                return activity.getString(cc.lib.mp.android.R.string.wifi_conn_status_connected);
            case WifiP2pDevice.FAILED:
                return activity.getString(cc.lib.mp.android.R.string.wifi_conn_status_failed);
            case WifiP2pDevice.INVITED:
                return activity.getString(cc.lib.mp.android.R.string.wifi_conn_status_invited);
            case WifiP2pDevice.UNAVAILABLE:
                return activity.getString(cc.lib.mp.android.R.string.wifi_conn_status_unavailable);
        }
        return activity.getString(cc.lib.mp.android.R.string.wifi_conn_status_unknown);
    }

}
