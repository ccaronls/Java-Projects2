package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;
import cc.lib.probot.Command;
import cc.lib.probot.CommandType;
import cc.lib.probot.Direction;
import cc.lib.probot.Guy;
import cc.lib.probot.Level;
import cc.lib.probot.Probot;
import cc.lib.utils.FileUtils;
import cc.lib.utils.HtmlUtils;
import cc.lib.utils.Reflector;
import javafx.scene.control.TextAreaBuilder;

public class AWTProbot extends AWTComponent {

    final JTextComponent txtEditor;
    final AWTFrame frame;
    final AWTButton run, pause, stop, clear, quit;
    final List<Level> levels = new ArrayList<>();
    int curLevel = 0;

    final Probot probot = new Probot() {
        @Override
        protected void onCommand(Guy guy, int line) {
            // wait for any animations to finish
            while (animations.size() > 0) {
                Utils.waitNoThrow(probot, 1000);
            }
            setProgramLine(line);

            // give a brief pause on loops
            switch (get(line).type) {
                case LoopStart:
                case LoopEnd:
                    Utils.waitNoThrow(this, 500);
            }
        }

        @Override
        protected void onFailed() {
            super.onFailed();
        }

        @Override
        protected void onAdvanceFailed(Guy guy) {
            animations.put(guy, new BlinkingAnim(guy).start(guy));
        }

        @Override
        protected void onAdvanced(Guy guy) {
            animations.put(guy, new ChompAnim(guy).start(guy));
        }

        @Override
        protected void onJumped(Guy guy) {
            animations.put(guy, new JumpAnim(guy).start(guy));
        }

        @Override
        protected void onTurned(Guy guy, int dir) {
            animations.put(guy, new TurnAnim(guy, dir).start(guy));
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

    public static void main(String [] args) {
        Utils.DEBUG_ENABLED=true;
        try {
            new AWTProbot();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public class MyCaret extends DefaultCaret {

        private String mark = "<";

        public MyCaret() {
            setBlinkRate(500);
        }

        @Override
        protected synchronized void damage(Rectangle r) {
            if (r == null) {
                return;
            }

            JTextComponent comp = getComponent();
            FontMetrics fm = comp.getFontMetrics(comp.getFont());
            int textWidth = fm.stringWidth(">");
            int textHeight = fm.getHeight();
            x = r.x;
            y = r.y;
            width = textWidth;
            height = textHeight;
            repaint(); // calls getComponent().repaint(x, y, width, height)
        }

        @Override
        public void paint(Graphics g) {
            JTextComponent comp = getComponent();
            if (comp == null) {
                return;
            }

            int dot = getDot();
            Rectangle r = null;
            try {
                r = comp.modelToView(dot);
            } catch (BadLocationException e) {
                return;
            }
            if (r == null) {
                return;
            }

            if ((x != r.x) || (y != r.y)) {
                repaint(); // erase previous location of caret
                damage(r);
            }

            if (isVisible()) {
                FontMetrics fm = comp.getFontMetrics(comp.getFont());
                int textWidth = fm.stringWidth(">");
                int textHeight = fm.getHeight();

                g.setColor(comp.getCaretColor());
                g.drawString(mark, x, y + fm.getAscent());
            }
        }

    }

    String getTemplateHTML() {
        try {
            return FileUtils.inputStreamToString(FileUtils.openFileOrResource("pr_template.html"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    boolean compileProgram() {
        String txt = txtEditor.getText();
        Pattern li = Pattern.compile("<li");
        Matcher m = li.matcher(txt);
        boolean success = true;
        String html = getTemplateHTML();

        String htmlFirst = html.substring(0, html.indexOf("<ol>")+4);
        String htmlLast  = html.substring(html.indexOf("</ol>"));
        String htmlList = "";

        try {
            while (m.find()) {
                String cmd = m.group().replaceAll("<[^>]+>", "").trim().toLowerCase();
                String color = "good";
                switch (cmd) {
                    case "chomp":
                        probot.add(new Command(CommandType.Advance, 1));
                        break;
                    case "right":
                        probot.add(new Command(CommandType.TurnRight, 1));
                        break;
                    case "left":
                        probot.add(new Command(CommandType.TurnLeft, 1));
                        break;
                    case "jump":
                        probot.add(new Command(CommandType.Jump, 2));
                        break;
                    default:
                        color = "bad";
                        success = false;
                        break;
                }
                htmlList += "<li class=\"" + color + "\">" + cmd + "</li><br/>";
            }
        } finally {
            txtEditor.setText(htmlFirst + htmlList + htmlLast);
        }
        return success;
    }

    AWTProbot() throws Exception {
        final File settingsDir = FileUtils.getOrCreateSettingsDirectory(getClass());
        final File lbSettingsDir = FileUtils.getOrCreateSettingsDirectory(AWTProbotLevelBuilder.class);

        setMouseEnabled(false);

        //JTextArea txtArea = new JTextArea();
        //final JEditorPane txtArea = new JEditorPane();
        final JTextPane txtArea = new JTextPane();
        txtArea.setEditable(true);
        txtArea.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 0), 5));
        txtArea.setContentType("text/html");
        txtArea.setText(getTemplateHTML());
        InputMap iMap = txtArea.getInputMap();
        ActionMap aMap = txtArea.getActionMap();
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        aMap.put("enter", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
//                txtArea.replaceSelection("</li><br/>\n   <li>");
//                txtArea.setText(txtArea.getText());
                HTMLDocument doc = (HTMLDocument)txtArea.getDocument();
                HTMLEditorKit kit = (HTMLEditorKit)txtArea.getEditorKit();
                int pos = txtArea.getCaretPosition();
                try {
                    kit.insertHTML(doc, pos, "</li><br/><li>", 1, 1, null);
                    FileUtils.stringToFile(txtArea.getText(), new File("/tmp/x.html"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                /*
                Element elem = doc.getElement("caret");
                elem.getStartOffset();
                String txt = txtArea.getText();
                int pos = txtArea.getCaretPosition();
                String newTxt = txt.substring(0, pos) + "</li><br/>\n   <li>";
                String remainingTxt = txt.substring(pos);
                txtArea.setText(newTxt + remainingTxt);*/
            }
        });
        txtArea.setCaret(new MyCaret());
        String curText = txtArea.getText();
        //int caretPos = curText.lastIndexOf("</li>");
        txtArea.setCaretPosition(1);

        txtEditor = txtArea;

        clear = new AWTButton("CLEAR") {
            @Override
            protected void onAction() {
                txtEditor.setText(getTemplateHTML());
            }
        };
        run = new AWTButton("RUN") {
            @Override
            protected void onAction() {
                if (compileProgram())
                    startProgramThread();
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
                System.exit(0);
            }
        };

        AWTPanel topButtons = new AWTPanel(run, clear, quit);
        AWTPanel left = new AWTPanel(new BorderLayout());

        left.add(txtEditor, BorderLayout.CENTER);
        left.add(topButtons, BorderLayout.NORTH);
        frame = new AWTFrame("Probot") {
            @Override
            protected void onWindowResized(int w, int h) {
                super.onWindowResized(w, h);
                //txtArea.grabFocus();
                txtEditor.grabFocus();
            }
        };
        frame.add(this, BorderLayout.CENTER);
        frame.add(left, BorderLayout.WEST);
        File saveFile = new File(settingsDir, "game.txt");
        File lbLevelsFile = new File(lbSettingsDir, "levels_backup.txt");

        levels.addAll(Reflector.deserializeFromFile(lbLevelsFile));
        if (saveFile.isFile()) {
            probot.tryLoadFromFile(saveFile);
        } else {
            probot.init(levels.get(0));
            probot.start();
        }

        frame.centerToScreen(640, 480);
    }

    void setProgramLine(int line) {

    }

    int radius = 0;
    int cw, ch;
    int rows, cols;

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        cols = probot.level.coins[0].length;
        rows = probot.level.coins.length;

        // get cell width/height
        cw = getWidth() / cols;
        ch = getHeight() / rows;
        radius = Math.round(0.2f * Math.min(cw, ch));

        g.clearScreen(GColor.BLACK);

        drawGrid(g);

        drawLazerBeams(g);
        drawChompers(g);
    }

    void drawGrid(AGraphics g) {
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

    void drawLazerBeams(AGraphics g) {
        int lazerThickness = 5;
        final int cols = probot.level.coins[0].length;
        final int rows = probot.level.coins.length;

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

                if (0 != (probot.lazer[i][ii] & Probot.LAZER_WEST)) {
                    g.drawLine(left, cy, cx, cy, lazerThickness);
                }

                if (0 != (probot.lazer[i][ii] & Probot.LAZER_EAST)) {
                    g.drawLine(cx, cy, right, cy, lazerThickness);
                }

                if (0 != (probot.lazer[i][ii] & Probot.LAZER_NORTH)) {
                    g.drawLine(cx, top, cx, cy, lazerThickness);
                }

                if (0 != (probot.lazer[i][ii] & Probot.LAZER_SOUTH)) {
                    g.drawLine(cx, cy, cx, bottom, lazerThickness);
                }

            }
        }
    }

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
        for (Guy guy : probot.getGuys()) {
            int x = guy.posx * cw + cw / 2;
            int y = guy.posy * ch + ch / 2;

            g.setColor(guy.color);

            AAnimation<AGraphics> animation = animations.get(guy);
            if (animation != null && !animation.isDone()) {
                animation.update(g);
                repaint();
            } else {
                drawGuy(g, x, y, guy.dir, 0);
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
            synchronized (animations) {
                for (Map.Entry<Guy, AAnimation<AGraphics>> e : animations.entrySet()) {
                    if (this == e.getValue()) {
                        animations.remove(e.getKey());
                        break;
                    }
                }
            }
            synchronized (probot) {
                probot.notify();
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
    };

    class JumpAnim extends BaseAnim {
        JumpAnim(Guy guy) {
            super(1000);
            this.guy = guy;
        }

        Bezier b = null;
        final Guy guy;

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
            Vector2D v = b.getPointAt(position);
            drawGuy(g, v.getX(), v.getY(), guy.dir, position);
        }

    }

    class ChompAnim extends BaseAnim {
        final Guy guy;

        ChompAnim(Guy guy) {
            super(800);
            this.guy = guy;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            float x = guy.posx*cw + cw/2;
            float y = guy.posy*ch + ch/2;
            float dx=0, dy=0;

            switch (guy.dir) {

                case Right:
                    dx = cw;
                    break;
                case Down:
                    dy = ch;
                    break;
                case Left:
                    dx = -cw;
                    break;
                case Up:
                    dy = -ch;
                    break;
            }
            drawGuy(g, x+dx*position, y+dy*position, guy.dir, position);
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
            drawGuy(g, 0, 0, guy.dir, 0);
            g.popMatrix();
        }
    }

    class BlinkingAnim extends BaseAnim {
        final Guy guy;

        BlinkingAnim(Guy guy) {
            super(500, 6, true);
            this.guy = guy;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            g.setColor(GColor.YELLOW.interpolateTo(GColor.RED, position));
            int x = guy.posx * cw + cw / 2;
            int y = guy.posy * ch + ch / 2;
            drawGuy(g, x, y, guy.dir, 0);
        }
    }

    /**
     * 2 Part animation: Guy turns gray then shatters.
     */
    abstract class LazeredAnim extends BaseAnim {

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

            /*
            int [] colors = { Color.YELLOW, Color.GRAY, Color.GRAY };
            float [] stops = { 0, 1f-position, 1 };
            float rad = (1f-position)*radius*2;
            pp.setColor(Color.GRAY);
            if (rad > 0)
                pp.setShader(new RadialGradient(rect.centerX(), rect.centerY(), radius, colors, stops, Shader.TileMode.CLAMP));
            else
                pp.setShader(null);
*/

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
                    g.setColor(GColor.GRAY.darkened(position/2));
                    float lw = 10f-position*10;
                    g.pushMatrix();
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


    void startProgramThread() {
        new Thread() {
            @Override
            public void run() {
                repaint();
                probot.runProgram();
                repaint();
            }
        }.start();
    }
}
