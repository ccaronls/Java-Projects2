package cc.lib.swing;

import java.io.File;

import cc.lib.checkerboard.CanadianDraughts;
import cc.lib.checkerboard.Checkers;
import cc.lib.checkerboard.Chess;
import cc.lib.checkerboard.Color;
import cc.lib.checkerboard.Dama;
import cc.lib.checkerboard.Draughts;
import cc.lib.checkerboard.Game;
import cc.lib.checkerboard.Move;
import cc.lib.checkerboard.PieceType;
import cc.lib.checkerboard.Suicide;
import cc.lib.checkerboard.UIGame;
import cc.lib.checkerboard.UIPlayer;
import cc.lib.checkerboard.Ugolki;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.utils.FileUtils;

public class AWTCheckerboard extends AWTComponent {

    public static void main(String[] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        new AWTCheckerboard();
    }

    final AWTFrame frame;
    final UIGame game;
    final File saveFile;

    int loadImage(AGraphics g, String path) {
        int id = g.loadImage(path);
        if (id < 0)
            throw new RuntimeException("Failed to load image '" + path + "'");
        return id;
    }

    int numImagesLoaded = 0;

    enum Images {
        wood_checkerboard_8x8(null, null),
        bk_bishop   (Color.BLACK, PieceType.BISHOP),
        bk_king     (Color.BLACK, PieceType.KING),
        bk_knight   (Color.BLACK, PieceType.KNIGHT),
        bk_pawn     (Color.BLACK, PieceType.PAWN),
        bk_queen    (Color.BLACK, PieceType.QUEEN),
        bk_rook     (Color.BLACK, PieceType.ROOK),
        blk_checker (Color.BLACK, PieceType.CHECKER),
        wt_bishop   (Color.WHITE, PieceType.BISHOP),
        wt_king     (Color.WHITE, PieceType.KING),
        wt_knight   (Color.WHITE, PieceType.KNIGHT),
        wt_pawn     (Color.WHITE, PieceType.PAWN),
        wt_queen    (Color.WHITE, PieceType.QUEEN),
        wt_rook     (Color.WHITE, PieceType.ROOK),
        red_checker (Color.RED,   PieceType.CHECKER),
        wt_checker  (Color.WHITE, PieceType.CHECKER);

        Images(Color color, PieceType pt) {
            this.color = color;
            this.pt = pt;
        }

        final Color color;
        final PieceType pt;
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
                    yield();
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
        frame = new AWTFrame("Checkerboard") {
            @Override
            protected void onMenuItemSelected(String menu, String subMenu) {
                switch (menu) {
                    case "Load Game": {
                        File file = frame.showFileOpenChooser("Load Game", ".game");
                        if (file != null) {
                            Game tmp = new Game();
                            if (tmp.tryLoadFromFile(file)) {
                                game.stopGameThread();
                                game.tryLoadFromFile(file);
                                game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                                game.setPlayer(Game.FAR, new UIPlayer(UIPlayer.Type.USER));
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
                            case "Ugolki":
                                game.setRules(new Ugolki());
                                break;
                        }
                        new Thread(() -> {
                            int num = frame.showItemChooserDialog("PLAYERS", "Choose Number of Players", "ONE PLAYER", "TWO PLAYERS");
                            switch (num) {
                                case 0:
                                    game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                                    game.setPlayer(Game.FAR,  new UIPlayer(UIPlayer.Type.AI));
                                    game.newGame();
                                    break;
                                case 1:
                                    game.setPlayer(Game.NEAR, new UIPlayer(UIPlayer.Type.USER));
                                    game.setPlayer(Game.FAR,  new UIPlayer(UIPlayer.Type.USER));
                                    game.newGame();
                                    break;
                            }
                            game.trySaveToFile(saveFile);
                            game.startGameThread();
                        }).start();
                }
            }
        };
        game = new UIGame() {
            @Override
            public void repaint() {
                AWTCheckerboard.this.repaint();
            }

            @Override
            public int getPieceImageId(PieceType p, Color color) {
                for (Images i : Images.values()) {
                    if (i.color == color && i.pt == p)
                        return ids[i.ordinal()];
                }
                return -1;
            }

            @Override
            protected int getCheckerboardImageId() {
                return ids[Images.wood_checkerboard_8x8.ordinal()];
            }
        };
        File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        saveFile = new File(settings, "game.save");
        frame.add(this);
        String [] items = { "Checkers", "Suicide", "Draughts", "Canadian Draughts", "Dama", "Chess", "Ugolki" };
        frame.addMenuBarMenu("New Game", items);
        frame.addMenuBarMenu("Load Game", "From File");
        frame.setPropertiesFile(new File(settings, "gui.properties"));
        if (!frame.restoreFromProperties())
            frame.centerToScreen(640, 640);

        game.init(saveFile);
        game.startGameThread();
    }

    @Override
    protected void onClick() {
        game.doClick();
    }

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        game.draw(g, mouseX, mouseY);
    }

    @Override
    protected void onKeyReleased(VKKey key) {
        switch (key) {
            case VK_U:
                game.undoAndRefresh();
                break;
            case VK_E: {
                Move m = game.getMoveHistory().get(0);
                long value = game.getRules().evaluate(game, m);
                System.out.println("EVALUATION [" + value + "] for move:" + m);
                ((UIPlayer)game.getCurrentPlayer()).forceRebuildMovesList(game);
                break;
            }
            default:
                super.onKeyReleased(key);
        }
    }
}
