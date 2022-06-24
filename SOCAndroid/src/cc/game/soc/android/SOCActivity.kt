package cc.game.soc.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;

import cc.game.soc.core.AITuning;
import cc.game.soc.core.Board;
import cc.game.soc.core.BuildableType;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.Rules;
import cc.game.soc.core.SOC;
import cc.game.soc.ui.MenuItem;
import cc.game.soc.ui.NetCommon;
import cc.game.soc.ui.RenderConstants;
import cc.game.soc.ui.UIBarbarianRenderer;
import cc.game.soc.ui.UIBoardRenderer;
import cc.game.soc.ui.UIConsoleRenderer;
import cc.game.soc.ui.UIDiceRenderer;
import cc.game.soc.ui.UIEventCardRenderer;
import cc.game.soc.ui.UIPlayer;
import cc.game.soc.ui.UIPlayerRenderer;
import cc.game.soc.ui.UIPlayerUser;
import cc.game.soc.ui.UISOC;
import cc.lib.android.ArrayListAdapter;
import cc.lib.android.CCActivityBase;
import cc.lib.android.CCNumberPicker;
import cc.lib.android.EmailHelper;
import cc.lib.android.SpinnerTask;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.net.GameServer;
import cc.lib.utils.FileUtils;

/**
 * Created by chriscaron on 2/15/18.
 */

public class SOCActivity extends CCActivityBase implements MenuItem.Action, View.OnClickListener, GameServer.Listener, GameClient.Listener {

    UISOC soc = null;
    File rulesFile;
    File gameFile;
    View content;

    SOCView<UIBarbarianRenderer> vBarbarian;
    SOCView<UIEventCardRenderer> vEvent;
    SOCView<UIBoardRenderer> vBoard;
    SOCView<UIDiceRenderer> vDice;
    SOCView<UIPlayerRenderer> [] vPlayers;
    SOCView<UIConsoleRenderer> vConsole;
    ListView lvMenu;
    ScrollView svPlayers;
    TextView tvHelpText;

    Stack<Dialog> dialogStack = new Stack<>();
    int helpItem = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (cc.game.soc.android.BuildConfig.DEBUG)
            dumpAssets();

        RenderConstants.textMargin = getResources().getDimension(R.dimen.margin);
        RenderConstants.textSizeBig = getResources().getDimension(R.dimen.text_big);
        RenderConstants.textSizeSmall = getResources().getDimension(R.dimen.text_sm);
        RenderConstants.thickLineThickness = getResources().getDimension(R.dimen.line_thick);
        RenderConstants.thinLineThickness = getResources().getDimension(R.dimen.line_thin);

        content = View.inflate(this, R.layout.soc_activity, null);
        setContentView(content);
        vBarbarian = (SOCView) findViewById(R.id.soc_barbarian);
        vEvent = (SOCView) findViewById(R.id.soc_event_cards);
        vBoard = (SOCView) findViewById(R.id.soc_board);
        vDice = (SOCView) findViewById(R.id.soc_dice);
        vPlayers = new SOCView[] {
                (SOCView)findViewById(R.id.soc_player_1),
                (SOCView)findViewById(R.id.soc_player_2),
                (SOCView)findViewById(R.id.soc_player_3),
                (SOCView)findViewById(R.id.soc_player_4),
                (SOCView)findViewById(R.id.soc_player_5),
                (SOCView)findViewById(R.id.soc_player_6),
        };

        if (vPlayers.length != SOC.MAX_PLAYERS) {
            throw new AssertionError();
        }

        lvMenu = (ListView) findViewById(R.id.soc_menu_list);
        vConsole = (SOCView) findViewById(R.id.soc_console);
        vConsole.renderer.setMinVisibleLines(5);
        svPlayers = (ScrollView)findViewById(R.id.svPlayers);
        vDice.renderer.initImages(R.drawable.dicesideship2, R.drawable.dicesidecity_red2, R.drawable.dicesidecity_green2, R.drawable.dicesidecity_blue2);
        tvHelpText = (TextView)findViewById(R.id.tvHelpText);

