package cc.game.zombicide.android;

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cc.game.zombicide.android.databinding.ActivityZombicideBinding;
import cc.game.zombicide.android.databinding.AssignDialogItemBinding;
import cc.lib.android.CCActivityBase;
import cc.lib.android.DroidGraphics;
import cc.lib.android.DroidUtils;
import cc.lib.android.EmailHelper;
import cc.lib.android.SpinnerTask;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.mp.android.P2PActivity;
import cc.lib.ui.IButton;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;
import cc.lib.zombicide.ZDifficulty;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ui.UIZBoardRenderer;
import cc.lib.zombicide.ui.UIZButton;
import cc.lib.zombicide.ui.UIZCharacterRenderer;
import cc.lib.zombicide.ui.UIZUser;
import cc.lib.zombicide.ui.UIZombicide;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 *
 */
public class ZombicideActivity extends P2PActivity implements View.OnClickListener, ListView.OnItemClickListener, ListView.OnItemLongClickListener {

    private final static String TAG = ZombicideActivity.class.getSimpleName();

    final static int MAX_PLAYERS = 4; // max number of characters on screen at one time
    final static int MAX_SAVES = 4;

    final static String PREF_P2P_NAME = "p2pname";
    final static String PREF_PLAYERS = "players";

    ActivityZombicideBinding zb;
    ActivityViewModel vm;

    File gameFile, statsFile, savesMapFile;

    UIZombicide game;
    final ZUser user = new UIZUser();

    ZClientMgr clientMgr = null;
    ZServerMgr serverMgr = null;

    UIZBoardRenderer<DroidGraphics> boardRenderer;
    UIZCharacterRenderer characterRenderer;

    final ArrayBlockingQueue<Integer> fileWriterQueue = new ArrayBlockingQueue(1);
    final Stats stats = new Stats();

    boolean isWolfburgUnlocked() {
        if (BuildConfig.DEBUG)
            return true;
        return stats.isQuestCompleted(ZQuests.Trial_by_Fire, ZDifficulty.MEDIUM);
    }

    final CharLock[] charLocks = {
            new CharLock(ZPlayerName.Ann, 0),
            new CharLock(ZPlayerName.Baldric, 0),
            new CharLock(ZPlayerName.Clovis, 0),
            new CharLock(ZPlayerName.Samson, 0),
            new CharLock(ZPlayerName.Nelly, 0),
            new CharLock(ZPlayerName.Silas, 0),
            new CharLock(ZPlayerName.Tucker, R.string.char_lock_tucker) {
                @Override
                boolean isUnlocked() {
                    return stats.isQuestCompleted(ZQuests.Big_Game_Hunting, ZDifficulty.MEDIUM);
                }
            },
            new CharLock(ZPlayerName.Jain, R.string.char_lock_jain) {
                @Override
                boolean isUnlocked() {
                    return stats.isQuestCompleted(ZQuests.The_Black_Book, ZDifficulty.HARD);
                }
            },
            new CharLock(ZPlayerName.Benson, R.string.char_lock_benson) {
                @Override
                boolean isUnlocked() {
                    return stats.isQuestCompleted(ZQuests.The_Evil_Temple, ZDifficulty.HARD);
                }
            },
            new CharLock(ZPlayerName.Karl, R.string.char_lock_wolfz) {
                @Override
                boolean isUnlocked() {
                    return isWolfburgUnlocked();
                }
            },
            new CharLock(ZPlayerName.Morrigan, R.string.char_lock_wolfz) {
                @Override
                boolean isUnlocked() {
                    return isWolfburgUnlocked();
                }
            },
            new CharLock(ZPlayerName.Ariane, R.string.char_lock_wolfz) {
                @Override
                boolean isUnlocked() {
                    return isWolfburgUnlocked();
                }
            },
            new CharLock(ZPlayerName.Theo, R.string.char_lock_wolfz) {
                @Override
                boolean isUnlocked() {
                    return isWolfburgUnlocked();
                }
            },
    };

    @Override
    protected int getConnectPort() {
        return ZMPCommon.CONNECT_PORT;
    }

    @Override
    protected String getVersion() {
        return ZMPCommon.VERSION;
    }

