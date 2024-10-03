package cc.lib.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.PowerManager;
import android.util.Log;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Type;
import org.xbill.mDNS.Lookup;
import org.xbill.mDNS.ServiceInstance;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.lib.game.Utils;

/**
 * Spawns a thread to poll the network for sleeptrackers
 *
 * Add/remove listeners as needed
 *
 * TODO: Does it make sense for this module to be in library?
 *
 * @author chriscaron
 *
 */
public class BonjourThread {

    private final static String TAG = BonjourThread.class.getSimpleName();

    public final static class BonjourRecord implements Comparable<BonjourRecord> {
        public final String name;
        public final InetAddress hostAddress;
        public final int port;
        public final Map headers;

        private BonjourRecord(ServiceInstance instance) {
            /*
            hostName = Utils.trimQuotes((String)instance.getTextAttributes().get("hostname"));
            serialNo = Utils.trimQuotes((String)instance.getTextAttributes().get("serial"));
            macAddress = Utils.trimQuotes((String)instance.getTextAttributes().get("mac_address"));
            osVersion = Utils.trimQuotes((String)instance.getTextAttributes().get("osVer"));
            appVersion = Utils.trimQuotes((String)instance.getTextAttributes().get("appVer"));
            unitName = Utils.reUnicodify(Utils.trimQuotes((String)instance.getTextAttributes().get("unit")));
            */
            InetAddress address = null;
            for (InetAddress i : instance.getAddresses()) {
                if (i instanceof Inet4Address) {
                    address = i;
//                  break;
                }
            }
            hostAddress = address;
            port = instance.getPort();
            headers = instance.getTextAttributes();
            name = instance.getName().toString();

            /*
            try {
                String envStr = (String)instance.getTextAttributes().get("environment");
                if (envStr != null && envStr.length() > 0)
                    env = FSPEnvironment.valueOf(Utils.trimQuotes(envStr));
            } catch (Exception e) {
                log.warn("Error parsing environment from TXT heeader: %s : %s", e.getClass().getSimpleName(), e.getMessage());
            }
            this.environment = env;
            String product = Utils.trimQuotes((String)instance.getTextAttributes().get("product"));
            if (product == null)
                type = Station.Type.ERGOZ;
            else if ("sleepz".equalsIgnoreCase(product))
                type = Station.Type.SLEEPZ;
            else
                type = Station.Type.UNKNOWN;
                */
        }

        public String getHostAddress() {
            return hostAddress == null ? "" : hostAddress.getHostName();
        }

        public String getIPAddress() {
            return hostAddress == null ? "" : hostAddress.getHostAddress();
        }
        
        @Override
        public String toString() {
            //return hostName + " " + hostAddress + ":" + port + " serialNo=" + serialNo + " osVer=" + osVersion + " appVer=" + appVersion + " env=" + environment + " type=" + type;
            return hostAddress + ":" + port;
        }

        @Override
        public int hashCode() {
            return hostAddress.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof BonjourRecord) {
                return ((BonjourRecord)o).hostAddress.equals(hostAddress);
            }
            return super.equals(o);
        }

