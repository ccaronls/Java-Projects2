package cc.game.dominos.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.RadioGroup;

import java.io.File;

import cc.game.dominos.core.Dominos;
import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.annotation.Keep;
import cc.lib.utils.FileUtils;

/**
 * Created by chriscaron on 2/15/18.
 */

public class DominosActivity extends DroidActivity {

    private final static String TAG = DominosActivity.class.getSimpleName();

    private Dominos dominos = null;
    private File saveFile=null;

    public DominosActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AndroidLogger.setLogFile(new File(Environment.getExternalStorageDirectory(), "dominos.log"));
        int padding = getResources().getDimensionPixelSize(R.dimen.border_padding);
        setMargin(padding);
        saveFile = new File(Environment.getExternalStorageDirectory(), "dominos.save");
        Dominos.SPACING = getResources().getDimension(R.dimen.element_spacing);
        Dominos.TEXT_SIZE = getResources().getDimension(R.dimen.info_txt_size);
        dominos = new Dominos() {
            @Override
            public void redraw() {
                DominosActivity.this.redraw();
            }

            @Override
            @Keep
            protected void onGameOver(int winner) {
                super.onGameOver(winner);
                getContent().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showNewGameDialog();
                    }
                }, 5000);
            }

            @Override
            protected void onMenuClicked() {
                showNewGameDialog();
            }
        };
        dominos.startDominosIntroAnimation(new Runnable() {
            @Override
            public void run() {
                if (!isCurrentDialogShowing())
                    showNewGameDialog();
            }
        });
    }

    void copyFileToExt() {
        try {
            FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        dominos.redraw();
    }

    final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onPause() {
        super.onPause();
        dominos.stopGameThread();
    }

    int tx=-1, ty=-1;
    boolean dragging = false;

    @Override
    protected void onDraw(DroidGraphics g) {
        synchronized (this) {
            dominos.draw(g, tx, ty);
        }
    }

    @Override
    protected void onTouchDown(float x, float y) {
        tx = Math.round(x);
        ty = Math.round(y);
        redraw();
    }

    @Override
    protected void onTouchUp(float x, float y) {
        if (dragging) {
            dominos.stopDrag();
            dragging = false;
        }
        tx = -1;//Math.round(x);
        ty = -1;//Math.round(y);
        redraw();
    }

    @Override
    protected void onDrag(float x, float y) {
        if (!dragging) {
            dominos.startDrag();
            dragging = true;
        }
        tx = Math.round(x);
        ty = Math.round(y);
        redraw();
    }

    @Override
    protected void onTap(float x, float y) {
        tx = Math.round(x);
        ty = Math.round(y);
        redraw();
        getContent().postDelayed(new Runnable() {
            public void run() {
                tx = ty = -1;
                dominos.onClick();
            }
        }, 100);
    }

    void showNewGameDialog() {

        final View v = View.inflate(this, R.layout.new_game_type_dialog, null);
        AlertDialog.Builder b = newDialogBuilder().setTitle(R.string.popup_title_choose_game_type)
                .setView(v).setNegativeButton(R.string.popup_button_cancel, null);

        if (dominos.isInitialized()) {
            b.setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!dominos.isGameRunning())
                        dominos.startGameThread();
                }
            });
        }
        b.show();

        v.findViewById(R.id.bSinglePlayer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewSinglePlayerSetupDialog();
            }
        });
        /*
        v.findViewById(R.id.bMultiPlayerHost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dominos.isGameRunning()) {
                    // this wont work until we get rid of MPPlayer stuff
                    showHostMultiplayerDialog();
                } else {
                    showNewMultiplayerPlayerSetupDialog(true);
                }
            }
        });
        v.findViewById(R.id.bMultiPlayerSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchMultiplayerHostsDialog();
            }
        });*/
        View button = v.findViewById(R.id.bResumeSP);
        if (saveFile.exists()) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dominos.clear();
                    if (dominos.tryLoadFromFile(saveFile) && dominos.isInitialized()) {
                        dominos.startGameThread();
                        dismissCurrentDialog();
                    } else {
                        dominos.clear();
                        newDialogBuilder().setTitle(R.string.popup_title_error).setMessage(R.string.popup_msg_failed_to_load).setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showNewGameDialog();
                            }
                        }).show();
                    }
                }
            });
        }  else {
            button.setVisibility(View.GONE);
        }
    }