    @Override
    protected int getMaxConnections() {
        return 2;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ZGame.DEBUG = BuildConfig.DEBUG;

        hideNavigationBar();
        vm = new ViewModelProvider(this).get(ActivityViewModel.class);
        zb = ActivityZombicideBinding.inflate(getLayoutInflater());
        zb.setViewModel(vm);
        zb.setLifecycleOwner(this);
        setContentView(zb.getRoot());

        zb.listMenu.setOnItemClickListener(this);
        zb.listMenu.setOnItemLongClickListener(this);
        zb.bZoom.setOnClickListener(this);
        zb.bUp.setOnClickListener(this);
        zb.bUseleft.setOnClickListener(this);
        zb.bUseright.setOnClickListener(this);
        zb.bCenter.setOnClickListener(this);
        zb.bVault.setOnClickListener(this);
        zb.bLeft.setOnClickListener(this);
        zb.bDown.setOnClickListener(this);
        zb.bRight.setOnClickListener(this);

        characterRenderer = new UIZCharacterRenderer(zb.consoleView);
        boardRenderer = new UIZBoardRenderer<DroidGraphics>(zb.boardView) {

            /*
                        Map<GColor, Paint> outlinePaints = new HashMap<>();
                        Paint getPaint(GColor color) {
                            Paint p = outlinePaints.get(color);
                            if (p != null)
                                return p;

                            ColorFilter filter = new PorterDuffColorFilter(color.toARGB(), PorterDuff.Mode.SRC_IN);
                            BlurMaskFilter blur = new BlurMaskFilter(15, BlurMaskFilter.Blur.INNER);
                            p = new Paint();
                            p.setColorFilter(filter);
                            p.setMaskFilter(blur);
                            p.setColor(color.toARGB());
                            outlinePaints.put(color, p);
                            return p;
                        }
            */
            @Override
            public void onLoaded() {
                zb.vgTop.setLayoutTransition(new LayoutTransition());
                zb.listMenu.setVisibility(View.VISIBLE);
                vm.loading.postValue(false);
                /*
                if (game.getQuest().getPercentComplete(game) == 0)
                    game.showObjectivesOverlay();
                else
                    game.showSummaryOverlay();
                 */
            }

            @Override
            public void onLoading() {
                zb.vgTop.setLayoutTransition(null);
                zb.listMenu.setVisibility(View.GONE);
                vm.loading.postValue(true);
            }
        };
        boardRenderer.setDrawTiles(true);
        boardRenderer.setMiniMapMode(getPrefs().getInt("miniMapMode", 1));

        game = new UIZombicide(characterRenderer, boardRenderer) {

            @Override
            public boolean runGame() {
                boolean changed = false;
                try {
                    changed = super.runGame();
                    log.debug("runGame changed=" + changed);
                    if (changed) {
                        //FileUtils.backupFile(gameFile, 32);
                        //trySaveToFile(gameFile);
                        fileWriterQueue.put(0);
                    }
                    zb.boardView.postInvalidate();
                    zb.consoleView.postInvalidate();
                    //synchronized (boardView) {
                    //    boardView.wait(2000); // wait for the board to render at least one frame
                    //}
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return changed;
            }

            @Override
            public <T> T waitForUser(Class<T> expectedType) {
                zb.boardView.post(() -> initMenu(getUiMode(), getOptions()));
                return super.waitForUser(expectedType);
            }

            @Override
            protected void onQuestComplete() {
                super.onQuestComplete();
                runOnUiThread(() -> {
                    stopGame();
                    completeQuest(getQuest().getQuest());
                    initHomeMenu();
                });
            }

            @Override
            public void setResult(Object result) {
                game.boardRenderer.setOverlay(null);
                super.setResult(result);
            }

            @Override
            public boolean isGameRunning() {
                return super.isGameRunning() || clientMgr != null;
            }

            @Override
            protected void onCurrentCharacterUpdated(ZPlayerName priorPlayer, ZPlayerName player) {
                super.onCurrentCharacterUpdated(priorPlayer, player);
                runOnUiThread(() -> initGameMenu());
            }

            @Override
            protected void onCurrentUserUpdated(ZUser user) {
                super.onCurrentUserUpdated(user);
                runOnUiThread(() -> initGameMenu());
            }

            @Override
            public void undo() {
                tryUndo();
            }

            @Override
            public ZUser getThisUser() {
                return user;
            }
        };

        int colorIdx = getPrefs().getInt("userColorIndex", 0);
        user.setColor(colorIdx);
        game.addUser(user);
    }

    void loadCharacters(Collection<String> playersSet) {
        game.clearCharacters();
        game.clearUsersCharacters();

        List<ZPlayerName> players = Utils.map(playersSet, ZPlayerName::valueOf);
        for (ZPlayerName player : players) {
            game.addCharacter(player);
            user.addCharacter(player);
        }
    }


    Thread startFileWriterThread() {
        Thread t = new Thread(() -> {
            log.debug("fileWriterThread ENTER");
            while (game.isGameRunning()) {
                try {
                    fileWriterQueue.take();
                } catch (InterruptedException e) {
                    break;
                }
                if (getServer() != null) {
                    getServer().broadcastCommand(serverMgr.newUpdateGameCommand(game));
                }
                log.debug("Backingup ... ");
                FileUtils.backupFile(gameFile, 32);
                game.trySaveToFile(gameFile);
            }
            log.debug("fileWriterThread EXIT");
        });
        t.start();
        return t;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setKeepScreenOn(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setKeepScreenOn(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        game.setDifficulty(getSavedDifficulty());
        gameFile = new File(getFilesDir(), "game.save");
        statsFile = new File(getFilesDir(), "stats.save");
        savesMapFile = new File(getFilesDir(), "saves.save");
        //if (!gameFile.exists() || !game.tryLoadFromFile(gameFile)) {
        //    showWelcomeDialog(true);
        //} else
        if (gameFile.exists() && game.tryLoadFromFile(gameFile)) {
            game.showSummaryOverlay();
        } else {
            game.loadQuest(ZQuests.Tutorial);
        }
        if (statsFile.exists()) {
            try {
                log.debug(FileUtils.fileToString(statsFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
            stats.tryLoadFromFile(statsFile);
        }

        initHomeMenu();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopGame();
    }

    @Override
    public AlertDialog.Builder newDialogBuilder() {
        AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_Holo_Dialog)) {
            @Override
            public AlertDialog show() {
                AlertDialog d = super.show();
                d.setCanceledOnTouchOutside(false);
                return d;
            }
        };
        if (!BuildConfig.DEBUG)
            b.setCancelable(false);
        return b;
    }

    enum MenuItem implements UIZButton {
        RESUME,
        CANCEL,
        LOAD,
        SAVE,
        NEW_GAME,
        JOIN_GAME,
        SETUP_PLAYERS,
        CONNECTIONS,
        START,
        ASSIGN,
        SUMMARY,
        UNDO,
        DIFFICULTY,
        OBJECTIVES,
        SKILLS,
        LEGEND,
        QUIT,
        CLEAR,
        SEARCHABLES,
        RULES,
        CHOOSE_COLOR,
        EMAIL_REPORT,
        MINIMAP_MODE;

        boolean isHomeButton(ZombicideActivity instance) {
            switch (this) {
                case LOAD:
                case SAVE:
                case ASSIGN:
                case CLEAR:
                case UNDO:
                case DIFFICULTY:
                case CHOOSE_COLOR:
                    return BuildConfig.DEBUG;
                case START:
                case NEW_GAME:
                case JOIN_GAME:
                case SETUP_PLAYERS:
                case SKILLS:
                case LEGEND:
                case EMAIL_REPORT:
                case MINIMAP_MODE:
                    return true;
                case CONNECTIONS:
                    return instance.serverControl != null;
                case RESUME:
                    return instance.gameFile != null && instance.gameFile.exists();
            }
            return false;
        }

        boolean isGameButton(ZombicideActivity instance) {
            switch (this) {
                case LOAD:
                case SAVE:
                case START:
                case ASSIGN:
                case RESUME:
                case NEW_GAME:
                case JOIN_GAME:
                case SETUP_PLAYERS:
                case CLEAR:
                    return false;
                case CONNECTIONS:
                    return instance.serverControl != null;
                case UNDO:
                case SEARCHABLES:
                case CHOOSE_COLOR:
                    return BuildConfig.DEBUG;
            }
            return true;
        }


        @Override
        public GRectangle getRect() {
            return null;
        }

        @Override
        public String getTooltipText() {
            return null;
        }

        @Override
        public String getLabel() {
            return Utils.toPrettyString(name());
        }

        public boolean isEnabled(ZombicideActivity z) {
            if (this == MenuItem.UNDO) {
                return z.getClient() != null || FileUtils.hasBackupFile(z.gameFile);
            }
            return true;
        }
    }

    ZDifficulty getSavedDifficulty() {
        return ZDifficulty.valueOf(getPrefs().getString("difficulty", ZDifficulty.MEDIUM.name()));
    }

    @Override
    public void onClick(View v) {
        try {
            game.boardRenderer.setOverlay(null);
            switch (v.getId()) {
                case R.id.b_zoom: {
                    float curZoom = game.boardRenderer.getZoomPercent();
                    if (curZoom < 1) {
                        game.boardRenderer.animateZoomTo(curZoom + .5f);
                    } else {
                        game.boardRenderer.animateZoomTo(0);
                    }
                    game.boardRenderer.redraw();
                    break;
                }
                case R.id.b_center:
                    game.boardRenderer.clearDragOffset();
                case R.id.b_useleft:
                case R.id.b_useright:
                case R.id.b_vault:
                case R.id.b_up:
                case R.id.b_left:
                case R.id.b_down:
                case R.id.b_right:
                    if (v.getTag() != null) {
                        game.setResult(v.getTag());
                        clearKeypad();
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof MenuItem) {
            processMainMenuItem((MenuItem) view.getTag());
        } else {
            UIZombicide.getInstance().setResult(view.getTag());
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() != null && view.getTag() instanceof IButton) {
            IButton button = (IButton) view.getTag();
            showToolTipTextPopup(parent, button);
            return true;
        }
        return false;
    }

    public void showToolTipTextPopup(View view, IButton button) {
        String toolTipText = button.getTooltipText();
        if (Utils.isEmpty(toolTipText))
            return;
        View popup = View.inflate(this, R.layout.tooltippopup_layout, null);
        ((TextView) popup.findViewById(R.id.header)).setText(button.getLabel());
        ((TextView) popup.findViewById(R.id.text)).setText(toolTipText);
        Dialog d = new AlertDialog.Builder(this, R.style.ZTooltipDialogTheme)
                .setView(popup).create();
        Window window = d.getWindow();
        int[] outPos = {0, 0};
        view.getLocationOnScreen(outPos);
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.x = outPos[0] + view.getWidth();
            lp.y = outPos[1];
            lp.width = DroidUtils.convertDipsToPixels(this, 200);
        }
        d.show();
    }

    Thread fileWriterThread = null;

    public void startGame() {
        game.startGameThread();
        fileWriterThread = startFileWriterThread();
        initGameMenu();
        game.refresh();
    }

    public void shutdownMP() {
        p2pShutdown();
        clientControl = null;
        serverControl = null;
        clientMgr = null;
        serverMgr = null;
    }

    public void stopGame() {
        game.stopGameThread();
        if (fileWriterThread != null) {
            fileWriterThread.interrupt();
            fileWriterThread = null;
        }
        game.setResult(null);
        initHomeMenu();
    }

    Set<String> getStoredCharacters() {
        return getPrefs().getStringSet(PREF_PLAYERS, getDefaultPlayers());
    }

    void processMainMenuItem(MenuItem item) {
        switch (item) {
            case START:
                if (game.getRoundNum() > 0) {
                    newDialogBuilder().setTitle(R.string.popup_title_confirm).setMessage(R.string.popup_message_confirm_restart)
                            .setNegativeButton(R.string.popup_button_cancel, null)
                            .setPositiveButton(R.string.popup_button_newgame, (dialogInterface, i) -> {
                                FileUtils.deleteFileAndBackups(gameFile);
                                game.reload();
                                loadCharacters(getStoredCharacters());
                                startGame();
                            }).show();
                } else {
                    FileUtils.deleteFileAndBackups(gameFile);
                    game.reload();
                    startGame();
                }
                break;
            case RESUME: {
                user.clearCharacters();
                for (ZPlayerName pl : game.getAllCharacters()) {
                    if (user.getColor().equals(pl.getCharacter().getColor()))
                        user.addCharacter(pl);
                    else
                        pl.getCharacter().setInvisible(true);
                }
                startGame();
                /*
                if (game.tryLoadFromFile(gameFile)) {
                    //user.setCharacters(game.getAllCharacters());
                    //game.boardRenderer.setOverlay(game.getQuest().getObjectivesOverlay(game));
                    startGame();
                }*/
                break;
            }
            case QUIT:
                if (getClient() != null) {
                    newDialogBuilder().setTitle(R.string.popup_title_confirm)
                            .setMessage(R.string.popup_message_confirm_disconnect)
                            .setNegativeButton(R.string.popup_button_cancel, null)
                            .setPositiveButton(R.string.popup_button_disconnect, (dialog, which) -> {
                                new SpinnerTask<Integer>(ZombicideActivity.this) {
                                    @Override
                                    protected void doIt(Integer... args) throws Exception {
                                        getClient().disconnect("Quit Game");
                                    }

                                    @Override
                                    protected void onCompleted() {
                                        shutdownMP();
                                        stopGame();
                                    }
                                }.execute();
                            }).show();
                } else if (getServer() != null) {
                    newDialogBuilder().setTitle(R.string.popup_title_confirm)
                            .setMessage(R.string.popup_message_confirm_disconnect)
                            .setNegativeButton(R.string.popup_button_cancel, null)
                            .setPositiveButton(R.string.popup_button_disconnect, (dialog, which) -> {
                                new SpinnerTask<Integer>(ZombicideActivity.this) {
                                    @Override
                                    protected void doIt(Integer... args) throws Exception {
                                        getServer().stop();
                                    }

                                    @Override
                                    protected void onCompleted() {
                                        stopGame();
                                        shutdownMP();
                                    }
                                }.execute();
                            }).show();
                } else {
                    stopGame();
                    game.setResult(null);
                }
                break;
            case CANCEL:
                if (game.isGameRunning()) {
                    game.setResult(null);
                } else {
                    initHomeMenu();
                }
                break;
            case OBJECTIVES: {
                game.showObjectivesOverlay();
                break;
            }
            case SUMMARY: {
                game.showSummaryOverlay();
                break;
            }
            case NEW_GAME: {
                showNewGameDialog();
                break;
            }
            case JOIN_GAME: {
                p2pInit(P2PMode.CLIENT);
                break;
            }
            case SETUP_PLAYERS:
                showSetupPlayersDialog();
                break;
            case CONNECTIONS:
                serverControl.openConnections();
                break;
            case CLEAR: {
                getPrefs().edit().remove("completedQuests").apply();
                long byteDeleted = FileUtils.deleteFileAndBackups(gameFile);
                statsFile.delete();
                log.debug("deleted " + Formatter.formatFileSize(this, byteDeleted) + " of memory");
                initHomeMenu();
                break;
            }
            case SEARCHABLES: {
                ListView lv = new ListView(this);
                List<ZEquipment> searchables = new ArrayList(game.getAllSearchables());
                Collections.reverse(searchables);
                //Collections.sort(searchables);

                lv.setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return searchables.size();
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
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView == null) {
                            convertView = new TextView(ZombicideActivity.this);
                        }
                        ((TextView) convertView).setText(searchables.get(position).getLabel());
                        return convertView;
                    }
                });
                newDialogBuilder().setTitle(R.string.popup_title_searchables).setView(lv).setNegativeButton("Close", null).show();
                break;
            }
            case LOAD:
                newDialogBuilder().setTitle(R.string.popup_title_choose).setItems(getResources().getStringArray(R.array.popup_message_choose_game_types),
                        (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    showLoadQuestDialog();
                                    break;
                                case 1:
                                    showLoadSavedGameDialog();
                                    break;
                            }
                        }).setNegativeButton(R.string.popup_button_cancel, null).show();

                break;
            case SAVE:
                showSaveGameDialog();
                break;
            case ASSIGN:
                showAssignDialog();
                break;
            case DIFFICULTY: {
                newDialogBuilder().setTitle(getString(R.string.popup_title_difficulty, getSavedDifficulty()))
                        .setItems(Utils.toStringArray(ZDifficulty.values()), (dialog, which) -> {
                            ZDifficulty difficulty = ZDifficulty.values()[which];
                            game.setDifficulty(difficulty);
                            getPrefs().edit().putString("difficulty", difficulty.name()).apply();
                        }).setNegativeButton(R.string.popup_button_cancel, null).show();
                break;
            }
            case SKILLS: {
                showSkillsDialog2();
                break;
            }

            case UNDO: {
                if (getClient() != null) {
                    new CLSendCommandSpinnerTask(this, ZMPCommon.SVR_UPDATE_GAME) {
                        @Override
                        protected void onSuccess() {
                            zb.boardView.postInvalidate();
                        }
                    }.execute(clientMgr.newUndoPressed());
                    //getClient().sendCommand(clientMgr.newUndoPressed());
                    game.setResult(null);
                } else {
                    tryUndo();
                }
                break;
            }

            case LEGEND: {
                showLegendDialog();
                break;
            }
            case RULES: {
                showWelcomeDialog(false);
                break;
            }
            case EMAIL_REPORT: {
                showEmailReportDialog();
                break;
            }
            case CHOOSE_COLOR: {
                showChooseColorDialog();
                break;
            }
            case MINIMAP_MODE: {
                getPrefs().edit().putInt("miniMapMode", boardRenderer.toggleDrawMinimap());
                break;
            }
        }
    }

