package cc.game.dominos.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.game.dominos.core.Dominos;
import cc.game.dominos.core.Player;
import cc.game.dominos.core.Tile;
import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.android.WifiP2pHelper;
import cc.lib.crypt.Cypher;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.game.Utils;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.net.GameCommandType;
import cc.lib.net.GameServer;
import cc.lib.utils.FileUtils;

/**
 * Created by chriscaron on 2/15/18.
 */

public class DominosActivity extends DroidActivity {

    public final static String DOMINOS_ID = "Dominos";
    public final static String USER_ID    = "User";

    private final static String TAG = DominosActivity.class.getSimpleName();

    private Dominos dominos = null;
    private File saveFile=null;
    private Cypher cypher = null;

    public DominosActivity() {
        try {
            int [] counts = {1006,0,0,0,0,0,0,0,0,258,996,0,0,936,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,236,821,536,382,748,66,35,966,170,539,365,189,261,125,183,869,226,108,132,427,317,641,286,842,325,247,132,927,608,447,817,14,927,481,1004,453,583,278,27,311,190,540,2147483647,986,544,703,394,388,627,412,1015,922,585,567,290,32,215,450,846,26,1021,324,110,488,896,74,78,216,231,372,917,81,537,959,522,968,433,515,115,318,879,455,984,82,962,675,760,961,565,128,736,287,143,607};
            cypher = new HuffmanEncoding(counts);
        } catch (Exception e) {
            throw new AssertionError();
        }

    }

    public abstract class SpinnerTask extends AsyncTask<Void, Void, Exception> {
        Dialog spinner;
        @Override
        protected final void onPreExecute() {
            spinner = showSpinner();
        }

