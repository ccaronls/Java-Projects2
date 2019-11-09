package cc.lib.swing;

import java.io.File;

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

    AWTCheckerboard() {
        setMouseEnabled(true);
        setPadding(5);
        frame = new AWTFrame("Checkerboard") {

        };
        File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        File saveFile = new File(settings, "game.save");
        game = new UIGame(saveFile) {
            @Override
            public void repaint() {
                AWTCheckerboard.this.repaint();
            }
        };
        frame.add(this);
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
}