    void tryUndo() {
        boolean isRunning = game.isGameRunning();
        stopGame();
        if (FileUtils.restoreFile(gameFile)) {
            game.tryLoadFromFile(gameFile);
            game.refresh();
            if (getServer() != null) {
                getServer().broadcastCommand(serverMgr.newUpdateGameCommand(game));
            }
        }
        if (isRunning)
            startGame();
    }

    void updateCharacters(ZQuests quest) {
        if (serverMgr != null) {
            getServer().broadcastCommand(serverMgr.newLoadQuest(quest));
            game.clearCharacters();
            for (ZUser user : game.getUsers()) {
                List<ZPlayerName> newPlayers = new ArrayList<>();
                for (ZPlayerName pl : user.getPlayers()) {
                    game.addCharacter(pl);
                    newPlayers.add(pl);
                }
                user.setCharacters(newPlayers);
            }
            getServer().broadcastCommand(serverMgr.newUpdateGameCommand(game));
        } else {
            loadCharacters(getStoredCharacters());
            game.trySaveToFile(gameFile);
        }
        startGame();
        zb.boardView.postInvalidate();
    }

    void showLoadQuestDialog() {
        newDialogBuilder().setTitle(R.string.popup_title_load_quest)
                .setItems(Utils.toStringArray(ZQuests.values(), true), (dialog, which) -> {
                    ZQuests q = ZQuests.values()[which];
                    game.loadQuest(q);
                    updateCharacters(q);
                }).setNegativeButton(R.string.popup_button_cancel, null).show();
    }

