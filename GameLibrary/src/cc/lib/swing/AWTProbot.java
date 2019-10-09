package cc.lib.swing;

import java.awt.BorderLayout;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.probot.Guy;
import cc.lib.probot.Probot;
import cc.lib.utils.FileUtils;

public class AWTProbot extends AWTComponent {

    JTextArea txtArea;
    AWTFrame frame;
    AWTButton run, pause, stop, clear, quit;
    Probot probot = new Probot() {
        @Override
        protected void onCommand(Guy guy, int line) {
            super.onCommand(guy, line);
        }

        @Override
        protected void onFailed() {
            super.onFailed();
        }

        @Override
        protected void onAdvanceFailed(Guy guy) {
            super.onAdvanceFailed(guy);
        }

        @Override
        protected void onAdvanced(Guy guy) {
            super.onAdvanced(guy);
        }

        @Override
        protected void onJumped(Guy guy) {
            super.onJumped(guy);
        }

        @Override
        protected void onTurned(Guy guy, int dir) {
            super.onTurned(guy, dir);
        }

        @Override
        protected void onSuccess() {
            super.onSuccess();
        }

        @Override
        protected void onLazered(Guy guy, boolean instantaneous) {
            super.onLazered(guy, instantaneous);
        }

        @Override
        protected void onDotsLeftUneaten() {
            super.onDotsLeftUneaten();
        }
    };

    public static void main(String [] args) throws Exception {
        Utils.DEBUG_ENABLED=true;
        new AWTProbot();
    }

    AWTProbot() throws Exception {
        final File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        final File lbSettings = FileUtils.getOrCreateSettingsDirectory(AWTProbotLevelBuilder.class);

        setMouseEnabled(false);

        txtArea = new JTextArea();
        clear = new AWTButton("CLEAR") {
            @Override
            protected void onAction() {
                super.onAction();
            }
        };
        run = new AWTButton("RUN") {
            @Override
            protected void onAction() {
                super.onAction();
            }
        };
        pause = new AWTButton("PAUSE") {
            @Override
            protected void onAction() {
                super.onAction();
            }
        };
        stop = new AWTButton("STOP") {
            @Override
            protected void onAction() {
                super.onAction();
            }
        };
        quit = new AWTButton("QUIT") {
            @Override
            protected void onAction() {
                super.onAction();
            }
        };

        AWTPanel topButtons = new AWTPanel(run, clear, quit);
        AWTPanel left = new AWTPanel(new BorderLayout());
        left.add(topButtons, BorderLayout.NORTH);
        left.add(txtArea, BorderLayout.CENTER);
        frame.add(this, BorderLayout.CENTER);
        frame.add(left, BorderLayout.WEST);

        frame.fullscreenMode();
    }

    int radius = 0;
    int cw, ch;

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        int cols = probot.level.coins[0].length;
        int rows = probot.level.coins.length;

        // get cell width/height
        cw = getWidth() / cols;
        ch = getHeight() / rows;
        radius = Math.round(0.2f * Math.min(cw, ch));

        g.clearScreen(GColor.BLACK);

        for (int i=0; i<rows; i++) {
            for (int ii=0; ii<cols; ii++) {
                int x = ii*cw + cw/2;
                int y = i*ch + ch/2;
                switch (probot.level.coins[i][ii]) {
                    case EM:
                        break;
                    case DD:
                        g.setColor(GColor.WHITE);
                        g.drawFilledCircle(x, y, radius);
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
                        drawLazer(g, x, y, true, GColor.RED);
                        break;
                    case LV0:
                        drawLazer(g, x, y, false, GColor.RED);
                        break;
                    case LB0:
                        drawButton(g, x, y, GColor.RED, probot.level.lazers[0]);
                        break;
                    case LH1:
                        drawLazer(g, x, y, true, GColor.BLUE);
                        break;
                    case LV1:
                        drawLazer(g, x, y, false, GColor.BLUE);
                        break;
                    case LB1:
                        drawButton(g, x, y, GColor.BLUE, probot.level.lazers[1]);
                        break;
                    case LH2:
                        drawLazer(g, x, y, true, GColor.GREEN);
                        break;
                    case LV2:
                        drawLazer(g, x, y, false, GColor.GREEN);
                        break;
                    case LB2:
                        drawButton(g, x, y, GColor.GREEN, probot.level.lazers[2]);
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
        g.drawFilledRects();
        g.popMatrix();
    }

    final Set<AAnimation<AGraphics>> animations = new HashSet<>();

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
            synchronized (animations) {
                animations.remove(this);
            }
            synchronized (probot) {
                probot.notify();
            }
        }

        @Override
        protected void onStarted() {
            synchronized (animations) {
                animations.add(this);
                Utils.assertTrue(animations.size() < 32);
            }
        }
    };

    class AdvanceAnim extends BaseAnim {

        final float advanceAmt;
        int x=0,y=0,angStart=0, sweepAng=0;
        final Guy guy;


        AdvanceAnim(Guy guy, int dur, float advanceAmt) {
            super(dur);
            this.guy = guy;
            this.advanceAmt = advanceAmt;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
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

            g.drawFilledArc(x+dx*position, y+dy*position, radius, angStart, sweepAng, 16);
        }

    }
}
