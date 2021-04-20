package cc.game.android.checkerboard;

import android.content.DialogInterface;
import android.os.Bundle;

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
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;

public class MainActivity extends DroidActivity {

    private UIGame game = new UIGame() {
        @Override
        public void repaint(long delayMs) {
            getContent().postInvalidate();
        }

        @Override
        protected int getCheckerboardImageId() {
            return R.drawable.wood_checkerboard_8x8;
        }

        @Override
        protected int getKingsCourtBoardId() {
            return R.drawable.kings_court_board_8x8;
        }


        public int getPieceImageId2(PieceType p, Color color) {
            for (Images i : Images.values()) {
                if (i.color == color && Utils.linearSearch(i.pt, p) >= 0)
                    return ids[i.ordinal()];
            }
            return -1;
        }

        public int getPieceImageId(PieceType p, Color color) {
            switch (p) {
                case PAWN:
                case PAWN_IDLE:
                case PAWN_ENPASSANT:
                case PAWN_TOSWAP:
                    return color == Color.WHITE ? R.drawable.wt_pawn : R.drawable.bk_pawn;
                case BISHOP:
                    return color == Color.WHITE ? R.drawable.wt_bishop : R.drawable.bk_bishop;
                case KNIGHT_R:
                case KNIGHT_L:
                    return color == Color.WHITE ? R.drawable.wt_knight : R.drawable.bk_knight;
                case ROOK:
                case ROOK_IDLE:
                    return color == Color.WHITE ? R.drawable.wt_rook : R.drawable.bk_rook;
                case QUEEN:
                    return color == Color.WHITE ? R.drawable.wt_queen : R.drawable.bk_queen;
                case CHECKED_KING:
                case CHECKED_KING_IDLE:
                case UNCHECKED_KING:
                case UNCHECKED_KING_IDLE:
                case KING:
                    return color == Color.WHITE ? R.drawable.wt_king : R.drawable.bk_king;
                case DRAGON_R:
                case DRAGON_L:
                case DRAGON_IDLE_R:
                case DRAGON_IDLE_L:
                    return color == Color.WHITE ? R.drawable.wt_dragon : R.drawable.bk_dragon;
                case FLYING_KING:
                    break;
                case CHECKER:
                case DAMA_MAN:
                case DAMA_KING:
                case CHIP_4WAY:
                    return color == Color.RED ? R.drawable.red_checker : R.drawable.blk_checker;
            }
            return 0;
        }
    };

    File saveFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveFile = new File(getFilesDir(), "save.game");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (saveFile.exists()) {
            newDialogBuilder().setMessage("Resume Previous Game?")
                    .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showNewGameDialog();
                        }
                    }).setPositiveButton("Resume", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (game.tryLoadFromFile(saveFile)) {
                                startGame();
                            } else {
                                showNewGameDialog();
                            }
                        }
            }).show();
        } else {
            showNewGameDialog();
        }
    }

    int loadImage(AGraphics g, String path) {
        int id = g.loadImage(path);
        if (id < 0)
            throw new RuntimeException("Failed to load image '" + path + "'");
        return id;
    }

    int numImagesLoaded = 0;

    enum Images {
        //wood_checkerboard_8x8(null),
        //kings_court_board_8x8(null),
        bk_bishop   (Color.BLACK, PieceType.BISHOP),
        bk_king     (Color.BLACK, PieceType.KING),
        bk_knight   (Color.BLACK, PieceType.KNIGHT_R, PieceType.KNIGHT_L),
        bk_pawn     (Color.BLACK, PieceType.PAWN),
        bk_queen    (Color.BLACK, PieceType.QUEEN),
        bk_rook     (Color.BLACK, PieceType.ROOK),
        bk_dragon   (Color.BLACK, PieceType.DRAGON_L, PieceType.DRAGON_R, PieceType.DRAGON_IDLE_L, PieceType.DRAGON_IDLE_R),
        wt_bishop   (Color.WHITE, PieceType.BISHOP),
        wt_king     (Color.WHITE, PieceType.KING),
        wt_knight   (Color.WHITE, PieceType.KNIGHT_R, PieceType.KNIGHT_L),
        wt_pawn     (Color.WHITE, PieceType.PAWN),
        wt_queen    (Color.WHITE, PieceType.QUEEN),
        wt_rook     (Color.WHITE, PieceType.ROOK),
        wt_dragon   (Color.WHITE, PieceType.DRAGON_L, PieceType.DRAGON_R, PieceType.DRAGON_IDLE_L, PieceType.DRAGON_IDLE_R),
        blk_checker (Color.BLACK, PieceType.CHECKER, PieceType.CHIP_4WAY),
        red_checker (Color.RED,   PieceType.CHECKER, PieceType.CHIP_4WAY),
        wt_checker  (Color.WHITE, PieceType.CHECKER);

        Images(Color color, PieceType ... pt) {
            this.color = color;
            this.pt = pt;
        }

        final Color color;
        final PieceType [] pt;
    }

    int [] ids = null;

    void loadAssets(DroidGraphics g) {
        ids = new int[Images.values().length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = loadImage(g, Images.values()[i].name() + ".png");
        }
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
        newDialogBuilder().setMessage("How many players?")
                .setNegativeButton("One", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showChooseDifficultyDialog();
                    }
                }).setPositiveButton("Two", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                        game.setPlayer(Game.FAR,  new UIPlayer(UIPlayer.Type.USER));
                        game.newGame();
                        game.trySaveToFile(saveFile);
                        game.startGameThread();
                    }
                }).show();
    }

    void showChooseDifficultyDialog() {
        newDialogBuilder().setTitle("Difficulty")
                .setItems(new String[]{"Easy", "Medium", "Hard"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getPrefs().edit().putInt("difficulty", which).apply();
                        game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                        game.setPlayer(Game.FAR,  new UIPlayer(UIPlayer.Type.AI, which));
                        startGame();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showChoosePlayersDialog();
            }
        }).show();
    }

    int getDifficulty() {
        return getPrefs().getInt("difficulty", 2);
    }

    void startGame() {
        game.newGame();
        game.trySaveToFile(saveFile);
        game.startGameThread();
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
        //if (ids == null) {
        //    loadAssets(g);
       // }
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
    protected void onTap(float x, float y) {
        touchX = (int)x;
        touchY = (int)y;
        clicked = true;
        game.repaint(-1);
    }
}