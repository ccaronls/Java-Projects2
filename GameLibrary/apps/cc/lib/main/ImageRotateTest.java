package cc.lib.main;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;

public class ImageRotateTest extends AWTComponent {

    public static void main(String[] args) {
        Utils.setDebugEnabled();

        AWTFrame frame = new AWTFrame("Image Roatte Test") {

            @Override
            protected void onWindowClosing() {
                try {
                    //app.figures.saveToFile(app.figuresFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        frame.add(new ImageRotateTest());
        frame.centerToScreen(600,600);
    }

    List<Integer> images = new ArrayList<>();

    float progress = 0;

    @Override
    protected float getInitProgress() {
        return progress;
    }

    @Override
    protected void init(AWTGraphics g) {

        int id = g.loadImage("zabomination.png");

        for (int i=0; i<360; i+=5) {
            images.add(g.createRotatedImage(id, i));
            progress += (float)i/360;
            repaint();
        }

        progress = 1;
        repaint();
    }

    int idx = 0;

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {

        Vector2D center = new Vector2D(getWidth()/2, getHeight()/2);
        g.clearScreen();
        g.drawImage(images.get(idx), center);
        idx = (idx+1) % images.size();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (this) {
                        wait(100);
                    }
                    repaint();
                } catch (Exception e) {}
            }
        }).start();
    }
}
