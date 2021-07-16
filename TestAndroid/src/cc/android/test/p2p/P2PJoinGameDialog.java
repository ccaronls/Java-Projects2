package cc.android.test.p2p;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import cc.android.test.R;
import cc.lib.android.CCActivityBase;
import cc.lib.android.SpinnerTask;
import cc.lib.mp.android.WifiP2pHelper;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;

public class P2PJoinGameDialog extends BaseAdapter
        implements AdapterView.OnItemClickListener,
        DialogInterface.OnClickListener,
        Runnable,
        GameClient.Listener {

    final CCActivityBase context;
    final ListView lvHost;
    final List<WifiP2pDevice> p2pDevices = new ArrayList<>();
    final Dialog dialog;
    final P2PHelper helper;
    final int connectPort;
    final GameClient client;

    public P2PJoinGameDialog(CCActivityBase activity, GameClient client, String clientName, int port) {
        this.context = activity;
        this.connectPort = port;
        this.client = client;
        lvHost = new ListView(context);
        lvHost.setAdapter(this);
        lvHost.setOnItemClickListener(this);
        helper = new P2PHelper(context) {
            @Override
            protected void onP2PEnabled(boolean enabled) {
                Log.d(TAG, "P2P Enabled: " + enabled);
            }

            @Override
            protected void onThisDeviceUpdated(WifiP2pDevice device) {
                Log.d(TAG, "Device Updated: " + device);
            }

            @Override
            protected void onPeersList(List<WifiP2pDevice> peers) {
                synchronized (p2pDevices) {
                    p2pDevices.clear();
                    p2pDevices.addAll(peers);
                }
                lvHost.post(P2PJoinGameDialog.this); // notify dataset changed
            }

            @Override
            protected void onGroupFormed(InetAddress addr, String ipAddress) {
                Log.d(TAG, "onGroupFormedAsClient: " + ipAddress);
                new Thread() {
                    public void run() {
                        try {
                            client.connect(addr, connectPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, e.getClass().getSimpleName() + " " + e.getMessage());
                            showError("Failed to client connect to " + ipAddress + ":" + connectPort);
                        }
                        synchronized (helper) {
                            helper.notify();
                        }
                    }
                }.start();
            }
        };
        client.addListener(this);
        helper.setDeviceName(clientName);
        helper.start(client);
        dialog = context.newDialogBuilder().setTitle("Join Game").setView(lvHost)
                .setNegativeButton("Cancel", this).show();
    }

    @Override
    public void run() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return p2pDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        if (v == null) {
            v = View.inflate(context, R.layout.list_item_peer, null);
        }

        WifiP2pDevice device;
        synchronized (p2pDevices) {
            device = p2pDevices.get(position);
        }

        v.setTag(device);
        TextView tvPeer = (TextView)v.findViewById(R.id.tvPeer);
        tvPeer.setText(device.deviceName + " " + WifiP2pHelper.statusToString(device.status, context));
        tvPeer.setBackgroundColor(position % 2 == 0 ? Color.BLACK : Color.DKGRAY);

        return v;
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        // cancel out of the dialog
        helper.stop();
        client.disconnect("Client Left");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final WifiP2pDevice d = (WifiP2pDevice)view.getTag();
        new SpinnerTask(context) {

            @Override
            protected String getProgressMessage() {
                return "Please wait for all players to connect";
            }

            @Override
            protected void doIt(String ... args) throws Exception {
                helper.connect(d);
                synchronized (helper) {
                    try {
                        helper.wait(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void onSuccess() {
            }
        }.execute();
    }

    @Override
    public void onCommand(GameCommand cmd) {

    }

    @Override
    public void onMessage(String msg) {

    }

    @Override
    public void onDisconnected(String reason) {

    }

    @Override
    public void onConnected() {
        client.removeListener(this); // TODO: We could stay alive and throw up dialog again if disconnected
        helper.stop();
        dialog.dismiss();
    }

    public void showError(String msg) {
        Log.e(P2PHelper.TAG, msg);
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

}
