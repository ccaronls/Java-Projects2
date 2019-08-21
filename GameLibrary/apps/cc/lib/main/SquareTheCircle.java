package cc.lib.main;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;

public class SquareTheCircle extends AWTComponent {

    public static void main(String[] args) {
        Utils.DEBUG_ENABLED = true;

        AWTFrame frame = new AWTFrame("Animation Maker") {

            @Override
            protected void onWindowClosing() {
                try {
                    //app.figures.saveToFile(app.figuresFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        frame.add(new SquareTheCircle());
        frame.centerToScreen(600,500);
    }
    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        float D = Math.min(g.getViewportWidth(), g.getViewportHeight());

        float SQRT_PI = (float)Math.sqrt(Math.PI);

        g.pushMatrix();
        g.translate(g.getViewportWidth()/2, g.getViewportHeight()/2);
        g.scale(D/3);
        g.setColor(GColor.BLUE);
        g.drawCircle(0, 0, 1, 100);
        g.setColor(GColor.RED);
        g.drawRect(-SQRT_PI/2, -SQRT_PI/2, SQRT_PI, SQRT_PI);


        g.popMatrix();

    }
}
