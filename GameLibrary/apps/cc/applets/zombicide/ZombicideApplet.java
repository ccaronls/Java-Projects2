package cc.applets.zombicide;

import java.io.File;

import cc.applets.typing.LearnToType;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.lib.utils.FileUtils;
import cc.lib.zombicide.ZBoard;

public class ZombicideApplet extends AWTKeyboardAnimationApplet {

    public static void main(String [] args) {
        //Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        //mode = 0;
        AWTFrame frame = new AWTFrame("Zombicide");
        AWTKeyboardAnimationApplet app = new ZombicideApplet();
        frame.add(app);
        app.init();
        app.start();
        app.setMillisecondsPerFrame(20);
        File settings = FileUtils.getOrCreateSettingsDirectory(LearnToType.class);
        frame.setPropertiesFile(new File(settings, "application.properties"));
        if (!frame.restoreFromProperties())
            frame.centerToScreen(800, 600);

    }

    ZBoard board = new ZBoard();

    @Override
    protected void doInitialization() {
        board.load(ZBoard.quest1);
    }

    int [] selected = null;

    @Override
    protected void drawFrame(AGraphics g) {

        int [] highlighted = board.drawDebug(g, getMouseX(), getMouseY());
        if (highlighted != null) {
            if (getKeyboardReset('a')) {
                board.toggleDorOpen(highlighted, ZBoard.DIR_WEST);
            } else if (getKeyboardReset('w')) {
                board.toggleDorOpen(highlighted, ZBoard.DIR_NORTH);
            } else if (getKeyboardReset('s')) {
                board.toggleDorOpen(highlighted, ZBoard.DIR_SOUTH);
            } else if (getKeyboardReset('d')) {
                board.toggleDorOpen(highlighted, ZBoard.DIR_EAST);
            }

            if (getMouseButtonClicked(0)) {
                selected = highlighted;
            } else if (selected != null) {
                g.setColor(GColor.RED);
                if (board.canSeeCell(selected, highlighted)) {
                    g.setColor(GColor.GREEN);
                }
                g.drawRect(board.getCell(selected).getRect());
            }
        }
    }

    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        board.initCellRects(g, g.getViewportWidth()-5, g.getViewportHeight()-5);
    }
}