    void showSaveGameDialog() {
        new SaveGameDialog(this, MAX_SAVES);
    }

    void showLoadSavedGameDialog() {
        Map<String,String> saves = getSaves();
        if (saves.size() > 0) {
            String [] items = new String[saves.size()];
            newDialogBuilder().setTitle(R.string.popup_title_load_saved)
                    .setItems(saves.keySet().toArray(items), (dialog, which) -> {
                        String fileName = saves.get(items[which]);
                        File file = new File(getFilesDir(), fileName);
                        if (game.tryLoadFromFile(file)) {
                            updateCharacters(game.getQuest().getQuest());
                        } else {
                            newDialogBuilder().setTitle(R.string.popup_title_error)
                                    .setMessage(getString(R.string.popup_message_err_fileopen, fileName))
                                    .setNegativeButton(R.string.popup_button_ok, null).show();
                        }
                    }).setNegativeButton(R.string.popup_button_cancel, null).show();
        }
    }

    void deleteSave(String key) {
        Map<String,String> saves = getSaves();
        String fileName = saves.get(key);
        if (fileName != null) {
            File file = new File(getFilesDir(), fileName);
            file.delete();
        }
        saves.remove(key);
        try {
            Reflector.serializeToFile(saves, savesMapFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Map<String,String> getSaves() {
        Map<String,String> saves = null;
        try {
            saves = Reflector.deserializeFromFile(savesMapFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (saves == null)
            saves = new LinkedHashMap<>();

        return saves;
    }

    void saveGame() {
        StringBuffer buf = new StringBuffer();
        buf.append(game.getQuest().getQuest().getDisplayName());
        buf.append(" ").append(game.getDifficulty().name());
        String delim = " ";
        for (ZPlayerName c : game.getAllCharacters()) {
            buf.append(delim).append(c.name());
            delim = ",";
        }
        int completePercent = game.getQuest().getPercentComplete(game);
        buf.append(String.format(" %d%% Completed", completePercent));
        buf.append(" ").append(new SimpleDateFormat("MMM dd").format(new Date()));

        int idx = 0;
        File file = null;
        while (idx < 10) {
            String fileName = "savegame" + idx;
            file = new File(getFilesDir(), fileName);
            if (!file.isFile())
                break;
            idx++;
        }

        try {
            game.saveToFile(file);
            Map<String,String> saves = getSaves();
            saves.put(buf.toString(), file.getName());
            Reflector.serializeToFile(saves, savesMapFile);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "There was a problem saving the game.", Toast.LENGTH_LONG).show();
        }

    }

    void showChooseColorDialog() {
        newDialogBuilder().setTitle(R.string.popup_title_choose_color)
                .setItems(ZUser.USER_COLOR_NAMES, (dialog, which) -> {
                    getPrefs().edit().putInt("userColorIndex", which).apply();
                    user.setColor(which);
                    game.refresh();
                }).setNegativeButton(R.string.popup_button_cancel, null).show();
    }

    public String getDisplayName() {
        return getPrefs().getString(PREF_P2P_NAME, "Unnamed");
    }

    @Override
    protected void onP2PReady() {
        String p2pName = getPrefs().getString(PREF_P2P_NAME, null);
        if (p2pName == null) {
            showEditTextInputPopup("Set P2P Name", p2pName, "Display Name", 32, (String txt) -> {
                if (Utils.isEmpty(txt)) {
                    newDialogBuilder().setMessage(R.string.popup_message_err_nonemptyname)
                            .setNegativeButton(R.string.popup_button_cancel_mp, (dialog, which) -> {
                                hideKeyboard();
                                p2pShutdown();
                            }).setPositiveButton(R.string.popup_button_ok, (dialog, which) -> {
                                hideKeyboard();
                                onP2PReady();
                            }).show();
                } else {
                    getPrefs().edit().putString(PREF_P2P_NAME, txt).apply();
                    p2pStart();
                }
            });
        } else {
            p2pStart();
        }
    }

    @Override
    public void p2pStart() {
        game.clearCharacters();
        game.clearUsersCharacters();
        game.reload();
        game.refresh();
        super.p2pStart();
    }

    P2PClient clientControl;


    @Override
    protected void onP2PClient(P2PClient p2pClient) {
        clientControl = p2pClient;
        clientMgr = new ZClientMgr(this, game, clientControl.getClient(), user);
    }

    P2PServer serverControl;

    @Override
    protected void onP2PServer(P2PServer p2pServer) {
        serverControl = p2pServer;
        serverMgr = new ZServerMgr(this, game, 2, p2pServer.getServer());
        user.setName(getPrefs().getString(PREF_P2P_NAME, null));
    }

    @Override
    protected void onP2PShutdown() {
        clientMgr = null;
        clientControl = null;
        serverMgr = null;
        serverControl = null;
        game.setServer(null);
        game.setClient(null);
        initHomeMenu();
    }

    void showSetupPlayersDialog() {
        Set<String> saved = getStoredCharacters();
        List<Assignee> assignments = new ArrayList<>();
        for (ZombicideActivity.CharLock c : charLocks) {
            Assignee a = new Assignee(c);
            if (saved.contains(a.name.name())) {
                a.checked = true;
                a.color = user.getColorId();
                a.userName = user.getName();
                a.isAssingedToMe = true;
            }
            assignments.add(a);
        }

        new CharacterChooserDialogMP(this, assignments, 6) {
            @Override
            protected void onAssigneeChecked(Assignee a, boolean checked) {
                a.checked = checked;
                if (a.checked) {
                    a.color = user.getColorId();
                    a.userName = user.getName();
                    a.isAssingedToMe = true;
                } else {
                    a.color = -1;
                    a.userName = null;
                    a.isAssingedToMe = false;
                }
            }

            @Override
            protected void onStart() {
                game.clearCharacters();
                game.clearUsersCharacters();
                Set<String> players = new HashSet<>();
                for (Assignee a : assignments) {
                    if (a.checked) {
                        game.addCharacter(a.name);
                        user.addCharacter(a.name);
                        players.add(a.name.name());
                    }
                }
                getPrefs().edit().putStringSet(PREF_PLAYERS, players).apply();
                game.refresh();
            }
        };
    }

    void showLegendDialog() {
        Object[][] legend = {
                {R.drawable.legend_chars, "CHARACTERS\nChoose your characters. Some are unlockable. Players can operate in any order but must execute all of their actions before switching to another player."},
                {R.drawable.legend_gamepad, "GAMEPAD\nUse gamepad to perform common actions.\nLH / RH - Use Item in Left/Right hand.\nO - Toggle active player.\nZM - Zoom in/out."},
                {R.drawable.legend_obj, "OBJECTIVES\nObjectives give player EXP, unlock doors, reveal special items and other things related to the Quest."},
                {R.drawable.legend_vault, "VAULTS\nVaults are very handy. You can find special loot or drop loot to be pickup up later. You can also close zombies in or out of the vault. Also they can sometimes be shortcuts across the map. The only limitation is you cannot fire ranged weapons or magic into the vault from the outside or fire outside of vault from inside of it."},
                {R.drawable.zwalker1, "WALKER\n" + ZZombieType.Walker.getDescription()},
                {R.drawable.zfatty1, "FATTY\n" + ZZombieType.Fatty.getDescription()},
                {R.drawable.zrunner1, "RUNNER\n" + ZZombieType.Runner.getDescription()},
                {R.drawable.znecro, "NECROMANCER\n" + ZZombieType.Necromancer.getDescription()},
                {R.drawable.zabomination, "ABOMINATION\n" + ZZombieType.Abomination.getDescription()},

        };

        ListView lv = new ListView(this);
        lv.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return legend.length;
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
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = View.inflate(ZombicideActivity.this, R.layout.legend_list_item, null);
                }

                ImageView iv_image = convertView.findViewById(R.id.iv_image);
                TextView tv_desc = convertView.findViewById(R.id.tv_description);

                Object[] item = legend[position];

                if (item[0] instanceof Integer) {
                    iv_image.setImageResource((Integer) item[0]);
                } else if (item[0] instanceof Bitmap) {
                    iv_image.setImageBitmap((Bitmap) item[0]);
                }

                if (item[1] instanceof Integer) {
                    tv_desc.setText((Integer) item[1]);
                } else if (item[1] instanceof String) {
                    tv_desc.setText((String) item[1]);
                }

                return convertView;
            }
        });

        newDialogBuilder().setTitle("Legend").setView(lv).setNegativeButton(R.string.popup_button_close, null).show();
    }

