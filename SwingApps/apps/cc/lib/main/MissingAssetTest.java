package cc.lib.main;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;

public class MissingAssetTest extends AWTComponent {

    public static void main(String[] args) {
        Utils.setDebugEnabled();

        AWTFrame frame = new AWTFrame("Missing Asset Test") {

            @Override
            protected void onWindowClosing() {
                try {
                    //app.figures.saveToFile(app.figuresFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        frame.add(new MissingAssetTest());
        frame.centerToScreen(256,256);
    }

    List<Integer> images = new ArrayList<>();

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        g.drawImage(0, 0, 0, getWidth(), getHeight());
    }
}
