package cc.android.test.p2p;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.List;

import cc.lib.android.CCActivityBase;
import cc.lib.android.SpinnerTask;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;
import cc.lib.net.GameServer;

public class P2PServerWaitingDialog extends BaseAdapter implements
        DialogInterface.OnClickListener,
        GameServer.Listener,
        Runnable,
        DialogInterface.OnDismissListener {

    final CCActivityBase context;
    final GameServer server;
    final P2PHelper helper;
    final Dialog dialog;
    final int listenPort;
    final ListView lvPlayers;

    public P2PServerWaitingDialog(CCActivityBase activity, GameServer server, String sevrerName, int listenPort) {
        this.context = activity;
        this.server = server;
        server.addListener(this);
        this.listenPort = listenPort;
        helper = new P2PHelper(activity) {
            @Override
            protected void onP2PEnabled(boolean enabled) {
                if (!enabled) {
                    dialog.dismiss();
                    onError("Failed to init P2P");
                }
            }

            @Override
            protected void onThisDeviceUpdated(WifiP2pDevice device) {

            }

            @Override
            protected void onPeersList(List<WifiP2pDevice> peers) {

            }

            @Override
            protected void onGroupFormed(InetAddress addr, String ipAddress) {
                try {
                    Log.d(TAG, "onGroupFormedAsServer: " + ipAddress);
                    if (!server.isRunning())
                        server.listen();
                } catch (Exception e) {
                    onError(e.getClass().getSimpleName() + " " + e.getMessage());
                    dialog.dismiss();
                }
            }

        };
        lvPlayers = new ListView(context);
        lvPlayers.setAdapter(this);
        dialog = activity.newDialogBuilder().setTitle(cc.lib.mp.android.R.string.popup_title_waiting_for_players)
                .setView(lvPlayers)
                .setPositiveButton(cc.lib.android.R.string.popup_button_close, null)
                .setNegativeButton(cc.lib.android.R.string.popup_button_cancel, this).show();
        helper.setDeviceName(sevrerName);
        helper.start(server);
        dialog.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        server.removeListener(this);
    }

    @Override
    public void onConnected(ClientConnection conn) {
        lvPlayers.post(this);
    }

    @Override
    public void onReconnection(ClientConnection conn) {
        lvPlayers.post(this);
    }

    @Override
    public void onClientDisconnected(ClientConnection conn) {
        lvPlayers.post(this);
    }

    @Override
    public void onCommand(ClientConnection conn, GameCommand cmd) {

    }

    @Override
    public void run() {
        notifyDataSetChanged();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        new SpinnerTask(context) {
            @Override
            protected void doIt(String... args) throws Exception {
                helper.stop();
                server.stop();

            }
        }.execute();
    }

    @Override
    public int getCount() {
        return server.getNumClients();
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
            v = new TextView(context);
        }

        ClientConnection conn = server.getConnection(position);
        TextView tv = (TextView)v;
        tv.setText(conn.getName());
        tv.setTextColor(conn.isConnected() ? Color.GREEN : Color.RED);
        tv.setBackgroundColor(position % 2 == 0 ? Color.BLACK : Color.DKGRAY);

        return v;
    }

    public void onError(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
}