        QUIT         = new MenuItem(getString(R.string.menu_item_quit),      getString(R.string.menu_item_quit_help), this);
        BUILDABLES   = new MenuItem(getString(R.string.menu_item_buildables), getString(R.string.menu_item_buildables_help), this);
        RULES        = new MenuItem(getString(R.string.menu_item_rules),     getString(R.string.menu_item_rules_help), this);
        START        = new MenuItem(getString(R.string.menu_item_start),     getString(R.string.menu_item_start_help), this);
        CONSOLE      = new MenuItem(getString(R.string.menu_item_console), getString(R.string.menu_item_console_help), this);

        SINGLE_PLAYER= new MenuItem(getString(R.string.menu_item_sp), null, this);
        MULTI_PLAYER = new MenuItem(getString(R.string.menu_item_mp), null, this);
        RESUME       = new MenuItem(getString(R.string.menu_item_resume), null, this);
        LOADSAVED    = new MenuItem("Load Saved", null, this);
        BOARDS       = new MenuItem(getString(R.string.menu_item_boards), null, this);
        SCENARIOS    = new MenuItem(getString(R.string.menu_item_scenarios), null, this);

        final ArrayList<Object[]> menu = new ArrayList<>();
        final BaseAdapter adapter = new ArrayListAdapter<Object[]>(this, menu, R.layout.menu_list_item) {

            @Override
            protected void initItem(View v, final int position, Object[] item) {
                final MenuItem mi = (MenuItem) item[0];
                final String title = (String) item[1];
                final String helpText = (String) item[2];
                final Object extra = item[3];

                View vDivider = v.findViewById(R.id.ivDivider);
                TextView tvTitle = (TextView) v.findViewById(R.id.tvTitle);
                final TextView tvHelp = (TextView) v.findViewById(R.id.tvHelp);
                View bAction = v.findViewById(R.id.bAction);
                View content= v.findViewById(R.id.layoutContent);

                if (mi == DIVIDER) {
                    vDivider.setVisibility(View.VISIBLE);
                    content.setVisibility(View.GONE);
                    tvHelp.setVisibility(View.GONE);
                } else {
                    vDivider.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                    tvTitle.setText(title);
                    tvHelp.setText(helpText);
                    bAction.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mi.action.onAction(mi, extra);
                        }
                    });
                    tvHelp.setVisibility(helpItem == position ? View.VISIBLE : View.GONE);

