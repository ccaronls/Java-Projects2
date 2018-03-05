package cc.android.game.robots;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import cc.lib.android.DroidUtils;
import cc.lib.game.AAnimation;
import cc.lib.game.Utils;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;

/**
 * Created by chriscaron on 12/7/17.
 */

public class ProbotView extends View {

    int maxLevel = 0;

    Probot probot = new Probot() {

        @Override
        protected void onCommand(int line) {
            Log.d("ProbotView", "onCommand: " + line);
            setProgramLine(line);
            switch (get(line).type) {
                case LoopStart:
                case LoopEnd:
                    Utils.waitNoThrow(this, 500);
            }
        }

        @Override
        protected void onFailed() {
            ((ProbotActivity)getContext()).lv.markFailed();
            startFailedAnim();
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onAdvanced() {
            startAdvanceAnim();
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onJumped() {
            startJumpAnim();
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onTurned(int dir) {
            startTurnAnim(dir);
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onSuccess() {
            startSuccessAnim();
            Utils.waitNoThrow(this, -1);
            ProbotView.this.nextLevel();
            postInvalidate();
        }

        @Override
        public void stop() {
            super.stop();
            setProgramLine(-1);
            if (animation != null) {
                animation.stop();
                animation = null;
            }

        }
    };

    Paint p = new Paint();
    Rect r = new Rect();
    RectF rf = new RectF();
    int radius = 0;
    int cw, ch;

    AAnimation<Canvas> animation = null;

    void init(Context c, AttributeSet a) {
    }

    public ProbotView(Context context) {
        super(context);
    }

    public ProbotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ProbotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
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

        int cols = probot.coins[0].length;
        int rows = probot.coins.length;

        // get cell width/height
        cw = getWidth() / cols;
        ch = getHeight() / rows;
        radius = Math.round(0.2f * Math.min(cw, ch));

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);
        for (int i=0; i<rows; i++) {
            for (int ii=0; ii<cols; ii++) {
                if (probot.coins[i][ii] != 0) {
                    int x = ii*cw + cw/2;
                    int y = i*ch + ch/2;
                    canvas.drawCircle(x, y, radius, p);
                }
            }
        }

        // draw the pacman
        radius = Math.round(0.4f * Math.min(cw, ch));
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
                    angStart = Math.round((1.0f - position) * 2 * 45);
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

    void startJumpAnim() {
        animation = new AAnimation<Canvas>(1000) {

            Bezier b = null;

            @Override
            protected void draw(Canvas canvas, float position, float dt) {
                if (b == null) {
                    b = new Bezier();
                    int x = probot.posx*cw + cw/2;
                    int y = probot.posy*ch + ch/2;
                    b.addPoint(x, y);
                    switch (probot.dir) {
                        case Right:
                            b.addPoint(x+cw*2/3, y-ch/2);
                            b.addPoint(x+cw*4/3, y-ch/2);
                            b.addPoint(x+cw*2, y);
                            break;
                        case Down:
                            b.addPoint(x+cw, y+ch*2/3);
                            b.addPoint(x+cw, y+ch*4/3);
                            b.addPoint(x, y+ch*2);
                            break;
                        case Left:
                            b.addPoint(x-cw*2/3, y-ch/2);
                            b.addPoint(x-cw*4/3, y-ch/2);
                            b.addPoint(x+cw*2, y);
                            break;
                        case Up:
                            b.addPoint(x+cw, y-ch*2/3);
                            b.addPoint(x+cw, y-ch*4/3);
                            b.addPoint(x, y-ch*2);
                            break;
                    }
                }
                rf.set(-radius, -radius, radius, radius);
                int angStart=0, sweepAng=270;
                if (position < 0.5f) {
                    angStart = Math.round(position * 2 * 45);
                } else {
                    angStart = Math.round((1.0f - position) * 2 * 45);
                }
                sweepAng = 360 - angStart*2;

                switch (probot.dir) {

                    case Right:
                        break;
                    case Down:
                        angStart+=90;
                        break;
                    case Left:
                        angStart+=180;
                        break;
                    case Up:
                        angStart+=270;
                        break;
                }
                Vector2D v = b.getPointAt(position);
                int x = v.Xi();
                int y = v.Yi();
                canvas.save();
                canvas.translate(x, y);
                canvas.drawArc(rf, angStart, sweepAng, true, p);
                canvas.restore();
            }

            @Override
            protected void onDone() {
                animation = null;
                synchronized (probot) {
                    probot.notifyAll();
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

    void startTurnAnim(final int dir) {
        animation = new AAnimation<Canvas>(500) {
            @Override
            protected void draw(Canvas g, float position, float dt) {
                int x = probot.posx*cw + cw/2;
                int y = probot.posy*ch + ch/2;

                g.save();
                g.translate(x, y);
                g.rotate(Math.round(position * 90 * dir));
                drawGuy(g, 0, 0);
                g.restore();
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

    void startSuccessAnim() {
        animation = new AAnimation<Canvas>(3000) {

            final int [] faces = new int [] {
                    R.drawable.guy_smile1,
                    R.drawable.guy_smile2,
                    R.drawable.guy_smile3
            };

            @Override
            protected void draw(Canvas g, float position, float dt) {
                float parts = 1.0f / faces.length;
                int x = probot.posx*cw + cw/2;
                int y = probot.posy*ch + ch/2;
                for (int i=faces.length-1; i>=0; i--) {
                    if (position >= parts*i) {
                        Drawable d = getContext().getResources().getDrawable(faces[i]);
                        d.setBounds(x-radius, y-radius, x+radius, y+radius);
                        d.draw(g);
                        break;
                    }
                }

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

    void nextLevel() {
        setLevel(probot.level+1);
    }

    void setLevel(int level) {
        this.maxLevel = Math.max(maxLevel, level);
        ((ProbotActivity)getContext()).getPrefs().edit()
                .putInt("Level", level)
                .putInt("MaxLevel", maxLevel)
                .apply();
        probot.setLevel(level);
        postInvalidate();
        setProgramLine(-1);
    }

    void setProgramLine(int line) {
        ((ProbotActivity)getContext()).lv.setProgramLineNum(line);
    }
}