    void completeQuest(ZQuests quest) {
        stats.completeQuest(quest, game.getDifficulty());
        stats.trySaveToFile(statsFile);
    }

    void showWelcomeDialog(boolean showNewGame) {
        AlertDialog.Builder b = newDialogBuilder().setTitle(R.string.popup_title_welcome).setMessage(R.string.welcome_msg);
        if (showNewGame) {
            b.setPositiveButton(R.string.popup_button_newgame, (dialog, which) -> showNewGameDialog());
        } else {
            b.setPositiveButton(R.string.popup_button_close, null);
        }
        b.show();
    }

    void showNewGameDialog() {
        newDialogBuilder().setTitle(R.string.popup_title_choose_version)
                .setItems(getResources().getStringArray(BuildConfig.DEBUG ? R.array.popup_message_choose_game_version_debug : R.array.popup_message_choose_game_version), (dialog, which) -> {
                    switch (which) {
                        case 0: // Black Plague
                            showNewGameChooseQuestDialog(ZQuests.questsBlackPlague(), stats.getCompletedQuests());
                            break;

                        case 1:
                            if (isWolfburgUnlocked())
                                showNewGameChooseQuestDialog(ZQuests.questsWolfsburg(), stats.getCompletedQuests());
                            else
                                newDialogBuilder().setTitle(R.string.popup_title_wolflocked)
                                    .setMessage(R.string.popup_message_unlock_wolfburg)
                                    .setPositiveButton(R.string.popup_button_ok, (dialog1, which1) -> showNewGameDialog()).show();
                            break;

                        case 2:
                            showNewGameChooseQuestDialog(Arrays.asList(ZQuests.values()), new HashSet(Arrays.asList(ZQuests.values())));
                            break;

                    }
                }).setNegativeButton(R.string.popup_button_cancel, null).show();

    }

