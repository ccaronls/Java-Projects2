package cc.lib.mp.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cc.lib.android.BonjourThread;
import cc.lib.android.CCActivityBase;
import cc.lib.android.SpinnerTask;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.net.GameServer;

public abstract class MPGameManager implements Application.ActivityLifecycleCallbacks {

    private static Logger log = LoggerFactory.getLogger(MPGameManager.class);

    private WifiP2pHelper helper;
    private final CCActivityBase activity;
    private final GameServer server;
    private final GameClient client;
    private final int port;
    private final int maxClients;
    private final String bonjourName;

    /**
     * New manager in client mode
     *
     * @param activity
     * @param client
     * @param port
     */
    public MPGameManager(CCActivityBase activity, GameClient client, int port, String bonjourName) {
        this.activity = activity;
        this.client = client;
        this.port = port;
        this.maxClients = 0;
        this.server = null;
        this.bonjourName = bonjourName;
        //activity.getApplication().registerActivityLifecycleCallbacks(this);
    }

    /**
     * New manager in host mode
     *
     * @param activity
     * @param maxClients
     */
    public MPGameManager(CCActivityBase activity, GameServer server, int maxClients) {
        this.activity = activity;
        this.maxClients = maxClients;
        this.server = server;
        this.client = null;
        this.port = -1;
        this.bonjourName = null;
    }

    public void killMPGame() {
        if (server != null) {
            server.stop();
        }
        if (client != null) {
            client.disconnect();
        }
        if (helper != null) {
            helper.destroy();
            helper = null;
        }
    }

    // *********************************************************************************
    //
    //       SERVER
    //
    // *********************************************************************************

