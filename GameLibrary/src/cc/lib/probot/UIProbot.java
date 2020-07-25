package cc.lib.probot;

import java.util.HashMap;
import java.util.Map;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;
import cc.lib.utils.StopWatch;

public abstract class UIProbot extends Probot {

    private int radius = 0;
    private int cw, ch;
    private int rows, cols;
    private final StopWatch sw = new StopWatch();

    public UIProbot() {
        sw.start();
    }

    protected abstract void repaint();

    protected abstract void setProgramLine(int line);

    public final void paint(AGraphics g, int mouseX, int mouseY) {
        cols = level.coins[0].length;
        rows = level.coins.length;

        // get cell width/height
        cw = g.getViewportWidth() / cols;
        ch = g.getViewportHeight() / rows;
        radius = Math.round(0.2f * Math.min(cw, ch));

        g.clearScreen(GColor.BLACK);

        drawGrid(g);

        drawLazerBeams(g);
        drawChompers(g);
    }

    public void setPaused(boolean paused) {
        if (paused) {
            sw.pause();
        } else {
            sw.unpause();
        }
    }

    public boolean isPaused() {
        return sw.isPaused();
    }

    void drawGrid(AGraphics g) {
        for (int i=0; i<rows; i++) {
            for (int ii=0; ii<cols; ii++) {
                int x = ii*cw + cw/2;
                int y = i*ch + ch/2;
                switch (level.coins[i][ii]) {
                    case EM:
                        break;
                    case DD:
                        g.setColor(GColor.WHITE);
                        g.drawFilledCircle(x, y, radius);
                        break;
                    case SE:
//                        drawGuy(g, x, y, Direction.Right, 0);
                        break;
                    case SS:
                        //                      drawGuy(g, x, y, Direction.Down, 0);
                        break;
                    case SW:
                        //                    drawGuy(g, x, y, Direction.Left, 0);
                        break;
                    case SN:
                        //                  drawGuy(g, x, y, Direction.Up, 0);
                        break;
                    case LH0:
                        drawLazer(g, x, y, true, GColor.RED);
                        break;
                    case LV0:
                        drawLazer(g, x, y, false, GColor.RED);
                        break;
                    case LB0:
                        drawButton(g, x, y, GColor.RED, level.lazers[0]);
                        break;
                    case LH1:
                        drawLazer(g, x, y, true, GColor.BLUE);
                        break;
                    case LV1:
                        drawLazer(g, x, y, false, GColor.BLUE);
                        break;
                    case LB1:
                        drawButton(g, x, y, GColor.BLUE, level.lazers[1]);
                        break;
                    case LH2:
                        drawLazer(g, x, y, true, GColor.GREEN);
                        break;
                    case LV2:
                        drawLazer(g, x, y, false, GColor.GREEN);
                        break;
                    case LB2:
                        drawButton(g, x, y, GColor.GREEN, level.lazers[2]);
                        break;
                    case LB:
                        // toggle all button
                        g.setColor(GColor.RED);
                        g.drawFilledCircle(x, y, radius*3/2);
                        g.setColor(GColor.GREEN);
                        g.drawFilledCircle(x, y, radius);
                        g.setColor(GColor.BLUE);
                        g.drawFilledCircle(x, y, radius*2/3);
                        break;
                }
            }
        }
    }

    void drawLazerBeams(AGraphics g) {
        int lazerThickness = 5;
        final int cols = level.coins[0].length;
        final int rows = level.coins.length;

        // draw lazers
        g.setColor(GColor.RED);
        for (int i=0; i<rows; i++) {
            for (int ii=0; ii<cols; ii++) {
                int cx = ii*cw + cw/2;
                int cy = i*ch + ch/2;
                int left = ii*cw;
                int right = left + cw;
                int top = i*ch;
                int bottom = top + ch;

                if (0 != (lazer[i][ii] & Probot.LAZER_WEST)) {
                    g.drawLine(left, cy, cx, cy, lazerThickness);
                }

                if (0 != (lazer[i][ii] & Probot.LAZER_EAST)) {
                    g.drawLine(cx, cy, right, cy, lazerThickness);
                }

                if (0 != (lazer[i][ii] & Probot.LAZER_NORTH)) {
                    g.drawLine(cx, top, cx, cy, lazerThickness);
                }

                if (0 != (lazer[i][ii] & Probot.LAZER_SOUTH)) {
                    g.drawLine(cx, cy, cx, bottom, lazerThickness);
                }

            }
        }
    }

