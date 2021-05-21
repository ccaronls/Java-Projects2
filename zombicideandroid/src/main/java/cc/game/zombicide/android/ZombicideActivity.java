package cc.game.zombicide.android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.lib.android.CCActivityBase;
import cc.lib.android.DroidGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.ui.IButton;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZDiffuculty;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZMove;
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
        findViewById(R.id.b_zoom).setOnClickListener(this);
        findViewById(R.id.b_up).setOnClickListener(this);
        findViewById(R.id.b_useleft).setOnClickListener(this);
        findViewById(R.id.b_useright).setOnClickListener(this);
        findViewById(R.id.b_switch_character).setOnClickListener(this);
        findViewById(R.id.b_vault).setOnClickListener(this);
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
                Canvas canvas = g.getCanvas();
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

            @Override
            protected void onQuestComplete() {
                super.onQuestComplete();
                completeQuest(getQuest().getQuest());
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
        NEW_GAME,
        ASSIGN,
        SUMMARY,
        DIFFICULTY,
        OBJECTIVES,
        SKILLS;

        boolean isHomeButton(ZombicideActivity instance) {
            switch (this) {
                case LOAD:
                case ASSIGN:
                    return BuildConfig.DEBUG;
                case START:
                case NEW_GAME:
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
            case R.id.b_zoom: {
                float curZoom = game.boardRenderer.getZoomAmt();
                if (curZoom >= game.boardRenderer.getMaxZoom()) {
                    game.boardRenderer.animateZoomTo(0);
                } else {
                    game.boardRenderer.animateZoomAmount(1.5f);
                }
                game.boardRenderer.redraw();
                break;
            }
            case R.id.b_useleft:
                game.setResult(ZMove.newUseLeftHand());
                break;
            case R.id.b_useright:
                game.setResult(ZMove.newUseRightHand());
                break;
            case R.id.b_switch_character:
                game.trySwitchActivePlayer();
                break;
            case R.id.b_vault:
                if (game.getBoard().canMove(game.getCurrentCharacter(), ZDir.DESCEND)) {
                    game.setResult(ZMove.newWalkDirMove(ZDir.DESCEND));
                } else if (game.getBoard().canMove(game.getCurrentCharacter(), ZDir.ASCEND)) {
                    game.setResult(ZMove.newWalkDirMove(ZDir.ASCEND));
                }
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
            case NEW_GAME: {
                // choose level (if multiple available)
                //   choose difficulty
                //      assign players
                showNewGameDialog();
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
                showAssignDialog2();
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

    Set<String> getCompletedQuests() {
        return getPrefs().getStringSet("completedQuests", new HashSet<>());
    }

    void completeQuest(ZQuests quest) {
        Set<String> unlocked = getCompletedQuests();
        unlocked.add(quest.name());
        getPrefs().edit().putStringSet("completedQuests", unlocked).apply();
    }

    void showNewGameDialog() {
        Set<String> playable = getCompletedQuests();
        List<ZQuests> allQuests = ZQuests.valuesRelease();
        int firstPage = 0;
        for (ZQuests q: allQuests) {
            if (!playable.contains(q.name())) {
                playable.add(q.name());
                break;
            }
            firstPage = Utils.clamp(firstPage+1, 0, allQuests.size()-1);
        }
        View view = View.inflate(this, R.layout.assign_dialog2, null);
        ViewPager pager = view.findViewById(R.id.view_pager);
        final Dialog [] dialog = new Dialog[1];
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return allQuests.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                ZQuests q = allQuests.get(position);
                View content = View.inflate(ZombicideActivity.this, R.layout.choose_quest_dialog_item, null);
                TextView title = content.findViewById(R.id.tv_title);
                TextView body  = content.findViewById(R.id.tv_body);
                ImageView lockedOverlay = content.findViewById(R.id.lockedOverlay);

                title.setText(q.getDisplayName());
                body.setText(q.getDescription());

                if (playable.contains(q.name())) {
                    lockedOverlay.setVisibility(View.INVISIBLE);
                    content.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog[0].dismiss();
                            showNewGameDailogChooseDifficulty(q);
                        }
                    });
                } else {
                    lockedOverlay.setVisibility(View.VISIBLE);
                    content.setOnClickListener(null);
                }
                container.addView(content);
                return content;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View)object);
            }
        });
        dialog[0] = newDialogBuilder().setTitle("Choose Quest")
                .setView(view).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).show();
    }

    void showNewGameDailogChooseDifficulty(ZQuests quest) {
        newDialogBuilder().setTitle("Quest " + quest.ordinal() + " : " + quest.getDisplayName())
                .setItems(Utils.toStringArray(ZDiffuculty.values()), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ZDiffuculty difficulty = ZDiffuculty.values()[which];
                        showNewGameDialogChoosePlayers(quest, difficulty);
                    }
                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showNewGameDialog();
            }
        }).show();

    }

    void showNewGameDialogChoosePlayers(ZQuests quest, ZDiffuculty difficulty) {

        Set<String> selectedPlayers = new HashSet(getPrefs().getStringSet("players", getDefaultPlayers()));
        Set<String> unlockedPlayers = new HashSet(getPrefs().getStringSet("unlockedPlayers", getDefaultUnlockedPlayers()));
        View view = View.inflate(this, R.layout.assign_dialog2, null);
        ViewPager pager = view.findViewById(R.id.view_pager);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return ZPlayerName.values().length;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View view = LayoutInflater.from(ZombicideActivity.this).inflate(R.layout.assign_dialog_item, null);
                ImageView image = view.findViewById(R.id.image);
                CheckBox checkbox = view.findViewById(R.id.checkbox);
                ImageView lockedOverlay = view.findViewById(R.id.lockedOverlay);

                ZPlayerName player = ZPlayerName.values()[position];
                if (!unlockedPlayers.contains(player.name()) && !selectedPlayers.contains(player.name())) {
                    lockedOverlay.setVisibility(View.VISIBLE);
                    checkbox.setEnabled(false);
                } else {
                    lockedOverlay.setVisibility(View.INVISIBLE);
                    checkbox.setEnabled(true);
                    image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (selectedPlayers.contains(player.name())) {
                                if (selectedPlayers.size() > 1) {
                                    selectedPlayers.remove(player.name());
                                    checkbox.setChecked(false);
                                } else {
                                    Toast.makeText(ZombicideActivity.this, "Must have at least one player", Toast.LENGTH_LONG).show();
                                }
                            } else if (selectedPlayers.size() >= MAX_PLAYERS) {
                                Toast.makeText(ZombicideActivity.this, "Can only have " + MAX_PLAYERS + " at a time", Toast.LENGTH_LONG).show();
                            } else {
                                selectedPlayers.add(player.name());
                                checkbox.setChecked(true);
                            }
                        }
                    });
                }

                if (selectedPlayers.contains(player.name())) {
                    checkbox.setChecked(true);
                } else {
                    checkbox.setChecked(false);
                }

                image.setImageResource(player.cardImageId);


                container.addView(view);
                return view;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View)object);
            }
        });
        newDialogBuilder().setTitle("Choose Players").setView(view).setNegativeButton("CANCEL", null)
                .setPositiveButton("START", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Selected players: " + selectedPlayers);
                        getPrefs().edit().putStringSet("players", selectedPlayers).apply();
                        user.clear();
                        for (ZPlayerName pl : Utils.convertToEnumArray(selectedPlayers, ZPlayerName.class, new ZPlayerName[selectedPlayers.size()])) {
                            user.addCharacter(pl);
                        }
                        getPrefs().edit().putString("difficulty", difficulty.name()).apply();
                        game.setUsers(user);
                        game.loadQuest(quest);
                        game.setDifficulty(difficulty);
                        game.reload();
                        game.startGameThread();
                    }
                }).show();

    }

    // TODO: Make this more organized. Should be able to order by character, danger level or all POSSIBLE
    void showSkillsDialog() {
        ZSkill [] sorted = Utils.copyOf(ZSkill.values());
        Arrays.sort(sorted, (o1, o2) -> o1.name().compareTo(o2.name()));
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

    final int MAX_PLAYERS = 4; // max number of characters on screen at one time

    void showAssignDialog2() {
        Set<String> selectedPlayers = new HashSet(getPrefs().getStringSet("players", getDefaultPlayers()));
        Set<String> unlockedPlayers = new HashSet(getPrefs().getStringSet("unlockedPlayers", getDefaultUnlockedPlayers()));
        View view = View.inflate(this, R.layout.assign_dialog2, null);
        ViewPager pager = view.findViewById(R.id.view_pager);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return ZPlayerName.values().length;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View view = LayoutInflater.from(ZombicideActivity.this).inflate(R.layout.assign_dialog_item, null);
                ImageView image = view.findViewById(R.id.image);
                CheckBox checkbox = view.findViewById(R.id.checkbox);
                ImageView lockedOverlay = view.findViewById(R.id.lockedOverlay);

                ZPlayerName player = ZPlayerName.values()[position];
                if (!unlockedPlayers.contains(player.name()) && !selectedPlayers.contains(player.name())) {
                    lockedOverlay.setVisibility(View.VISIBLE);
                    checkbox.setEnabled(false);
                } else {
                    lockedOverlay.setVisibility(View.INVISIBLE);
                    checkbox.setEnabled(true);
                    image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (selectedPlayers.contains(player.name())) {
                                if (selectedPlayers.size() > 1) {
                                    selectedPlayers.remove(player.name());
                                    checkbox.setChecked(false);
                                } else {
                                    Toast.makeText(ZombicideActivity.this, "Must have at least one player", Toast.LENGTH_LONG).show();
                                }
                            } else if (selectedPlayers.size() >= MAX_PLAYERS) {
                                Toast.makeText(ZombicideActivity.this, "Can only have " + MAX_PLAYERS + " at a time", Toast.LENGTH_LONG).show();
                            } else {
                                selectedPlayers.add(player.name());
                                checkbox.setChecked(true);
                            }
                        }
                    });
                }

                if (selectedPlayers.contains(player.name())) {
                    checkbox.setChecked(true);
                } else {
                    checkbox.setChecked(false);
                }

                image.setImageResource(player.cardImageId);


                container.addView(view);
                return view;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View)object);
            }
        });
        newDialogBuilder().setTitle("Choose Players").setView(view).setNegativeButton("CANCEL", null)
                .setPositiveButton("Assign", new DialogInterface.OnClickListener() {
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



    void showAssignDialog() {
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
