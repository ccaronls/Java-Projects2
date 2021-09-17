package cc.game.android.risk;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import cc.lib.android.DroidGraphics;
import cc.lib.android.DroidView;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.utils.Lock;

/**
 * Created by Chris Caron on 8/20/21.
 */
public class DiceView extends DroidView implements Runnable, View.OnClickListener {

    int [] dieNums = { 6, 8, 9, 10, 12, 15, 20 };

    int finalNum=6;
    int dieNum = 0;
    int maxDieNums = 0;
    long delay = 0;
    boolean rolling = false;
    GColor dieColor = GColor.WHITE;
    GColor pipColor = GColor.BLACK;
    Lock lock = null;

    public DiceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.DiceView);
        dieColor = GColor.fromARGB(arr.getColor(R.styleable.DiceView_dieColor, dieColor.toARGB()));
        pipColor = GColor.fromARGB(arr.getColor(R.styleable.DiceView_pipColor, pipColor.toARGB()));
        arr.recycle();
    }

    @Override
    protected void onPaint(DroidGraphics g) {
        g.setLineWidth(5);
        if (rolling)
            drawDie(g, getHeight(), dieNum);
        else {
            drawDie(g, getHeight(), finalNum);
            g.setColor(GColor.CYAN);
            g.drawRoundedRect(0, 0, getWidth(), getHeight(), getWidth()/4);
        }
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

    void setColors(GColor dieColor, GColor pipColor) {
        this.dieColor = dieColor;
        this.pipColor = pipColor;
        invalidate();
    }

    public synchronized void rollDice(int resultDie, Lock lock) {
        delay = 10;
        rolling = true;
        finalNum = resultDie;
        this.lock = lock;
        lock.acquire();
        run();
    }

    @Override
    public void onClick(View v) {
        delay = 10;
        rolling = true;
        run();
    }

    @Override
    public synchronized void run() {
        if (rolling) {
            dieNum = Utils.rand() % dieNums[maxDieNums] + 1;
            if (delay < 500) {
                delay += 20;
                postDelayed(this, delay);
            } else {
                rolling = false;
                if (lock != null) {
                    lock.release();
                }
            }
        }
        invalidate();
    }

    public void addPips() {
        if (maxDieNums < dieNums.length-1) {
            maxDieNums++;
            dieNum = dieNums[maxDieNums];
            invalidate();
        }
    }

    public void removePips() {
        if (maxDieNums > 0) {
            maxDieNums --;
            dieNum = dieNums[maxDieNums];
            invalidate();
        }
    }

    public void drawDie(AGraphics g, float dim, int pips) {
        g.setColor(dieColor);
        float arc = dim/4;
        g.drawFilledRoundedRect(0, 0, dim, dim, arc);
        g.setColor(pipColor);

        float dd2 = dim/2;
        float dd4 = dim/4;
        float dd34 = (dim*3)/4;
        float dotSize = dim/8;
        float oldDotSize = g.setPointSize(dotSize);
        if (dieNums[maxDieNums] > 9) {
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