        @Override
        public int compareTo(BonjourRecord r) {
            String hn1 = hostAddress == null ? "" : hostAddress.getCanonicalHostName();
            String hn2 = r.hostAddress == null ? "" : r.hostAddress.getCanonicalHostName();
            return hn1.compareTo(hn2);
        }
    }

    public interface BonjourListener {
        /**
         * Return a map of hostnames mapped to records.
         * @param records
         */
        void onRecords(Map<String, BonjourRecord> records);

        void onWifiStateChanged(String ssid);

        // TODO
        //void onRecordsChanged(List<BonjourRecord> records);
    }

    final String [] names;

    public BonjourThread(String ... names) {
        this.names = names;
    }

    public void addListener(BonjourListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
            checkStart();
        }
    }

    public void removeListener(BonjourListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
            if (listeners.size() == 0)
                stop();
        }
    }

    private final Set<BonjourListener> listeners = new HashSet<BonjourListener>();
    private Context context;
    private WifiManager wifi;
    private int runState = 0; // 0 == STOPPED, 1 == RUNNING, 2 == STOPPING

    public void attach(Context context) {
        if (this.context != null) {
            throw new AssertionError();
        }
        this.context = context;
        checkStart();
    }

    private void checkStart() {
        if (runState == 0 && context != null && listeners.size() > 0) {
            wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            lock = wifi.createMulticastLock(getClass().getSimpleName());
            lock.setReferenceCounted(true);
            lock.acquire();
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            this.context.registerReceiver(receeiver, filter);
            runState = 1;
            new Thread(worker).start();
        }
    }

    private void stop() {
        if (runState == 1 && context != null) {
            Log.i(TAG, "Stop called");
            runState = 2;
            context.unregisterReceiver(receeiver);
            if (lock != null) {
                lock.release();
                lock = null;
            }
            synchronized (worker) {
                worker.notify();
            }
            // block until thread stopped
            synchronized (this) {
                try {
                    while (runState == 2)
                        wait(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "Stop done");
        }
    }

    public void detatch() {
        stop();
        context = null;
    }

    private static class WifiState {
        String ssid; // quotes stripped
        int linkspeed;


        public WifiState(String ssid, int linkspeed) {
            this.ssid = ssid;
            this.linkspeed = linkspeed;
        }

        public void copy(WifiState other) {
            ssid = other.ssid;
            linkspeed = other.linkspeed;
        }

        public boolean equals(Object o) {
            WifiState w = (WifiState)o;
            return w.ssid.equals(ssid) && Utils.signOf(w.linkspeed) == Utils.signOf(linkspeed);
        }

        public void set(String ssid, int linkspeed) {
            this.ssid = ssid;
            this.linkspeed = linkspeed;
        }

        public boolean isConnected() {
            return ssid.length() > 0 && linkspeed > 0;// && !Utils.isSleeptrackerSSID(ssid);
        }
    }

    private final WifiState currentState = new WifiState("", -1);

    public static String getWifiStateString(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_DISABLED:
                return "DISABLED";
            case WifiManager.WIFI_STATE_DISABLING:
                return "DISABLING";
            case WifiManager.WIFI_STATE_ENABLED:
                return "ENABLED";
            case WifiManager.WIFI_STATE_ENABLING:
                return "ENABLING";
        }
        return "UNKNOWN";
    }

    /**
     * We need to track wifi states so we can restart bonjour stack on certain changes.
     */
    private final BroadcastReceiver receeiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            WifiState state = new WifiState("", -1);
            Log.d("WifiReciever", "Wifi state changed:");
            if (wifi == null) {
                Log.e("WifiReciever",  "WIFI Handle is null");
            } else {
                Log.d("WifiReciever", String.format("%20s : %s", "WIFI STATE", getWifiStateString(wifi.getWifiState())));

                if (wifi.getConnectionInfo() != null) {
                    Log.v("WifiReciever", String.format("%20s : %s", "WIFI SSID", wifi.getConnectionInfo().getSSID()));
                    Log.v("WifiReciever", String.format("%20s : %s", "LINK SPEED", "" + wifi.getConnectionInfo().getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS));
                    //Log.i("WifiReciever", String.format("%20s : %s", "FREQUENCY", "" + wifi.getConnectionInfo().getFrequency() + WifiInfo.FREQUENCY_UNITS)); // API 21
                    Log.v("WifiReciever", String.format("%20s : %s", "IP ADDR", Utils.getIpAddressString(wifi.getConnectionInfo().getIpAddress())));

                    state.set(Utils.trimEnclosure(wifi.getConnectionInfo().getSSID()), wifi.getConnectionInfo().getLinkSpeed());
                }
            }

            if (!currentState.equals(state)) {
                currentState.copy(state);
                wifiChanged = true;
                for (BonjourListener l : listeners) {
                    l.onWifiStateChanged(state.ssid);
                }
            }
        }
    };

    private boolean wifiChanged = true; // flag to signal thread to restart the lookup

    private final Map<String, BonjourRecord> allRecords = new HashMap<>();

    private void onRecordList(Map<String, BonjourRecord> records) {
        try {
            Log.v(TAG, String.format("Bonjour Records:%s", records.toString().replace(',', '\n')));
            synchronized (allRecords) {
                allRecords.clear();
                allRecords.putAll(records);
            }
            for (BonjourListener l : listeners) {
                l.onRecords(allRecords);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Runnable worker = new Runnable() {
        @Override
        public final void run() {
            Log.d(TAG, "Worker thread starting");
            while (runState == 1 && context != null){
                try {

                    while (runState == 1 && wifiChanged) {
                        wifiChanged = false;
                        Log.d(TAG, "Wifi changed, clearing records");
                        clearRecords();
                        release();
                        synchronized (this) {
                            wait(3000);
                        }
                    }

                    if (runState != 1)
                        break;

                    if (resolve == null) {
                        if (currentState.isConnected() && ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn()) {
                            Log.d(TAG, "Initializing resolver");
                            resolve = new Lookup(names, Type.ANY, DClass.IN);
                        }
                    }

                    if (resolve != null) {
                        ServiceInstance[] records = resolve.lookupServices();
                        final Map<String, BonjourRecord> r  = new HashMap<>();
                        for (ServiceInstance record : records) {
                            //Log.d(TAG, record.toString());
                            //Map txtMap = record.getTextAttributes();
                            if (record.getAddresses() != null && record.getAddresses().length > 0) {
                                BonjourRecord rec = new BonjourRecord(record);
                                if (!Utils.isEmpty(rec.getHostAddress()))
                                    r.put(rec.hostAddress.getHostAddress(), rec);
                            }
                        }
                        onRecordList(r);
                        if (!resolve.getQuerier().isOperational()) {
                            resolve.close();
                            resolve = null;
                        }
                    }

                    synchronized (this) {
                        wait(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            Log.d(TAG, "Worker thread stopped");
            runState = 0;
            release();
            synchronized (BonjourThread.this) {
                BonjourThread.this.notify();
            }
        }
    };

    private void clearRecords() {
        onRecordList(new HashMap<String, BonjourRecord>());
    }

    private MulticastLock lock = null;
    private Lookup resolve = null;

    private void release() {
        if (resolve != null) {
            try {
                resolve.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing the bonjour stack: " + e.getClass().getSimpleName() + " : " + e.getMessage());
                e.printStackTrace();
            }
            resolve = null;
        }
    }

    public boolean isAttached() {
        return context != null;
    }

}
