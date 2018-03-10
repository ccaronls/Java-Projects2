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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Executor;

import cc.game.dominos.core.Dominos;
import cc.game.dominos.core.Move;
import cc.game.dominos.core.Player;
import cc.game.dominos.core.PlayerUser;
import cc.game.dominos.core.Tile;
import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.android.WifiP2pHelper;
import cc.lib.game.Utils;
import cc.lib.net.ClientConnection;
import cc.lib.net.ClientForm;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.net.GameCommandType;
import cc.lib.net.GameServer;
import cc.lib.utils.FileUtils;

/**
 * Created by chriscaron on 2/15/18.
 */

public class DominosActivity extends DroidActivity {

    private final static String TAG = DominosActivity.class.getSimpleName();

    enum Mode {
        NONE,
        SINGLE,
        HOST,
        CLIENT
    }

    private Dominos dominos = null;
    private File saveFile=null;
    private Mode mode = Mode.NONE;

    public abstract class SpinnerTask extends AsyncTask<Void, Void, Exception> {
        Dialog spinner;
        @Override
        protected final void onPreExecute() {
            spinner = ProgressDialog.show(DominosActivity.this, "", "", true);
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
            protected void onGameOver() {
                getContent().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showNewGameDialog(false);
                    }
                }, 5000);
            }

            @Override
            protected void onNewGameClicked() {
                //showNewGameDialog(true);
                switch (mode) {
                    case HOST:
                        newDialogBuilder().setTitle("Confirm")
                                .setMessage("End multiplayer session?")
                                .setNegativeButton("No", null)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new SpinnerTask() {
                                            @Override
                                            protected void doIt() {
                                                dominos.stopGameThread();
                                                server.stop();
                                                helper.disconnect();
                                                helper.destroy();
                                            }

                                            @Override
                                            protected void onDone() {
                                                mode = Mode.NONE;
                                                showNewGameDialog(false);
                                            }

                                        }.run();
                                    }
                                }).show();
                    case CLIENT:
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
                                                mode = Mode.NONE;
                                                showNewGameDialog(false);
                                            }
                                        }.execute();
                                    }
                                }).show();
                        break;
                    case SINGLE:
                        dominos.stopGameThread();
                    case NONE:
                        showNewGameDialog(true);
                        break;
                }
            }

            @Override
            protected void onPiecePlaced(Player player, Tile pc) {
                super.onPiecePlaced(player, pc);
                if (server.isRunning()) {
                }
            }

            @Override
            protected void onTilePlaced(Player p, Move mv) {
                super.onTilePlaced(p, mv);
            }

            @Override
            protected void onTileFromPool(Player p, Tile pc) {
                super.onTileFromPool(p, pc);
            }

            @Override
            protected void onKnock(Player p) {
                super.onKnock(p);
            }

            @Override
            protected void onEndRound() {
                super.onEndRound();
            }

            @Override
            protected void onPlayerEndRoundPoints(Player p, int pts) {
                super.onPlayerEndRoundPoints(p, pts);
            }

            @Override
            protected void onPlayerPoints(Player p, int pts) {
                super.onPlayerPoints(p, pts);
            }
        };

        try {
            FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
            dominos.loadFromFile(saveFile);
            mode = Mode.SINGLE;
        } catch (FileNotFoundException e) {
            // ignore
        } catch (Exception e) {
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
        if (dominos.getNumPlayers() > 0 && dominos.getWinner() == null)
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
        }

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
                dominos.onClick();
            }
        }, 100);
    }

    private void showNewGameDialog(final boolean cancleable) {

        final View v = View.inflate(this, R.layout.new_game_type_dialog, null);
        AlertDialog.Builder b = newDialogBuilder().setTitle("New Game Type")
                .setView(v).setCancelable(cancleable);

        if (cancleable) {
            b.setNegativeButton("Cancel", null);
        }
        b.show();

        v.findViewById(R.id.bSinglePlayer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewSinglePlayerSetupDialog(cancleable);
            }
        });
        v.findViewById(R.id.bMultiPlayerHost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewMultiplayerPlayerSetupDialog(cancleable);
            }
        });
        v.findViewById(R.id.bMultiPlayerSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchMultiplayerHostsDialog(cancleable);
            }
        });
    }

    WifiP2pHelper helper = null;

    private void showHostMultiplayerDialog(final int numPlayers, final int maxPoints, final int maxPips, final boolean cancelable) {
        new SpinnerTask() {
            @Override
            protected void doIt() throws Exception {
                server.listen();
                mode = Mode.HOST;
                helper = new WifiP2pHelper(DominosActivity.this);
                helper.p2pInitialize();
                helper.startGroup(); // make sure we are the group owner
                dominos.initGame(maxPips, maxPoints, 0);
            }

            @Override
            protected void onDone() {
                showWaitingForPlayersDialog(numPlayers-1, cancelable);
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
                                showNewGameDialog(cancelable);
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
        mode = Mode.NONE;
    }

    private void showWaitingForPlayersDialog(final int maxPlayers, final boolean cancelable) {
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
                                showNewGameDialog(cancelable);
                            }
                        }.run();
                    }
                }).show();
        server.addListener(new GameServer.Listener() {
            @Override
            public synchronized void onConnected(ClientConnection conn) {
                // if enough clients have connected then start the game
                if (server.getNumClients() == maxPlayers) {
                    server.removeListener(this);
                    Player [] players = new Player[1 + server.getNumClients()];
                    players[0] = new PlayerUser();
                    Iterator<ClientConnection> it = server.getConnectionValues().iterator();
                    for (int i=1; i<players.length; i++) {
                        ClientConnection c = it.next();
                        c.sendCommand(new GameCommand(SVR_TO_CL_INIT_GAME)
                                .setArg("numPlayers", maxPlayers)
                                .setArg("maxPips", dominos.getMaxPips())
                                .setArg("maxScore", dominos.getMaxScore())
                                .setArg("playerNum", i)
                        );
                        players[i] = new PlayerRemoteClient(c);
                    }
                    dominos.setPlayers(players);
                    dominos.startGameThread();
                    currentDialog.dismiss();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playersAdapter.notifyDataSetChanged();
                        }
                    });
                    int num = maxPlayers - server.getNumClients();
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

            @Override
            public void onClientCommand(ClientConnection conn, GameCommand command) {
                Log.w(TAG, "unhandled onClientCommand: " + command);
            }

            @Override
            public void onFormSubmited(ClientConnection conn, int id, Map<String, String> params) {
                Log.w(TAG, "unhandled onFormSubmitted: " + id + " " + params);
            }
        });
    }

    private void showSearchMultiplayerHostsDialog(final boolean canceleble) {
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
                    devices.addAll(peers);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo info) {

                new SpinnerTask() {
                    @Override
                    protected void doIt() throws Exception {
                        client.connect(info.groupOwnerAddress, PORT);
                        stopPeerDiscovery();
                        if (!client.isConnected()) {
                            synchronized (client) {
                                client.wait(20*1000);
                            }
                        }
                    }

                    @Override
                    protected void onDone() {
                        synchronized (lvHost) {
                            lvHost.notify();
                        }
                    }
                }.run();
            }
        };

        lvHost.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice d = (WifiP2pDevice)view.getTag();
                newDialogBuilder().setTitle("Connecting")
                        .setMessage("Please wait while your connect request is accepted")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new SpinnerTask() {
                                    @Override
                                    protected void doIt() throws Exception {
                                        helper.cancelConnect();
                                        killGame();
                                        synchronized (lvHost) {
                                            lvHost.notify();
                                        }
                                    }

                                    @Override
                                    protected void onDone() {
                                        showNewGameDialog(canceleble);
                                    }
                                }.run();
                            }
                        }).show();
                new SpinnerTask() {
                    @Override
                    protected void doIt() {
                        helper.connect(d);
                        Utils.waitNoThrow(lvHost, 20*1000);
                    }

                    @Override
                    protected void onDone() {
                        if (client.isConnected()) {
                            showWaitingForPlayersDialogClient(canceleble);
                        } else {
                            newDialogBuilder().setTitle("Error")
                                    .setMessage("Failed to connect to host")
                                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            showNewGameDialog(canceleble);
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
                                showNewGameDialog(canceleble);
                            }
                        }).setCancelable(false).show();

            }
        }.run();

    }

    private void showWaitingForPlayersDialogClient(final boolean cancelable) {
        final AlertDialog d = newDialogBuilder().setTitle("Waiting")
                .setTitle("Waiting for more players")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        client.disconnect();
                        showNewGameDialog(cancelable);
                    }
                }).setCancelable(false).show();
        client.addListener(new GameClient.Listener() {
            @Override
            public void onCommand(GameCommand cmd) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();
                    }
                });
            }

            @Override
            public void onMessage(final String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        d.setMessage(msg);
                    }
                });
            }
        });
    }

    final static String VERSION = "1.0";
    final static int PORT = 16342;

    GameClient client = new GameClient(Build.PRODUCT + Build.MANUFACTURER, VERSION) {

        PlayerUser user = new PlayerUser();

        @Override
        protected void onMessage(final String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(DominosActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        protected void onDisconnected(String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    newDialogBuilder().setTitle("Disconnected")
                            .setMessage("You have been disconnected from the server.")
                            .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    showSearchMultiplayerHostsDialog(false);
                                }
                            }).setCancelable(false).show();
                }
            });
            killGame();
        }

        @Override
        protected void onConnected() {
            synchronized (this) {
                notify();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(DominosActivity.this, "Connected", Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        protected void onCommand(GameCommand cmd) {
            if (cmd.getType() == SVR_TO_CL_REMOVE_TILE) {
                Tile t = new Tile();
                try {
                    t.deserialize(cmd.getArg("tile"));
                    user.removeTile(t.pip1, t.pip2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (cmd.getType() == SVR_TO_CL_SET_SCORE) {
                user.setScore(cmd.getInt("score"));
                dominos.redraw();
            } else if (cmd.getType() == SVR_TO_CL_INIT_GAME) {
                int numPlayers = cmd.getInt("numPlayers");
                int maxPips = cmd.getInt("maxPips");
                int maxScore = cmd.getInt("maxScore");
                int playerNum = cmd.getInt("playerNum");
                Player [] players = new Player[numPlayers];
                int idx = 0;
                for (; idx<playerNum; idx++)
                    players[idx] = new Player();
                players[idx++] = user;
                for ( ; idx<numPlayers; idx++)
                    players[idx] = new Player();
                dominos.initGame(maxPips, maxScore, 0);
                dominos.setPlayers(players);
            }
        }

        @Override
        protected void onForm(ClientForm form) {
            Log.e(TAG, "Unhandled form: " + form);
        }
    };

    private AlertDialog currentDialog = null;

    AlertDialog.Builder newDialogBuilder() {
        return new AlertDialog.Builder(this) {
            @Override
            public AlertDialog show() {
                if (currentDialog != null) {
                    currentDialog.dismiss();
                }
                return currentDialog = super.show();
            }
        };
    }

    private GameServer server = new GameServer(PORT, 10000, VERSION, null, 3);

    private final static GameCommandType SVR_TO_CL_NEW_PEER    = new GameCommandType("SVR_NEW_PEER");
    private final static GameCommandType SVR_TO_CL_CHOOSE_MOVE = new GameCommandType("SVR_CHOOSE_MOVE");
    private final static GameCommandType CL_TO_SVR_MOVE_CHOSEN = new GameCommandType("CL_MOVE_CHOSEN");

    /** include:
           numPlayers(int)
           maxPips(int)
           maxScore(int)
           playerNum(int) of the client
     */
    private final static GameCommandType SVR_TO_CL_INIT_GAME   = new GameCommandType("SVR_INIT_GAME");
    private final static GameCommandType SVR_TO_CL_REMOVE_TILE = new GameCommandType("SVR_REMOVE_TILE");
    private final static GameCommandType SVR_TO_CL_SET_SCORE   = new GameCommandType("SVR_SET_SCORE");
    private final static GameCommandType SVR_TO_CL_EXEC_METHOD = new GameCommandType("SVR_EXEC_METHOD");

    // inctance of players when we are the host
    class PlayerRemoteClient extends Player implements GameServer.Listener {

        PlayerRemoteClient(ClientConnection conn) {
            this.connection = conn;
        }

        private final ClientConnection connection;
        private Move chosen;

        @Override
        public Tile removeTile(int n1, int n2) {
            if (!connection.isConnected())
                return super.removeTile(n1, n2);
            Tile t = super.removeTile(n1, n2);
            if (t != null) {
                connection.sendCommand(new GameCommand(SVR_TO_CL_REMOVE_TILE).setArg("tile", t));
            }
            return t;
        }

        @Override
        public Move chooseMove(Dominos game, List<Move> moves) {
            if (!connection.isConnected())
                return super.chooseMove(game, moves);
            //blocking request for client to tell their move
            connection.sendCommand(new GameCommand(SVR_TO_CL_CHOOSE_MOVE));
            Utils.waitNoThrow(this, 2000);
            return chosen;
        }

        @Override
        public void setScore(int score) {
            if (connection.isConnected()) {
                super.setScore(score);
                connection.sendCommand(new GameCommand(SVR_TO_CL_SET_SCORE).setArg("score", score));
            }
        }

        void process(GameCommand cmd) {
            if (cmd.getType() == CL_TO_SVR_MOVE_CHOSEN) {
                Tile t = new Tile();
                try {
                    Move m = new Move();
                    m.deserialize(cmd.getArg("move"));
                    chosen = new Move();
                    synchronized (this) {
                        notify();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onConnected(ClientConnection conn) {

        }

        @Override
        public void onReconnection(ClientConnection conn) {

        }

        @Override
        public void onClientDisconnected(ClientConnection conn) {
            if (conn == connection) {
                server.removeListener(this);
            }
        }

        @Override
        public void onClientCommand(ClientConnection conn, GameCommand command) {
            if (conn != connection) {

            }
        }

        @Override
        public void onFormSubmited(ClientConnection conn, int id, Map<String, String> params) {

        }
    }
/*
    @Override
    public synchronized void onConnected(ClientConnection conn) {
        if (dominos.isGameRunning()) {
            Log.i(TAG, "Rejecting client connection because already have all players");
            conn.disconnect("Game in Progress");
            return;
        }
    }

    @Override
    public synchronized void onReconnection(ClientConnection conn) {
        Log.i(TAG, "client reconnected");
    }

    @Override
    public synchronized void onClientDisconnected(final ClientConnection conn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DominosActivity.this, "Client " + conn.getName() + " has left", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public synchronized void onClientCommand(ClientConnection conn, GameCommand command) {
        //clients.get(conn).process(command);
    }

    @Override
    public void onFormSubmited(ClientConnection conn, int id, Map<String, String> params) {
        Log.e(TAG, "Unhandled form submission");
    }*/

    private void showNewMultiplayerPlayerSetupDialog(final boolean cancleable) {
        final View v = View.inflate(this, R.layout.game_setup_dialog, null);
        final RadioGroup rgNumPlayers = (RadioGroup)v.findViewById(R.id.rgNumPlayers);
        final RadioGroup rgDifficulty = (RadioGroup)v.findViewById(R.id.rgDifficulty);
        final RadioGroup rgTiles      = (RadioGroup)v.findViewById(R.id.rgTiles);
        final RadioGroup rgMaxPoints  = (RadioGroup)v.findViewById(R.id.rgMaxPoints);
        rgDifficulty.setVisibility(View.GONE);
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
                        showHostMultiplayerDialog(numPlayers, maxPoints, maxPips, cancleable);

                    }


                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                showNewGameDialog(cancleable);
            }
        }).show();
    }


    private void showNewSinglePlayerSetupDialog(final boolean cancleable) {
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
                        dominos.initGame(maxPips, maxPoints, difficulty);
                        dominos.setNumPlayers(numPlayers);
                        dominos.startNewGame();
                        dominos.startGameThread();
                        mode = Mode.SINGLE;

                    }


                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                showNewGameDialog(cancleable);
            }
        }).show();
    }


}
