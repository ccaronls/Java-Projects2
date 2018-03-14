package cc.lib.android;

import android.app.Activity;
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
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.game.Utils;

/**
 * Created by chriscaron on 2/16/18.
 */

public class WifiP2pHelper implements
        WifiP2pManager.ChannelListener,
        WifiP2pManager.PeerListListener,
        WifiP2pManager.DnsSdTxtRecordListener,
        WifiP2pManager.DnsSdServiceResponseListener,
        WifiP2pManager.UpnpServiceResponseListener,
        WifiP2pManager.GroupInfoListener,
        WifiP2pManager.ConnectionInfoListener {

    private final static String TAG = WifiP2pHelper.class.getSimpleName();
    private Handler handler = new Handler(Looper.getMainLooper());
    private WifiP2pDevice connection = null;
    private Object lock = new Object();

    private BroadcastReceiver rcvr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BR:RECV = " + intent.getAction());
            switch (intent.getAction()) {
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    //NetworkInfo ni = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    //WifiP2pInfo wi = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                    //WifiP2pGroup gi = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                    if (networkInfo.isConnected()) {

                        // We are connected with the other device, request connection
                        // info to find group owner IP
                        p2p.requestConnectionInfo(channel, WifiP2pHelper.this);
                    }
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
                    //WifiP2pDeviceList list = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                    //onPeersAvailable(list);
                    requestPeers();
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
                            destroy();
                            break;
                        case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                            Log.i(TAG, "BR:RCV = WIFI P2P STATE ENABLED");
                            break;
                    }
                    break;
                }
            }
        }
    };

    final Activity ctxt;
    final WifiP2pManager p2p;
    WifiP2pManager.Channel channel;
    final IntentFilter p2pFilter;

    enum State {
        READY,
        INITIALIZED,
        PEERS,
        SERVICES,
    }

    private State state = State.READY;

    public WifiP2pHelper(Activity ctxt) {
        this.ctxt = ctxt;
        p2p = (WifiP2pManager)ctxt.getSystemService(Context.WIFI_P2P_SERVICE);
        p2pFilter = new IntentFilter();
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        //p2pFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    }

    /**
     *
     */
    public final void p2pInitialize() {
        if (state == State.READY) {
            channel = p2p.initialize(ctxt, ctxt.getMainLooper(), this);
            ctxt.registerReceiver(rcvr, p2pFilter);
            state = State.INITIALIZED;
            Log.i(TAG, "p2pInitialized success");
        }
    }

    @Override
    public final void onChannelDisconnected() {
        if (state != State.READY) {
            Log.d(TAG, "channel disconnected");
            destroy();
            ctxt.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    newDialog().setTitle("Channel Disconnected")
                            .setMessage("Attempt ot re-connect?")
                            .setNegativeButton("No", null)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    p2pInitialize();
                                }
                            }).show();
                }
            });
        }
    }

    protected void onError(String message) {
        Toast.makeText(ctxt, "Failed: " + message, Toast.LENGTH_LONG).show();
    }

    protected AlertDialog.Builder newDialog() {
        return new AlertDialog.Builder(ctxt);
    }

    private abstract class MyActionListener implements WifiP2pManager.ActionListener {
        @Override
        public final void onSuccess() {
            synchronized (lock) {
                lock.notify();
            }
            onDone();
        }

        protected abstract void onDone();

        @Override
        public final void onFailure(final int reason) {
            Log.e(TAG, "Discover peers failure: " + getFailureReasonString(reason));
            synchronized (lock) {
                lock.notify();
            }
            ctxt.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(ctxt, "Failed: " + getFailureReasonString(reason), Toast.LENGTH_LONG).show();
                    onError(getFailureReasonString(reason));
                }
            });
        }
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

    public static String statusToString(int status) {
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

    private WifiP2pServiceRequest discoverServicesRequest = null;

    /**
     *
     */
    public final void discoverServicesDNS() {
        deviceToDeviceInfoMap.clear();
        addressToDeviceMap.clear();
        discoverServices(WifiP2pDnsSdServiceRequest.newInstance());
    }

    /**
     *
     */
    public final void discoverServicesUpnp() {
        deviceToDeviceInfoMap.clear();
        addressToDeviceMap.clear();
        discoverServices(WifiP2pUpnpServiceRequest.newInstance());
    }

    private void discoverServices(final WifiP2pServiceRequest serviceRequest) {
        Log.d(TAG, "discover services state="+state);

        switch (state) {
            case PEERS:
                stopPeerDiscovery();
            case SERVICES:
                stopDiscoverServices();
            case INITIALIZED:
                p2p.setDnsSdResponseListeners(channel, this, this);
                p2p.setUpnpServiceResponseListener(channel, this);

                executeServiceRequest(serviceRequest);
                synchronized (this) {
                    try {
                        wait(2000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (deviceToDeviceInfoMap.size() > 0) {
                    onServiceUpdated(deviceToDeviceInfoMap);
                }
                break;
            default:
                Log.e(TAG, "P2P Not initialized!");
        }
    }

    private void executeServiceRequest(final WifiP2pServiceRequest serviceRequest) {
//WifiP2pDnsSdServiceRequest.newInstance();
        p2p.addServiceRequest(channel,
                serviceRequest,
                new MyActionListener() {
                    @Override
                    public void onDone() {
                        discoverServicesRequest = serviceRequest;
                        p2p.discoverServices(channel, new MyActionListener() {
                            @Override
                            public void onDone() {
                                Log.i(TAG, "discoverServcies SUCCESS");
                                state = State.SERVICES;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (state == State.SERVICES && discoverServicesRequest != null) {
                                            p2p.removeServiceRequest(channel, discoverServicesRequest, new MyActionListener() {
                                                @Override
                                                protected void onDone() {
                                                    executeServiceRequest(discoverServicesRequest);
                                                }
                                            });
                                        }
                                    }
                                }, 5000);
                            }
                        });

                    }
                });
    }

    /**
     *
     */
    public final void stopDiscoverServices() {
        Log.d(TAG, "stop discover services state="+state);
        p2p.clearLocalServices(channel, new MyActionListener() {
            @Override
            protected void onDone() {
                if (discoverServicesRequest != null) {
                    p2p.removeServiceRequest(channel, discoverServicesRequest, new MyActionListener() {
                        @Override
                        public void onDone() {
                            Log.i(TAG, "stopDiscoverServices SUCCESS");
                        }
                    });
                    synchronized (this) {
                        try {
                            wait(2000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    discoverServicesRequest = null;
                    state = State.INITIALIZED;
                }
            }
        });

        handler.removeCallbacks(null);
    }

    @Override
    public final void onPeersAvailable(WifiP2pDeviceList peers) {
        if (state == State.PEERS) {
            p2p.requestGroupInfo(channel, this);
            onPeersAvailable(peers.getDeviceList());
        }
    }

    /**
     *
     * @param peers
     */
    protected void onPeersAvailable(Collection<WifiP2pDevice> peers) {
        Log.i(TAG, "Peers Available: " + peers.size());
    }

    /**
     *
     */
    public final synchronized void discoverPeers() {
        Log.d(TAG, "discover peers state="+state);
        switch (state) {
            case SERVICES:
                stopDiscoverServices();
            case PEERS:
            case INITIALIZED:
                p2p.discoverPeers(channel, new MyActionListener() {
                    @Override
                    public void onDone() {
                        state = State.PEERS;
                        requestPeers();
                    }
                });
                Utils.waitNoThrow(lock, 2000);
                break;
            default:
                Log.e(TAG, "P2P Not initialized!!!");
        }
    }

    private final Map<String, WifiP2pDevice> addressToDeviceMap = new HashMap<>();
    private final Map<WifiP2pDevice, Map> deviceToDeviceInfoMap = new HashMap<>();

    private WifiP2pDevice resolveDevice(WifiP2pDevice device) {
        if (device != null && !Utils.isEmpty(device.deviceAddress)) {
            WifiP2pDevice d = addressToDeviceMap.get(device.deviceAddress);
            if (d == null) {
                addressToDeviceMap.put(device.deviceAddress, device);
            } else {
                device = d;
            }
            return device;
        }
        return null;
    }

    public final synchronized void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {

        if ((device = resolveDevice(device))==null)
            return;

        Map m = deviceToDeviceInfoMap.get(device);
        if (m == null) {
            m = new HashMap();
            deviceToDeviceInfoMap.put(device, m);
        }
        m.put("__fullDomain__", fullDomain);
        m.putAll(record);

        if (state == State.SERVICES) {
            onServiceUpdated(deviceToDeviceInfoMap);
        }
    }

    @Override
    public final synchronized void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice device) {
        Log.i(TAG, "ServiceAvailable: " + instanceName + ", " + registrationType + ", " + device.deviceName);

        if ((device = resolveDevice(device))==null)
            return;

        Map m = deviceToDeviceInfoMap.get(device);
        if (m == null) {
            m = new HashMap();
            deviceToDeviceInfoMap.put(device, m);
        }
        m.put("__instanceName__", instanceName);
        m.put("__registrationType__", registrationType);

        if (state == State.SERVICES) {
            onServiceUpdated(deviceToDeviceInfoMap);
        }
    }

    /**
     *
     * @param services
     */
    protected void onServiceUpdated(Map<WifiP2pDevice, Map> services) {

    }

    @Override
    public final synchronized void onUpnpServiceAvailable(List<String> uniqueServiceNames, WifiP2pDevice device) {

        if ((device = resolveDevice(device))==null)
            return;

        Map m = deviceToDeviceInfoMap.get(device);
        if (m == null) {
            m = new HashMap();
            deviceToDeviceInfoMap.put(device, m);
        }
        m.put("uniqueServiceNames", uniqueServiceNames);

        if (state == State.SERVICES) {
            onServiceUpdated(deviceToDeviceInfoMap);
        }
    }

    /**
     *
     */
    public final void destroy() {
        Log.d(TAG, "destroy state="+state);
        endRegistration();
        stopPeerDiscovery();
        stopDiscoverServices();
        switch (state) {
            case READY:
                break;
            case PEERS:
            case SERVICES:
            case INITIALIZED:
                ctxt.unregisterReceiver(rcvr);
        }
        state = State.READY;
    }

    /**
     *
     */
    public final void pause() {
        Log.d(TAG, "pause state="+state);
        switch (state) {
            case READY:
            case INITIALIZED:
                break;
            case PEERS:
                stopPeerDiscovery();
                break;
            case SERVICES:
                stopDiscoverServices();
                break;
        }
    }

    public final void stopPeerDiscovery() {
        Log.d(TAG, "stop peer discovery state="+state);

        if (state == State.PEERS) {
            p2p.stopPeerDiscovery(channel, new MyActionListener() {
                @Override
                public void onDone() {
                    Log.i(TAG, "stopPeerDiscovery SUCCESS");
                }
            });
            state = State.INITIALIZED;
        }
    }

    /**
     *
     */
    public final void resume() {
        Log.d(TAG, "resume state="+state);

        switch (state) {
            case READY:
                p2pInitialize();
                break;
        }
    }

    private void requestPeers() {
        Log.d(TAG, "request peers state="+state);
        p2p.requestPeers(channel, this);
    }

    private WifiP2pDnsSdServiceInfo registeredServiceInfo = null;

    /**
     *
     * @param listenPort
     * @param serviceName
     */
    public final void startRegistration(int listenPort, String serviceName) {
        Log.d(TAG, "startRegistration " + listenPort + ". " + serviceName);
        endRegistration();
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("listenport", String.valueOf(listenPort));
        record.put("serviceName", serviceName);
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        final WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        p2p.addLocalService(channel, serviceInfo, new MyActionListener() {
            @Override public void onDone() {
                Log.i(TAG, "register service success");
                registeredServiceInfo = serviceInfo;
            }
        });
    }

    /**
     *
     */
    public final void endRegistration() {
        Log.d(TAG, "endRegistration state=" + state + ", registeredServiceInfo=" + registeredServiceInfo);
        if (registeredServiceInfo != null) {
            p2p.removeLocalService(channel, registeredServiceInfo, new MyActionListener() {
                @Override
                protected void onDone() {
                    Log.i(TAG, "registered service done");
                }
            });
            registeredServiceInfo = null;
        }
    }


    public void startGroup() {
        p2p.createGroup(channel, new MyActionListener() {
            @Override
            protected void onDone() {
                Log.i(TAG, "Group created SUCCESS");
            }
        });
    }

    public void removeGroup() {
        p2p.removeGroup(channel, new MyActionListener() {
            @Override
            protected void onDone() {
                Log.i(TAG, "Group removed SUCCESS");
            }
        });
        Utils.waitNoThrow(lock, 2000);
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if (group != null) {
            Log.i(TAG, "Group Info"
                    + "\n   interface: " + group.getInterface()
                    + "\n   network name: " + group.getNetworkName()
                    + "\n   passphrase: " + group.getPassphrase()
                    + "\n   owner: " + group.getOwner().deviceName
                    + "\n   clients: " + group.getClientList().size());
        }
    }

    public final void connect(final WifiP2pDevice another) {
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = another.deviceAddress;
        config.groupOwnerIntent = 0; // TODO: ????? - for now we want to force the owner, in this case the owner is the device we are connecting too

        p2p.connect(channel, config, new MyActionListener() {
            @Override
            protected void onDone() {
                Log.i(TAG, "Connect SUCCESS");
                // dont do anthing, wait for onConnectionInfo
                connection = another;
            }
        });
    }

    public final void cancelConnect() {
        p2p.cancelConnect(channel, new MyActionListener() {
            @Override
            protected void onDone() {
                Log.i(TAG, "Connect cancelled");
            }
        });
    }

    public void disconnect() {
        removeGroup();
        connection = null;
    }

    @Override
    public final void onConnectionInfoAvailable(WifiP2pInfo info) {
        // override this to establish a connection to a server
        // TODO: should this be abstract? there are no situations when cannot override?
        Log.i(TAG, "Connection info available: " + info);
        if (state == State.PEERS && info.groupOwnerAddress != null) {
            onConnectionAvailable(info);
        }
    }

    /**
     * Override this handle incoming connectionss
     * @param info
     */
    protected void onConnectionAvailable(WifiP2pInfo info) {

    }
}
