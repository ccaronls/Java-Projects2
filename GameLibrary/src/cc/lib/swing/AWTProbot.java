package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import cc.lib.game.Utils;
import cc.lib.probot.Command;
import cc.lib.probot.CommandType;
import cc.lib.probot.Level;
import cc.lib.probot.UIProbot;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

public class AWTProbot extends AWTComponent {

    final JTextComponent txtEditor;
    final AWTFrame frame;
    final AWTButton run, pause, stop, clear, quit;
    final List<Level> levels = new ArrayList<>();
    int curLevel = 0;

    final UIProbot probot = new UIProbot() {
        @Override
        protected void repaint() {
            AWTProbot.this.repaint();
        }

        @Override
        protected void setProgramLine(int line) {

        }

        @Override
        protected void onSuccess() {
            curLevel += 1;
            if (curLevel >= levels.size())
                curLevel = 0;
            clear();
            setLevel(curLevel+1, levels.get(curLevel));
            txtEditor.setText(getTemplateHTML());
            start();
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

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        probot.paint(g, mouseX, mouseY);
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
        String txt = txtEditor.getText().replaceAll("[\n ]+", " ");
        Pattern li = Pattern.compile("<li[^<]*</li>");
        Matcher m = li.matcher(txt);
        boolean success = true;
        String html = getTemplateHTML();

        String htmlFirst = html.substring(0, html.indexOf("<ol>")+4);
        String htmlLast  = html.substring(html.indexOf("</ol>"));
        String htmlList = "";

        probot.clear();
        try {
            int nesting = 0;
            while (m.find()) {
                String group = m.group();
                String cmdStr = group.replaceAll("<[^>]+>", "").trim().toLowerCase();
                if (cmdStr.length() == 0)
                    continue;
                String [] cmd = cmdStr.split("[ ]+");
                String color = "good";
                switch (cmd[0]) {
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
                    case "done":
                        if (nesting > 0) {
                            probot.add(new Command(CommandType.LoopEnd, 1));
                            break;
                        }
                    case "repeat":
                        try {
                            probot.add(new Command(CommandType.LoopStart, Integer.parseInt(cmd[1])));
                            break;
                        } catch (Exception e) {
                            // fallthrough
                        }
                    default:
                        color = "bad";
                        success = false;
                        break;
                }
                htmlList += "<li class=\"" + color + "\">" + cmdStr + "</li><br/>";
            }
            if (nesting > 0)
                success = false;
        } finally {
            txtEditor.setText(htmlFirst + htmlList + htmlLast);
        }
        return success;
    }

    AWTProbot() throws Exception {
        final File settingsDir = FileUtils.getOrCreateSettingsDirectory(getClass());
        final File lbSettingsDir = FileUtils.getOrCreateSettingsDirectory(AWTProbotLevelBuilder.class);
        final File propertiesFile = new File(settingsDir, "awtprobot.properties");

        setMouseEnabled(false);

        //JTextArea txtArea = new JTextArea();
        //final JEditorPane txtArea = new JEditorPane();
        final JTextPane txtArea = new JTextPane();
        txtArea.setEditable(true);
        txtArea.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 0), 5));

        txtArea.setContentType("text/html");
        txtArea.setText(getTemplateHTML());
/*
        HTMLEditorKit editorKit = new HTMLEditorKit();
        Document document = (HTMLDocument)editorKit.createDefaultDocument();
        try (InputStream in = FileUtils.openFileOrResource("pr_template.html")) {
            editorKit.read(in, document, 0);
        }
        //document.addUndoableEditListener(undoHandler);
        txtArea.setDocument(document);
*/


        InputMap iMap = txtArea.getInputMap();
        ActionMap aMap = txtArea.getActionMap();
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        aMap.put("enter", new HTMLEditorKit.InsertHTMLTextAction("Bullets", "<li></li>",HTML.Tag.OL,HTML.Tag.LI));

        txtEditor = txtArea;
        HTMLDocument doc = (HTMLDocument)txtArea.getDocument();
        HTMLEditorKit kit = (HTMLEditorKit)txtArea.getEditorKit();

        kit.setDefaultCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

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
                    probot.startProgramThread();
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
            probot.setLevel(1, levels.get(0));
            probot.start();
        }

        frame.centerToScreen(640, 480);
        txtArea.grabFocus();
    }

}
