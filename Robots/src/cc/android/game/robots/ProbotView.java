package cc.android.game.robots;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import cc.lib.android.DroidUtils;
import cc.lib.game.AAnimation;
import cc.lib.game.Utils;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;
import cc.lib.probot.Direction;
import cc.lib.probot.Guy;
import cc.lib.probot.Level;
import cc.lib.probot.Probot;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 12/7/17.
 */

public class ProbotView extends View {

    int maxLevel = 0;

    Probot probot = new Probot() {

        @Override
        protected void onCommand(Guy guy, int line) {
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
            postInvalidate();
        }

        @Override
        protected void onDotsLeftUneaten() {
            for (Guy guy : probot.getGuys())
                startFailedAnim(guy);
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onAdvanced(Guy guy) {
            super.onAdvanced(guy);
            startAdvanceAnim(guy);
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onAdvanceFailed(Guy guy) {
            startFailedAnim(guy);
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onJumped(Guy guy) {
            startJumpAnim(guy);
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onTurned(Guy guy, int dir) {
            startTurnAnim(guy, dir);
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onLazered(Guy guy, boolean instantaneous) {
            startLazeredAnim(guy, instantaneous);
            Utils.waitNoThrow(this, -1);
            postInvalidate();
        }

        @Override
        protected void onSuccess() {
            for (Guy guy : probot.getGuys())
                startSuccessAnim(guy);
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
        try {
            InputStream in = c.getAssets().open("levels.txt");
            try {
                levels = Reflector.deserializeObject(new BufferedReader(new InputStreamReader(in)));
            } finally {
                in.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        int cols = probot.level.coins[0].length;
        int rows = probot.level.coins.length;

        // get cell width/height
        cw = getWidth() / cols;
        ch = getHeight() / rows;
        radius = Math.round(0.2f * Math.min(cw, ch));

        for (int i=0; i<rows; i++) {
            for (int ii=0; ii<cols; ii++) {
                int x = ii*cw + cw/2;
                int y = i*ch + ch/2;
                switch (probot.level.coins[i][ii]) {
                    case EM:
                        break;
                    case DD:
                        p.setStyle(Paint.Style.FILL);
                        p.setColor(Color.WHITE);
                        canvas.drawCircle(x, y, radius, p);
                        break;
                    case SE:
                        break;
                    case SS:
                        break;
                    case SW:
                        break;
                    case SN:
                        break;
                    case LH0:
                        drawLazer(canvas, x, y, true, Color.RED);
                        break;
                    case LV0:
                        drawLazer(canvas, x, y, false, Color.RED);
                        break;
                    case LB0:
                        drawButton(canvas, x, y, Color.RED, probot.level.lazers[0]);
                        break;
                    case LH1:
                        drawLazer(canvas, x, y, true, Color.BLUE);
                        break;
                    case LV1:
                        drawLazer(canvas, x, y, false, Color.BLUE);
                        break;
                    case LB1:
                        drawButton(canvas, x, y, Color.BLUE, probot.level.lazers[1]);
                        break;
                    case LH2:
                        drawLazer(canvas, x, y, true, Color.GREEN);
                        break;
                    case LV2:
                        drawLazer(canvas, x, y, false, Color.GREEN);
                        break;
                    case LB2:
                        drawButton(canvas, x, y, Color.GREEN, probot.level.lazers[2]);
                        break;
                    case LB:
                        // toggle all button
                        p.setColor(Color.RED);
                        canvas.drawCircle(x, y, radius*3/2, p);
                        p.setColor(Color.GREEN);
                        canvas.drawCircle(x, y, radius, p);
                        p.setColor(Color.BLUE);
                        canvas.drawCircle(x, y, radius*2/3, p);
                        break;
                }
            }
        }

        // draw lazers
        p.setColor(Color.RED);
        for (int i=0; i<rows; i++) {
            for (int ii=0; ii<cols; ii++) {
                int cx = ii*cw + cw/2;
                int cy = i*ch + ch/2;
                int left = ii*cw;
                int right = left + cw;
                int top = i*ch;
                int bottom = top + ch;

                if (0 != (probot.lazer[i][ii] & Probot.LAZER_WEST)) {
                    canvas.drawLine(left, cy, cx, cy, p);
                }

                if (0 != (probot.lazer[i][ii] & Probot.LAZER_EAST)) {
                    canvas.drawLine(cx, cy, right, cy, p);
                }

                if (0 != (probot.lazer[i][ii] & Probot.LAZER_NORTH)) {
                    canvas.drawLine(cx, top, cx, cy, p);
                }

                if (0 != (probot.lazer[i][ii] & Probot.LAZER_SOUTH)) {
                    canvas.drawLine(cx, cy, cx, bottom, p);
                }

            }
        }

        // draw the pacman
        radius = Math.round(0.4f * Math.min(cw, ch));
        for (Guy guy : probot.getGuys()) {
            int x = guy.posx * cw + cw / 2;
            int y = guy.posy * ch + ch / 2;
            rf.set(x - radius, y - radius, x + radius, y + radius);

            p.setColor(Color.YELLOW);
            p.setStyle(Paint.Style.FILL);

            if (animation != null) {
                animation.update(canvas);
                invalidate();
            } else {
                canvas.drawCircle(x, y, radius, p);
                p.setColor(Color.BLACK);
                p.setStyle(Paint.Style.STROKE);
                switch (guy.dir) {
                    case Right:
                        canvas.drawLine(x, y, x + radius, y, p);
                        break;
                    case Down:
                        canvas.drawLine(x, y, x, y + radius, p);
                        break;
                    case Left:
                        canvas.drawLine(x, y, x - radius, y, p);
                        break;
                    case Up:
                        canvas.drawLine(x, y, x, y - radius, p);
                        break;
                }
            }
        }
    }

    abstract class BaseAnim extends AAnimation<Canvas> {
        public BaseAnim(long durationMSecs) {
            super(durationMSecs);
        }

        public BaseAnim(long durationMSecs, int repeats) {
            super(durationMSecs, repeats);
        }

        public BaseAnim(long durationMSecs, int repeats, boolean oscilateOnRepeat) {
            super(durationMSecs, repeats, oscilateOnRepeat);
        }

        @Override
        protected void onDone() {
            animation = null;
            synchronized (probot) {
                probot.notify();
            }

        }
    };

    Path lazerPath = new Path();
    void drawLazer(Canvas c, int cx, int cy, boolean horz, int color) {
        c.save();
        c.translate(cx, cy);
        if (!horz) {
            c.rotate(90);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.GRAY);
        float radius = this.radius*3/2;
        c.drawCircle(0, 0, radius, p);
        p.setColor(color);
        lazerPath.reset();
        lazerPath.moveTo(-radius, 0);
        lazerPath.lineTo(0, -radius/2);
        lazerPath.lineTo(radius, 0);
        lazerPath.lineTo(0, radius/2);
        lazerPath.close();
        c.drawPath(lazerPath, p);
        c.restore();
    }

    void drawButton(Canvas c, int cx, int cy, int color, boolean on) {
        p.setColor(Color.GRAY);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx, cy, radius, p);
        p.setColor(color);
        p.setStyle(on ? Paint.Style.FILL : Paint.Style.STROKE);
        c.drawCircle(cx, cy, radius/2, p);
    }

    void startLazeredAnim(final Guy guy, boolean instantaneous) {
        if (!instantaneous) {
            animation = new AdvanceAnim(guy,600, 0.6f) {
                @Override
                protected void onDone() {
                    animation = new LazeredAnim(rect) {
                        @Override
                        void drawMan(Canvas g, Paint p) {
                            drawPM(g, p);
                        }
                    }.start();
                }
            }.start();
        } else {
            RectF rect = new RectF();
            final float x = guy.posx*cw + cw/2;
            final float y = guy.posy*ch + ch/2;
            rect.set(x-radius, y-radius,
                    x+radius, y+radius);
            animation = new LazeredAnim(rect) {
                @Override
                void drawMan(Canvas g, Paint p) {
                    g.drawArc(rect, 0, 360, true, p);
                }
            }.start();
        }
        postInvalidate();
    }

    void startAdvanceAnim(Guy guy) {
        animation = new AdvanceAnim(guy, 1000, 1).start();
        postInvalidate();
    }


    abstract class LazeredAnim extends BaseAnim {

        final Paint pp;
        final RectF rect;

        LazeredAnim(RectF rect) {
            super(1000);
            pp = new Paint();
            pp.setStyle(Paint.Style.FILL);
            pp.setColor(p.getColor());
            this.rect = rect;
        }

        @Override
        protected void draw(Canvas g, float position, float dt) {

            int [] colors = { Color.YELLOW, Color.GRAY, Color.GRAY };
            float [] stops = { 0, 1f-position, 1 };
            float rad = (1f-position)*radius*2;
            pp.setColor(Color.GRAY);
            if (rad > 0)
                pp.setShader(new RadialGradient(rect.centerX(), rect.centerY(), radius, colors, stops, Shader.TileMode.CLAMP));
            else
                pp.setShader(null);

            drawMan(g, pp);
        }

        abstract void drawMan(Canvas g, Paint p);

        @Override
        protected void onDone() {
            animation = new BaseAnim(2000) {
                @Override
                protected void draw(Canvas g, float position, float dt) {
                    pp.setShader(null);
                    pp.setColor(Color.argb(Math.round(255*(1f-position)), Color.red(Color.GRAY), Color.green(Color.GRAY), Color.blue(Color.GRAY)));
                    pp.setStyle(Paint.Style.FILL_AND_STROKE);
                    pp.setStrokeWidth(10f-position*10);
                    g.save();
                    float cx = rect.centerX();
                    float cy = rect.centerY();
                    g.translate(cx, cy);
                    g.scale(1f+position*5, 1f+position*5);
                    // draw a circle with just dots
                    for (float i=1; i<=radius; i+=radius/10) {
                        for (int rad=0; rad<360; rad+=10) {
                            g.rotate(10);
                            g.drawPoint(i,0,pp);
                        }
                        g.rotate(5);
                    }
                    g.restore();
                }
            }.start();
        }
    }

    class AdvanceAnim extends BaseAnim {

        final float advanceAmt;
        int x=0,y=0,angStart=0, sweepAng=0;
        final RectF rect = new RectF();
        final Guy guy;

        AdvanceAnim(Guy guy, int dur, float advanceAmt) {
            super(dur);
            this.guy = guy;
            this.advanceAmt = advanceAmt;
        }

        @Override
        protected void draw(Canvas canvas, float position, float dt) {
            x = guy.posx*cw + cw/2;
            y = guy.posy*ch + ch/2;
            angStart=0;
            sweepAng=270;
            float dx=0, dy=0;
            if (position < 0.5f) {
                angStart = Math.round(position * 2 * 45);
            } else {
                angStart = Math.round((1.0f - position) * 2 * 45);
            }
            sweepAng = 360 - angStart*2;

            switch (guy.dir) {

                case Right:
                    dx = advanceAmt * cw;
                    break;
                case Down:
                    angStart+=90;
                    dy = advanceAmt * ch;
                    break;
                case Left:
                    angStart+=180;
                    dx = advanceAmt * -cw;
                    break;
                case Up:
                    angStart+=270;
                    dy = advanceAmt * -ch;
                    break;
            }

            rect.set(x-radius+Math.round(dx * position), y-radius+Math.round(dy * position),
                    x+radius+Math.round(dx * position), y+radius+Math.round(dy * position));

            drawPM(canvas, p);
        }

        void drawPM(Canvas canvas, Paint p) {
            canvas.drawArc(rect, angStart, sweepAng, true, p);
        }

    }

    void startJumpAnim(final Guy guy) {
        animation = new BaseAnim(1000) {

            Bezier b = null;

            @Override
            protected void draw(Canvas canvas, float position, float dt) {
                if (b == null) {
                    b = new Bezier();
                    int x = guy.posx*cw + cw/2;
                    int y = guy.posy*ch + ch/2;
                    b.addPoint(x, y);
                    switch (guy.dir) {
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
                            b.addPoint(x-cw*2, y);
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

                switch (guy.dir) {

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


        }.start();
        postInvalidate();
    }

    void startFailedAnim(final Guy guy) {
        animation = new BaseAnim(500, 6, true) {
            @Override
            protected void draw(Canvas canvas, float position, float dt) {
                p.setColor(DroidUtils.interpolateColor(Color.YELLOW, Color.RED, position));
                int x = guy.posx*cw + cw/2;
                int y = guy.posy*ch + ch/2;
                drawGuy(canvas, x, y, guy.dir);
            }

        }.start();
        postInvalidate();
    }

    void startTurnAnim(final Guy guy, final int dir) {
        animation = new BaseAnim(500) {
            @Override
            protected void draw(Canvas g, float position, float dt) {
                int x = guy.posx*cw + cw/2;
                int y = guy.posy*ch + ch/2;

                g.save();
                g.translate(x, y);
                g.rotate(Math.round(position * 90 * dir));
                drawGuy(g, 0, 0, guy.dir);
                g.restore();
            }
        }.start();
        postInvalidate();
    }

    void startSuccessAnim(final Guy guy) {
        animation = new BaseAnim(3000) {

            final int [] faces = new int [] {
                    R.drawable.guy_smile1,
                    R.drawable.guy_smile2,
                    R.drawable.guy_smile3
            };

            @Override
            protected void draw(Canvas g, float position, float dt) {
                float parts = 1.0f / faces.length;
                int x = guy.posx*cw + cw/2;
                int y = guy.posy*ch + ch/2;
                for (int i=faces.length-1; i>=0; i--) {
                    if (position >= parts*i) {
                        Drawable d = getContext().getResources().getDrawable(faces[i]);
                        d.setBounds(x-radius, y-radius, x+radius, y+radius);
                        d.draw(g);
                        break;
                    }
                }

            }
        }.start();
        postInvalidate();
    }

    void drawGuy(Canvas canvas, int x, int y, Direction dir) {
        canvas.drawCircle(x, y, radius, p);
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        switch (dir) {
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
        setLevel(probot.getLevelNum()+1);
    }

    List<Level> levels = null;

    void setLevel(int level) {
        this.maxLevel = Math.max(maxLevel, level);
        ((ProbotActivity)getContext()).getPrefs().edit()
                .putInt("Level", level)
                .putInt("MaxLevel", maxLevel)
                .apply();
        if (level >= levels.size())
            level = 0;
        probot.setLevel(level, levels.get(level).deepCopy());
        probot.start();
        postInvalidate();
        setProgramLine(-1);
    }

    void setProgramLine(int line) {
        ((ProbotActivity)getContext()).lv.setProgramLineNum(line);
    }
}
