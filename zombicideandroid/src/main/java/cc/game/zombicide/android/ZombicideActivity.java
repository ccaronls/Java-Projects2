package cc.game.zombicide.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
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
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
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
import cc.lib.utils.FileUtils;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZDiffuculty;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZEquipSlot;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZSkillLevel;
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
 */
public class ZombicideActivity extends CCActivityBase implements View.OnClickListener, ListView.OnItemClickListener, RadioButton.OnCheckedChangeListener {

    private final static String TAG = ZombicideActivity.class.getSimpleName();

    ListView menu;
    ZBoardView boardView;
    ZCharacterView consoleView;
    File gameFile;
    ViewGroup buttonsGrid;

    UIZombicide game;
    final ZUser user = new UIZUser();

    View bLH, bUp, bRH, bLeft, bToggle, bRight, bZoom, bDown, bVault;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zombicide);
        menu = findViewById(R.id.list_menu);
        menu.setOnItemClickListener(this);
        boardView = findViewById(R.id.board_view);
        consoleView = findViewById(R.id.console_view);
        buttonsGrid = findViewById(R.id.buttons_layout);
        buttonsGrid.setVisibility(View.GONE);
        (bZoom = findViewById(R.id.b_zoom)).setOnClickListener(this);
        (bUp = findViewById(R.id.b_up)).setOnClickListener(this);
        (bLH = findViewById(R.id.b_useleft)).setOnClickListener(this);
        (bRH = findViewById(R.id.b_useright)).setOnClickListener(this);
        (bToggle = findViewById(R.id.b_switch_character)).setOnClickListener(this);
        (bVault = findViewById(R.id.b_vault)).setOnClickListener(this);
        (bLeft = findViewById(R.id.b_left)).setOnClickListener(this);
        (bDown = findViewById(R.id.b_down)).setOnClickListener(this);
        (bRight = findViewById(R.id.b_right)).setOnClickListener(this);
        ((CompoundButton)findViewById(R.id.b_toggleConsole)).setOnCheckedChangeListener(this);

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
                    if (BuildConfig.DEBUG)
                        FileUtils.backupFile(gameFile, 20);
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
                runOnUiThread(() -> {
                    stopGame();
                    completeQuest(getQuest().getQuest());
                    initHomeMenu();
                });
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
        game.setDifficulty(getSavedDifficulty());
        gameFile = new File(getFilesDir(), "game.save");
        if (!gameFile.exists() || !game.tryLoadFromFile(gameFile)) {
            showWelcomeDialog();
        } else {
            game.showSummaryOverlay();
        }

        initHomeMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopGame();
    }

    @Override
    public AlertDialog.Builder newDialogBuilder() {
        return new AlertDialog.Builder(this, R.style.ZDialogTheme) {
            @Override
            public AlertDialog show() {
                AlertDialog d = super.show();
                d.setCanceledOnTouchOutside(false);
                return d;
            }
        };
    }

    enum MenuItem implements UIZButton {
        RESUME,
        CANCEL,
        LOAD,
        NEW_GAME,
        START,
        ASSIGN,
        SUMMARY,
        UNDO,
        DIFFICULTY,
        OBJECTIVES,
        SKILLS,
        LEGEND,
        QUIT,
        CLEAR_PLAYABLE,
        VIEW_REMAINING;

        boolean isHomeButton(ZombicideActivity instance) {
            switch (this) {
                case LOAD:
                case ASSIGN:
                case UNDO:
                case CLEAR_PLAYABLE:
                    return BuildConfig.DEBUG;
                case START:
                case NEW_GAME:
                case DIFFICULTY:
                case SKILLS:
                case LEGEND:
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
                case NEW_GAME:
                case CLEAR_PLAYABLE:
                    return false;
                case CANCEL:
                    return instance.game.canGoBack();
                case VIEW_REMAINING:
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
    }

    ZDiffuculty getSavedDifficulty() {
        return ZDiffuculty.valueOf(getPrefs().getString("difficulty", ZDiffuculty.MEDIUM.name()));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        findViewById(R.id.sv_console).setVisibility(isChecked ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
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

    void startGame() {
        game.startGameThread();
        buttonsGrid.setVisibility(View.VISIBLE);
    }

    void stopGame() {
        game.stopGameThread();
        buttonsGrid.setVisibility(View.GONE);
    }

    void processMainMenuItem(MenuItem item) {
        switch (item) {
            case START:
                game.reload();
                startGame();
                break;
            case RESUME:
                if (game.tryLoadFromFile(gameFile)) {
                    game.boardRenderer.setOverlay(game.getQuest().getObjectivesOverlay(game));
                    startGame();
                    boardView.redraw();
                }
                break;
            case QUIT:
                stopGame();
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
                game.showSummaryOverlay();
                break;
            }
            case NEW_GAME: {
                // choose level (if multiple available)
                //   choose difficulty
                //      assign players
                showNewGameDialog();
                break;
            }
            case CLEAR_PLAYABLE: {
                getPrefs().edit().remove("completedQuests").apply();
                break;
            }
            case VIEW_REMAINING: {
                ListView lv = new ListView(this);
                List<ZEquipment> searchables = new ArrayList(game.getAllSearchables());
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
                        ((TextView)convertView).setText(searchables.get(position).getLabel());
                        return convertView;
                    }
                });
                newDialogBuilder().setTitle("SEARCHABLES").setView(lv).setNegativeButton("Close", null).show();
                break;
            }
            case LOAD:
                newDialogBuilder().setTitle("Load Quest")
                        .setItems(Utils.toStringArray(ZQuests.values(), true), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ZQuests q = ZQuests.values()[which];
                                game.loadQuest(q);
                                game.showObjectivesOverlay();
                                game.trySaveToFile(gameFile);
                                boardView.postInvalidate();
                                initHomeMenu();
                            }
                        }).setNegativeButton("CANCEL", null).show();
                break;
            case ASSIGN:
                showAssignDialog();
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
                showSkillsDialog2();
                break;
            }

            case UNDO: {
                stopGame();
                game.setResult(null);
                initHomeMenu();
                if (FileUtils.restoreFile(gameFile)) {
                    game.tryLoadFromFile(gameFile);
                    boardView.redraw();
                }
                break;
            }

            case LEGEND: {
                showLegendDialog();
                break;
            }
        }
    }

    void showLegendDialog() {
        Object [][] legend = {
                { R.drawable.legend_chars, "CHARACTERS\nChoose your characters. Some are unlockable. Players can operate in any order but must execute all of their actions before switching to another player." },
                { R.drawable.legend_gamepad, "GAMEPAD\nUse gamepad to perform common actions.\nLH / RH - Use Item in Left/Right hand.\nO - Toggle active player.\nZM - Zoom in/out."},
                { R.drawable.legend_obj, "OBJECTIVES\nObjectives give player EXP, unlock doors, reveal special items and other things related to the Quest."},
                { R.drawable.legend_vault, "VAULTS\nVaults are very handy. You can find special loot or drop loot to be pickup up later. You can also close zombies in or out of the vault. Also they can sometimes be shortcuts across the map. The only limitation is you cannot fire ranged weapons or magic into the vault from the outside or fire outside of vault from inside of it." },
                { R.drawable.zwalker1, "WALKER\n" + ZZombieType.Walker.getDescription() },
                { R.drawable.zfatty1, "FATTY\n" + ZZombieType.Fatty.getDescription() },
                { R.drawable.zrunner1, "RUNNER\n" + ZZombieType.Runner.getDescription() },
                { R.drawable.znecro, "NECROMANCER\n" + ZZombieType.Necromancer.getDescription() },
                { R.drawable.zabomination, "ABOMINATION\n" + ZZombieType.Abomination.getDescription() },

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
                TextView  tv_desc  = convertView.findViewById(R.id.tv_description);

                Object [] item = legend[position];

                if (item[0] instanceof Integer) {
                    iv_image.setImageResource((Integer)item[0]);
                } else if (item[0] instanceof Bitmap) {
                    iv_image.setImageBitmap((Bitmap)item[0]);
                }

                if (item[1] instanceof Integer) {
                    tv_desc.setText((Integer)item[1]);
                } else if (item[1] instanceof String) {
                    tv_desc.setText((String)item[1]);
                }

                return convertView;
            }
        });

        newDialogBuilder().setTitle("Legend").setView(lv).setNegativeButton("Close", null).show();
    }

    Set<String> getCompletedQuests() {
        return new HashSet(getPrefs().getStringSet("completedQuests", new HashSet<>()));
    }

    void completeQuest(ZQuests quest) {
        Set<String> unlocked = getCompletedQuests();
        unlocked.add(quest.name());
        getPrefs().edit().putStringSet("completedQuests", unlocked).apply();
    }

    void showWelcomeDialog() {
        newDialogBuilder().setTitle("Welcome").setMessage(R.string.welcome_msg).setPositiveButton("New Game", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showNewGameDialog();
            }
        }).show();
    }

    void showNewGameDialog() {
        Set<String> playable = getCompletedQuests();
        Log.d(TAG, "playable quests: " + playable);
        List<ZQuests> allQuests = ZQuests.valuesRelease();
        int firstPage = 0;
        for (ZQuests q: allQuests) {
            if (!playable.contains(q.name())) {
                playable.add(q.name());
                break;
            }
            firstPage = Utils.clamp(firstPage+1, 0, allQuests.size()-1);
        }
        View view = View.inflate(this, R.layout.viewpager_dialog, null);
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
        pager.setCurrentItem(firstPage);
        dialog[0] = newDialogBuilder().setTitle("Choose Quest")
                .setView(view).setPositiveButton("Start", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ZQuests q = allQuests.get(pager.getCurrentItem());
                if (playable.contains(q.name())) {
                    showNewGameDailogChooseDifficulty(q);
                } else {
                    Toast.makeText(ZombicideActivity.this, "Quest Locked", Toast.LENGTH_LONG).show();
                }
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
                }).setNegativeButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showNewGameDialog();
            }
        }).show();

    }

    void showNewGameDialogChoosePlayers(ZQuests quest, ZDiffuculty difficulty) {

        Set<String> selectedPlayers = new HashSet(getPrefs().getStringSet("players", getDefaultPlayers()));
        Set<String> unlockedPlayers = new HashSet(getPrefs().getStringSet("unlockedPlayers", getDefaultUnlockedPlayers()));
        View view = View.inflate(this, R.layout.viewpager_dialog, null);
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
                    checkbox.setClickable(false);
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
                        game.trySaveToFile(gameFile);
                        game.showQuestTitleOverlay();
                        game.setDifficulty(difficulty);
                        game.reload();
                        startGame();
                    }
                }).show();

    }

    class SkillAdapter extends BaseAdapter {

        class Item {
            final String name;
            final String description;
            final GColor color;
            final boolean owned;

            public Item(String name, String description, GColor color, boolean owned) {
                this.name = name;
                this.description = description;
                this.color = color;
                this.owned = owned;
            }
        }

        final List<Item> items = new ArrayList<>();

        SkillAdapter(ZPlayerName pl, ZSkill [][] skills) {
            for (ZSkillLevel lvl : ZSkillLevel.values()) {
                items.add(new Item(lvl.name() + " " + lvl.getDangerPts() + " Danger Points", null, lvl.getColor(), false));
                for (ZSkill skill : skills[lvl.ordinal()]) {
                    boolean owned = pl.getCharacter() != null && pl.getCharacter().hasSkill(skill);
                    items.add(new Item(skill.getLabel(), skill.description, null, owned));
                }
            }
        }

        @Override
        public int getCount() {
            return items.size();
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
                convertView = View.inflate(ZombicideActivity.this, R.layout.skill_list_item, null);
            }
            TextView name = convertView.findViewById(R.id.tv_label);
            TextView desc = convertView.findViewById(R.id.tv_description);

            Item item = items.get(position);
            if (item.color == null) {
                name.setTextColor(GColor.WHITE.toARGB());
                desc.setVisibility(View.VISIBLE);
                desc.setText(item.description);
                if (item.owned) {
                    SpannableString content = new SpannableString(item.name);
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    name.setText(content);
                } else {
                    name.setText(item.name);
                }
            } else {
                name.setText(item.name);
                name.setTextColor(item.color.toARGB());
                desc.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    void showSkillsDialog2() {
        // series of TABS, one for each character and then one for ALL
        ZSkill [][][] skills = new ZSkill[ZPlayerName.values().length][][];
        String [] labels = new String[skills.length];
        int idx = 0;
        for (ZPlayerName pl : ZPlayerName.values()) {
            skills[idx] = new ZSkill[ZSkillLevel.values().length][];
            labels[idx] = pl.getLabel();
            for (ZSkillLevel lvl : ZSkillLevel.values()) {
                skills[idx][lvl.ordinal()] = pl.getSkillOptions(lvl);
            }
            idx++;
        }

        View view = View.inflate(this, R.layout.viewpager_dialog, null);
        ViewPager pager = view.findViewById(R.id.view_pager);
        //pager.setAdapter(new Page
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return skills.length;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View page = View.inflate(ZombicideActivity.this, R.layout.skills_page, null);
                TextView title = page.findViewById(R.id.tv_title);
                title.setText(labels[position]);
                ListView lv = page.findViewById(R.id.lv_list);
                lv.setAdapter(new SkillAdapter(ZPlayerName.values()[position], skills[position]));
                container.addView(page);
                return page;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View)object);
            }
        });

        if (game.getCurrentCharacter() != null) {
            pager.setCurrentItem(game.getCurrentCharacter().getPlayerName().ordinal());
        }

        newDialogBuilder().setTitle("Skills").setView(view).setNegativeButton("Close", null).show();
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

        bToggle.setVisibility((game.getCurrentCharacter() == null || game.canSwitchActivePlayer()) ? View.VISIBLE : View.INVISIBLE);
        bLH.setVisibility(game.canUse(ZEquipSlot.LEFT_HAND) ? View.VISIBLE : View.INVISIBLE);
        bRH.setVisibility(game.canUse(ZEquipSlot.RIGHT_HAND)  ? View.VISIBLE : View.INVISIBLE);
        bUp.setVisibility(game.canWalk(ZDir.NORTH)  ? View.VISIBLE : View.INVISIBLE);
        bDown.setVisibility(game.canWalk(ZDir.SOUTH)  ? View.VISIBLE : View.INVISIBLE);
        bLeft.setVisibility(game.canWalk(ZDir.WEST)  ? View.VISIBLE : View.INVISIBLE);
        bRight.setVisibility(game.canWalk(ZDir.EAST)  ? View.VISIBLE : View.INVISIBLE);
        bVault.setVisibility(game.canWalk(ZDir.ASCEND) || game.canWalk(ZDir.DESCEND)  ? View.VISIBLE : View.INVISIBLE);
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
        View view = View.inflate(this, R.layout.viewpager_dialog, null);
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