    /**
     *
     * @param g
     * @param x
     * @param y
     * @param dir
     * @param chompPosition 0-1 value. 0 is full open and 1 is full closed
     */
    public void drawGuy(AGraphics g, float x, float y, Direction dir, float chompPosition) {

        int angStart=0;
        int sweepAng=270;
        if (chompPosition < 0.5f) {
            angStart = Math.round(chompPosition * 2 * 45);
        } else {
            angStart = Math.round((1.0f - chompPosition) * 2 * 45);
        }
        sweepAng = 360 - angStart*2;

        switch (dir) {

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

        g.drawFilledArc(x, y, radius, angStart, sweepAng, 16);
        if (chompPosition <= 0) {
            g.setColor(GColor.BLACK);
            switch (dir) {
                case Right:
                    g.drawLine(x, y, x+radius, y, 2);
                    break;
                case Down:
                    g.drawLine(x, y, x, y+radius, 2);
                    break;
                case Left:
                    g.drawLine(x, y, x-radius, y, 2);
                    break;
                case Up:
                    g.drawLine(x, y, x, y-radius, 2);
                    break;
            }
        }

    }

    synchronized void drawChompers(AGraphics g) {
        // draw the pacman
        radius = Math.round(0.4f * Math.min(cw, ch));
        synchronized (animations) {
            if (isRunning()) {
                if (!animations.isEmpty()) {
                    for (AAnimation<AGraphics> a : animations.values()) {
                        a.update(g);
                    }
                    repaint();
                    return;
                }
            } else {
                animations.clear();
            }
        }
        for (Guy guy : getGuys()) {
            int x = guy.posx * cw + cw / 2;
            int y = guy.posy * ch + ch / 2;

            g.setColor(guy.color);

            drawGuy(g, x, y, guy.dir, 0);
        }

    }

    void drawButton(AGraphics g, int cx, int cy, GColor color, boolean on) {
        g.setColor(GColor.GRAY);
        g.drawFilledCircle(cx, cy, radius);
        g.setColor(color);
        if (on) {
            g.drawFilledCircle(cx, cy, radius/2);
        } else {
            g.drawCircle(cx, cy, radius/2);
        }
    }

    void drawLazer(AGraphics g, int cx, int cy, boolean horz, GColor color) {
        g.pushMatrix();
        g.translate(cx, cy);
        if (!horz) {
            g.rotate(90);
        }
        g.setColor(GColor.GRAY);
        float radius = this.radius*3/2;
        g.drawFilledCircle(0, 0, radius);
        g.setColor(color);
        g.begin();
        g.vertex(-radius, 0);
        g.vertex(0, -radius/2);
        g.vertex(radius, 0);
        g.vertex(0, radius/2);
        g.drawTriangleFan();
        g.popMatrix();
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public void setLevel(int num, Level level) {
        super.setLevel(num, level);
        animations.clear();
        repaint();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        synchronized (animations) {
            animations.clear();
        }
        setPaused(false);
        repaint();
    }

    final Map<Guy, AAnimation<AGraphics>> animations = new HashMap<>();

    abstract class BaseAnim extends AAnimation<AGraphics> {
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
            synchronized (UIProbot.this) {
                UIProbot.this.notify();
            }
        }

        private boolean startedCorrectly = false;

        @Override
        protected void onStarted() {
            Utils.assertTrue(startedCorrectly);
        }

        public AAnimation<AGraphics> start(Guy guy) {
            synchronized (animations) {
                animations.put(guy, this);
                Utils.assertTrue(animations.size() < 32);
            }
            startedCorrectly = true;
            repaint();
            return start();
        }

        @Override
        protected long getCurrentTimeMSecs() {
            sw.capture();
            return sw.getTime();
        }
    };

    class JumpAnim extends BaseAnim {
        JumpAnim(Guy guy) {
            this(guy, 1, 1000);
        }

        JumpAnim(Guy guy, float advanceAmt, long durMs) {
            super(durMs);
            this.guy = guy;
            this.advanceAmt = advanceAmt;
        }

        Bezier b = null;
        final Guy guy;
        float x, y;
        final float advanceAmt;

        @Override
        protected void draw(AGraphics g, float position, float dt) {
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
            Vector2D v = b.getPointAt(position*advanceAmt);
            g.setColor(guy.color);
            x = v.getX();
            y = v.getY();
            drawGuy(g, v.getX(), v.getY(), guy.dir, position*advanceAmt);
        }

    }

    class ChompAnim extends BaseAnim {
        final Guy guy;
        float x, y;
        final float advanceAmt;

        ChompAnim(Guy guy) {
            this(guy, 1, 800);
        }

        ChompAnim(Guy guy, float advanceAmt, int durMSecs) {
            super(durMSecs);
            this.guy = guy;
            this.advanceAmt = advanceAmt;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            final float dx = guy.dir.dx * cw;
            final float dy = guy.dir.dy * ch;
            x = guy.posx*cw + cw/2 +dx*position*advanceAmt;
            y = guy.posy*ch + ch/2 +dy*position*advanceAmt;

            g.setColor(guy.color);
            drawGuy(g, x, y, guy.dir, position*advanceAmt);
        }

    }

    class TurnAnim extends BaseAnim {
        final Guy guy;
        final int dir;

        TurnAnim(Guy guy, int dir) {
            super(500);
            this.guy = guy;
            this.dir = dir;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            int x = guy.posx*cw + cw/2;
            int y = guy.posy*ch + ch/2;

            g.pushMatrix();
            g.translate(x, y);
            g.rotate(Math.round(position * 90 * dir));
            g.setColor(guy.color);
            drawGuy(g, 0, 0, guy.dir, 0);
            g.popMatrix();
        }
    }

    /**
     * 2 Part animation: Guy turns gray then shatters.
     */
    class LazeredAnim extends BaseAnim {

        final float sx, sy;
        final float chompPos;
        final Guy guy;

        LazeredAnim(float x, float y, Guy guy, float chompPos) {
            super(1000);
            sx = x; sy = y; this.guy = guy;
            this.chompPos = chompPos;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            g.pushMatrix();
            g.translate(sx, sy);
            g.setColor(GColor.YELLOW);
            drawGuy(g, 0, 0, guy.dir, chompPos);
            g.scale(position);
            g.setColor(GColor.GRAY);
            drawGuy(g, 0, 0, guy.dir, chompPos);
            g.popMatrix();
        }

        @Override
        protected void onDone() {
            animations.put(guy, new BaseAnim(2000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    g.setColor(GColor.GRAY.darkened(position));
                    float lw = 10f-position*10;
                    g.pushMatrix();
                    g.begin();
                    g.translate(sx, sy);
                    g.scale(1f+position*5, 1f+position*5);
                    // draw a circle with just dots
                    for (float i=1; i<=radius; i+=radius/10) {
                        for (int rad=0; rad<360; rad+=10) {
                            g.rotate(10);
                            g.vertex(i, 0);
                        }
                        g.rotate(5);
                    }
                    g.drawPoints(lw);
                    g.popMatrix();
                }
            }.start(guy));
        }
    }

