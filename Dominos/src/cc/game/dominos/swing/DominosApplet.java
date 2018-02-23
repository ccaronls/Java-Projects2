package cc.game.dominos.swing;

import java.io.File;

import cc.game.dominos.core.*;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;

public class DominosApplet extends AWTComponent {

    public static void main(String [] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        new DominosApplet();
    }

    final EZFrame frame;

    DominosApplet() {
        super(false);
        frame = new EZFrame("Dominos") {
            protected void onWindowClosing() {
                dominos.trySaveToFile(saveFile);
            }

            @Override
            protected void onMenuItemSelected(String menu, String subMenu) {
                switch (subMenu) {
                    case "New Game":
                        dominos.startNewGame(9, 150);
                        dominos.startGameThread();
                        break;
                }
            }
        };
        frame.addMenuBarMenu("File", "New Game");
        frame.add(this);
        try {
            dominos.loadFromFile(saveFile);
        } catch (Exception e) {
            dominos.setNumPlayers(4);
            dominos.startNewGame(9, 150);
        }

        if (!frame.loadFromFile(new File("dominos.properties")))
            frame.centerToScreen(800, 600);
        dominos.startGameThread();
    }

    final File saveFile = new File("dominos.save");
    final Dominos dominos = new Dominos() {

        @Override
        public void redraw() {
            repaint();
        }
    };

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        dominos.draw(g, mouseX, mouseY);
    }

    @Override
    protected void startDrag() {
        dominos.startDrag();
    }

    @Override
    protected void stopDrag() {
        dominos.stopDrag();
    }

    @Override
    protected void onClick() {
        dominos.onClick();
    }

}
