package cc.android.test.dice;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import cc.lib.android.DroidGraphics;
import cc.lib.android.DroidView;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;

/**
 * Created by Chris Caron on 8/20/21.
 */
public class DiceView extends DroidView implements Runnable, View.OnClickListener {

    GColor [][] colors = {
            { GColor.WHITE, GColor.BLACK },
            { GColor.RED, GColor.WHITE },
            { GColor.BLUE, GColor.WHITE },
            { GColor.MAGENTA, GColor.YELLOW },
            { GColor.GREEN, GColor.WHITE },
            { GColor.YELLOW, GColor.BLUE },
    };

    private DiceActivity.DiceEntry entry = null;
    int [] dieNums = { 6, 8, 9, 10, 12, 15, 20 };

    public DiceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
        setClickable(true);
    }

    @Override
    protected void onPaint(DroidGraphics g) {
        g.setLineWidth(5);
        drawDie(g, getHeight(), colors[entry.curColor][0], colors[entry.curColor][1], entry.dieNum);
        if (!entry.rolling) {
            g.setColor(GColor.CYAN);
            g.drawRoundedRect(0, 0, getWidth(), getHeight(), getWidth()/4);
        }
    }

    void toggleColor() {
        entry.curColor = (entry.curColor + 1) % colors.length;
        invalidate();
    }

    public synchronized void setEntry(DiceActivity.DiceEntry entry) {
        this.entry = entry;
        run();
    }

    @Override
    public void onClick(View v) {
        entry.delay = 10;
        entry.rolling = true;
        run();
    }

    @Override
    public synchronized void run() {
        if (entry.rolling) {
            entry.dieNum = Utils.rand() % dieNums[entry.maxDieNums] + 1;
            if (entry.delay < 500) {
                entry.delay += 20;
                postDelayed(this, entry.delay);
            } else {
                entry.rolling = false;
            }
        }
        invalidate();
    }

    public void addPips() {
        if (entry.maxDieNums < dieNums.length-1) {
            entry.maxDieNums++;
            entry.dieNum = dieNums[entry.maxDieNums];
            invalidate();
        }
    }

    public void removePips() {
        if (entry.maxDieNums > 0) {
            entry.maxDieNums --;
            entry.dieNum = dieNums[entry.maxDieNums];
            invalidate();
        }
    }

    public void drawDie(AGraphics g, float dim, GColor dieColor, GColor dotColor, int pips) {
        g.setColor(dieColor);
        float arc = dim/4;
        g.drawFilledRoundedRect(0, 0, dim, dim, arc);
        g.setColor(dotColor);

        float dd2 = dim/2;
        float dd4 = dim/4;
        float dd34 = (dim*3)/4;
        float dotSize = dim/8;
        float oldDotSize = g.setPointSize(dotSize);
        if (dieNums[entry.maxDieNums] > 9) {
            g.setTextHeight(dim/2);
            g.drawJustifiedString(dd2, dd2, Justify.CENTER, Justify.CENTER, String.valueOf(pips));
            return;
        }
        g.begin();
        switch (pips) {
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
            case 7:
                g.vertex(dd2, dd2);
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                g.vertex(dd4, dd2);
                g.vertex(dd34, dd2);
                break;
            case 8:
                g.vertex(dd2, dd4);
                g.vertex(dd2, dd34);
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                g.vertex(dd4, dd2);
                g.vertex(dd34, dd2);
                break;
            case 9:
                g.vertex(dd2, dd4);
                g.vertex(dd2, dd2);
                g.vertex(dd2, dd34);
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                g.vertex(dd4, dd2);
                g.vertex(dd34, dd2);
                break;
            default:
                g.drawJustifiedString(dd2, dd2, Justify.CENTER, Justify.CENTER, String.valueOf(pips));
                return;
        }
        g.drawPoints();
        g.setPointSize(oldDotSize);
    }
}
