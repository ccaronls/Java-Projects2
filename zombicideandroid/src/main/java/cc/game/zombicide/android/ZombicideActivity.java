package cc.game.zombicide.android;

import android.content.DialogInterface;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.lib.android.CCActivityBase;
import cc.lib.android.DroidGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.ui.IButton;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZDiffuculty;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ui.UIZBoardRenderer;
import cc.lib.zombicide.ui.UIZButton;
import cc.lib.zombicide.ui.UIZCharacterRenderer;
import cc.lib.zombicide.ui.UIZUser;
import cc.lib.zombicide.ui.UIZombicide;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ZombicideActivity extends CCActivityBase implements View.OnClickListener, ListView.OnItemClickListener {

    private final static String TAG = ZombicideActivity.class.getSimpleName();

    ListView menu;
    ZBoardView boardView;
    ZCharacterView consoleView;
    File gameFile;

    UIZombicide game;
    final ZUser user = new UIZUser();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zombicide);
        menu = findViewById(R.id.list_menu);
        menu.setOnItemClickListener(this);
        boardView = findViewById(R.id.board_view);
        consoleView = findViewById(R.id.console_view);
        findViewById(R.id.b_zoomin).setOnClickListener(this);
        findViewById(R.id.b_up).setOnClickListener(this);
        findViewById(R.id.b_zoomout).setOnClickListener(this);
        findViewById(R.id.b_left).setOnClickListener(this);
        findViewById(R.id.b_down).setOnClickListener(this);
        findViewById(R.id.b_right).setOnClickListener(this);
        UIZCharacterRenderer cr = new UIZCharacterRenderer(consoleView);
        UIZBoardRenderer br = new UIZBoardRenderer<DroidGraphics>(boardView) {

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


            @Override
            protected void drawActor(DroidGraphics g, ZActor actor, GColor outline) {
                if (outline == null)
                    outline = GColor.WHITE;
                Paint outlinePaint = getPaint(outline);
                Paint cur = g.getPaint();
                Canvas canvas = g.getCanvas();
                AImage img = g.getImage(actor.getImageId());
                RectF rect = g.setRectF(actor.getRect());
                canvas.save();
                float cx = rect.centerX();
                float cy = rect.centerY();
                canvas.scale(1.15f, 1.1f, cx, cy);
                canvas.drawBitmap(g.getBitmap(actor.getImageId()), null, rect, outlinePaint);
                canvas.restore();
                actor.draw(g);
            }
        };
        br.setDrawTiles(true);

        game = new UIZombicide(cr, br) {

            @Override
            public void runGame() {
                try {
                    super.runGame();
                    trySaveToFile(gameFile);
                    boardView.postInvalidate();
                    consoleView.postInvalidate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public <T> T waitForUser(Class<T> expectedType) {
                boardView.post(() -> initMenu(getUiMode(), getOptions()));
                return super.waitForUser(expectedType);
            }
        };

        Set<String> playersSet = getPrefs().getStringSet("players", getDefaultPlayers());
        for (ZPlayerName player : Utils.convertToEnumArray(playersSet, ZPlayerName.class, new ZPlayerName[playersSet.size()])) {
            user.addCharacter(player);
        }
        game.setUsers(user);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            game.loadQuest(ZQuests.valueOf(getPrefs().getString("quest", ZQuests.Tutorial.name())));
        } catch (Exception e) {
            e.printStackTrace();
            game.loadQuest(ZQuests.Tutorial);
        }
        game.setDifficulty(getSavedDifficulty());

        gameFile = new File(getFilesDir(), "game.save");
        initHomeMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        game.stopGameThread();
    }

    enum MenuItem implements UIZButton {
        START,
        RESUME,
        QUIT,
        CANCEL,
        LOAD,
        ASSIGN,
        SUMMARY,
        DIFFICULTY,
        OBJECTIVES,
        SKILLS;

        boolean isHomeButton(ZombicideActivity instance) {
            switch (this) {
                case LOAD:
                case START:
                case ASSIGN:
                case DIFFICULTY:
                case SKILLS:
                    return true;
                case RESUME:
                    return instance.gameFile != null && instance.gameFile.exists();
            }
            return false;
        }

        boolean isGameButton(ZombicideActivity instance) {
            switch (this) {
                case LOAD:
                case START:
                case ASSIGN:
                case RESUME:
                    return false;
                case CANCEL:
                    return instance.game.canGoBack();
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
    }

    ZDiffuculty getSavedDifficulty() {
        return ZDiffuculty.valueOf(getPrefs().getString("difficulty", ZDiffuculty.MEDIUM.name()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_zoomin:
                game.getBoard().zoom(1);
                game.boardRenderer.redraw();
                break;
            case R.id.b_zoomout:
                game.getBoard().zoom(-1);
                game.boardRenderer.redraw();
                break;
            case R.id.b_up:
                game.tryWalk(ZDir.NORTH);
                break;
            case R.id.b_left:
                game.tryWalk(ZDir.WEST);
                break;
            case R.id.b_down:
                game.tryWalk(ZDir.SOUTH);
                break;
            case R.id.b_right:
                game.tryWalk(ZDir.EAST);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof MenuItem) {
            processMainMenuItem((MenuItem)view.getTag());
        } else {
            UIZombicide.getInstance().setResult(view.getTag());
        }
    }

    void processMainMenuItem(MenuItem item) {
        switch (item) {
            case START:
                game.reload();
                game.startGameThread();
                break;
            case RESUME:
                if (game.tryLoadFromFile(gameFile)) {
                    game.boardRenderer.setOverlay(game.getQuest().getObjectivesOverlay(game));
                    game.startGameThread();
                }
                break;
            case QUIT:
                game.stopGameThread();
                game.setResult(null);
                initHomeMenu();
                break;
            case CANCEL:
                if (game.isGameRunning()) {
                    game.goBack();
                    game.setResult(null);
                } else {
                    initHomeMenu();
                }
                break;
            case OBJECTIVES: {
                game.boardRenderer.setOverlay(game.getQuest().getObjectivesOverlay(game));
                break;
            }
            case SUMMARY: {
                game.boardRenderer.setOverlay(game.getGameSummaryTable());
                break;
            }
            case LOAD:
                newDialogBuilder().setTitle("Load Quest")
                        .setItems(Utils.toStringArray(ZQuests.values(), true), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ZQuests q = ZQuests.values()[which];
                                game.loadQuest(q);
                                getPrefs().edit().putString("quest", q.name()).apply();
                                boardView.postInvalidate();
                                initHomeMenu();
                            }
                        }).setNegativeButton("CANCEL", null).show();
                break;
            case ASSIGN:
                openAssignDialog();
                break;
            case DIFFICULTY: {
                newDialogBuilder().setTitle("DIFFICULTY: " + getSavedDifficulty())
                        .setItems(Utils.toStringArray(ZDiffuculty.values()), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ZDiffuculty difficulty = ZDiffuculty.values()[which];
                                game.setDifficulty(difficulty);
                                getPrefs().edit().putString("difficulty", difficulty.name()).apply();
                            }
                        }).setNegativeButton("CANCEL", null).show();
                break;
            }
            case SKILLS: {
                showSkillsDialog();
            }
        }
    }

    // TODO: Make this more organized. Should be able to order by character, danger level or all POSSIBLE
    void showSkillsDialog() {
        ZSkill [] sorted = Utils.copyOf(ZSkill.values());//, ZSkill.values().length);
        Arrays.sort(sorted, new Comparator<ZSkill>() {
            @Override
            public int compare(ZSkill o1, ZSkill o2) {
                return o1.name().compareTo(o2.name());
            }
        });
        newDialogBuilder().setTitle("SKILLS")
                .setItems(Utils.toStringArray(sorted, true), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ZSkill skill = sorted[which];
                        newDialogBuilder().setTitle(skill.getLabel())
                                .setMessage(skill.description)
                                .setNegativeButton("CANCEL", null)
                                .setPositiveButton("BACK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        showSkillsDialog();
                                    }
                                }).show();
                    }
                }).setNegativeButton("CANCEL", null).show();

    }

    void initHomeMenu() {
        List<View> buttons = new ArrayList<>();
        for (MenuItem i : Utils.filterItems(object -> object.isHomeButton(ZombicideActivity.this), MenuItem.values())) {
            buttons.add(ZButton.build(this, i));
        }
        initMenuItems(buttons);
    }

    void initMenu(UIZombicide.UIMode mode, List<IButton> options) {
        List<View> buttons = new ArrayList<>();
        if (mode == UIZombicide.UIMode.PICK_MENU) {
            for (IButton e : options) {
                buttons.add(ZButton.build(this, e));
            }
            buttons.add(new ListSeparator(this));
        }
        for (MenuItem i : Utils.filterItems(object -> object.isGameButton(ZombicideActivity.this), MenuItem.values())) {
            buttons.add(ZButton.build(this, i));
        }
        initMenuItems(buttons);
    }

    void initMenuItems(List<View> buttons) {
        menu.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return buttons.size();
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
                return buttons.get(position);
            }
        });
    }

    Set<String> getDefaultPlayers() {
        HashSet<String> players = new HashSet<>();
        players.add(ZPlayerName.Baldric.name());
        players.add(ZPlayerName.Clovis.name());
        players.add(ZPlayerName.Silas.name());
        return players;
    }


    void openAssignDialog() {
        Set<String> selectedPlayers = new HashSet(getPrefs().getStringSet("players", getDefaultPlayers()));

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new RecyclerView.Adapter<Holder>() {
            @Override
            public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new Holder(LayoutInflater.from(ZombicideActivity.this).inflate(R.layout.assign_dialog_item, parent, false));
            }

            @Override
            public void onBindViewHolder(Holder holder, int position) {
                ZPlayerName player = ZPlayerName.values()[position];
                holder.image.setImageResource(player.cardImageId);
                holder.image.setTag(player.name());
                holder.checkbox.setChecked(selectedPlayers.contains(player.name()));
                holder.image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selectedPlayers.contains(player.name())) {
                            selectedPlayers.remove(player.name());
                            holder.checkbox.setChecked(false);
                        } else {
                            selectedPlayers.add(player.name());
                            holder.checkbox.setChecked(true);
                        }
                    }
                });
                holder.checkbox.setClickable(false);
            }

            @Override
            public int getItemCount() {
                return ZPlayerName.values().length;
            }
        });
        newDialogBuilder().setTitle("ASSIGN").setView(recyclerView).setNegativeButton("CANCEL", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Selected players: " + selectedPlayers);
                        getPrefs().edit().putStringSet("players", selectedPlayers).apply();
                        user.clear();
                        for (ZPlayerName pl : Utils.convertToEnumArray(selectedPlayers, ZPlayerName.class, new ZPlayerName[selectedPlayers.size()])) {
                            user.addCharacter(pl);
                        }
                        game.setUsers(user);
                        game.reload();
                    }
                }).show();
    }

    public static class Holder extends RecyclerView.ViewHolder {

        ImageView image;
        CheckBox checkbox;

        public Holder(View itemView) {
            super(itemView);
            image = (ImageView)itemView.findViewById(R.id.image);
            checkbox = (CheckBox)itemView.findViewById(R.id.checkbox);
        }
    }

}
