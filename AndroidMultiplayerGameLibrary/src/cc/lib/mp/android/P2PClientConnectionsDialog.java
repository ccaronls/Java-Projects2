package cc.lib.mp.android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.List;

import cc.lib.android.SpinnerTask;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameServer;
import cc.lib.utils.Lock;

public class P2PClientConnectionsDialog extends BaseAdapter implements
        DialogInterface.OnClickListener,
        GameServer.Listener,
        Runnable,
        DialogInterface.OnDismissListener,
        View.OnClickListener {

    private final P2PActivity context;
    private final GameServer server;
    private final P2PHelper helper;
    private Dialog dialog;
    private ListView lvPlayers;

    public P2PClientConnectionsDialog(P2PActivity activity, GameServer server, String serverName) {
        this.context = activity;
        this.server = server;
        server.addListener(this);
        Lock groupFormedLock = new Lock(1);
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
                    groupFormedLock.release();
                } catch (Exception e) {
                    onError(e.getClass().getSimpleName() + " " + e.getMessage());
                    if (dialog != null)
                        dialog.dismiss();
                }
            }

        };
        helper.setDeviceName(serverName);
        helper.start(server);
        new SpinnerTask<Void>(activity) {

            @Override
            protected String getProgressMessage() {
                return activity.getString(R.string.popup_title_starting_server);
            }

            @Override
            protected void doIt(Void... args) throws Exception {
                groupFormedLock.block(60000);
            }

            @Override
            protected void onCancelled() {
                groupFormedLock.release();
                super.onCancelled();
            }

            @Override
            protected void onCompleted() {
                if (!server.isRunning()) {
                    activity.newDialogBuilder().setTitle(R.string.popup_title_error)
                            .setMessage("Failed to strat server")
                            .setPositiveButton(R.string.popup_button_ok, (dialog1, which) -> helper.stop())
                            .show();
                } else {
                    Toast.makeText(activity, "Server started", Toast.LENGTH_LONG).show();
                    activity.onP2PServer(new P2PActivity.P2PServer() {
                        @Override
                        public GameServer getServer() {
                            return server;
                        }

                        @Override
                        public void openConnections() {
                            show();
                        }
                    });
                }

            }
        }.execute();
    }

    public void show() {
        if (dialog == null) {
            lvPlayers = new ListView(context);
            lvPlayers.setAdapter(this);
            dialog = context.newDialogBuilder().setTitle(R.string.popup_title_connected_clients)
                    .setView(lvPlayers)
                    .setPositiveButton(R.string.popup_button_close, null)
                    .setNegativeButton(R.string.popup_button_disconnect, this).show();
            dialog.setOnDismissListener(this);
        }
    }

    @Override
    public final void onDismiss(DialogInterface dialog) {
        this.dialog = null;
        this.lvPlayers = null;
    }

    @Override
    public final void onConnected(ClientConnection conn) {
        if (lvPlayers != null)
            lvPlayers.post(this);
    }

    @Override
    public final void onReconnection(ClientConnection conn) {
        if (lvPlayers != null)
            lvPlayers.post(this);
    }

    @Override
    public final void onClientDisconnected(ClientConnection conn) {
        if (lvPlayers != null)
            lvPlayers.post(this);
    }

    @Override
    public void onServerStopped() {
        helper.stop();
    }

    @Override
    public final void run() {
        notifyDataSetChanged();
    }

    @Override
    public final void onClick(DialogInterface dialog, int which) {
        context.p2pShutdown();
    }

    @Override
    public final int getCount() {
        return server.getNumClients();
    }

    @Override
    public final Object getItem(int position) {
        return null;
    }

    @Override
    public final long getItemId(int position) {
        return 0;
    }

    @Override
    public final View getView(int position, View v, ViewGroup parent) {
        if (v == null) {
            v = View.inflate(context, R.layout.client_connections_dialog_item, null);
        }

        ClientConnection conn = server.getConnection(position);

        TextView tv = v.findViewById(R.id.tv_clientname);
        Button b_kick = v.findViewById(R.id.b_kickclient);
        if (conn.isKicked()) {
            b_kick.setText(R.string.popup_button_unkick);
        } else {
            b_kick.setText(R.string.popup_button_kick);
        }
        b_kick.setTag(conn);
        b_kick.setOnClickListener(this);

        tv.setText(conn.getName());
        tv.setTextColor(conn.isConnected() ? Color.GREEN : Color.RED);
        tv.setBackgroundColor(position % 2 == 0 ? Color.BLACK : Color.DKGRAY);

        return v;
    }

    @Override
    public final void onClick(View v) {
        ClientConnection conn = (ClientConnection)v.getTag();
        if (conn.isKicked()) {
            conn.unkick();
        } else {
            conn.kick();
        }
        notifyDataSetChanged();
    }

    public void onError(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
}
