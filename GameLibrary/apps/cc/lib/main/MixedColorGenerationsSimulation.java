package cc.lib.main;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;

public class MixedColorGenerationsSimulation extends AWTComponent {

    public static void main(String[] args) {
        Utils.setDebugEnabled();

        AWTFrame frame = new AWTFrame("Mixed Color Generations Simulation") {

            @Override
            protected void onWindowClosing() {
                try {
                    //app.figures.saveToFile(app.figuresFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        frame.add(new MixedColorGenerationsSimulation());
        frame.centerToScreen(800,800);
    }

    class Ball {
        final MutableVector2D p = new MutableVector2D();
        final MutableVector2D v = new MutableVector2D();
        GColor color;
    }


    List<Ball> balls = new ArrayList<>();

    final float RADIUS = 20;
    final float SPACING = 100;
    final float RATIO = .85f; // ratio of white to black

    @Override
    protected void init(AWTGraphics g) {
        final int H = g.getViewportHeight();
        final int W = g.getViewportWidth();

        float x = RADIUS * 2;
        float y = RADIUS * 2;

        while (true) {
            Ball ball = new Ball();
            ball.p.set(x, y);
            ball.v.set(3*Utils.randFloatX(1), 3*Utils.randFloatX(1));
            ball.color = GColor.WHITE;
            balls.add(ball);

            y += SPACING;
            if (y > H - RADIUS) {
                y = RADIUS *2;
                x += SPACING;
            }

            if ( x > W - RADIUS)
                break;
        }

        final int numWhite = Math.round(RATIO * balls.size());
        final int numBlack = balls.size() - numWhite;

        for (int i=0; i<numBlack; ) {
            Ball b = balls.get(Utils.rand() % balls.size());
            if (b.color.equals(GColor.BLACK))
                continue;

            b.color = GColor.BLACK;
            i++;
        }
    }

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {

        final long startT = System.currentTimeMillis();

        final int H = g.getViewportHeight();
        final int W = g.getViewportWidth();

        g.clearScreen(GColor.CYAN);

        // see if all balls the same color, then stop
        boolean allSame = true;
        float avgR=balls.get(0).color.getRed();
        float avgG=balls.get(0).color.getGreen();
        float avgB=balls.get(0).color.getBlue();
        for (int i=1; i<balls.size(); i++) {
            avgR += balls.get(i).color.getRed();
            avgG += balls.get(i).color.getGreen();
            avgB += balls.get(i).color.getBlue();
            if (!balls.get(0).color.equalsWithinThreshold(balls.get(i).color, 2)) {
                allSame = false;
            }
        }

        for (int i=0; i<balls.size(); i++) {
            Ball b = balls.get(i);

            g.setColor(b.color);
            g.drawFilledCircle(b.p.X(), b.p.Y(), RADIUS);

            b.p.addEq(b.v);

            if (b.p.X() > W- RADIUS || b.p.X() < RADIUS) {
                b.p.setX(b.p.X() - b.v.X()*2);
                b.v.scaleEq(-1, 1);
            }

            if (b.p.Y() > H- RADIUS || b.p.Y() < RADIUS) {
                b.p.setY(b.p.Y() - b.v.Y()*2);
                b.v.scaleEq(1, -1);
            }
        }

        // collision detect

        for (int i=0; i<balls.size()-1; i++) {
            for (int ii= i+1; ii<balls.size(); ii++) {
                Ball b0 = balls.get(i);
                Ball b1 = balls.get(ii);

                Vector2D dv = b1.p.sub(b0.p);
                float d = dv.magSquared();
                if (d < 4* RADIUS * RADIUS) {
                    // bounce
                    GColor newColor = b0.color.interpolateTo(b1.color, .5f);
                    b0.color = newColor;
                    b1.color = newColor;
                    b0.p.subEq(b0.v);
                    b1.p.subEq(b1.v);
                    b1.v.reflectEq(dv);
                    b0.v.reflectEq(dv.scaledBy(-1));
                }
            }
        }

        GColor avg = new GColor(avgR/balls.size(), avgG/balls.size(), avgB/balls.size(), 1);

        g.setColor(GColor.MAGENTA);
        g.drawString(avg.toString(), 10, 10);

        final long endT = System.currentTimeMillis();

        if (endT - startT < 33) {
            Utils.waitNoThrow(this, endT - startT);
        }

        if (!allSame)
            repaint();

    }
}
