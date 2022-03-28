package cc.lib.mp.android;

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

import cc.lib.android.SpinnerTask;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.utils.Lock;

public class P2PJoinGameDialog extends BaseAdapter
        implements AdapterView.OnItemClickListener,
        DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener,
        Runnable,
        GameClient.Listener {

    final P2PActivity context;
    final ListView lvHost;
    final List<WifiP2pDevice> p2pDevices = new ArrayList<>();
    final Dialog dialog;
    final P2PHelper helper;
    final int connectPort;
    final GameClient client;
    final Lock connectLock = new Lock(1);

    public P2PJoinGameDialog(P2PActivity activity, GameClient client, String clientName, int port) {
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
                if (lvHost != null)
                    lvHost.post(P2PJoinGameDialog.this); // notify dataset changed
            }

            @Override
            protected void onGroupFormed(InetAddress addr, String ipAddress) {
                Log.d(TAG, "onGroupFormedAsClient: " + ipAddress);
                client.connectAsync(addr, connectPort, (success)-> {
                    if (!success) {
                        showError(context.getString(R.string.popup_error_msg_failed_to_connect));
                    }
                    connectLock.release();
                });
            }
        };
        client.addListener(this);
        helper.setDeviceName(clientName);
        helper.start(client);
        dialog = context.newDialogBuilder().setTitle(R.string.popup_title_join_game).setView(lvHost)
                .setNegativeButton(R.string.popup_button_cancel, this).setOnCancelListener(this).show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        shutdown();
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
        TextView tvPeer = v.findViewById(R.id.tvPeer);
        tvPeer.setText(context.getString(R.string.join_game_dialog_client_label, device.deviceName, P2PHelper.statusToString(device.status, context)));
        tvPeer.setBackgroundColor(position % 2 == 0 ? Color.BLACK : Color.DKGRAY);

        return v;
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        // cancel out of the dialog
        shutdown();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final WifiP2pDevice d = (WifiP2pDevice)view.getTag();
        new SpinnerTask<Void>(context) {

            @Override
            protected String getProgressMessage() {
                return context.getString(R.string.popup_progress_msg_pleasewait_for_invite);
            }

            @Override
            protected void doIt(Void ... args) throws Throwable {
                helper.connect(d);
                connectLock.block(30000);
            }

            @Override
            protected void onSuccess() {
            }

            @Override
            protected void onError(Exception e) {
                connectLock.release();
                super.onError(e);
            }

            @Override
            protected void onCancelButtonClicked() {
                connectLock.release();
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
    public void onDisconnected(String reason, boolean serverInitiated) {

    }

    @Override
    public void onConnected() {
        client.removeListener(this); // TODO: We could stay alive and throw up dialog again if disconnected
        helper.stop();
        dialog.dismiss();
    }

    public void showError(String msg) {
        Log.e(P2PHelper.TAG, msg);
        context.runOnUiThread(() -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show());
    }

    private void shutdown() {
        dialog.dismiss();
        helper.stop();
        context.p2pShutdown();
    }
}