    class GlowAnim extends BaseAnim {
        final Guy guy;
        final GColor glow;
        GlowAnim(Guy guy) {
            super(500, 6, true);
            this.guy = guy;
            this.glow = guy.color.inverted();
        }
        protected void draw(AGraphics g, float position, float dt) {
            g.setColor(guy.color.interpolateTo(GColor.RED, position));
            int x = guy.posx*cw + cw/2;
            int y = guy.posy*ch + ch/2;
            drawGuy(g, x, y, guy.dir, 0);
        }
    }

    class FallingAnim extends BaseAnim {
        final Guy guy;
        FallingAnim(Guy guy ) {
            super(1500);
            this.guy = guy;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            g.setColor(guy.color);
            float x = (guy.dir.dx*position + guy.posx)*cw + cw/2;
            float y = (guy.dir.dy*position + guy.posy)*ch + ch/2;
            g.pushMatrix();
            g.translate(x, y);
            g.rotate(1000f*position);
            g.scale(1f-position);
            drawGuy(g, 0, 0, guy.dir, .5f);
            g.popMatrix();
        }
    }


    public void startProgramThread() {
        animations.clear();
        new Thread() {
            @Override
            public void run() {
                repaint();
                runProgram();
                repaint();
            }
        }.start();
    }

    @Override
    protected void onCommand(int line) {
        // wait for any animations to finish
        while (!isAnimationsDone()) {
            Utils.waitNoThrow(this, 1000);
        }
        setProgramLine(line);

        // give a brief pause on loops
        switch (get(line).type) {
            case LoopStart:
            case LoopEnd:
            case IfElse:
            case IfEnd:
            case IfThen:
                //Utils.waitNoThrow(this, 500);
        }
    }

