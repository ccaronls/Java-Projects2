package cc.android.test.p2p;

import android.app.Activity;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cc.android.test.R;
import cc.lib.android.BonjourThread;
import cc.lib.android.WifiP2pHelper;

/**
 * Created by chriscaron on 3/5/18.
 */

public class WifiTest extends Activity implements RadioGroup.OnCheckedChangeListener, View.OnClickListener, BonjourThread.BonjourListener {

    List<Object> devices = new ArrayList<>();
    List<String> deviceInfos = new ArrayList<>();
    WifiP2pHelper wifi = null;
    TextView tvHeader;
    RadioGroup rgDiscovery;
    BonjourThread thread = new BonjourThread("_http._tcp");


    BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            if (v == null) {
                v = View.inflate(WifiTest.this, R.layout.wifi_list_item, null);
            }

            if (position % 2 == 0)
                v.setBackgroundColor(Color.BLACK);
            else
                v.setBackgroundColor(Color.DKGRAY);

            TextView tv = (TextView)v.findViewById(R.id.textView);
            Object device;
            String info;
            synchronized (devices) {
                device = devices.get(position);
                info = deviceInfos.get(position);
            }

            tv.setText(info);
            v.setTag(device);
            v.setOnClickListener(WifiTest.this);

            return v;
        }
    };

    @Override
    public void onRecords(Map<String, BonjourThread.BonjourRecord> records) {
        synchronized (devices) {
            devices.clear();
            deviceInfos.clear();
            for (BonjourThread.BonjourRecord r : records.values()) {
                String s = r.name
                        + "\n" + r.getHostAddress()
                        + "\n" + r.getIPAddress()
                        + "\n" + r.headers.toString().replace(',', '\n');
                        ;
                devices.add(r);
                deviceInfos.add(s);
            }
        }

        runOnUiThread(new Runnable() {
            public void run() {
                tvHeader.setText("Services Found: " + devices.size());
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onWifiStateChanged(String ssid) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_test_activity);
        tvHeader = (TextView)findViewById(R.id.tvHeader);

        ListView lv = (ListView)findViewById(R.id.lvDevices);
        lv.setAdapter(adapter);
        wifi = new WifiP2pHelper(this) {
            @Override
            public void onPeersAvailable(Collection<WifiP2pDevice> peers) {
                synchronized (devices) {
                    devices.clear();
                    deviceInfos.clear();
                    for (WifiP2pDevice d : peers) {
                        devices.add(d);
                        deviceInfos.add(deviceToString(d));
                    }
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        tvHeader.setText("Peers Found: " + devices.size());
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onServiceUpdated(Map<WifiP2pDevice, Map> dnsDevices) {

                synchronized (devices) {
                    devices.clear();
                    deviceInfos.clear();
                    for (Map.Entry<WifiP2pDevice, Map> e : dnsDevices.entrySet()) {
                        devices.add(e.getKey());
                        deviceInfos.add(deviceToString(e.getKey()) + "\nTXT HEADERS:" + e.getValue());
                    }
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        tvHeader.setText("Services Found: " + deviceInfos.size());
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onConnectionAvailable(final WifiP2pInfo info) {
                super.onConnectionInfoAvailable(info);
                runOnUiThread(new Runnable() {
                    public void run() {
                        newDialog().setTitle("Connection Info")
                                .setMessage(info.toString())
                                .setNegativeButton("Ok", null).show();
                    }
                });
            }
        };
        wifi.p2pInitialize();

        rgDiscovery = (RadioGroup)findViewById(R.id.rgDiscovery);
        rgDiscovery.setOnCheckedChangeListener(this);
        wifi.startRegistration(1234, "cmc service");
    }

    String deviceToString(WifiP2pDevice device) {
        return device.deviceName
                + "\nAddress: " + device.deviceAddress
                + "\nStatus: " + WifiP2pHelper.statusToString(device.status)
                + "\nPrimary: " + device.primaryDeviceType
                + "\nSecondary: " + device.secondaryDeviceType
                + "\ngroup owner: " + device.isGroupOwner()
                + "\nserv disc available: " + device.isServiceDiscoveryCapable()
                + "\nwps Display Supported: " + device.wpsDisplaySupported()
                + "\nwps Keypad Supported: " + device.wpsKeypadSupported()
                + "\nwps PBC Supported" + device.wpsPbcSupported();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wifi.resume();
        // trigger service or peers discovery based on whatever was checked last
        onCheckedChanged(rgDiscovery, rgDiscovery.getCheckedRadioButtonId());
    }

    @Override
    protected void onPause() {
        super.onPause();
        wifi.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wifi.destroy();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        thread.detatch();
        thread.removeListener(this);
        tvHeader.setText("Discovering...");
        synchronized (deviceInfos) {
            deviceInfos.clear();
            devices.clear();
        }
        adapter.notifyDataSetChanged();
        switch (checkedId) {
            case R.id.rbPeers:
                wifi.discoverPeers(); break;
            case R.id.rbUpnp:
                wifi.discoverServicesUpnp(); break;
            case R.id.rbDns1:
                wifi.discoverServicesDNS(); break;
            case R.id.rbDns2:
                discoverServices2(); break;
        }
    }

    @Override
    public void onClick(View v) {
        Object d = v.getTag();
        if (d == null)
            return;
        if (d instanceof WifiP2pDevice) {
            WifiP2pDevice device = (WifiP2pDevice)v.getTag();
            switch (device.status) {
                case WifiP2pDevice.AVAILABLE:
                    wifi.connect((WifiP2pDevice) v.getTag());
                    break;
                case WifiP2pDevice.CONNECTED:
                    wifi.disconnect();
                    break;
                case WifiP2pDevice.FAILED:
                case WifiP2pDevice.INVITED:
                case WifiP2pDevice.UNAVAILABLE:
                    Toast.makeText(this, "Device is " + WifiP2pHelper.statusToString(device.status), Toast.LENGTH_SHORT).show();
                    break;
            }
        } else if (d instanceof BonjourThread.BonjourRecord) {
            BonjourThread.BonjourRecord record = (BonjourThread.BonjourRecord)d;
        }
    }

    public void discoverServices2() {
        wifi.stopDiscoverServices();
        wifi.stopPeerDiscovery();
        thread.attach(this);
        thread.addListener(this);
    }
}
