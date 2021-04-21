package cc.game.android.checkerboard;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.checkerboard.CanadianDraughts;
import cc.lib.checkerboard.Checkers;
import cc.lib.checkerboard.Chess;
import cc.lib.checkerboard.Color;
import cc.lib.checkerboard.Columns;
import cc.lib.checkerboard.Dama;
import cc.lib.checkerboard.DragonChess;
import cc.lib.checkerboard.Draughts;
import cc.lib.checkerboard.Game;
import cc.lib.checkerboard.KingsCourt;
import cc.lib.checkerboard.PieceType;
import cc.lib.checkerboard.Shashki;
import cc.lib.checkerboard.Suicide;
import cc.lib.checkerboard.UIGame;
import cc.lib.checkerboard.UIPlayer;
import cc.lib.checkerboard.Ugolki;

public class MainActivity extends DroidActivity {

    private UIGame game = new UIGame() {
        @Override
        public void repaint(long delayMs) {
            getContent().postInvalidate();
        }

        @Override
        protected int getCheckerboardImageId() {
            return R.mipmap.wood_checkerboard_8x8;
        }

        @Override
        protected int getKingsCourtBoardId() {
            return R.mipmap.kings_court_board_8x8;
        }

        public int getPieceImageId(PieceType p, Color color) {
            switch (p) {
                case PAWN:
                case PAWN_IDLE:
                case PAWN_ENPASSANT:
                case PAWN_TOSWAP:
                    return color == Color.WHITE ? R.mipmap.wt_pawn : R.mipmap.bk_pawn;
                case BISHOP:
                    return color == Color.WHITE ? R.mipmap.wt_bishop : R.mipmap.bk_bishop;
                case KNIGHT_R:
                case KNIGHT_L:
                    return color == Color.WHITE ? R.mipmap.wt_knight : R.mipmap.bk_knight;
                case ROOK:
                case ROOK_IDLE:
                    return color == Color.WHITE ? R.mipmap.wt_rook : R.mipmap.bk_rook;
                case QUEEN:
                    return color == Color.WHITE ? R.mipmap.wt_queen : R.mipmap.bk_queen;
                case CHECKED_KING:
                case CHECKED_KING_IDLE:
                case UNCHECKED_KING:
                case UNCHECKED_KING_IDLE:
                case KING:
                    return color == Color.WHITE ? R.mipmap.wt_king : R.mipmap.bk_king;
                case DRAGON_R:
                case DRAGON_L:
                case DRAGON_IDLE_R:
                case DRAGON_IDLE_L:
                    return color == Color.WHITE ? R.mipmap.wt_dragon : R.mipmap.bk_dragon;
                case FLYING_KING:
                    break;
                case CHECKER:
                case DAMA_MAN:
                case DAMA_KING:
                case CHIP_4WAY:
                    switch (color) {
                        case RED:
                            return R.mipmap.red_checker;
                        case WHITE:
                            return R.mipmap.wt_checker;
                        case BLACK:
                            return R.mipmap.blk_checker;
                    }
            }
            return 0;
        }
    };