        protected Dialog showSpinner() {
            ProgressDialog d = new ProgressDialog(DominosActivity.this, R.style.DialogTheme);
            d.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                }
            });
            d.show();
            return d;
        }

        @Override
        protected final Exception doInBackground(Void... voids) {
            try {
                doIt();
            } catch (Exception e) {
                e.printStackTrace();
                return e;
            }
            return null;
        }

        @Override
        protected final void onPostExecute(Exception e) {
            spinner.dismiss();
            if (e == null) {
                onDone();
            } else {
                onError(e);
            }
        }

        protected abstract void doIt() throws Exception;

        protected abstract void onDone();

        protected void onError(Exception e) {
            newDialogBuilder().setTitle("Error").setMessage("An error occured\n  " + e.getMessage()).setNegativeButton("Cancel", null)
                    .setPositiveButton("Proceed anyway", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onDone();
                        }
                    }).show();
        }

        public void run() {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveFile = new File(getFilesDir(), "dominos.save");
        dominos = new Dominos() {
            @Override
            public void redraw() {
                getContent().postInvalidate();
            }

            @Override
            protected void onGameOver(int winner) {
                super.onGameOver(winner);
                if (server.isRunning()) {
                    server.broadcastExecuteOnRemote(DOMINOS_ID, winner);
                    getContent().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showNewMultiplayerPlayerSetupDialog(false);
                        }
                    }, 5000);
                } else if (client.isConnected()) {
                    getContent().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            newDialogBuilder().setTitle("Game Over")
                                    .setMessage("Standby for next game!")
                                    .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
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
                    }, 5000);
                } else {
                    getContent().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showNewGameDialog();
                        }
                    }, 5000);
                }
            }

            @Override
            protected void onMenuClicked() {
                if (server.isRunning()) {
                    String[] options = {
                            "View Clients",
                            "New Game",
                            "Stop Session"
                    };
                    newDialogBuilder().setTitle("Multiplayer Host")
                            .setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0: {
                                            // View clients
                                            final String[] clientItems = new String[server.getNumClients()];
                                            final ClientConnection[] clients = new ClientConnection[server.getNumClients()];
                                            int index = 0;
                                            for (ClientConnection c : server.getConnectionValues()) {
                                                clients[index] = c;
                                                clientItems[index++] = c.getName() + " " + (c.isConnected() ? " Connected" : " Disconnected");
                                            }
                                            newDialogBuilder().setTitle("Clients")
                                                    .setItems(clientItems, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            final ClientConnection client = clients[which];
                                                            newDialogBuilder().setTitle("Client " + client.getName())
                                                                    .setMessage("Kick this client off?")
                                                                    .setNegativeButton("No", null)
                                                                    .setPositiveButton("Kick", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            client.disconnect("Kicked out");
                                                                        }
                                                                    }).show();
                                                        }

                                                    }).show();
                                            break;
                                        }
                                        case 1: {
                                            newDialogBuilder().setTitle("Confirm").setMessage("Start a new Game?")
                                                    .setNegativeButton("Cancel", null)
                                                    .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dominos.stopGameThread();
                                                            dominos.startNewGame();
                                                            server.broadcastCommand(new GameCommand(SVR_TO_CL_INIT_ROUND).setArg("dominos", dominos));
                                                            dominos.startGameThread();
                                                        }
                                                    }).show();
                                            break;
                                        }
                                        case 2:
                                            newDialogBuilder().setTitle("Confirm")
                                                    .setMessage("End multiplayer session?")
                                                    .setNegativeButton("No", null)
                                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            new SpinnerTask() {
                                                                @Override
                                                                protected void doIt() {
                                                                    server.stop();
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
                                }
                            })
                            .setNegativeButton("Cancel", null).show();
                } else if (client.isConnected()) {
                    String[] options = {
                            "Forfeit",
                            "Exit Multiplayer"
                    };
                    newDialogBuilder().setTitle("Clietn Session")
                            .setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0: // forfeit
                                            newDialogBuilder().setTitle("Confirm")
                                                    .setMessage("Are you sure you want to forfeit? A new game will get started if you do.")
                                                    .setNegativeButton("No", null)
                                                    .setPositiveButton("Forfeit", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            client.sendCommand(new GameCommand(CL_TO_SVR_FORFEIT));
                                                            synchronized (dominos) {
                                                                dominos.notify();
                                                            }
                                                        }
                                                    }).show();
                                            break;
                                        case 1: // exit
                                            newDialogBuilder().setTitle("Confirm")
                                                    .setMessage("Disconnect from server?")
                                                    .setNegativeButton("No", null)
                                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            new SpinnerTask() {
                                                                @Override
                                                                protected void doIt() {
                                                                    client.disconnect();
                                                                    helper.disconnect();
                                                                    helper.destroy();
                                                                }

                                                                @Override
                                                                protected void onDone() {
                                                                    showNewGameDialog();
                                                                }
                                                            }.execute();
                                                        }
                                                    }).show();
                                    }
                                }
                            }).setNegativeButton("Cancel", null).show();

                } else {
                    if (dominos.isGameRunning()) {
                        dominos.stopGameThread();
                    }
                    showNewGameDialog();
                }
            }

            @Override
            protected void onTilePlaced(int player, Tile tile, int endpoint, int placement) {
                server.broadcastExecuteOnRemote(DOMINOS_ID, player, tile, endpoint, placement);
                super.onTilePlaced(player, tile, endpoint, placement);
            }

            @Override
            protected void onTileFromPool(int player, Tile pc) {
                server.broadcastExecuteOnRemote(DOMINOS_ID, player, pc);
                super.onTileFromPool(player, pc);
            }

            @Override
            protected void onKnock(int player) {
                server.broadcastExecuteOnRemote(DOMINOS_ID, player);
                super.onKnock(player);
            }

            @Override
            protected void onEndRound() {
                super.onEndRound();
                server.broadcastCommand(new GameCommand(SVR_TO_CL_INIT_ROUND).setArg("dominos", dominos.toString()));
            }

            @Override
            protected void onPlayerEndRoundPoints(int player, int pts) {
                server.broadcastExecuteOnRemote(DOMINOS_ID, player, pts);
                super.onPlayerEndRoundPoints(player, pts);
            }

            @Override
            protected void onPlayerPoints(int player, int pts) {
                server.broadcastExecuteOnRemote(DOMINOS_ID, player,pts);
                super.onPlayerPoints(player, pts);
            }

            @Override
            public void setTurn(int turn) {
                server.broadcastExecuteOnRemote(DOMINOS_ID, turn);
                super.setTurn(turn);
            }
        };

        if (false)
        try {
            FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
            dominos.loadFromFile(saveFile);
        } catch (FileNotFoundException e) {
            // ignore
        } catch (Exception e) {
            dominos.clear();
            e.printStackTrace();
        }
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
        if (!dominos.isInitialized()) {
            if (!dominos.tryLoadFromFile(saveFile))
                dominos.clear();
            else
                dominos.redraw();
        }
        if (server.isRunning()) {
            if (dominos.isInitialized())
                dominos.startGameThread();
            else if (!currentDialog.isShowing())
                showWaitingForPlayersDialog();

        } else if (!client.isConnected()) {
            if (dominos.isInitialized()) {
                dominos.startGameThread();
            } else {
                showNewGameDialog();
            }
        }
        /*
        if (mode == Mode.SINGLE && dominos.getNumPlayers() > 0 && dominos.getWinner()<0)
            dominos.startGameThread();
        else if (currentDialog != null && currentDialog.isShowing()) {
            // ignore
        } else if (!dominos.isGameRunning())
            showNewGameDialog(false);

        if (saveFile.exists()) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkPermission()) {
                    copyFileToExt();
                } else {
                    requestPermission();
                }
            }
        }*/

    }

    final int PERMISSION_REQUEST_CODE = 1001;

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    copyFileToExt();
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        dominos.stopGameThread();
        if (!client.isConnected())
            dominos.trySaveToFile(saveFile);
    }

    int tx, ty;
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
        getContent().postInvalidate();
    }

    @Override
    protected void onTouchUp(float x, float y) {
        if (dragging) {
            dominos.stopDrag();
            dragging = false;
        }
        tx = -1;//Math.round(x);
        ty = -1;//Math.round(y);
        getContent().postInvalidate();
    }

    @Override
    protected void onDrag(float x, float y) {
        if (!dragging) {
            dominos.startDrag();
            dragging = true;
        }
        tx = Math.round(x);
        ty = Math.round(y);
        getContent().postInvalidate();
    }

    @Override
    protected void onTap(float x, float y) {
        tx = Math.round(x);
        ty = Math.round(y);
        getContent().postInvalidate();
        getContent().postDelayed(new Runnable() {
            public void run() {
                tx = ty = -1;
                dominos.onClick();
            }
        }, 100);
    }

    void showNewGameDialog() {

        final View v = View.inflate(this, R.layout.new_game_type_dialog, null);
        AlertDialog.Builder b = newDialogBuilder().setTitle("New Game Type")
                .setView(v);

        if (dominos.isInitialized()) {
            b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
        v.findViewById(R.id.bMultiPlayerHost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewMultiplayerPlayerSetupDialog(true);
            }
        });
        v.findViewById(R.id.bMultiPlayerSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchMultiplayerHostsDialog();
            }
        });
        View button = v.findViewById(R.id.bResumeSP);
        if (saveFile.exists()) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dominos.tryLoadFromFile(saveFile) && dominos.isInitialized()) {
                        dominos.startGameThread();
                        currentDialog.dismiss();
                    } else {
                        dominos.clear();
                        newDialogBuilder().setTitle("Error").setMessage("Failed to load from save file").setNegativeButton("Ok", new DialogInterface.OnClickListener() {
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

    WifiP2pHelper helper = null;

    GameServer.Listener connectionListener = new GameServer.Listener() {
        @Override
        public synchronized void onConnected(ClientConnection conn) {
            int maxClients = dominos.getNumPlayers()-1;

            // if enough clients have connected then start the game
            MPPlayerRemote remote = null;
            for (int i=1; i<dominos.getNumPlayers(); i++) {
                MPPlayerRemote p = (MPPlayerRemote)dominos.getPlayer(i);
                if (p.connection == null || !p.connection.isConnected()) {
                    remote = p;
                } else if (p.connection.getName().equals(conn.getName())) {
                    remote = p;
                }
            }
            if (remote != null) {
                remote.setConnection(conn);
                conn.sendCommand(new GameCommand(SVR_TO_CL_INIT_GAME)
                        .setArg("numPlayers", dominos.getNumPlayers())
                        .setArg("playerNum", remote.playerNum)
                        .setArg("dominos", dominos.toString()));
                currentDialog.dismiss();
            }
        }

        @Override
        public void onReconnection(ClientConnection conn) {

        }

        @Override
        public void onClientDisconnected(ClientConnection conn) {

        }
    };

    void showHostMultiplayerDialog(final int numPlayers, final int maxPoints, final int maxPips) {
        new SpinnerTask() {
            @Override
            protected void doIt() throws Exception {
                server.listen();
                server.addListener(connectionListener);
                helper = new WifiP2pHelper(DominosActivity.this);
                helper.p2pInitialize();
                String build = Build.PRODUCT
                        +"\n"+Build.DEVICE
                        +"\n"+Build.BOARD
                        +"\n"+Build.BRAND
                        +"\n"+Build.HARDWARE
                        +"\n"+Build.HARDWARE
                        +"\n"+Build.HOST
                        +"\n"+Build.MANUFACTURER
                        +"\n"+Build.MODEL
                        +"\n"+Build.TYPE
                        +"\n"+Build.VERSION.CODENAME;
                log.debug("Device info=%s", build);
                helper.setDeviceName(server.getName() + "-Dominos");
                helper.startGroup(); // make sure we are the group owner
                dominos.stopGameThread();
                dominos.initGame(maxPips, maxPoints, 0);
                // now populate the game with remote players so that when they connect we can send them
                // the game state right away.
                Player [] players = new Player[numPlayers];
                players[0] = user;
                for (int i=1; i<players.length; i++) {
                    players[i] = new MPPlayerRemote(i, dominos, DominosActivity.this);
                }
                dominos.setPlayers(players);
                dominos.startNewGame();
            }

            @Override
            protected void onDone() {
                showWaitingForPlayersDialog();
            }

            @Override
            protected void onError(Exception e) {
                newDialogBuilder().setTitle("Error").setMessage("Failed to start server\n" + e.getLocalizedMessage()).setNegativeButton("Ok", new DialogInterface.OnClickListener() {
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

    void killGame() {
        try {
            server.stop();
            server.removeListener(connectionListener);
        } catch (Exception e) {
        }
        try {
            client.disconnect();
        } catch (Exception e) {
        }
        try {
            helper.disconnect();
        } catch (Exception e) {
        }
        try {
            helper.destroy();
        } catch (Exception e) {
        }
        dominos.stopGameThread();
        dominos.clear();
    }

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
        newDialogBuilder().setTitle("Waiting for players")
                .setView(lvPlayers)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
                    currentDialog.dismiss();
                } else {
                    int num = maxClients - server.getNumConnectedClients();
                    server.broadcastMessage("Waiting for " + num + " more players");
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
        final List<WifiP2pDevice> devices = new ArrayList<>();
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
                    WifiP2pDevice device = devices.get(position);
                    v.setTag(device);
                    TextView tvPeer = (TextView)v.findViewById(R.id.tvPeer);
                    tvPeer.setText(device.deviceName + " " + WifiP2pHelper.statusToString(device.status));
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
            protected void onPeersAvailable(Collection<WifiP2pDevice> peers) {
                synchronized (devices) {
                    devices.clear();
                    for (WifiP2pDevice p : peers) {
                        if (p.isGroupOwner())
                            devices.add(p); // we only want to connect to dominos servers
                    }
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
                currentDialog.dismiss();
                new SpinnerTask() {
                    @Override
                    protected void doIt() throws Exception {
                        stopPeerDiscovery();
                        user.connect(client, DominosActivity.this, dominos);
                        client.connect(info.groupOwnerAddress, PORT);
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
                        newDialogBuilder().setTitle("Error")
                                .setMessage("Failed to connect " + e.getMessage())
                                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
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
        };

        lvHost.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice d = (WifiP2pDevice)view.getTag();
                new SpinnerTask() {

                    @Override
                    protected Dialog showSpinner() {
                        return newDialogBuilder().setTitle("Connecting")
                                .setMessage("Please wait while your connect request is accepted")
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
                            Toast.makeText(DominosActivity.this, "Connection SUCCESS!", Toast.LENGTH_LONG).show();
                        } else if (!isCancelled()) {
                            newDialogBuilder().setTitle("Error")
                                    .setMessage("Failed to connect to host")
                                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
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
                newDialogBuilder().setMessage("Hosts")
                        .setView(lvHost)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                helper.destroy();
                                showNewGameDialog();
                            }
                        }).setCancelable(false).show();

            }
        }.run();

    }

    void showWaitingForPlayersDialogClient(final boolean cancelable) {
        final AlertDialog d = newDialogBuilder().setTitle("Waiting")
                .setTitle("Waiting for more players")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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

    final static String VERSION = "DOMINOS.1.0";
    final static int PORT = 16342;
    final static int CLIENT_READ_TIMEOUT = 15000;
    final static String NAME= Build.BRAND.toUpperCase()+ Build.MODEL;

    GameClient client = new GameClient(NAME, VERSION, cypher);

    final MPPlayerUser user = new MPPlayerUser();

    AlertDialog currentDialog = null;

    AlertDialog.Builder newDialogBuilder() {
        final AlertDialog previous = currentDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme) {
            @Override
            public AlertDialog show() {
                if (currentDialog != null) {
                    currentDialog.dismiss();
                }
                return currentDialog = super.show();
            }
        }.setCancelable(false);
        if (currentDialog != null && currentDialog.isShowing()) {
            builder.setNeutralButton("Back", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    currentDialog = previous;
                    previous.show();
                }
            });
        }
        return builder;
    }

    private GameServer server = new GameServer(NAME, PORT, CLIENT_READ_TIMEOUT, VERSION, cypher, 3);

    /** include:
     *  numPlayers(int)
     *  playerNum(int) of the client
     *  dominos(serialixed)
     */
    final static GameCommandType SVR_TO_CL_INIT_GAME   = new GameCommandType("SVR_INIT_GAME");

    final static GameCommandType CL_TO_SVR_FORFEIT      = new GameCommandType("CL_FORFEIT");

    /**
     * Just pass the serialized dominos
     */
    final static GameCommandType SVR_TO_CL_INIT_ROUND  = new GameCommandType("SVR_INIT_ROUND");

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
        newDialogBuilder().setTitle("New Multi Player Game")
                .setView(v)
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
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

                        if (firstGame)
                            showHostMultiplayerDialog(numPlayers, maxPoints, maxPips);
                        else {
                            dominos.initGame(maxPips, maxPoints, 0);
                            dominos.startNewGame();
                            server.broadcastMessage("Starting a new game!");
                            server.broadcastCommand(new GameCommand(SVR_TO_CL_INIT_ROUND)
                                    .setArg("dominos", dominos.toString()));
                            dominos.startGameThread();
                        }
                    }


                }).setNegativeButton("Quit", new DialogInterface.OnClickListener() {
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
        server.broadcastMessage("Starting a new game!");
        server.broadcastCommand(new GameCommand(SVR_TO_CL_INIT_ROUND)
                .setArg("dominos", dominos.toString()));
        dominos.startGameThread();
    }

    void showNewSinglePlayerSetupDialog() {
        final View v = View.inflate(this, R.layout.game_setup_dialog, null);
        final RadioGroup rgNumPlayers = (RadioGroup)v.findViewById(R.id.rgNumPlayers);
        final RadioGroup rgDifficulty = (RadioGroup)v.findViewById(R.id.rgDifficulty);
        final RadioGroup rgTiles      = (RadioGroup)v.findViewById(R.id.rgTiles);
        final RadioGroup rgMaxPoints  = (RadioGroup)v.findViewById(R.id.rgMaxPoints);
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
        newDialogBuilder().setTitle("New Single Player Game")
                .setView(v)
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
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