    /**
     *
     */
    public void startHostMultiplayer() {
        new SpinnerTask(activity) {
            @Override
            protected void doIt(String ... args) throws Exception {
                server.listen();
                helper = new WifiP2pHelper(activity) {
                    @Override
                    public void onGroupInfo(WifiP2pGroup group) {
                        super.onGroupInfoAvailable(group);
                        server.setPassword(group.getPassphrase());
                    }
                };
                helper.p2pInitialize();
                String build =
                        "\nPRODUCT   :"+Build.PRODUCT
                                +"\nDEVICE    :"+Build.DEVICE
                                +"\nBOARD     :"+Build.BOARD
                                +"\nBRAND     :"+Build.BRAND
                                +"\nHARDWARE  :"+Build.HARDWARE
                                +"\nHOST      :"+Build.HOST
                                +"\nMANFUC    :"+Build.MANUFACTURER
                                +"\nMODEL     :"+Build.MODEL
                                +"\nTYPE      :"+Build.TYPE
                                +"\nVERSION   :"+Build.VERSION.CODENAME;
                log.debug("Device info=%s", build);
                helper.setDeviceName(server.getName() + "-" + activity.getString(cc.lib.android.R.string.app_name));
                helper.startGroup(); // make sure we are the group owner
            }

            @Override
            protected void onSuccess() {
                showWaitingForPlayersDialog();
            }

            @Override
            protected void onError(Exception e) {
                activity.newDialogBuilder().setTitle(cc.lib.android.R.string.popup_title_error).setMessage("Failed to start server: " + e.getLocalizedMessage()).setNegativeButton(cc.lib.android.R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SpinnerTask(activity) {
                            @Override
                            protected void doIt(String ... args) throws Exception {
                                killMPGame();
                            }

                            @Override
                            protected void onSuccess() {
                                onMPGameKilled();
                            }
                        }.execute();
                    }
                }).show();
            }

        }.execute();
    }

    protected void onMPGameKilled() {}

    void showWaitingForPlayersDialog() {
        ListView lvPlayers = new ListView(activity);
        final BaseAdapter playersAdapter = new BaseAdapter() {
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
                    v = new TextView(activity);
                }

                ClientConnection conn = server.getConnection(position);
                TextView tv = (TextView)v;
                tv.setText(conn.getName());
                tv.setTextColor(conn.isConnected() ? Color.GREEN : Color.RED);
                tv.setBackgroundColor(position % 2 == 0 ? Color.BLACK : Color.DKGRAY);

                return v;
            }
        };
        lvPlayers.setAdapter(playersAdapter);
        final Dialog d = activity.newDialogBuilder().setTitle(R.string.popup_title_waiting_for_players)
                .setView(lvPlayers)
                .setNegativeButton(cc.lib.android.R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SpinnerTask(activity) {
                            @Override
                            protected void doIt(String ... args) throws Exception {
                                killMPGame();
                            }

                            @Override
                            protected void onSuccess() {
                                onMPGameKilled();
                            }
                        }.execute();
                    }
                }).show();
        server.addListener(new GameServer.Listener() {
            @Override
            public synchronized void onConnected(ClientConnection conn) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playersAdapter.notifyDataSetChanged();
                    }
                });

                if (server.getNumConnectedClients() == maxClients) {
                    server.removeListener(this);
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            d.dismiss();
                            //helper.destroy();
                            //helper = null;
                            onAllClientsJoined();
                        }
                    });
                } else {
                    int num = maxClients - server.getNumConnectedClients();
                    server.broadcastMessage("Waiting for " + num + " more players");//getString(R.string.server_broadcast_waiting_for_n_more_players, num));
                }
            }

            @Override
            public void onReconnection(ClientConnection conn) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playersAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onClientDisconnected(ClientConnection conn) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playersAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onCommand(ClientConnection conn, GameCommand cmd) {

            }
        });
    }

    public abstract void onAllClientsJoined();

    // *********************************************************************************
    //
    //       CLIENT
    //
    // *********************************************************************************

    private Dialog clientDialog = null;

    public void showJoinGameDialog() {
        final ListView lvHost = new ListView(activity);
        final List<WifiP2pDevice> p2pDevices = new ArrayList<>();
        final List<BonjourThread.BonjourRecord> dnsDevices = new ArrayList<>();
        final List<Object> devices = new ArrayList<>();
        final BaseAdapter adapter = new BaseAdapter() {
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
                return 0;
            }

            @Override
            public View getView(int position, View v, ViewGroup parent) {
                if (v == null) {
                    v = View.inflate(activity, cc.lib.android.R.layout.list_item_peer, null);
                }

                synchronized (devices) {
                    Object d = devices.get(position);
                    v.setTag(d);
                    TextView tvPeer = (TextView)v.findViewById(cc.lib.android.R.id.tvPeer);
                    if (d instanceof WifiP2pDevice) {
                        WifiP2pDevice device = (WifiP2pDevice)d;
                        tvPeer.setText(device.deviceName + " " + WifiP2pHelper.statusToString(device.status, activity));
                    } else if (d instanceof BonjourThread.BonjourRecord) {
                        BonjourThread.BonjourRecord record = (BonjourThread.BonjourRecord)d;
                        tvPeer.setText("DNS: " + record.getHostAddress());
                    }
                    tvPeer.setBackgroundColor(position % 2 == 0 ? Color.BLACK : Color.DKGRAY);
                }

                return v;
            }
        };
        lvHost.setAdapter(adapter);

        helper = new WifiP2pHelper(activity) {
            @Override
            protected AlertDialog.Builder newDialog() {
                return activity.newDialogBuilder();
            }

            @Override
            protected synchronized void onPeersAvailable(Collection<WifiP2pDevice> peers) {
                p2pDevices.clear();
                for (WifiP2pDevice p : peers) {
                    if (p.isGroupOwner())
                        p2pDevices.add(p); // we only want to connect to host servers
                }
                synchronized (devices) {
                    devices.clear();
                    devices.addAll(p2pDevices);
                    devices.addAll(dnsDevices);
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });

            }

            boolean connecting = false;

            @Override
            public void onConnectionAvailable(final WifiP2pInfo info) {
                if (connecting || client.isConnected()) {
                    return;
                }
                connecting = true;
                if (clientDialog != null) {
                    clientDialog.dismiss();
                    clientDialog = null;
                }
                new SpinnerTask(activity) {
                    @Override
                    protected void doIt(String ... args) throws Exception {
                        stopPeerDiscovery();
                        client.connect(info.groupOwnerAddress, port);
                    }

                    @Override
                    protected void onSuccess() {
                        connecting = false;
                        synchronized (helper) {
                            helper.notify();
                        }
                        if (clientDialog != null) {
                            clientDialog.dismiss();
                        }
                    }

                    @Override
                    protected void onError(Exception e) {
                        connecting = false;
                        activity.newDialogBuilder().setTitle(cc.lib.android.R.string.popup_title_error)
                                .setMessage("Failed to connect to server " + e.getMessage())
                                .setNegativeButton(cc.lib.android.R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new SpinnerTask(activity) {
                                            @Override
                                            protected void doIt(String ... args) throws Exception {
                                                killMPGame();
                                            }

                                            @Override
                                            protected void onSuccess() {
                                                onMPGameKilled();
                                            }
                                        }.execute();
                                    }
                                }).show();
                    }
                }.execute();

            }

            @Override
            public void onGroupInfo(WifiP2pGroup group) {
                client.setPassphrase(group.getPassphrase());
            }
        };

        final BonjourThread bonjour = new BonjourThread(bonjourName);
        //bonjour.attach(this);
        bonjour.addListener(new BonjourThread.BonjourListener() {
            @Override
            public synchronized void onRecords(Map<String, BonjourThread.BonjourRecord> records) {
                dnsDevices.clear();
                dnsDevices.addAll(records.values());
                synchronized (devices) {
                    devices.clear();
                    devices.addAll(p2pDevices);
                    devices.addAll(dnsDevices);
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onWifiStateChanged(String ssid) {

            }
        });

        lvHost.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice d = (WifiP2pDevice)view.getTag();
                new SpinnerTask(activity) {

                    @Override
                    protected String getProgressMessage() {
                        return "Please wait for all players to connect";
                    }

                    @Override
                    protected void onCancelled() {
                        super.onCancelled();
                        new SpinnerTask(activity) {
                            @Override
                            protected void doIt(String ... args) throws Exception {
                                helper.cancelConnect();
                                bonjour.detatch();
                                killMPGame();
                                synchronized (helper) {
                                    helper.notify();
                                }
                            }

                            @Override
                            protected void onSuccess() {
                                onMPGameKilled();
                            }
                        }.execute();
                    }

                    @Override
                    protected void doIt(String ... args) throws Exception {
                        clientDialog.dismiss();
                        if (d.status == WifiP2pDevice.CONNECTED)
                            return;
                        helper.connect(d);
                        //synchronized (helper) {
                        //    helper.wait();
                        //}
                    }

                    @Override
                    protected void onSuccess() {
                        if (client.isConnected()) {
                            //showWaitingForPlayersDialogClient(canceleble);
                            Toast.makeText(activity, R.string.toast_connect_success, Toast.LENGTH_LONG).show();
                            //helper.destroy();
                            //helper = null;
                            onAllClientsJoined();
                        } else if (!isCancelled()) {
                            activity.newDialogBuilder().setTitle(cc.lib.android.R.string.popup_title_error)
                                    .setMessage(R.string.popup_msg_failed_connect_host)
                                    .setNegativeButton(cc.lib.android.R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            killMPGame();
                                            onMPGameKilled();
                                        }
                                    }).setCancelable(false).show();
                        }
                    }
                }.execute();
            }
        });

        new SpinnerTask(activity) {
            @Override
            protected void doIt(String ... args) {
                helper.p2pInitialize();
                helper.discoverPeers();
            }

            @Override
            protected void onSuccess() {
                if (!client.isConnected()) {
                    clientDialog = activity.newDialogBuilder().setTitle("Hosts")
                            .setView(lvHost)
                            .setNegativeButton(cc.lib.android.R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    killMPGame();
                                    onMPGameKilled();
                                }
                            }).setCancelable(false).show();
                }
            }
        }.execute();

    }

    void showWaitingForPlayersDialogClient(final boolean cancelable) {
        final AlertDialog d = activity.newDialogBuilder().setTitle(R.string.popup_title_waiting)
                .setMessage(R.string.popup_msg_waiting_for_players)
                .setNegativeButton(cc.lib.android.R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        client.disconnect();
                        onMPGameKilled();
                    }
                }).setCancelable(false).show();
        client.addListener(new GameClient.Listener() {
            @Override
            public void onCommand(GameCommand cmd) {
                client.removeListener(this);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();
                    }
                });
            }

            @Override
            public void onMessage(final String msg) {
                client.removeListener(this);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        d.setMessage(msg);
                    }
                });
            }

            @Override
            public void onDisconnected(String reason) {

            }

            @Override
            public void onConnected() {

            }
        });
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if(helper != null)
            helper.resume();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (helper != null)
            helper.pause();
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        killMPGame();
    }
}
