package cc.lib.swing;

import java.awt.event.KeyEvent;
import java.io.File;

import cc.lib.checkerboard.AIPlayer;
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
import cc.lib.checkerboard.Move;
import cc.lib.checkerboard.PieceType;
import cc.lib.checkerboard.Player;
import cc.lib.checkerboard.Shashki;
import cc.lib.checkerboard.Suicide;
import cc.lib.checkerboard.UIGame;
import cc.lib.checkerboard.UIPlayer;
import cc.lib.checkerboard.Ugolki;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.utils.EventQueue;
import cc.lib.utils.FileUtils;

import static java.awt.event.KeyEvent.*;

public class AWTCheckerboard extends AWTComponent {

    public static void main(String[] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.setDebugEnabled();

        new AWTCheckerboard();
    }

    final AWTFrame frame;
    final UIGame game;
    final File saveFile;
    int difficulty = 2;
    final EventQueue eq = new EventQueue();

    int loadImage(AGraphics g, String path) {
        int id = g.loadImage(path);
        if (id < 0)
            throw new RuntimeException("Failed to load image '" + path + "'");
        return id;
    }

    int numImagesLoaded = 0;

    enum Images {
        wood_checkerboard_8x8(null),
        kings_court_board_8x8(null),
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

    int [] ids = new int[Images.values().length];

    @Override
    protected void init(AWTGraphics g) {
        new Thread() {
            public void run() {
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = loadImage(g, "images/" + Images.values()[i].name() + ".png");
                    numImagesLoaded++;
                    repaint();
                }
            }

        }.start();
    }

    @Override
    protected float getInitProgress() {
        return (float)numImagesLoaded / ids.length;
    }

    AWTCheckerboard() {
        setMouseEnabled(true);
        setPadding(5);
        new Thread(eq).start();
        frame = new AWTFrame("Checkerboard") {
            @Override
            protected void onMenuItemSelected(String menu, String subMenu) {
                switch (menu) {
                    case "Load Game": {
                        File file = frame.showFileOpenChooser("Load Game", ".save", "checkerboard games");
                        if (file != null) {
                            Game tmp = new Game();
                            if (tmp.tryLoadFromFile(file)) {
                                game.stopGameThread();
                                game.tryLoadFromFile(file);
                                //game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                                //game.setPlayer(Game.FAR, new UIPlayer(UIPlayer.Type.USER));
                                game.startGameThread();
                            } else {
                                System.err.println("Cannot load " + file);
                            }
                        }
                        break;
                    }
                    case "New Game":
                        game.stopGameThread();
                        switch (subMenu) {
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
                        new Thread(() -> {
                            int num = frame.showItemChooserDialog("PLAYERS", "Choose Number of Players", "ONE PLAYER", "TWO PLAYERS");
                            switch (num) {
                                case 0:
                                    game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                                    game.setPlayer(Game.FAR,  new UIPlayer(UIPlayer.Type.AI, difficulty));
                                    game.newGame();
                                    break;
                                case 1:
                                    game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                                    game.setPlayer(Game.FAR,  new UIPlayer(UIPlayer.Type.USER, difficulty));
                                    game.newGame();
                                    break;
                            }
                            game.trySaveToFile(saveFile);
                            game.startGameThread();
                        }).start();
                        break;
                    case "Game":
                        switch (subMenu) {
                            case "Stop Thinking": {
                                Player pl = game.getCurrentPlayer();
                                if ((pl instanceof AIPlayer)) {
                                    ((AIPlayer) pl).cancel();
                                }
                                break;
                            }

                            case "Resume":
                                game.startGameThread();
                                redraw();
                                break;

                            case "Stop":
                                game.stopGameThread();
                                redraw();
                                break;

                            case "One Player":
                                game.stopGameThread();
                                ((UIPlayer)game.getPlayer(Game.FAR)).setType(UIPlayer.Type.AI);
                                redraw();
                                break;
                            case "Two Players":
                                game.stopGameThread();
                                ((UIPlayer)game.getPlayer(Game.FAR)).setType(UIPlayer.Type.USER);
                                redraw();
                                break;
                        }
                        break;
                    case "Difficulty":
                        switch (subMenu) {
                            case "Easy":
                                ((UIPlayer)game.getPlayer(Game.FAR)).setMaxSearchDepth(difficulty=1);
                                break;
                            case "Medium":
                                ((UIPlayer)game.getPlayer(Game.FAR)).setMaxSearchDepth(difficulty=2);
                                break;
                            case "Hard":
                                ((UIPlayer)game.getPlayer(Game.FAR)).setMaxSearchDepth(difficulty=3);
                                break;

                        }
                        frame.setProperty("difficulty", difficulty);
                        break;
                }
            }
        };

        game = new UIGame() {
            @Override
            public void repaint(long delayMs) {
                if (delayMs <= 0)
                    AWTCheckerboard.this.repaint();
                else {
                    eq.enqueue(delayMs, ()-> AWTCheckerboard.this.repaint());
                }
            }

            @Override
            public int getPieceImageId(PieceType p, Color color) {
                for (Images i : Images.values()) {
                    if (i.color == color && Utils.linearSearch(i.pt, p) >= 0)
                        return ids[i.ordinal()];
                }
                return -1;
            }

            @Override
            protected int getCheckerboardImageId() {
                return ids[Images.wood_checkerboard_8x8.ordinal()];
            }

            @Override
            protected int getKingsCourtBoardId() {
                return ids[Images.kings_court_board_8x8.ordinal()];
            }
        };
        File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        saveFile = new File(settings, "game.save");
        frame.add(this);
        String [] items = { "Checkers", "Suicide", "Draughts", "Canadian Draughts", "Dama", "Chess", "Dragon Chess", "Ugolki", "Columns", "Kings Court", "Shashki" };
        frame.addMenuBarMenu("New Game", items);
        frame.addMenuBarMenu("Load Game", "From File");
        frame.addMenuBarMenu("Game", "Stop Thinking", "Resume", "Stop", "One Player", "Two Players");
        frame.addMenuBarMenu("Difficulty", "Easy", "Medium", "Hard");
        frame.setPropertiesFile(new File(settings, "gui.properties"));
        if (!frame.restoreFromProperties())
            frame.centerToScreen(640, 640);

        game.init(saveFile);
        difficulty = frame.getIntProperty("difficulty", difficulty);
    }

    @Override
    protected void onClick() {
        game.doClick();
    }

    @Override
    protected void onDragStarted(int x, int y) {
        game.startDrag();
    }

    @Override
    protected void onDragStopped() {
        game.stopDrag();
    }

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        game.draw(g, mouseX, mouseY);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_U:
                game.stopGameThread();
                game.undoAndRefresh();
                break;
            case VK_E: {
                Move m = game.getMoveHistory().get(0);
                long value = game.getRules().evaluate(game);
                System.out.println("EVALUATION [" + value + "] for move:" + m);
                ((UIPlayer)game.getCurrentPlayer()).forceRebuildMovesList(game);
                break;
            }
            case VK_R: {
                game.startGameThread();
                break;
            }
        }
    }
}
