package cc.lib.mp.android;

import android.Manifest;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.lib.android.CCActivityBase;
import cc.lib.android.SpinnerTask;
import cc.lib.crypt.Cypher;
import cc.lib.net.AGameClient;
import cc.lib.net.AGameServer;
import cc.lib.net.GameClient;
import cc.lib.net.GameServer;

/**
 * Created by Chris Caron on 7/17/21.
 *
 * Usage:
 *
 * p2pInit() - Does permissions / availability checks. onP2PReady called when ready or error popup.
 * p2pStart() - Shows a start as server or client dialog. onP2PClient / onP2PServer called when user chooses
 */
public abstract class P2PActivity extends CCActivityBase {

    private AGameServer server = null;
    private AGameClient client = null;
    private P2PMode mode = P2PMode.DONT_KNOW;

    public enum P2PMode {
        CLIENT, SERVER, DONT_KNOW
    }

    @Override
    protected void onStop() {
        super.onStop();
        p2pShutdown();
    }

    /**
     * Call this before anything else to make sure the system can run p2p
     */
    public final void p2pInit() {
        p2pInit(P2PMode.DONT_KNOW);
    }

    /**
     * Call this before anything else to make sure the system can run p2p
     */
    public final void p2pInit(P2PMode mode) {
        client = null;
        server = null;
        this.mode = mode;
        if (!P2PHelper.isP2PAvailable(this)) {
            newDialogBuilder().setTitle(R.string.p2p_popup_title_unsupported)
                    .setMessage(R.string.p2p_popup_message_unsupported)
                    .setNegativeButton(R.string.popup_button_ok, null).show();
        } else {
            checkPermissions(getRequiredPermissions());
        }
    }

    @Override
    public final void checkPermissions(String... permissions) {
        super.checkPermissions(permissions);
    }

    String[] getRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.addAll(Arrays.asList(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.INTERNET
        ));
        perms.addAll(Arrays.asList(getExtraPermissions()));
        return perms.toArray(new String[perms.size()]);
    }

    protected String [] getExtraPermissions() { return new String[0]; }

    @Override
    protected final void onPermissionLimited(List<String> permissionsNotGranted) {
        newDialogBuilder().setTitle(R.string.p2p_popup_title_missing_permissions)
                .setMessage(getString(R.string.p2p_popup_message_missing_permissions, permissionsNotGranted.toString()))
                .setNegativeButton(R.string.popup_button_ok, null).show();
    }

    @Override
    protected final void onAllPermissionsGranted() {
        onP2PReady();
    }

    /**
     * Called when p2pInit has completed successfully. Default is to just call p2pStart
     */
    protected void onP2PReady() {
        p2pStart();
    }

    /**
     * Override this to use your own method for choosing to start as host or client. If not overridden then
     * the default behavior is a dialog to choose mode. Choosing client mode executes: p2pInitAsClient and choosing
     * server mode executes p2pInitAsHost. Those methods bring up their own respective dialogs to guide user through
     * connection process.
     */
    public void p2pStart() {
        switch (mode) {
            case CLIENT:
                p2pInitAsClient();
                return;
            case SERVER:
                p2pInitAsServer();
                return;
        }
        newDialogBuilder().setTitle(R.string.p2p_popup_title_choose_mode)
                .setItems(getResources().getStringArray(R.array.p2p_popup_choose_mode_items), (dialog, which) -> {
                    switch (which) {
                        case 0:
                            p2pInitAsClient();
                            break;
                        case 1:
                            p2pInitAsServer();
                            break;
                    }

                }).setNegativeButton(R.string.popup_button_cancel, null).show();
    }

    /**
     * Interface to functions available only when in host mode
     */
    public interface P2PServer {
        AGameServer getServer();

        void openConnections();
    }

    /**
     * Interface to methods available only when in client mode
     */
    public interface P2PClient {
        AGameClient getClient();
    }

    protected AGameClient newGameClient() {
        return new GameClient(getDeviceName(), getVersion(), getCypher());
    }

    public final void p2pInitAsClient() {
        if (server != null || client != null) {
            throw new IllegalArgumentException("P2P Mode already in progress. Call p2pShutdown first.");
        }
        server = null;
        client = newGameClient();//getDeviceName(), getVersion(), getCypher());
        client.addListener(new AGameClient.Listener() {
            @Override
            public void onDisconnected(String reason, boolean serverInitiated) {
                runOnUiThread(() -> p2pShutdown());
            }
        });
        new P2PJoinGameDialog(this, client, getDeviceName(), getConnectPort());
        onP2PClient(new P2PClient() {
            @Override
            public AGameClient getClient() {
                return client;
            }
        });
    }

    /**
     * Called when the client mode is initialized
     *
     * @param p2pClient
     */
    protected abstract void onP2PClient(P2PClient p2pClient);

    protected AGameServer newGameServer() {
        return new GameServer(getDeviceName(), getConnectPort(), getVersion(), getCypher(), getMaxConnections());
    }

    public final void p2pInitAsServer() {
        if (server != null || client != null) {
            throw new IllegalArgumentException("P2P Mode already in progress. Call p2pShutdown first.");
        }

        client = null;
        server = newGameServer();
        new P2PClientConnectionsDialog(this, server, getDeviceName());
    }

    /**
     * Called when the server context is ready
     *
     * @param p2pServer
     */
    protected abstract void onP2PServer(P2PServer p2pServer);

    /**
     * Called from UI thread
     */
    public final void p2pShutdown() {
        log.debug("p2pShutdown");
        new SpinnerTask<Void>(this) {

            @Override
            protected String getProgressMessage() {
                return getString(R.string.p2p_progress_message_disconnecting);
            }

            @Override
            protected void doIt(Void... args) {
                if (server != null)
                    server.stop();
                if (client != null)
                    client.disconnect();
            }

            @Override
            protected void onCompleted() {
                server = null;
                client = null;
                onP2PShutdown();
            }
        }.execute();
    }

    public boolean isP2PConnected() {
        return client != null || server != null;
    }

    protected void onP2PShutdown() {}

    public String getDeviceName() {
        String name = getString(R.string.app_name);
        return String.format("%s-%s-%s %s-%s", Build.BRAND ,Build.MODEL, Build.VERSION.SDK_INT, name, getVersion());
    }

    protected abstract int getConnectPort();

    protected abstract String getVersion();

    protected abstract int getMaxConnections();

    protected Cypher getCypher() {
        return null;
    }

    public final AGameClient getClient() {
        return client;
    }

    public final AGameServer getServer() {
        return server;
    }
}