/*
    void showHostMultiplayerDialog() {
        new SpinnerTask() {
            @Override
            protected void doIt() throws Exception {
                server.listen();
                helper = new WifiP2pHelper(DominosActivity.this) {
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
                helper.setDeviceName(server.getName() + "-" + getString(R.string.app_name));
                helper.startGroup(); // make sure we are the group owner
            }

            @Override
            protected void onDone() {
                showWaitingForPlayersDialog();
            }

            @Override
            protected void onError(Exception e) {
                newDialogBuilder().setTitle(R.string.popup_title_error).setMessage(getString(R.string.popup_msg_failed_to_start_server, e.getLocalizedMessage())).setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SpinnerTask() {
                            @Override
                            protected void doIt() throws Exception {
                                killGame();
                            }

                            @Override
                            protected void onDone() {
                                showNewGameDialog();
                            }
                        }.run();
                    }
                }).show();
            }

        }.run();
    }*/

    void killGame() {
        dominos.stopGameThread();
        dominos.clear();
    }

    /*
    void showWaitingForPlayersDialog() {
        dominos.stopGameThread();
        ListView lvPlayers = new ListView(this);
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
                    v = new TextView(DominosActivity.this);
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
        newDialogBuilder().setTitle(R.string.popup_title_waiting_for_players)
                .setView(lvPlayers)
                .setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SpinnerTask() {
                            @Override
                            protected void doIt() throws Exception {
                                killGame();
                            }

                            @Override
                            protected void onDone() {
                                showNewGameDialog();
                            }
                        }.run();
                    }
                }).show();
        server.addListener(new GameServer.Listener() {
            @Override
            public synchronized void onConnected(ClientConnection conn) {
                int maxClients = dominos.getNumPlayers()-1;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playersAdapter.notifyDataSetChanged();
                    }
                });

                if (server.getNumConnectedClients() == maxClients) {
                    server.removeListener(this);
                    dominos.startGameThread();
                    dismissCurrentDialog();
                } else {
                    int num = maxClients - server.getNumConnectedClients();
                    server.broadcastMessage(getString(R.string.server_broadcast_waiting_for_n_more_players, num));
                }
            }

            @Override
            public void onReconnection(ClientConnection conn) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playersAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onClientDisconnected(ClientConnection conn) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playersAdapter.notifyDataSetChanged();
                    }
                });

            }
        });
    }

    void showSearchMultiplayerHostsDialog() {
        final ListView lvHost = new ListView(this);
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
                    v = View.inflate(DominosActivity.this, R.layout.list_item_peer, null);
                }

                synchronized (devices) {
                    Object d = devices.get(position);
                    v.setTag(d);
                    TextView tvPeer = (TextView)v.findViewById(R.id.tvPeer);
                    if (d instanceof WifiP2pDevice) {
                        WifiP2pDevice device = (WifiP2pDevice)d;
                        tvPeer.setText(device.deviceName + " " + WifiP2pHelper.statusToString(device.status, DominosActivity.this));
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

        helper = new WifiP2pHelper(this) {
            @Override
            protected AlertDialog.Builder newDialog() {
                return newDialogBuilder();
            }

            @Override
            protected synchronized void onPeersAvailable(Collection<WifiP2pDevice> peers) {
                p2pDevices.clear();
                for (WifiP2pDevice p : peers) {
                    if (p.isGroupOwner())
                        p2pDevices.add(p); // we only want to connect to dominos servers
                }
                synchronized (devices) {
                    devices.clear();
                    devices.addAll(p2pDevices);
                    devices.addAll(dnsDevices);
                }
                runOnUiThread(new Runnable() {
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
                dismissCurrentDialog();
                new SpinnerTask() {
                    @Override
                    protected void doIt() throws Exception {
                        dominos.clear();
                        stopPeerDiscovery();
                        client.connect(info.groupOwnerAddress, MPConstants.PORT);
                        new MPPlayerUser(client, DominosActivity.this, dominos);
                    }

                    @Override
                    protected void onDone() {
                        connecting = false;
                        synchronized (helper) {
                            helper.notify();
                        }
                    }

                    @Override
                    protected void onError(Exception e) {
                        connecting = false;
                        newDialogBuilder().setTitle(R.string.popup_title_error)
                                .setMessage(getString(R.string.popup_msg_failed_to_connect_reason, e.getMessage()))
                                .setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new SpinnerTask() {
                                            @Override
                                            protected void doIt() throws Exception {
                                                killGame();
                                            }

                                            @Override
                                            protected void onDone() {
                                                showNewGameDialog();
                                            }
                                        }.run();
                                    }
                                }).show();
                    }
                }.run();

            }

            @Override
            public void onGroupInfo(WifiP2pGroup group) {
                client.setPassphrase(group.getPassphrase());
            }
        };

        final BonjourThread bonjour = new BonjourThread("dom");
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

                runOnUiThread(new Runnable() {
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
                new SpinnerTask() {

                    @Override
                    protected Dialog showSpinner() {
                        return newDialogBuilder().setTitle(R.string.popup_title_connecting)
                                .setMessage(R.string.popup_msg_please_wait_forconnect_accept)
                                .setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new SpinnerTask() {
                                            @Override
                                            protected void doIt() throws Exception {
                                                helper.cancelConnect();
                                                cancel(false);
                                                killGame();
                                                synchronized (helper) {
                                                    helper.notify();
                                                }
                                            }

                                            @Override
                                            protected void onDone() {
                                                showNewGameDialog();
                                            }
                                        }.run();
                                    }
                                }).show();
                    }

                    @Override
                    protected void doIt() {
                        if (d.status == WifiP2pDevice.CONNECTED)
                            return;
                        helper.connect(d);
                        Utils.waitNoThrow(helper, 60*1000);
                    }

                    @Override
                    protected void onDone() {
                        if (client.isConnected()) {
                            //showWaitingForPlayersDialogClient(canceleble);
                            Toast.makeText(DominosActivity.this, R.string.toast_connect_success, Toast.LENGTH_LONG).show();
                        } else if (!isCancelled()) {
                            newDialogBuilder().setTitle(R.string.popup_title_error)
                                    .setMessage(R.string.popup_msg_failed_connect_host)
                                    .setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            showNewGameDialog();
                                        }
                                    }).setCancelable(false).show();
                        }
                    }
                }.run();
            }
        });

        new SpinnerTask() {
            @Override
            protected void doIt() {
                helper.p2pInitialize();
                helper.discoverPeers();
            }

            @Override
            protected void onDone() {
                newDialogBuilder().setTitle(R.string.popup_title_hosts)
                        .setView(lvHost)
                        .setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                helper.destroy();
                                bonjour.detatch();
                                showNewGameDialog();
                            }
                        }).setCancelable(false).show();

            }
        }.run();

    }

    void showWaitingForPlayersDialogClient(final boolean cancelable) {
        final AlertDialog d = newDialogBuilder().setTitle(R.string.popup_title_waiting)
                .setMessage(R.string.popup_msg_waiting_for_players)
                .setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        client.disconnect();
                        showNewGameDialog();
                    }
                }).setCancelable(false).show();
        client.addListener(new GameClient.Listener() {
            @Override
            public void onCommand(GameCommand cmd) {
                client.removeListener(this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();
                    }
                });
            }

            @Override
            public void onMessage(final String msg) {
                client.removeListener(this);
                runOnUiThread(new Runnable() {
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

    final static String NAME= Build.MODEL;

    void showNewMultiplayerPlayerSetupDialog(final boolean firstGame) {
        final View v = View.inflate(this, R.layout.game_setup_dialog, null);
        final RadioGroup rgNumPlayers = (RadioGroup)v.findViewById(R.id.rgNumPlayers);
        final RadioGroup rgDifficulty = (RadioGroup)v.findViewById(R.id.rgDifficulty);
        final RadioGroup rgTiles      = (RadioGroup)v.findViewById(R.id.rgTiles);
        final RadioGroup rgMaxPoints  = (RadioGroup)v.findViewById(R.id.rgMaxPoints);
        rgDifficulty.setVisibility(View.GONE);
        if (!firstGame)
            rgNumPlayers.setVisibility(View.GONE);
        switch (dominos.getNumPlayers()) {
            case 2:
                rgNumPlayers.check(R.id.rbPlayersTwo); break;
            case 3:
                rgNumPlayers.check(R.id.rbPlayersThree); break;
            case 4:
                rgNumPlayers.check(R.id.rbPlayersFour); break;
        }
        switch (dominos.getDifficulty()) {
            case 0:
                rgDifficulty.check(R.id.rbDifficultyEasy); break;
            case 1:
                rgDifficulty.check(R.id.rbDifficultyMedium); break;
            case 2:
                rgDifficulty.check(R.id.rbDifficultyHard); break;
        }
        switch (dominos.getMaxPips()) {
            case 6:
                rgTiles.check(R.id.rbTiles6x6); break;
            case 9:
                rgTiles.check(R.id.rbTiles9x9); break;
            case 12:
                rgTiles.check(R.id.rbTiles12x12); break;
        }
        switch (dominos.getMaxScore()) {
            case 150:
                rgMaxPoints.check(R.id.rbMaxPoints150); break;
            case 200:
                rgMaxPoints.check(R.id.rbMaxPoints200); break;
            case 250:
                rgMaxPoints.check(R.id.rbMaxPoints250); break;
        }
        newDialogBuilder().setTitle(R.string.popup_title_new_mp_game)
                .setView(v)
                .setPositiveButton(R.string.popup_button_start, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        int difficulty = 0;
                        int numPlayers = 4;
                        int maxPoints = 150;
                        int maxPips = 9;

                        switch (rgDifficulty.getCheckedRadioButtonId()) {
                            case R.id.rbDifficultyEasy:
                                difficulty = 0; break;
                            case R.id.rbDifficultyMedium:
                                difficulty = 1; break;
                            case R.id.rbDifficultyHard:
                                difficulty = 2; break;
                        }

                        switch (rgNumPlayers.getCheckedRadioButtonId()) {
                            case R.id.rbPlayersTwo:
                                numPlayers = 2; break;
                            case R.id.rbPlayersThree:
                                numPlayers = 3; break;
                            case R.id.rbPlayersFour:
                                numPlayers = 4; break;
                        }

                        switch (rgMaxPoints.getCheckedRadioButtonId()) {
                            case R.id.rbMaxPoints150:
                                maxPoints = 150; break;
                            case R.id.rbMaxPoints200:
                                maxPoints = 200; break;
                            case R.id.rbMaxPoints250:
                                maxPoints = 250; break;
                        }

                        switch (rgTiles.getCheckedRadioButtonId()) {
                            case R.id.rbTiles6x6:
                                maxPips = 6; break;
                            case R.id.rbTiles9x9:
                                maxPips = 9; break;
                            case R.id.rbTiles12x12:
                                maxPips = 12; break;
                        }

                        if (firstGame) {
                            dominos.initGame(maxPips, maxPoints, 0);
                            // now populate the game with remote players so that when they connect we can send them
                            // the game state right away.
                            Player [] players = new Player[numPlayers];
                            players[0] = new PlayerUser(0);
                            players[0].setName(server.getName());
                            for (int i=1; i<players.length; i++) {
                                players[i] = new Player(i);
                            }
                            dominos.setPlayers(players);
                            dominos.startNewGame();
                            showHostMultiplayerDialog();
                        } else {
                            dominos.initGame(maxPips, maxPoints, 0);
                            dominos.startNewGame();
                            server.broadcastMessage(getString(R.string.server_broadcast_starting_new_game));
                            dominos.startGameThread();
                        }
                    }


                }).setNegativeButton(R.string.popup_button_quit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new SpinnerTask() {
                    @Override
                    protected void doIt() throws Exception {
                        killGame();
                    }

                    @Override
                    protected void onDone() {
                        showNewGameDialog();
                    }
                }.run();
            }
        }).show();
    }

    void startNewGame() {
        dominos.startNewGame();
        server.broadcastMessage(getString(R.string.server_broadcast_starting_new_game));
        dominos.startGameThread();
    }*/

    void showNewSinglePlayerSetupDialog() {
        final View v = View.inflate(this, R.layout.game_setup_dialog, null);
        final RadioGroup rgNumPlayers = (RadioGroup)v.findViewById(R.id.rgNumPlayers);
        final RadioGroup rgDifficulty = (RadioGroup)v.findViewById(R.id.rgDifficulty);
        final RadioGroup rgTiles      = (RadioGroup)v.findViewById(R.id.rgTiles);
        final RadioGroup rgMaxPoints  = (RadioGroup)v.findViewById(R.id.rgMaxPoints);

        final int numPlayer = getPrefs().getInt("numPlayers", 0);
        int difficulty = getPrefs().getInt("difficulty", 0);
        int maxPips = getPrefs().getInt("maxPips", 0);
        final int maxScore = getPrefs().getInt("maxScore", 0);

        switch (numPlayer) {
            case 2:
                rgNumPlayers.check(R.id.rbPlayersTwo); break;
            case 3:
                rgNumPlayers.check(R.id.rbPlayersThree); break;
            case 4:
                rgNumPlayers.check(R.id.rbPlayersFour); break;
        }
        switch (difficulty) {
            case 0:
                rgDifficulty.check(R.id.rbDifficultyEasy); break;
            case 1:
                rgDifficulty.check(R.id.rbDifficultyMedium); break;
            case 2:
                rgDifficulty.check(R.id.rbDifficultyHard); break;
        }
        switch (maxPips) {
            case 6:
                rgTiles.check(R.id.rbTiles6x6); break;
            case 9:
                rgTiles.check(R.id.rbTiles9x9); break;
            case 12:
                rgTiles.check(R.id.rbTiles12x12); break;
        }
        switch (maxScore) {
            case 150:
                rgMaxPoints.check(R.id.rbMaxPoints150); break;
            case 200:
                rgMaxPoints.check(R.id.rbMaxPoints200); break;
            case 250:
                rgMaxPoints.check(R.id.rbMaxPoints250); break;
        }
        newDialogBuilder().setTitle(R.string.popup_title_new_sp_game)
                .setView(v)
                .setPositiveButton(R.string.popup_button_start, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        int difficulty = 0;
                        int numPlayers = 4;
                        int maxPoints = 150;
                        int maxPips = 9;

                        switch (rgDifficulty.getCheckedRadioButtonId()) {
                            case R.id.rbDifficultyEasy:
                                difficulty = 0; break;
                            case R.id.rbDifficultyMedium:
                                difficulty = 1; break;
                            case R.id.rbDifficultyHard:
                                difficulty = 2; break;
                        }

                        switch (rgNumPlayers.getCheckedRadioButtonId()) {
                            case R.id.rbPlayersTwo:
                                numPlayers = 2; break;
                            case R.id.rbPlayersThree:
                                numPlayers = 3; break;
                            case R.id.rbPlayersFour:
                                numPlayers = 4; break;
                        }

                        switch (rgMaxPoints.getCheckedRadioButtonId()) {
                            case R.id.rbMaxPoints150:
                                maxPoints = 150; break;
                            case R.id.rbMaxPoints200:
                                maxPoints = 200; break;
                            case R.id.rbMaxPoints250:
                                maxPoints = 250; break;
                        }

                        switch (rgTiles.getCheckedRadioButtonId()) {
                            case R.id.rbTiles6x6:
                                maxPips = 6; break;
                            case R.id.rbTiles9x9:
                                maxPips = 9; break;
                            case R.id.rbTiles12x12:
                                maxPips = 12; break;
                        }
                        getPrefs().edit()
                                .putInt("numPlayers", numPlayers)
                                .putInt("difficulty", difficulty)
                                .putInt("maxPips", maxPips)
                                .putInt("maxScore", maxScore)
                                .apply();
                        dominos.stopGameThread();
                        dominos.initGame(maxPips, maxPoints, difficulty);
                        dominos.setNumPlayers(numPlayers);
                        dominos.startNewGame();
                        dominos.startGameThread();

                    }


                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                showNewGameDialog();
            }
        }).show();
    }

}