                    if (!Utils.isEmpty(helpText)) {
                        v.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (helpItem == position)
                                    helpItem = -1;
                                else
                                    helpItem = position;
                                notifyDataSetChanged();
                            }
                        });

                    } else {
                        v.setOnClickListener(null);
                    }
                }
            }
        };
        lvMenu.setAdapter(adapter);

        gameFile = new File(getFilesDir(), "save.txt");
        rulesFile = new File(getFilesDir(), "rules.txt");

        final UIPlayerRenderer[] players = new UIPlayerRenderer[SOC.MAX_PLAYERS];

        for (int i=0; i<players.length; i++) {
            players[i] = this.vPlayers[i].renderer;
        }

        soc = new UISOC(players, vBoard.renderer, vDice.renderer, vConsole.renderer, vEvent.renderer, vBarbarian.renderer) {
            @Override
            protected void addMenuItem(MenuItem item, String title, String helpText, Object extra) {
                menu.add(new Object[]{
                        item, title, helpText, extra
                });
            }

            @Override
            public void completeMenu() {
                addMenuItem(DIVIDER);
                super.completeMenu();
//                    addMenuItem(CONSOLE);
                addMenuItem(BUILDABLES);
                addMenuItem(RULES);
                addMenuItem(QUIT);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void clearMenu() {
                helpItem = -1;
                menu.clear();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void redraw() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (soc.getCurPlayerNum() > 0) {
                            svPlayers.smoothScrollTo(0, vPlayers[soc.getCurPlayerNum() - 1].getTop());
                            //content.invalidate();
                            //vConsole.requestLayout();
                            vConsole.redraw();
                            for (final SOCView v : vPlayers) {
                                //v.requestLayout();
                                v.redraw();
                            }
                            vBarbarian.redraw();
                            tvHelpText.setText(getHelpText());
                            vBoard.redraw();
                            vDice.redraw();
                        }
                    }
                });
            }

            @Override
            protected void showOkPopup(final String title, final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        newDialog(true).setTitle(title).setMessage(message).setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                synchronized (soc) {
                                    soc.notify();
                                }
                            }
                        }).show();
                    }
                });
                Utils.waitNoThrow(this, -1);
            }

            @Override
            protected String showChoicePopup(final String title, final List<String> choices) {
                final String [] choice = new String[1];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final String [] items = choices.toArray(new String[choices.size()]);
                        newDialog(false).setTitle(title).setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = items[which];
                            }
                        });
                    }
                });
                Utils.waitNoThrow(this, -1);
                return choice[0];
            }

            @Override
            protected String getServerName() {
                return Build.BRAND + "." + Build.PRODUCT;
            }

            @Override
            protected void onShouldSaveGame() {
                trySaveToFile(gameFile);
                if (BuildConfig.DEBUG) try {
                    FileUtils.backupFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/socsave.txt", 20);
                    FileUtils.copyFile(gameFile, new File(Environment.getExternalStorageDirectory(), "socsave.txt"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            protected void onGameOver(int winnerNum) {
                super.onGameOver(winnerNum);
                clearMenu();
                addMenuItem(QUIT);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            protected void onRunError(final Throwable e) {
                super.onRunError(e);
                if (BuildConfig.DEBUG) {
                    // write crashes to sdcard for eval later
                    try {
                        File tmpDir = File.createTempFile("crash", "", Environment.getExternalStorageDirectory());
                        tmpDir.delete();
                        tmpDir.mkdir();
                        File crash = new File(tmpDir, "stack.txt");
                        FileUtils.stringToFile(e.toString(), crash);
                        FileUtils.copyFile(gameFile, tmpDir);
                    } catch (Exception ee) {
                    }
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        showStartMenu();
                        newDialog(true).setTitle("Error").setMessage("An error occurred:\n" + e.getClass().getSimpleName() + "  " + e.getMessage())
                                .setNegativeButton("Ignore", null)
                                .setPositiveButton("Report", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            File saveFile = new File(getCacheDir(),"gameError.txt");
                                            if (gameFile.exists()) {
                                                FileUtils.copyFile(gameFile, saveFile);
                                            } else {
                                                saveToFile(saveFile);
                                            }
                                            EmailHelper.sendEmail(SOCActivity.this, saveFile, "sebisoftware@gmail.com", "SOC Crash log", Utils.toString(e.getStackTrace()));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).show();
                    }
                });
            }

            @Override
            public void printinfo(int playerNum, String txt) {
                vConsole.scrollTo(0, 0);
                super.printinfo(playerNum, txt);
            }

            @Override
            public void cancel() {
                if (user.client.isConnected()) {
                    user.client.cancelRemote();
                }
                super.cancel();
            }
        };
        soc.setBoard(vBoard.getRenderer().getBoard());
        Rules rules = new Rules();
        if (rules.tryLoadFromFile(rulesFile)) {
            soc.setRules(rules);
        }

        final Properties aiTuning = new Properties();
        try {
            InputStream in = getAssets().open("aituning.properties");
            try {
                aiTuning.load(in);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        AITuning.setInstance(new AITuning() {

            @Override
            public double getScalingFactor(String property) {
                if (!aiTuning.containsKey(property)) {
                    aiTuning.setProperty(property, "1.0");
                    return 1.0;
                }

                return Double.valueOf(aiTuning.getProperty(property));
            }
        });

    }



    final UIPlayerUser user = new UIPlayerUser();

    final MenuItem DIVIDER = new MenuItem(null, null, null);
    MenuItem QUIT;
    MenuItem BUILDABLES;
    MenuItem RULES;
    MenuItem START;
    MenuItem CONSOLE;

    MenuItem SINGLE_PLAYER;
    MenuItem MULTI_PLAYER;
    MenuItem RESUME;
    MenuItem LOADSAVED;
    MenuItem BOARDS;
    MenuItem SCENARIOS;

    void quitGame() {
        soc.server.stop();
        soc.clear();
        vBoard.renderer.setPickHandler(null);
        soc.stopRunning();
        user.client.disconnect("player quit");
        soc.clearMenu();
        showStartMenu();
    }

    @Override
    public void onAction(MenuItem item, Object extra) {
        if (item == QUIT) {
            newDialog(true).setTitle("Confirm").setMessage("Ae you sure you want to quit the game?")
                    .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            quitGame();
                        }
                    }).show();
        } else if (item == BUILDABLES) {
            showBuildablesDialog();
        } else if (item == RULES) {
            showRulesDialog();
        } else if (item == START) {
            soc.clearMenu();
            soc.getBoard().assignRandom();
            soc.startGameThread();
            vBoard.renderer.clearCached();
        } else if (item == CONSOLE) {
            //showConsole();
        } else if (item == SINGLE_PLAYER) {
            showSinglePlayerDialog();
        } else if (item == MULTI_PLAYER) {
            showMultiPlayerDialog();
        } else if (item == RESUME) {
            try {
                if (cc.game.soc.android.BuildConfig.DEBUG) {
                    //FileUtils.copyFile(gameFile, Environment.getExternalStorageDirectory());
                    // User can put a fixed file onto sdcard to overwrite the current save file.
                    // It will be deleted when done
                    File fixed = new File(Environment.getExternalStorageDirectory(), "fixed.txt");
                    if (fixed.exists()) {
                        FileUtils.copyFile(fixed, gameFile);
                        fixed.delete();
                    }
                }

                new SpinnerTask<String>(this) {

                    @Override
                    protected void doIt(String... args) throws Exception {
                        soc.loadFromFile(gameFile);
                    }

                    @Override
                    protected void onSuccess() {
                        initGame();
                    }

                }.execute();
            } catch (Exception e) {
                if (extra != null)
                    ((View) extra).setEnabled(false);
                soc.clear();
                showError(e);
            }
        } else if (item == LOADSAVED) {
            final String [] files = Environment.getExternalStorageDirectory().list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".txt");
                }
            });
            newDialog(true).setTitle("Load saved").setItems(files, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (soc.tryLoadFromFile(new File(Environment.getExternalStorageDirectory(), files[which]))) {
                        initGame();
                    } else {
                        Toast.makeText(SOCActivity.this, "Problem loading '" + files[which], Toast.LENGTH_LONG).show();
                    }
                }
            }).setNegativeButton("Cancel", null).show();
        } else if (item == BOARDS) {
            showBoardsDialog();
        } else if (item == SCENARIOS) {
            showScenariosDialog();
        }
    }

    void showError(Exception e) {
        newDialog(true).setTitle("ERROR").setMessage("AN error occured: " + e.getClass().getSimpleName() + " " + e.getMessage()).show();
    }

    void showMultiPlayerDialog() {
        String [] mpItems = {
                "Host",
                "Join",
                "Resume"
        };
        newDialog(true).setTitle("MULTIPLAYER")
                .setItems(mpItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // host
                                showHostMultiPlayerDialog();
                                break;
                            case 1: // join
                                showJoinMultiPlayerDialog();
                                break;
                            case 2:
                                showResumeMultiPlayerDialog();
                                break;
                        }
                    }
                }).show();
    }

    void showJoinMultiPlayerDialog() {
        /*
        user.client.register(NetCommon.SOC_ID, UISOC.getInstance());
        user.client.register(NetCommon.USER_ID, user);

        mpGame = new MPGameManager(this, user.client, NetCommon.PORT, "soc") {
            @Override
            public void onAllClientsJoined() {
//                initGame();
            }
        };
        mpGame.showJoinGameDialog();
        user.client.addListener(this);*/
    }

    @Override
    public void onCommand(GameCommand cmd) {
        try {

            if (cmd.getType().equals(NetCommon.SVR_TO_CL_INIT)) {
                final UISOC soc = UISOC.getInstance();
                soc.clear();
                int num = cmd.getInt("numPlayers");
                int playerNum = cmd.getInt("playerNum");
                for (int i=0; i<playerNum-1; i++) {
                    soc.addPlayer(new UIPlayer());
                }
                soc.addPlayer(user);
                for (int i=playerNum; i<num; i++) {
                    soc.addPlayer(new UIPlayer());
                }

                cmd.parseReflector("soc", soc);
                runOnUiThread(new Runnable() {
                    public void run() {
                        initGame();
                        soc.redraw();
                    }
                });
            } else if (cmd.getType().equals(NetCommon.SVR_TO_CL_UPDATE)) {
                UISOC.getInstance().merge(cmd.getString("diff"));
                UISOC.getInstance().refreshComponents();
                UISOC.getInstance().redraw();
            }

            soc.clearMenu();
            soc.completeMenu();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onMessage(String msg) {
        log.info("onMessage: %s", msg);
    }

    @Override
    public void onDisconnected(String reason, boolean serverInitiated) {
        log.info("onDisconnected: %s", reason);
    }

    @Override
    public void onConnected() {
        log.info("onConnected");
    }

    interface Callback<T> {
        void onComplete(T argument);
    }

    final String [] colorStrings = {
            "Red", "Green", "Blue", "Yellow", "Orange", "Pink"
    };
    final GColor[]  colors = {
            GColor.RED, GColor.GREEN, GColor.BLUE.lightened(.2f), GColor.YELLOW, GColor.ORANGE, GColor.PINK
    };

    void showChooseColorDialog(final Callback<Integer> andThen) {
        newDialog(true).setTitle("Pick Color").setItems(colorStrings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                andThen.onComplete(which);
            }
        }).show();

    }

    void showResumeMultiPlayerDialog() {
        new SpinnerTask<String>(this) {

            @Override
            protected void doIt(String... args) throws Exception {
                soc.loadFromFile(gameFile);
            }

            @Override
            protected void onSuccess() {
                soc.redraw();
                /*
                mpGame = new MPGameManager(SOCActivity.this, soc.server, soc.getNumPlayers()-1) {
                    @Override
                    public void onAllClientsJoined() {
                        log.info("All client joined");
                        initGame();
                    }
                };
                mpGame.startHostMultiplayer();*/
            }

        }.execute();
    }

    void showHostMultiPlayerDialog() {
        showChooseNumPlayersDialog(new Callback<Integer>() {
            @Override
            public void onComplete(final Integer numPlayers) {
                showChooseColorDialog(new Callback<Integer>() {
                    @Override
                    public void onComplete(Integer which) {
                        user.setColor(colors[which]);
                        soc.clear();
                        soc.addPlayer(user);
                        while (soc.getNumPlayers() < numPlayers) {
                            UIPlayer p = new UIPlayer();
                            p.setColor(soc.getAvailableColors().entrySet().iterator().next().getValue());
                            soc.addPlayer(p);
                        }
                        /*
                        mpGame = new MPGameManager(SOCActivity.this, soc.server, numPlayers-1) {
                            @Override
                            public void onAllClientsJoined() {
                                log.info("All client joined");
                                soc.initGame();
                                initGame();
                            }
                        };
                        mpGame.startHostMultiplayer();
                         */
                    }
                });
            }
        });

    }

    void showChooseNumPlayersDialog(final Callback<Integer> andThen) {
        final int minPlayers = soc.getRules().getMinPlayers();
        final int maxPlayers = soc.getRules().getMaxPlayers();

        if (minPlayers >=  maxPlayers) {
            andThen.onComplete(minPlayers);
        } else {
            final String [] items = new String[maxPlayers-minPlayers];
            int index = 0;
            for (int i=minPlayers; i<maxPlayers; i++) {
                items[index++] = String.valueOf(i);
            }
            newDialog(true).setTitle("Num Players").setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final int numPlayers = which + minPlayers;
                    andThen.onComplete(numPlayers);
                }
            }).setNegativeButton("Cancel", null).show();
        }
    }

    void showSinglePlayerDialog() {
        showChooseNumPlayersDialog(new Callback<Integer>() {
            @Override
            public void onComplete(final Integer numPlayers) {
                showChooseColorDialog(new Callback<Integer>() {
                    @Override
                    public void onComplete(Integer which) {
                        user.setColor(colors[which]);
                        soc.clear();
                        soc.addPlayer(user);
                        for (int i = 1; i < numPlayers; i++) {
                            int nextColor = (which + i) % colors.length;
                            soc.addPlayer(new UIPlayer(colors[nextColor]));
                        }
                        soc.initGame();
                        initGame();
                    }
                });
            }
        });
    }

    void showScenariosDialog() {
        try {
            final String [] scenarios = getAssets().list("scenarios");
            newDialog(true).setTitle("Load Scenario").setItems(scenarios, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, final int which) {

                    new SpinnerTask<String>(SOCActivity.this) {
                        @Override
                        protected void doIt(String... args) throws Exception {
                            InputStream in = getAssets().open("scenarios/" + scenarios[which]);
                            try {
                                SOC scenario = new SOC();
                                scenario.deserialize(in);
                                soc.copyFrom(scenario);
                            } finally {
                                in.close();
                            }
                        }

                        @Override
                        protected void onSuccess() {
                            vBoard.invalidate();
                            vBoard.renderer.clearCached();
                        }
                    }.execute();
                }
            }).setNegativeButton("Cancel", null).show();
        } catch (Exception e) {
            log.error(e);
        }
    }

    void showBoardsDialog() {
        try {
            final String[] boards = getAssets().list("boards");
            newDialog(true).setTitle("Load Board").setItems(boards, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {
                        InputStream in = getAssets().open("boards/" + boards[which]);
                        try {
                            Board b = new Board();
                            b.deserialize(in);
                            soc.setBoard(b);
                            vBoard.renderer.clearCached();
                            vBoard.invalidate();
                        } finally {
                            in.close();
                        }
                    } catch (IOException e) {
                        showError(e);
                    }
                }
            }).setNegativeButton("Cancel", null).show();
        } catch (Exception e) {
            log.error(e);
        }
    }

    void showStartMenu() {
        vBarbarian.setVisibility(View.GONE);
        vEvent.setVisibility(View.GONE);
        vDice.setVisibility(View.GONE);
        svPlayers.setVisibility(View.GONE);
        tvHelpText.setVisibility(View.GONE);
        lvMenu.setVisibility(View.VISIBLE);
        soc.clearMenu();
        soc.addMenuItem(SINGLE_PLAYER);
        soc.addMenuItem(MULTI_PLAYER);
        soc.addMenuItem(RESUME);
        if (cc.game.soc.android.BuildConfig.DEBUG) {
            soc.addMenuItem(LOADSAVED);
        }
        if (cc.game.soc.android.BuildConfig.DEBUG)
            soc.addMenuItem(BOARDS);
        soc.addMenuItem(SCENARIOS);
        soc.addMenuItem(RULES);

        vBoard.renderer.clearCached();
    }

    void initGame() {

        int index = 1;
        svPlayers.setVisibility(View.VISIBLE);
        for (int i = 1; i< vPlayers.length; i++) {
            vPlayers[i].setVisibility(View.GONE);
        }
        for (int i=1; i<=soc.getNumPlayers(); i++) {
            if (soc.getPlayerByPlayerNum(i) instanceof UIPlayerUser) {
                vPlayers[0].renderer.setPlayer(i);
            } else {
                vPlayers[index].setVisibility(View.VISIBLE);
                vPlayers[index++].renderer.setPlayer(i);
            }
        }

        soc.clearMenu();
        if (user.client.isConnected()) {
            soc.clearMenu();
            vBoard.renderer.clearCached();
        } else
            soc.addMenuItem(START);
        soc.completeMenu();
        vBarbarian.setVisibility(soc.getRules().isEnableCitiesAndKnightsExpansion() ? View.VISIBLE  : View.GONE);
        if (soc.getRules().isEnableEventCards()) {
            vEvent.setVisibility(View.VISIBLE);
            vDice.setVisibility(View.GONE);
        } else {
            vEvent.setVisibility(View.GONE);
            vDice.setVisibility(View.VISIBLE);
        }

        clearDialogs();
        vBoard.renderer.clearCached();
        tvHelpText.setVisibility(View.VISIBLE);
    }

    void showBuildablesDialog() {
        Vector<String> columnNames = new Vector<String>();
        columnNames.add("Buildable");
        for (ResourceType r : ResourceType.values()) {
            columnNames.add(" " + r.getName() + " ");

        }
        Vector<Vector<String>> rowData = new Vector<>();
        for (BuildableType b : BuildableType.values()) {
            if (b.isAvailable(soc)) {
                Vector<String> row = new Vector<>();
                row.add(b.name());
                for (ResourceType r : ResourceType.values())
                    row.add(String.valueOf(b.getCost(r)));
                rowData.add(row);
            }
        }

        TableLayout table = new TableLayout(this);
        TableRow header = new TableRow(this);
        TableLayout.LayoutParams params = new TableLayout.LayoutParams();
        params.width = TableLayout.LayoutParams.WRAP_CONTENT;
        //params.rightMargin = (int)getResources().getDimension(R.dimen.margin);

        for (String s : columnNames) {
            TextView t = new TextView(this);
            t.setText(s);
            header.addView(t);
        }

        table.addView(header);
        for (Vector<String> r : rowData) {
            int gravity = Gravity.LEFT;
            TableRow row = new TableRow(this);
            for (String s : r) {
                TextView t = new TextView(this);
                t.setText(s);
                t.setGravity(gravity);
                gravity = Gravity.CENTER;
                row.addView(t);
            }
            table.addView(row);
        }
        newDialog(true).setTitle("Buildables").setView(table).setNegativeButton("Ok", null).show();
    }

    class RuleItem implements Comparable<RuleItem> {
        final Rules.Variation var;
        final int min, max;
        final String stringId;
        final int order;
        final Field field;

        public RuleItem(Rules.Variation var) {
            this.var = var;
            this.min = max = 0;
            field = null;
            this.stringId = var.stringId;
            this.order = 0;
        }

        public RuleItem(Rules.Rule rule, Field field) {
            this.var = rule.variation();
            this.min = rule.minValue();
            this.max = rule.maxValue();
            this.field = field;
            this.stringId = rule.stringId();
            this.order = rule.order();
        }

        @Override
        public int compareTo(RuleItem o) {
            if (var != o.var)
                return var.compareTo(o.var);
            if (order != o.order)
                return order - o.order;
            if (field == null)
                return -1;
            if (o.field == null)
                return 1;
            return field.getName().compareTo(o.field.getName());
        }
    }

    class RulesAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

        final boolean canEdit;
        final Rules rules;
        final List<RuleItem> rulesList = new ArrayList<>();

        RulesAdapter(Rules rules, boolean canEdit) {
            this.rules = rules;
            this.canEdit = canEdit;
            for (Rules.Variation v : Rules.Variation.values()) {
                if (!canEdit) {
                    if (v == Rules.Variation.SEAFARERS && !rules.isEnableSeafarersExpansion())
                        continue;
                    if (v == Rules.Variation.CAK && !rules.isEnableCitiesAndKnightsExpansion())
                        continue;
                }
                rulesList.add(new RuleItem(v));
            }
            for (Field f : Rules.class.getDeclaredFields()) {
                Annotation[] anno = f.getAnnotations();
                for (Annotation a : anno) {
                    if (a.annotationType().equals(Rules.Rule.class)) {
                        final Rules.Rule ruleVar = (Rules.Rule) a;
                        if (!canEdit) {
                            if (ruleVar.variation() == Rules.Variation.SEAFARERS && !rules.isEnableSeafarersExpansion())
                                continue;
                            if (ruleVar.variation() == Rules.Variation.CAK && !rules.isEnableCitiesAndKnightsExpansion())
                                continue;
                        }

                        f.setAccessible(true);
                        rulesList.add(new RuleItem(ruleVar, f));
                    }
                }
            }
            Collections.sort(rulesList);
        }

        @Override
        public int getCount() {
            return rulesList.size();
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
                v = View.inflate(SOCActivity.this, R.layout.rules_list_item, null);
            }
            TextView tvHeader  = (TextView)v.findViewById(R.id.tvHeader);
            //TextView tvName    = (TextView)v.findViewById(R.id.tvName);
            TextView tvDesc    = (TextView)v.findViewById(R.id.tvDescription);
            CompoundButton cb  = (CompoundButton)v.findViewById(R.id.cbEnabled);
            final Button bEdit = (Button)v.findViewById(R.id.bEdit);

            try {
                final RuleItem item = rulesList.get(position);
                if (item.field == null) {
                    // header
                    tvHeader.setVisibility(View.VISIBLE);
                    //tvName.setVisibility(View.GONE);
                    tvDesc.setVisibility(View.GONE);
                    cb.setVisibility(View.GONE);
                    bEdit.setVisibility(View.GONE);
                    tvHeader.setText(item.stringId);
                } else if (item.field.getType().equals(boolean.class)) {
                    // checkbox
                    tvHeader.setVisibility(View.GONE);
                    //tvName.setVisibility(View.GONE);
                    tvDesc.setVisibility(View.VISIBLE);
                    cb.setVisibility(View.VISIBLE);
                    bEdit.setVisibility(View.GONE);
                    //tvName.setText(item.field.getName());
                    tvDesc.setText(item.stringId);
                    cb.setOnCheckedChangeListener(null);
                    cb.setChecked(item.field.getBoolean(rules));
                    cb.setEnabled(canEdit);
                    cb.setTag(item);
                    cb.setOnCheckedChangeListener(this);
                } else if (item.field.getType().equals(int.class)) {
                    // numberpicker
                    tvHeader.setVisibility(View.GONE);
                    //tvName.setVisibility(View.GONE);
                    tvDesc.setVisibility(View.VISIBLE);
                    cb.setVisibility(View.GONE);
                    bEdit.setVisibility(View.VISIBLE);
                    //tvName.setText(item.field.getName());
                    tvDesc.setText(item.stringId);
                    final int value = item.field.getInt(rules);
                    bEdit.setText(String.valueOf(value));
                    bEdit.setEnabled(canEdit);
                    bEdit.setTag(item);
                    bEdit.setOnClickListener(this);
                } else {
                    throw new AssertionError("Dont know how to handle field: " + item.field.getName());
                }

            } catch (Exception e) {
                throw new AssertionError(e);
            }
            return v;
        }

        @Override
        public void onClick(final View v) {
            try {
                final RuleItem item = (RuleItem) v.getTag();
                final int value = item.field.getInt(rules);

                final NumberPicker np = CCNumberPicker.newPicker(SOCActivity.this, value, item.min, item.max, null);
                newDialog(false).setTitle(item.field.getName()).setView(np).setNegativeButton("Cancel", null)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    item.field.setInt(rules, np.getValue());
                                    ((Button) v).setText(String.valueOf(np.getValue()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).show();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            final RuleItem item = (RuleItem)buttonView.getTag();
            try {
                item.field.setBoolean(rules, isChecked);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void showRulesDialog() {
        final boolean canEdit = !soc.isRunning() && !user.client.isConnected();
        final Rules rules = soc.getRules().deepCopy();
        ListView lv = new ListView(this);
        lv.setAdapter(new RulesAdapter(rules, canEdit));
        AlertDialog.Builder b = newDialog(true).setTitle("Rules").setView(lv);
        if (canEdit) {
            b.setNegativeButton("Discard", null)
            .setNeutralButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    rules.trySaveToFile(rulesFile);
                    soc.setRules(rules);
                }
            }).setPositiveButton("Keep", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    soc.setRules(rules);
                }
            }).show();
        } else {
            b.setNegativeButton("Ok", null).show();
        }
    }

    void copyFileToExt() {
        try {
//            FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showStartMenu();
    }

    final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onPause() {
        super.onPause();
        soc.stopRunning();
    }

    protected AlertDialog.Builder newDialog(boolean cancelable) {
        AlertDialog.Builder b = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Dialog) {
            @Override
            public AlertDialog show() {
                AlertDialog d = super.show();
                dialogStack.push(d);
                return d;
            }
        };
        if (cancelable) {
            if (dialogStack.size()>0) {
                b.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialogStack.pop().show();
                    }
                });
            } else {
                b.setNegativeButton("Cancel", null);
            }
        }
        return b;
    }

    void clearDialogs() {
        while (dialogStack.size() > 0) {
            dialogStack.pop().dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

        }
    }

    @Override
    public void onConnected(ClientConnection conn) {
        log.info("Clinet connected: " + conn.getDisplayName());
    }

    @Override
    public void onReconnection(ClientConnection conn) {
        log.info("Clinet reconnected: " + conn.getDisplayName());
    }

    @Override
    public void onClientDisconnected(ClientConnection conn) {
        log.info("Clinet disconnected: " + conn.getDisplayName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newDialog(false).setTitle("ERROR").setMessage("You have been disconnected")
                        .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                quitGame();
                            }
                        }).show();
            }
        });
    }
}
