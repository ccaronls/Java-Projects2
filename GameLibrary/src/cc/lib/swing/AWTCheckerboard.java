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
    final Map<String, Integer> pieceImages = new HashMap<>();

    int loadImage(AGraphics g, String path) {
        int id = g.loadImage(path);
        if (id < 0)
            throw new RuntimeException("Failed to load image '" + path + "'");
        return id;
    }

    String getId(Color color, PieceType type) {
        return color.name() + " " + type.name();
    }

    void loadImages(AGraphics g) {
        pieceImages.put(getId(Color.BLACK, PieceType.BISHOP), loadImage(g, "images/bk_bishop.png"));
        pieceImages.put(getId(Color.BLACK, PieceType.KING), loadImage(g, "images/bk_king.png"));
        pieceImages.put(getId(Color.BLACK, PieceType.KNIGHT), loadImage(g, "images/bk_knight.png"));
        pieceImages.put(getId(Color.BLACK, PieceType.PAWN), loadImage(g, "images/bk_pawn.png"));
        pieceImages.put(getId(Color.BLACK, PieceType.QUEEN), loadImage(g, "images/bk_queen.png"));
        pieceImages.put(getId(Color.BLACK, PieceType.ROOK), loadImage(g, "images/bk_rook.png"));
        pieceImages.put(getId(Color.BLACK, PieceType.CHECKER), loadImage(g, "images/blk_checker.png"));
        pieceImages.put(getId(Color.WHITE, PieceType.BISHOP), loadImage(g, "images/wt_bishop.png"));
        pieceImages.put(getId(Color.WHITE, PieceType.KING), loadImage(g, "images/wt_king.png"));
        pieceImages.put(getId(Color.WHITE, PieceType.KNIGHT), loadImage(g, "images/wt_knight.png"));
        pieceImages.put(getId(Color.WHITE, PieceType.PAWN), loadImage(g, "images/wt_pawn.png"));
        pieceImages.put(getId(Color.WHITE, PieceType.QUEEN), loadImage(g, "images/wt_queen.png"));
        pieceImages.put(getId(Color.WHITE, PieceType.ROOK), loadImage(g, "images/wt_rook.png"));
        pieceImages.put(getId(Color.RED  , PieceType.CHECKER), loadImage(g, "images/red_checker.png"));
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
                try {
                    String id = getId(color, p);
                    //Utils.println("id = " + id);
                    return pieceImages.get(id);
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
            }
        };
        frame.add(this);
        frame.addMenuBarMenu("New Game", "Checkers", "Suicide", "Draughts", "Canadian Draughts", "Dama", "Chess");
        frame.centerToScreen(640, 640);
        game.startGameThread();
    }

    @Override
    protected void onClick() {
        game.doClick();
    }

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        if (pieceImages.size() == 0) {
            loadImages(g);
        }
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
