package cc.game.zombicide.android;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.lib.android.CCActivityBase;
import cc.lib.android.DroidGraphics;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.ui.IButton;
import cc.lib.zombicide.ZDiffuculty;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ui.UIZBoardRenderer;
import cc.lib.zombicide.ui.UIZCharacterRenderer;
import cc.lib.zombicide.ui.UIZUser;
import cc.lib.zombicide.ui.UIZombicide;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ZombicideActivity extends CCActivityBase {

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
        menu = (ListView)findViewById(R.id.list_menu);
        boardView = (ZBoardView)findViewById(R.id.board_view);
        consoleView = (ZCharacterView) findViewById(R.id.console_view);
        UIZCharacterRenderer cr = new UIZCharacterRenderer(consoleView);
        UIZBoardRenderer br = new UIZBoardRenderer(boardView);

        game = new UIZombicide(cr, br) {

            @Override
            public void runGame() {
                try {
                    super.runGame();
                    boardView.postInvalidate();
                    consoleView.postInvalidate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.runGame();
            }

            @Override
            public <T> T waitForUser(Class<T> expectedType) {
                boardView.post(() -> initMenu(getUiMode(), getOptions()));
                return super.waitForUser(expectedType);
            }

            @Override
            public int[] loadTiles(AGraphics g, String[] names, int[] orientations) {
                return boardView.loadTiles((DroidGraphics)g, names, orientations);
            }
        };

        Set<String> playersSet = getPrefs().getStringSet("players", getDefaultPlayers());
        for (ZPlayerName player : Utils.convertToEnumArray(playersSet, ZPlayerName.class, new ZPlayerName[playersSet.size()])) {
            user.addCharacter(player);
        }
        game.setUsers(user);
        try {
            game.loadQuest(ZQuests.valueOf(getPrefs().getString("quest", ZQuests.Tutorial.name())));
        } catch (Exception e) {
            e.printStackTrace();
            game.loadQuest(ZQuests.Tutorial);
        }
        game.setDifficulty(ZDiffuculty.valueOf(getPrefs().getString("difficulty", ZDiffuculty.MEDIUM.name())));

        gameFile = new File(getFilesDir(), "game.save");
        initHomeMenu();
    }

    enum MenuItem {
        START,
        RESUME,
        QUIT,
        BACK,
        LOAD,
        ASSIGN,
        SUMMARY,
        DIFFICULTY,
        OBJECTIVES;

        boolean isHomeButton(ZombicideActivity instance) {
            switch (this) {
                case LOAD:
                case START:
                case ASSIGN:
                case DIFFICULTY:
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
            }
            return true;
        }

    }

    View.OnClickListener optionClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            UIZombicide.getInstance().setResult(v.getTag());
        }
    };

    View.OnClickListener mainMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MenuItem item = (MenuItem)v.getTag();
            switch (item) {
                case START:
                    game.reload();
                    game.boardRenderer.setOverlay(game.getQuest().getObjectivesOverlay(game));
                    game.startGameThread();
                    break;
                case RESUME:
                    if(game.tryLoadFromFile(gameFile)) {
                        game.startGameThread();
                    }
                    break;
                case QUIT:
                    game.stopGameThread();
                    game.setResult(null);
                    initHomeMenu();
                    break;
                case BACK:
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
                            .setItems(Utils.toStringArray(ZQuests.values()), new DialogInterface.OnClickListener() {
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
                    openAssignDislaog();
                    break;
                case DIFFICULTY:
                    newDialogBuilder().setTitle("DIFFICULTY")
                            .setItems(Utils.toStringArray(ZDiffuculty.values()), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ZDiffuculty difficulty = ZDiffuculty.values()[which];
                                    game.setDifficulty(difficulty);
                                    getPrefs().edit().putString("difficulty", difficulty.name()).apply();
                                }
                            }).setNegativeButton("CANCEL", null).show();
            }

        }
    };

    void initHomeMenu() {
        List<View> buttons = new ArrayList<>();
        for (MenuItem i : Utils.filterItems(object -> object.isHomeButton(ZombicideActivity.this), MenuItem.values())) {
            buttons.add(new ZButton(this, i, mainMenuClickListener));
        }
        initMenuItems(buttons);
    }

    void initMenu(UIZombicide.UIMode mode, List<IButton> options) {
        List<View> buttons = new ArrayList<>();
        if (mode == UIZombicide.UIMode.PICK_MENU) {
            for (IButton e : options) {
                buttons.add(new ZButton(this, e.getLabel(), e, optionClickListener));
            }
            buttons.add(new ListSeparator(this));
        }
        for (MenuItem i : Utils.filterItems(object -> object.isGameButton(ZombicideActivity.this), MenuItem.values())) {
            buttons.add(new ZButton(this, i, mainMenuClickListener));
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


    void openAssignDislaog() {
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
