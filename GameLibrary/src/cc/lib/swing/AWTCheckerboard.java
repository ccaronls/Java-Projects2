package cc.lib.swing;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cc.lib.checkerboard.CanadianDraughts;
import cc.lib.checkerboard.Checkers;
import cc.lib.checkerboard.Chess;
import cc.lib.checkerboard.Color;
import cc.lib.checkerboard.Dama;
import cc.lib.checkerboard.Draughts;
import cc.lib.checkerboard.PieceType;
import cc.lib.checkerboard.Suicide;
import cc.lib.checkerboard.UIGame;
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

    int loadImage(AGraphics g, String path) {
        int id = g.loadImage(path);
        if (id < 0)
            throw new RuntimeException("Failed to load image '" + path + "'");
        return id;
    }

    int numImagesLoaded = 0;

    enum Images {
        wood_checkboard_8x8(null, null),
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
        red_checker (Color.RED,   PieceType.CHECKER);

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
        for (int i=0; i<ids.length; i++) {
            ids[i] = loadImage(g, "images/" + Images.values()[i].name() + ".png");
            numImagesLoaded++;
            repaint();
        }
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
                        game.newGame();
                        game.startGameThread();
                }
            }
        };
        File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        File saveFile = new File(settings, "game.save");
        game = new UIGame(saveFile) {
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
                return ids[Images.wood_checkboard_8x8.ordinal()];
            }
        };
        frame.add(this);
        frame.addMenuBarMenu("New Game", "Checkers", "Suicide", "Draughts", "Canadian Draughts", "Dama", "Chess", "Ugolki");
        frame.setPropertiesFile(new File(settings, "gui.properties"));
        if (!frame.restoreFromProperties())
            frame.centerToScreen(640, 640);
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
                game.undo();
                break;
            default:
                super.onKeyReleased(key);
        }
    }
}