    File saveFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveFile = new File(getFilesDir(), "save.game");
        if (!game.init(saveFile)) {
            saveFile.delete();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (saveFile.exists()) {
            newDialogBuilder().setTitle("Resume Previous " + game.getRules().getClass().getSimpleName() + " Game?")
                    .setItems(new String[]{"Resume", "New Game"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    game.tryLoadFromFile(saveFile);
                                    game.repaint(-1);
                                    break;
                                case 1:
                                    showNewGameDialog();
                                    break;
                            }
                        }
                    }).show();
            return;
        }
        showNewGameDialog();
    }

    void showNewGameDialog() {
        String [] items = { "Checkers", "Suicide", "Draughts", "Canadian Draughts", "Dama", "Chess", "Dragon Chess", "Ugolki", "Columns", "Kings Court", "Shashki" };
        newDialogBuilder().setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                game.stopGameThread();
                switch (items[which]) {
                    case "Checkers":
                        game.setRules(new Checkers());
                        break;
                    case "Suicide":
                        game.setRules(new Suicide());
                        break;
                    case "Draughts":
                        game.setRules(new Draughts());
                        break;
                    case "Canadian Draughts":
                        game.setRules(new CanadianDraughts());
                        break;
                    case "Dama":
                        game.setRules(new Dama());
                        break;
                    case "Chess":
                        game.setRules(new Chess());
                        break;
                    case "Dragon Chess":
                        game.setRules(new DragonChess());
                        break;
                    case "Ugolki":
                        game.setRules(new Ugolki());
                        break;
                    case "Columns":
                        game.setRules(new Columns());
                        break;
                    case "Kings Court":
                        game.setRules(new KingsCourt());
                        break;
                    case "Shashki":
                        game.setRules(new Shashki());
                        break;
                }
                showChoosePlayersDialog();
            }
        }).show();
    }

    void showChoosePlayersDialog() {
        newDialogBuilder().setTitle("How many players?")
                .setItems(new String[]{"One", "Two"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                showChooseDifficultyDialog((dialog1, which1) -> showChoosePlayersDialog());
                                break;
                            case 1:
                                game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                                game.setPlayer(Game.FAR,  new UIPlayer(UIPlayer.Type.USER));
                                game.repaint(-1);
                        }
                    }
                }).setNegativeButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showNewGameDialog();
            }
        }).show();
    }

    void showChooseDifficultyDialog(DialogInterface.OnClickListener onCancelAction) {
        newDialogBuilder().setTitle("Difficulty set to " + getDifficultyString())
                .setItems(new String[]{"Easy", "Medium", "Hard"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getPrefs().edit().putInt("difficulty", which).apply();
                        game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                        game.setPlayer(Game.FAR,  new UIPlayer(UIPlayer.Type.AI, which+1));
                        startGame();
                    }
                }).setNegativeButton("Cancel", onCancelAction).show();
    }

    int getDifficulty() {
        return getPrefs().getInt("difficulty", 2);
    }

    String getDifficultyString() {
        int d;
        switch (d=getDifficulty()) {
            case 1:
                return "Easy";
            case 2:
                return "Medium";
            case 3:
                return "Hard";
        }
        return String.valueOf(d);
    }

    void startGame() {
        game.newGame();
        game.trySaveToFile(saveFile);
        game.repaint(-1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        File saveFile = new File(getFilesDir(), "save.game");
        game.stopGameThread();
        game.trySaveToFile(saveFile);
    }

    @Override
    protected void onDraw(DroidGraphics g) {
        g.setIdentity();
        g.ortho();
        g.getPaint().setTextSize(getResources().getDimension(R.dimen.txt_size_normal));
        game.draw(g, touchX, touchY);
        if (clicked) {
            game.doClick();
            clicked = false;
        }
    }

    int touchX, touchY;
    boolean clicked = false;

    @Override
    protected void onTouchDown(float x, float y) {
        touchX = (int)x;
        touchY = (int)y;
        clicked = true;
        if (!game.isGameRunning()) {
            game.startGameThread();
        } else {
            game.repaint(-1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        game.stopGameThread();
        menu.add("New Game");
        menu.add("Rules");
        menu.add("Difficulty");
        menu.add("Players");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getTitle().toString()) {
            case "New Game":
                game.stopGameThread();
                showNewGameDialog();
                break;
            case "Rules":
                game.stopGameThread();
                break;
            case "Difficulty":
                game.stopGameThread();
                showChooseDifficultyDialog(null);
                break;
            case "Players":
                showChoosePlayersDialog();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (game.canUndo()) {
            game.stopGameThread();
            game.undoAndRefresh();
        } else {
            super.onBackPressed();
        }
    }
}