    void showNewGameChooseQuestDialog(List<ZQuests> allQuests, Set<ZQuests> playable) {
        new NewGameChooseQuestDialog(this, allQuests, playable);
    }

    void showNewGameDialogChooseDifficulty(ZQuests quest) {
        newDialogBuilder().setTitle(getString(R.string.popup_title_quest, quest.ordinal(), quest.getDisplayName()))
                .setItems(Utils.toStringArray(ZDifficulty.values()), (dialog, which) -> {
                    ZDifficulty difficulty = ZDifficulty.values()[which];
                    getPrefs().edit().putString("difficulty", difficulty.name()).apply();
                    game.setDifficulty(difficulty);
                    showChooseGameModeDialog(quest);
                }).setNegativeButton(R.string.popup_button_back, (dialog, which) -> showNewGameDialog()).show();
    }

    void showChooseGameModeDialog(ZQuests quest) {
        String [] modes = {
                "Single Player",
                "Multi-player Host"
        };
        newDialogBuilder().setTitle("Choose Mode")
                .setItems(modes, (dialog, which) -> {
                    switch (which) {
                        case 0: // single player
                            showNewGameDialogChoosePlayers(quest);
                            break;
                        case 1: // multiplayer
                            game.loadQuest(quest);
                            p2pInit(P2PMode.SERVER);
                            break;
                    }
                }).setNegativeButton(R.string.popup_button_back, (dialog, which) -> showNewGameDialogChooseDifficulty(quest))
                .show();
    }