    boolean isAnimationsDone() {
        synchronized (animations) {
            for (AAnimation<AGraphics> a : animations.values()) {
                if (!a.isDone())
                    return false;
            }
        }
        return true;
    }

    void addAnimation(Guy guy, BaseAnim anim) {
        synchronized (animations) {
            animations.put(guy, anim.start(guy));
        }
        repaint();
    }

    @Override
    protected void onFailed() {
        reset();
        repaint();
    }

    @Override
    protected void onAdvanceFailed(Guy guy) {
        addAnimation(guy, new FallingAnim(guy));
    }

    @Override
    final protected void onAdvanced(Guy guy) {
        addAnimation(guy, new ChompAnim(guy));
    }

    protected abstract int [] getFaceImageIds(AGraphics g);


    @Override
    protected void onSuccess() {
        for (Guy guy : guys) {
            addAnimation(guy, new BaseAnim(3000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {

                    int [] faces = getFaceImageIds(g);

                    float parts = 1.0f / faces.length;
                    int x = guy.posx*cw;
                    int y = guy.posy*ch;
                    for (int i=faces.length-1; i>=0; i--) {
                        if (position >= parts*i) {
                            g.drawImage(faces[i], x, y, cw, ch);
                            break;
                        }
                    }
                }
            });
        }
    }

    @Override
    final protected void onJumped(Guy guy) {
        addAnimation(guy, new JumpAnim(guy));
    }

    @Override
    final protected void onTurned(Guy guy, int dir) {
        addAnimation(guy, new TurnAnim(guy, dir));
    }

    @Override
    final protected void onLazered(final Guy guy, int type) {
        switch (type) {
            case 0: {
                int x = guy.posx * cw + cw / 2;
                int y = guy.posy * ch + ch / 2;
                addAnimation(guy, new LazeredAnim(x, y, guy, 0));
                break;
            }

            case 1: {
                addAnimation(guy, new ChompAnim(guy, .6f, 500) {
                    @Override
                    protected void onDone() {
                        addAnimation(guy, new LazeredAnim(x, y, guy, advanceAmt));
                    }
                });
                break;
            }
            case 2: {
                addAnimation(guy, new JumpAnim(guy, .8f, 800) {
                    @Override
                    protected void onDone() {
                        addAnimation(guy, new LazeredAnim(x, y, guy, advanceAmt));
                    }
                });
                break;
            }
        }
    }

    @Override
    final protected void onDotsLeftUneaten() {
        for (Guy guy : guys) {
            addAnimation(guy, new GlowAnim(guy));
        }
    }


}
