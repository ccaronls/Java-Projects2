package cc.lib.mp.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
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
import android.os.SystemClock;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * Created by chriscaron on 2/16/18.
 */
@SuppressLint("MissingPermission")
public class WifiP2pHelper implements
        WifiP2pManager.ChannelListener,
        WifiP2pManager.PeerListListener,
        WifiP2pManager.DnsSdTxtRecordListener,
        WifiP2pManager.DnsSdServiceResponseListener,
        WifiP2pManager.UpnpServiceResponseListener,
        WifiP2pManager.GroupInfoListener,
        WifiP2pManager.ConnectionInfoListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Handler handler = new Handler(Looper.getMainLooper());
    private WifiP2pDevice connection = null;

    private BroadcastReceiver rcvr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            log.debug("BR:RECV = " + intent.getAction());
            switch (intent.getAction()) {
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    //NetworkInfo ni = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    //WifiP2pInfo wi = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                    //WifiP2pGroup gi = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                    if (networkInfo.isConnected()) {
                        log.info("BR:RCV = WIFI_P2P_CONNECTION_CHANGED_ACTION - network Connected - requestConnectionInfo");

                        // We are connected with the other device, request connection
                        // info to find group owner IP
                        p2p.requestConnectionInfo(channel, WifiP2pHelper.this);
                    } else {
                        log.info("BR:RCV = WIFI_P2P_CONNECTION_CHANGED_ACTION - network not Connected - ignore");
                    }
                    break;
                }
                case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
                    switch (state) {
                        case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                            log.info("BR:RCV = WIFI_P2P_DISCOVERY_CHANGED_ACTION - STARTED - not handled");
                            break;

                        case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                            log.info("BR:RCV = WIFI_P2P_DISCOVERY_CHANGED_ACTION - STOPPED - not handled");
                            break;

                        default:
                            log.info("BR:RCV = WIFI_P2P_DISCOVERY_CHANGED_ACTION - UNKNOWN(%d) - not handled", state);

                    }
                    break;
                }
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
                    log.info("BR:RCV = WIFI_P2P_PEERS_CHANGED_ACTION - requestPeers");
                    //WifiP2pDeviceList list = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                    //onPeersAvailable(list);
                    requestPeers();
                    break;
                }
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: {
                    log.info("BR:RCV = WIFI_P2P_THIS_DEVICE_CHANGES_ACTION - not handled");
                    break;
                }
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    switch (state) {
                        case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                            log.info("BR:RCV = WIFI_P2P_STATE_CHANGED_ACTION - WIFI P2P STATE DISABLED - destroy");
                            destroy();
                            break;
                        case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                            log.info("BR:RCV = WIFI_P2P_STATE_CHANGED_ACTION - WIFI P2P STATE ENABLED - ignore");
                            break;
                        default:
                            log.info("BR:RCV = WIFI_P2P_STATE_CHANGED_ACTION - UNKNOWN(%d) - ignore", state);
                    }
                    break;
                }

                default:
                    log.info("BR:RCV = UNKNOWN ACTION(%s) - ignore", intent.getAction());
            }
        }
    };

    final Context ctxt;
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

    public WifiP2pHelper(Context ctxt) {
        this.ctxt = ctxt;
        p2p = (WifiP2pManager) ctxt.getSystemService(Context.WIFI_P2P_SERVICE);
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
            log.info("p2pInitialized success");
        }
    }

    @Override
    public final void onChannelDisconnected() {
        if (state != State.READY) {
            log.debug("channel disconnected");
            destroy();
            tryRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    newDialog().setTitle(R.string.wifi_popup_title_channel_disconnected)
                            .setMessage(R.string.wifi_popup_msg_channel_auto_reconnect)
                            .setNegativeButton(R.string.popup_button_no, null)
                            .setPositiveButton(R.string.popup_button_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    p2pInitialize();
                                }
                            }).show();
                }
            });
        }
    }

    void tryRunOnUiThread(Runnable r) {
        if (ctxt instanceof Activity) {
            ((Activity) ctxt).runOnUiThread(r);
        }
    }

    protected void onError(String message) {
        Toast.makeText(ctxt, "Failed: " + message, Toast.LENGTH_LONG).show();
    }

    protected AlertDialog.Builder newDialog() {
        return new AlertDialog.Builder(ctxt);
    }

    private abstract class MyActionListener implements WifiP2pManager.ActionListener {

        final String caller;

        MyActionListener(String caller) {
            this.caller = caller;
        }

        @Override
        public final void onSuccess() {
            synchronized (caller) {
                caller.notify();
            }
            onDone();
        }

        protected abstract void onDone();

        @Override
        public final void onFailure(final int reason) {
            log.error(caller + " : " + getFailureReasonString(reason));
            synchronized (caller) {
                caller.notify();
            }
            if (reason != WifiP2pManager.BUSY) {
                // Ignore BUSY, it appears to be benign.
                tryRunOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(ctxt, "Failed: " + getFailureReasonString(reason), Toast.LENGTH_LONG).show();
                        onError(caller + ":" + getFailureReasonString(reason));
                    }
                });
            }
        }
    }


    private String getFailureReasonString(int reason) {
        switch (reason) {
            case WifiP2pManager.ERROR:
                return ctxt.getString(R.string.wifi_failure_reason_error);
            case WifiP2pManager.BUSY:
                return ctxt.getString(R.string.wifi_failure_reason_busy);
            case WifiP2pManager.P2P_UNSUPPORTED:
                return ctxt.getString(R.string.wifi_failure_reason_p2p_unsupported);
        }
        return ctxt.getString(R.string.wifi_failure_reason_unknown);
    }

    public static String statusToString(int status, Context ctxt) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return ctxt.getString(R.string.wifi_conn_status_available);
            case WifiP2pDevice.CONNECTED:
                return ctxt.getString(R.string.wifi_conn_status_connected);
            case WifiP2pDevice.FAILED:
                return ctxt.getString(R.string.wifi_conn_status_failed);
            case WifiP2pDevice.INVITED:
                return ctxt.getString(R.string.wifi_conn_status_invited);
            case WifiP2pDevice.UNAVAILABLE:
                return ctxt.getString(R.string.wifi_conn_status_unavailable);
        }
        return ctxt.getString(R.string.wifi_conn_status_unknown);
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
        log.debug("discover services state=" + state);

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
                log.error("P2P Not initialized!");
        }
    }

    private void executeServiceRequest(final WifiP2pServiceRequest serviceRequest) {
//WifiP2pDnsSdServiceRequest.newInstance();
        p2p.addServiceRequest(channel,
                serviceRequest,
                new MyActionListener("addServiceRequest") {
                    @Override
                    public void onDone() {
                        discoverServicesRequest = serviceRequest;
                        p2p.discoverServices(channel, new MyActionListener("discoverServices") {
                            @Override
                            public void onDone() {
                                log.info("discoverServcies SUCCESS");
                                state = State.SERVICES;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (state == State.SERVICES && discoverServicesRequest != null) {
                                            p2p.removeServiceRequest(channel, discoverServicesRequest, new MyActionListener("removeServiceRequest") {
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
        log.debug("stop discover services state=" + state);
        p2p.clearLocalServices(channel, new MyActionListener("clearLocalServices") {
            @Override
            protected void onDone() {
                if (discoverServicesRequest != null) {
                    p2p.removeServiceRequest(channel, discoverServicesRequest, new MyActionListener("removeServiceRequest") {
                        @Override
                        public void onDone() {
                            log.info("stopDiscoverServices SUCCESS");
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
        log.debug("onPeersAvailable state = " + state);
        if (state == State.PEERS || state == State.INITIALIZED) {
            p2p.requestGroupInfo(channel, this);
            Collection<WifiP2pDevice> list = peers.getDeviceList();
            int index = 0;
            for (WifiP2pDevice d : list) {
                log.debug("onPeersAvailable peer[%d]=%s", index, d);
                index++;
            }
            onPeersAvailable(list);
        } else {
            log.warn("Ignoring");
        }
    }

    /**
     *
     * @param peers
     */
    protected void onPeersAvailable(Collection<WifiP2pDevice> peers) {
        log.info("Peers Available: " + peers.size());
    }

    /**
     *
     */
    public final synchronized void discoverPeers() {
        log.debug("discover peers state=" + state);
        switch (state) {
            case SERVICES:
                stopDiscoverServices();
            case PEERS:
            case INITIALIZED:
                p2p.discoverPeers(channel, new MyActionListener("discoverPeers") {
                    @Override
                    public void onDone() {
                        state = State.PEERS;
                        requestPeers();
                    }
                });
                break;
            default:
                log.error("P2P Not initialized!!!");
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
        log.info("ServiceAvailable: " + instanceName + ", " + registrationType + ", " + device.deviceName);

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
        log.debug("destroy state="+state);
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
        log.debug("pause state="+state);
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
        log.debug("stop peer discovery state="+state);

        if (state == State.PEERS) {
            p2p.stopPeerDiscovery(channel, new MyActionListener("stopPeerDiscovery") {
                @Override
                public void onDone() {
                    log.info("stopPeerDiscovery SUCCESS");
                }
            });
            state = State.INITIALIZED;
        }
    }

    /**
     *
     */
    public final void resume() {
        log.debug("resume state="+state);

        switch (state) {
            case READY:
                p2pInitialize();
                break;
        }
    }

    @SuppressLint("MissingPermission")
    private void requestPeers() {
        log.debug("request peers state="+state);
        p2p.requestPeers(channel, this);
    }

    private WifiP2pDnsSdServiceInfo registeredServiceInfo = null;

    /**
     *
     * @param listenPort
     * @param serviceName
     */
    public final void startRegistration(int listenPort, String serviceName) {
        log.debug("startRegistration " + listenPort + ". " + serviceName);
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
        p2p.addLocalService(channel, serviceInfo, new MyActionListener("addLocalService") {
            @Override public void onDone() {
                log.info("register service success");
                registeredServiceInfo = serviceInfo;
            }
        });
    }

    /**
     *
     */
    public final void endRegistration() {
        log.debug("endRegistration state=" + state + ", registeredServiceInfo=" + registeredServiceInfo);
        if (registeredServiceInfo != null) {
            p2p.removeLocalService(channel, registeredServiceInfo, new MyActionListener("removeLocalService") {
                @Override
                protected void onDone() {
                    log.info("registered service done");
                }
            });
            registeredServiceInfo = null;
        }
    }


    public void startGroup() {
        p2p.createGroup(channel, new MyActionListener("createGroup") {
            @Override
            protected void onDone() {
                log.info("Group created SUCCESS");
                p2p.requestGroupInfo(channel, WifiP2pHelper.this);
            }
        });
    }

    public void removeGroup() {
        p2p.removeGroup(channel, new MyActionListener("removeGroup") {
            @Override
            protected void onDone() {
                log.info("Group removed SUCCESS");
            }
        });
    }

    @Override
    public final void onGroupInfoAvailable(WifiP2pGroup group) {
        if (group != null) {
            log.info("Group Info"
                    + "\n   interface: " + group.getInterface()
                    + "\n   network name: " + group.getNetworkName()
                    + "\n   passphrase: " + group.getPassphrase()
                    + "\n   owner: " + group.getOwner().deviceName
                    + "\n   clients: " + group.getClientList().size());
            //onGroupInfo(group);
        }
    }

    /**
     *
     * @param group
     */
    protected void onGroupInfo(WifiP2pGroup group) {
        log.debug("onGroupInfo: " + group);
    }

    /**
     * Call to join a host. onConnected will be called omn success. onConnectionInfo will be called asyncronously
     * @param another
     */
    public final void connect(final WifiP2pDevice another) {
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = another.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        Random r = new Random(SystemClock.uptimeMillis());
        config.groupOwnerIntent = r.nextInt(14);

        p2p.connect(channel, config, new MyActionListener("connect") {
            @Override
            protected void onDone() {
                log.info("Connect SUCCESS");
                // dont do anthing, wait for onConnectionInfo
                connection = another;
            }
        });
    }

    public final void cancelConnect() {
        p2p.cancelConnect(channel, new MyActionListener("cancelConnect") {
            @Override
            protected void onDone() {
                log.info("Connect cancelled");
            }
        });
    }

    public void disconnect() {
        deletePersistentGroups();
        connection = null;
    }

    private void deletePersistentGroups(){
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(p2p, channel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void onConnectionInfoAvailable(WifiP2pInfo info) {
        // override this to establish a connection to a server
        // TODO: should this be abstract? there are no situations when cannot override?
        log.info("Connection info available: " + info);
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

    public void setDeviceName(String name) {
        try {
            final Method m = p2p.getClass().getMethod(
                    "setDeviceName",
                    new Class[] { WifiP2pManager.Channel.class, String.class,
                            WifiP2pManager.ActionListener.class });

            new Thread() {
                public void run() {
                    try {
                        m.invoke(p2p, channel, name, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
