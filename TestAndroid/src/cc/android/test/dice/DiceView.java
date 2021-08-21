package cc.android.test.dice;

import android.content.Context;
import android.util.AttributeSet;

import cc.lib.android.DroidGraphics;
import cc.lib.android.DroidView;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;

/**
 * Created by Chris Caron on 8/20/21.
 */
public class DiceView extends DroidView implements Runnable {

    GColor [][] colors = {
            { GColor.WHITE, GColor.BLACK },
            { GColor.RED, GColor.WHITE },
            { GColor.BLUE, GColor.WHITE },
            { GColor.MAGENTA, GColor.YELLOW },
            { GColor.GREEN, GColor.WHITE },
            { GColor.YELLOW, GColor.BLUE },
    };

    int curColor = 0;
    int dieNum = 6;
    long delay = 0;
    boolean rolling = false;

    public DiceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPaint(DroidGraphics g) {
        g.setLineWidth(5);
        drawDie(g, getHeight(), colors[curColor][0], colors[curColor][1], dieNum);
        if (!rolling) {
            g.setColor(GColor.CYAN);
            g.drawRoundedRect(0, 0, getWidth(), getHeight(), getWidth()/4);
        }
    }

    void toggleColor() {
        curColor = (curColor + 1) % colors.length;
        invalidate();
    }

    void rollDice() {
        delay = 10;
        rolling = true;
        run();
    }

    @Override
    public void run() {
        dieNum = Utils.rand() % 6 + 1;
        if (delay < 500) {
            delay += 20;
            postDelayed(this, delay);
        } else {
            rolling = false;
        }
        invalidate();
    }

    public void drawDie(AGraphics g, float dim, GColor dieColor, GColor dotColor, int numDots) {
        g.setColor(dieColor);
        float arc = dim/4;
        g.drawFilledRoundedRect(0, 0, dim, dim, arc);
        g.setColor(dotColor);
        float dd2 = dim/2;
        float dd4 = dim/4;
        float dd34 = (dim*3)/4;
        float dotSize = dim/8;
        float oldDotSize = g.setPointSize(dotSize);
        g.begin();
        switch (numDots) {
            case 1:
                g.vertex(dd2, dd2);
                break;
            case 2:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                break;
            case 3:
                g.vertex(dd4, dd4);
                g.vertex(dd2, dd2);
                g.vertex(dd34, dd34);
                break;
            case 4:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                break;
            case 5:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                g.vertex(dd2, dd2);
                break;
            case 6:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                g.vertex(dd4, dd2);
                g.vertex(dd34, dd2);
                break;
            default:
                assert(false);// && "Invalid die");
                break;
        }
        g.drawPoints();
        g.setPointSize(oldDotSize);
    }
}
