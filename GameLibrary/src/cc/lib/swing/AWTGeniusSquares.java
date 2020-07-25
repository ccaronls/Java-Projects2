package cc.lib.swing;

import java.io.File;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.geniussqaure.UIGeniusSquares;
import cc.lib.utils.FileUtils;

public class AWTGeniusSquares extends AWTComponent {

    public static void main(String[] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        new AWTGeniusSquares();
    }

    final AWTFrame frame;
    final UIGeniusSquares game;

    AWTGeniusSquares() {
        setMouseEnabled(true);
        final File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        final File saveFile = new File(settings, "gs.save");
        frame = new AWTFrame("Genius Squares") {
            @Override
            protected void onWindowClosing() {
                game.pauseTimer();
                game.trySaveToFile(saveFile);
            }

            @Override
            protected void onMenuItemSelected(String menu, String subMenu) {
                switch (subMenu) {
                    case "New Game":
                        game.newGame();
                        break;
                    case "Reset Pieces":
                        game.resetPieces();
                        break;
                }

                repaint();
            }
        };
        frame.addMenuBarMenu("GeniusSquares", "New Game", "Reset Pieces");

        game = new UIGeniusSquares() {
            @Override
            public void repaint() {
                AWTGeniusSquares.this.repaint();

            }
        };
        if (!game.tryLoadFromFile(saveFile))
            game.newGame();
        frame.add(this);
        frame.setPropertiesFile(new File(settings, "gui.properties"));
        if (!frame.restoreFromProperties()) {
            frame.centerToScreen(640, 480);
        }
        game.resumeTimer();
    }

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        game.paint(g, mouseX, mouseY);
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
}
