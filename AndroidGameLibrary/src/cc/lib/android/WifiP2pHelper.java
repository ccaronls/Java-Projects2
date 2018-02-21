package cc.lib.android;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by chriscaron on 2/16/18.
 */

public class WifiP2pHelper implements WifiP2pManager.ChannelListener, WifiP2pManager.ActionListener, WifiP2pManager.PeerListListener {

    private final static String TAG = WifiP2pHelper.class.getSimpleName();
    private boolean p2pEnabled = false;

    BroadcastReceiver rcvr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BR:RECV = " + intent.getAction());
            switch (intent.getAction()) {
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    NetworkInfo ni = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    WifiP2pInfo wi = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                    WifiP2pGroup gi = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                    break;
                }
                case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
                    switch (state) {
                        case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                            Log.i(TAG, "BR:RCV = WIFI P2P DISCOVERY STARTED");
                            break;

                        case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                            Log.i(TAG, "BR:RCV = WIFI P2P DISCOVERY STOPPED");
                            break;
                    }
                    break;
                }
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
                    WifiP2pDeviceList list = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);

                    break;
                }
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: {

                    break;
                }
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    switch (state) {
                        case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                            Log.i(TAG, "BR:RCV = WIFI P2P STATE DISABLED");
                            p2pEnabled = false;
                            break;
                        case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                            Log.i(TAG, "BR:RCV = WIFI P2P STATE ENABLED");
                            p2pEnabled = true;
                            break;
                    }
                    break;
                }
            }
        }
    };

    final Context ctxt;
    final WifiP2pManager p2p;
    WifiP2pManager.Channel channel;
    final IntentFilter p2pFilter;
    enum State {
        READY,
        DISCOVERING
    }

    private State state = State.READY;

    WifiP2pHelper(Context ctxt) {
        this.ctxt = ctxt;
        p2p = ctxt.getSystemService(WifiP2pManager.class);
        p2pFilter = new IntentFilter();
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        //p2pFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    }

    public void p2pInitialize() {
        if (state == State.READY) {
            channel = p2p.initialize(ctxt, ctxt.getMainLooper(), this);
            ctxt.registerReceiver(rcvr, p2pFilter);
            state = State.DISCOVERING;
            Log.i(TAG, "p2pInitialized success");
            discover();
        }
    }

    @Override
    public final void onChannelDisconnected() {
        if (state != State.READY) {
            Log.d(TAG, "channel disconnected");
            close();
        }
    }

    protected AlertDialog.Builder newDialog() {
        return new AlertDialog.Builder(ctxt);
    }

    @Override
    public final void onSuccess() {
        Log.i(TAG, "Discover peers success");
        // discover peers success
        requestPeers();
    }

    private String getFailureReasonString(int reason) {
        switch (reason) {
            case WifiP2pManager.ERROR:
                return "ERROR";
            case WifiP2pManager.BUSY:
                return "BUSY";
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "P2P UNSUPPORTED";
        }
        return "UNKNOWN";
    }

    @Override
    public final void onFailure(int reason) {
        Log.e(TAG, "Discover peers failure: " + getFailureReasonString(reason));
        // discover peers error
        switch (reason) {
            case WifiP2pManager.ERROR:
            case WifiP2pManager.BUSY:
            case WifiP2pManager.P2P_UNSUPPORTED:
        }
    }

    private String statusToString(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
        }
        return "UNKNOWN";
    }

    @Override
    public final void onPeersAvailable(WifiP2pDeviceList peers) {
        Collection<WifiP2pDevice> devices = peers.getDeviceList();
        String [] items = new String[devices.size()];
        int index = 0;
        for (WifiP2pDevice d : devices) {
            items[index++] = d.deviceName + " " + d.primaryDeviceType + " " + statusToString(d.status);
        }
        Log.i(TAG, "Peers Available:\n" + Arrays.toString(items));
        newDialog()
                .setTitle("Available Devices")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WifiP2pConfig config = new WifiP2pConfig();
                        //config.
                        p2p.connect(channel, config, WifiP2pHelper.this);
                    }
                }
                ).setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    public void discover() {
        p2p.discoverPeers(channel, this);
    }

    void close() {
        if (state != State.READY)
            ctxt.unregisterReceiver(rcvr);
        state = State.READY;
    }

    void pause() {
        if (state != State.READY)
            ctxt.unregisterReceiver(rcvr);
        state = State.READY;
    }

    void resume() {
        ctxt.registerReceiver(rcvr, p2pFilter);
    }

    private void requestPeers() {
        p2p.requestPeers(channel, this);
    }
}
