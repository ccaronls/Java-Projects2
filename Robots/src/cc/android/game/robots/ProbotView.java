package cc.android.game.robots;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import cc.lib.android.DroidUtils;
import cc.lib.android.SquareLinearLayout;
import cc.lib.game.AAnimation;

/**
 * Created by chriscaron on 12/7/17.
 */

public class ProbotView extends View {

    Probot probot = new Probot() {
        @Override
        protected void onFailed() {
            startFailedAnim();
            try {
                synchronized (this) {
                    wait();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            postInvalidate();
        }

        @Override
        protected void onAdvanced() {
            startAdvanceAnim();
            try {
                synchronized (this) {
                    wait();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            postInvalidate();
        }

        @Override
        protected void onTurned() {
            postInvalidate();
        }

        @Override
        protected void onSuccess() {
        }
    };

    Paint p = new Paint();
    Rect r = new Rect();
    RectF rf = new RectF();
    int radius = 0;
    int cw, ch;

    AAnimation<Canvas> animation = null;

    public ProbotView(Context context) {
        super(context);
    }

    public ProbotView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProbotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // clear screen
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.BLACK);
        r.set(0, 0, getWidth(), getHeight());
        canvas.drawRect(r, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(10);
        p.setColor(Color.RED);
        canvas.drawRect(r, p);

        int cols = probot.coins.length;
        int rows = probot.coins[0].length;

        // get cell width/height
        cw = getWidth() / cols;
        ch = getHeight() / rows;
        radius = Math.min(cw, ch) / 4;

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);
        for (int i=0; i<cols; i++) {
            for (int ii=0; ii<rows; ii++) {
                if (probot.coins[i][ii] != 0) {
                    int x = i*cw + cw/2;
                    int y = ii*ch + ch/2;
                    canvas.drawCircle(x, y, radius, p);
                }
            }
        }

        // draw the pacman
        radius = Math.min(cw, ch) * 2 / 3;
        int x = probot.posx*cw + cw/2;
        int y = probot.posy*ch + ch/2;
        rf.set(x-radius, y-radius, x+radius, y+radius);

        p.setColor(Color.YELLOW);
        if (animation != null) {
            animation.update(canvas);
            invalidate();
        } else {
            canvas.drawCircle(x, y, radius, p);
            p.setColor(Color.BLACK);
            p.setStyle(Paint.Style.STROKE);
            switch (probot.dir) {
                case Right:
                    canvas.drawLine(x, y, x+radius, y, p);
                    break;
                case Down:
                    canvas.drawLine(x, y, x, y+radius, p);
                    break;
                case Left:
                    canvas.drawLine(x, y, x-radius, y, p);
                    break;
                case Up:
                    canvas.drawLine(x, y, x, y-radius, p);
                    break;
            }
        }
    }

    void startAdvanceAnim() {
        animation = new AAnimation<Canvas>(1000) {
            @Override
            protected void draw(Canvas canvas, float position, float dt) {
                int x = probot.posx*cw + cw/2;
                int y = probot.posy*ch + ch/2;
                int angStart=0, sweepAng=270;
                float dx=0, dy=0;
                if (position < 0.5f) {
                    angStart = Math.round(position * 2 * 45);
                } else {
                    angStart = Math.round((1.0f - position * 2) * 45);
                }
                sweepAng = 360 - angStart*2;

                switch (probot.dir) {

                    case Right:
                        dx = cw;
                        break;
                    case Down:
                        angStart+=90;
                        dy = ch;
                        break;
                    case Left:
                        angStart+=180;
                        dx = -cw;
                        break;
                    case Up:
                        angStart+=270;
                        dy = -ch;
                        break;
                }
                rf.set(x-radius+Math.round(dx * position), y-radius+Math.round(dy * position),
                        x+radius+Math.round(dx * position), y+radius+Math.round(dy * position));
                canvas.drawArc(rf, angStart, sweepAng, true, p);
            }

            @Override
            protected void onDone() {
                animation = null;
                synchronized (probot) {
                    probot.notify();
                }

            }
        }.start();
        postInvalidate();
    }

    void startFailedAnim() {
        animation = new AAnimation<Canvas>(500, 6, true) {
            @Override
            protected void draw(Canvas canvas, float position, float dt) {
                p.setColor(DroidUtils.interpolateColor(Color.YELLOW, Color.RED, position));
                int x = probot.posx*cw + cw/2;
                int y = probot.posy*ch + ch/2;
                drawGuy(canvas, x, y);
            }

            @Override
            protected void onDone() {
                animation = null;
                synchronized (probot) {
                    probot.notify();
                }
            }


        }.start();
        postInvalidate();
    }

    void drawGuy(Canvas canvas, int x, int y) {
        canvas.drawCircle(x, y, radius, p);
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        switch (probot.dir) {
            case Right:
                canvas.drawLine(x, y, x+radius, y, p);
                break;
            case Down:
                canvas.drawLine(x, y, x, y+radius, p);
                break;
            case Left:
                canvas.drawLine(x, y, x-radius, y, p);
                break;
            case Up:
                canvas.drawLine(x, y, x, y-radius, p);
                break;
        }
    }
}