    static class CharLock {
        final int unlockMessageId;
        final ZPlayerName player;

        public CharLock(ZPlayerName player, int unlockMessageId) {
            this.unlockMessageId = unlockMessageId;
            this.player = player;
        }

        boolean isUnlocked() {
            return true;
        }
    }

    void showNewGameDialogChoosePlayers(ZQuests quest) {
        new CharacterChooserDialogSP(this, quest) {
            @Override
            void onStarted() {
                getPrefs().edit().putStringSet(PREF_PLAYERS, selectedPlayers).apply();
                game.loadQuest(quest);
                loadCharacters(getStoredCharacters());
                game.trySaveToFile(gameFile);
                startGame();
            }
        };
    }

    void showSkillsDialog2() {
        new SkillsDialog(this);
    }

    // TODO: Make this more organized. Should be able to order by character, danger level or all POSSIBLE
    void showSkillsDialog() {
        ZSkill[] sorted = Utils.copyOf(ZSkill.values());
        Arrays.sort(sorted, (o1, o2) -> o1.name().compareTo(o2.name()));
        newDialogBuilder().setTitle(R.string.popup_title_skills)
                .setItems(Utils.toStringArray(sorted, true), (dialog, which) -> {
                    ZSkill skill = sorted[which];
                    newDialogBuilder().setTitle(skill.getLabel())
                            .setMessage(skill.description)
                            .setNegativeButton(R.string.popup_button_cancel, null)
                            .setPositiveButton(R.string.popup_button_back, (dialog1, which1) -> showSkillsDialog()).show();
                }).setNegativeButton(R.string.popup_button_cancel, null).show();

    }

    public void initHomeMenu() {
        vm.playing.postValue(false);
        List<View> buttons = new ArrayList<>();
        for (MenuItem i : Utils.filter(MenuItem.values(), object -> object.isHomeButton(ZombicideActivity.this))) {
            buttons.add(ZButton.build(this, i, i.isEnabled(this)));
        }
        initMenuItems(buttons);
    }

    public void initGameMenu() {
        vm.playing.postValue(true);
        initMenu(UIZombicide.UIMode.NONE, null);
        game.refresh();
    }

    void initKeypad(List options) {
        for (Iterator it = options.iterator(); it.hasNext(); ) {
            Object o = it.next();
            if (o instanceof ZMove) {
                ZMove move = (ZMove)o;
                switch (move.type) {
                    case WALK_DIR:
                        switch (move.dir) {
                            case NORTH:
                                zb.bUp.setTag(move);
                                zb.bUp.setVisibility(View.VISIBLE);
                                break;
                            case SOUTH:
                                zb.bDown.setTag(move);
                                zb.bDown.setVisibility(View.VISIBLE);
                                break;
                            case EAST:
                                zb.bRight.setTag(move);
                                zb.bRight.setVisibility(View.VISIBLE);
                                break;
                            case WEST:
                                zb.bLeft.setTag(move);
                                zb.bLeft.setVisibility(View.VISIBLE);
                                break;
                            case ASCEND:
                            case DESCEND:
                                zb.bVault.setTag(move);
                                zb.bVault.setVisibility(View.VISIBLE);
                                break;
                        }
                        it.remove();
                        break;

                    case USE_LEFT_HAND:
                        zb.bUseleft.setTag(move);
                        zb.bUseleft.setVisibility(View.VISIBLE);
                        it.remove();
                        break;
                    case USE_RIGHT_HAND:
                        zb.bUseright.setTag(move);
                        zb.bUseright.setVisibility(View.VISIBLE);
                        it.remove();
                        break;
                    case SWITCH_ACTIVE_CHARACTER:
                        zb.bCenter.setTag(move);
                        zb.bCenter.setVisibility(View.VISIBLE);
                        it.remove();
                        break;
                }
            }
        }
    }

    void clearKeypad() {
        zb.bUseleft.setVisibility(View.INVISIBLE);
        zb.bUseright.setVisibility(View.INVISIBLE);
        zb.bUp.setVisibility(View.INVISIBLE);
        zb.bDown.setVisibility(View.INVISIBLE);
        zb.bLeft.setVisibility(View.INVISIBLE);
        zb.bRight.setVisibility(View.INVISIBLE);
        zb.bVault.setVisibility(View.INVISIBLE);
        zb.bCenter.setTag(null);
    }

    void initMenu(UIZombicide.UIMode mode, List<IButton> _options) {
        List<View> buttons = new ArrayList<>();
        clearKeypad();
        if (_options != null) {
            List<IButton> options = new ArrayList<>(_options);
            initKeypad(options);
            switch (mode) {
                case PICK_CHARACTER:
                    zb.bCenter.setTag(options.get(0));
                case PICK_MENU:
                    for (IButton e : options) {
                        buttons.add(ZButton.build(this, e, e.isEnabled()));
                    }
                    buttons.add(new ListSeparator(this));
                    break;
            }
        }
        for (MenuItem i : Utils.filter(MenuItem.values(), object -> object.isGameButton(ZombicideActivity.this))) {
            buttons.add(ZButton.build(this, i, i.isEnabled(this)));
        }
        initMenuItems(buttons);
    }

    void initMenuItems(List<View> buttons) {
        vm.listAdapter.update(buttons);
    }

    Set<String> getDefaultPlayers() {
        HashSet<String> players = new HashSet<>();
        players.add(ZPlayerName.Baldric.name());
        players.add(ZPlayerName.Clovis.name());
        players.add(ZPlayerName.Silas.name());
        return players;
    }

    /*
        Set<String> getDefaultUnlockedPlayers() {
            HashSet<String> players = new HashSet<>();
            players.add(ZPlayerName.Baldric.name());
            players.add(ZPlayerName.Clovis.name());
            players.add(ZPlayerName.Silas.name());
            players.add(ZPlayerName.Ann.name());
            players.add(ZPlayerName.Nelly.name());
            players.add(ZPlayerName.Samson.name());
            return players;
        }
    */

    void showAssignDialog() {
        Set<String> selectedPlayers = new HashSet(getStoredCharacters());

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new RecyclerView.Adapter<Holder>() {
            @Override
            public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                AssignDialogItemBinding ab = AssignDialogItemBinding.inflate(getLayoutInflater(), parent, false);
                return new Holder(ab);
            }

            @Override
            public void onBindViewHolder(@NonNull Holder holder, int position) {
                ZPlayerName player = ZPlayerName.values()[position];
                holder.ab.image.setImageResource(player.cardImageId);
                holder.ab.image.setTag(player.name());
                holder.ab.checkbox.setChecked(selectedPlayers.contains(player.name()));
                holder.ab.image.setOnClickListener(v -> {
                    if (selectedPlayers.contains(player.name())) {
                        selectedPlayers.remove(player.name());
                        holder.ab.checkbox.setChecked(false);
                    } else {
                        selectedPlayers.add(player.name());
                        holder.ab.checkbox.setChecked(true);
                    }
                });
                holder.ab.checkbox.setClickable(false);
            }

            @Override
            public int getItemCount() {
                return ZPlayerName.values().length;
            }
        });
        newDialogBuilder().setTitle(R.string.popup_title_assign).setView(recyclerView).setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_ok, (dialog, which) -> {
                    Log.d(TAG, "Selected players: " + selectedPlayers);
                    getPrefs().edit().putStringSet(PREF_PLAYERS, selectedPlayers).apply();
                    loadCharacters(selectedPlayers);
                    game.reload();
                }).show();
    }

    public static class Holder extends RecyclerView.ViewHolder {

        final AssignDialogItemBinding ab;

        public Holder(AssignDialogItemBinding ab) {
            super(ab.getRoot());
            this.ab = ab;
        }
    }

    public void showEmailReportDialog() {
        EditText message = new EditText(this);
        message.setMinLines(5);
        newDialogBuilder().setTitle(R.string.popup_title_email)
                .setView(message)
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_send, (dialog, which) -> {
                    //char [] pw = { 'z', '0', 'm', 'b', '1', '3', '$', '4', 'e', 'v', 'a' };
                    new EmailTask(ZombicideActivity.this,
                            message.getEditableText().toString()
                            //, new String(pw)
                    ).execute(gameFile);
                }).show();
    }

    static class EmailTask extends AsyncTask<File, Void, Exception> {

        final static String TAG = "EmailTask";

        private final CCActivityBase context;
        private Dialog progress;
        private final String message;

        public EmailTask(CCActivityBase context, String message) {
            this.context = context;
            this.message = message;
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(context, null, context.getString(R.string.popup_message_sending_report));
        }

        @Override
        protected Exception doInBackground(File... inFile) {

            try {
                String date = DateFormat.format("MMddyyyy", new Date()).toString();
                File zipFile = new File(context.getFilesDir(), "zh_" + date + ".zip");
                List<File> files = FileUtils.getFileAndBackups(inFile[0]);
                FileUtils.zipFiles(zipFile, files);
                String fileSize = DroidUtils.getHumanReadableFileSize(context, zipFile);
                Log.d(TAG, "Zipped file size: " + fileSize);
                EmailHelper.sendEmail(context, zipFile, "ccaronsoftware@gmail.com", "Zombies Hide Report", message);

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            progress.dismiss();
            if (e != null) {
                context.newDialogBuilder().setTitle(R.string.popup_title_error)
                        .setMessage("An error occurred trying to send report:\n" + e.getClass().getSimpleName() + " " + e.getMessage())
                        .setNegativeButton(R.string.popup_button_ok, null).show();
            }
        }
    }

}

