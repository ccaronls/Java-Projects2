package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import cc.lib.game.AGraphics;
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
    final JButton run, pause, stop, clear, quit, next, previous;
    final JLabel currentLevel = new AWTLabel("-", 1, 14, true);
    final List<Level> levels = new ArrayList<>();

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
        aMap.put("enter", new HTMLEditorKit.InsertHTMLTextAction("Bullets", "<li>",HTML.Tag.OL,HTML.Tag.LI));

        txtEditor = txtArea;
        HTMLDocument doc = (HTMLDocument)txtArea.getDocument();
        HTMLEditorKit kit = (HTMLEditorKit)txtArea.getEditorKit();

        kit.setDefaultCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        clear = new AWTButton(ImageIO.read(FileUtils.openFileOrResource("clear_x.png"))) {
            @Override
            protected void onAction() {
                txtEditor.setText(getTemplateHTML());
            }
        };

        run = new AWTButton(ImageIO.read(FileUtils.openFileOrResource("play_triangle.png"))) {
            @Override
            protected void onAction() {
                if (probot.isRunning()) {
                    probot.setPaused(false);
                    run.setEnabled(false);
                    pause.setEnabled(true);
                } else if (compileProgram()) {
                    run.setEnabled(false);
                    pause.setEnabled(true);
                    stop.setEnabled(true);
                    probot.startProgramThread();
                }
            }
        };
        pause = new AWTButton(ImageIO.read(FileUtils.openFileOrResource("pause_bars.png"))) {
            @Override
            protected void onAction() {
                if (probot.isRunning()) {
                    probot.setPaused(true);
                    run.setEnabled(true);
                    pause.setEnabled(false);
                }
            }
        };
        stop = new AWTButton(ImageIO.read(FileUtils.openFileOrResource("stop_square.png"))) {
            @Override
            protected void onAction() {
                probot.stop();
                run.setEnabled(true);
                pause.setEnabled(false);
                stop.setEnabled(false);
            }
        };
        quit = new AWTButton("QUIT") {
            @Override
            protected void onAction() {
                System.exit(0);
            }
        };
        next = new AWTButton(ImageIO.read(FileUtils.openFileOrResource("forward_arrow.png"))) {
            @Override
            protected void onAction() {
                if (!probot.isRunning())
                    initLevel(frame.getIntProperty("curLevel", 0)+1);
            }
        };
        previous = new AWTButton(ImageIO.read(FileUtils.openFileOrResource("back_arrow.png"))) {
            @Override
            protected void onAction() {
                if (!probot.isRunning())
                    initLevel(frame.getIntProperty("curLevel", 0)-1);
            }
        };

        AWTPanel topButtons = new AWTPanel(previous, currentLevel, next);
        AWTPanel bottomButtons = new AWTPanel(run, pause, stop);
        AWTPanel left = new AWTPanel(new BorderLayout());

        left.add(txtEditor, BorderLayout.CENTER);
        left.add(topButtons, BorderLayout.NORTH);
        left.add(bottomButtons, BorderLayout.SOUTH);
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
        File lbLevelsFile = new File(lbSettingsDir, "levels_backup.txt");

        levels.addAll(Reflector.deserializeFromFile(lbLevelsFile));
        frame.setPropertiesFile(propertiesFile);
        int curLevel = frame.getIntProperty("curLevel", 0);

        initLevel(curLevel);
        probot.start();

        frame.centerToScreen(640, 480);
        txtArea.grabFocus();
    }

    final UIProbot probot = new UIProbot() {

        int [] faces = null;

        @Override
        protected void repaint() {
            AWTProbot.this.repaint();
        }

        @Override
        protected void setProgramLine(int line) {
            String html = getTemplateHTML();
            String htmlFirst = html.substring(0, html.indexOf("<ol>")+4);
            String htmlLast  = html.substring(html.indexOf("</ol>"));
            String htmlBody = "";
            String [] program = getProgram();
            int index = 0;
            for (String cmd : program) {
                if (index++ == line) {
                    htmlBody += "<li class=\"active\">" + cmd + "</li>\n";
                } else {
                    htmlBody += "<li class=\"good\">" + cmd + "</li>\n";
                }
            }
            txtEditor.setText(htmlFirst + htmlBody + htmlLast);
        }

        @Override
        protected void onSuccess() {
            super.onSuccess();
            Utils.waitNoThrow(this, 3000);
            int curLevel = frame.getIntProperty("curLevel", 0);
            int maxLevel = frame.getIntProperty("maxLevel", 0);
            curLevel += 1;
            if (curLevel >= levels.size())
                curLevel = 0;
            maxLevel = Math.max(maxLevel, curLevel);
            frame.setProperty("curLevel", curLevel);
            frame.setProperty("maxLevel", maxLevel);
            clear();
            initLevel(curLevel);
        }

        @Override
        protected void onFailed() {
            Utils.waitNoThrow(this, 3000);
            super.onFailed();
            run.setEnabled(true);
            pause.setEnabled(false);
        }

        @Override
        protected int[] getFaceImageIds(AGraphics g) {
            if (faces == null) {
                faces = new int[]{
                        g.loadImage("guy_smile1.png"),
                        g.loadImage("guy_smile2.png"),
                        g.loadImage("guy_smile3.png")
                };
            }
            return faces;
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

    String getTemplateHTML() {
        try {
            return FileUtils.inputStreamToString(FileUtils.openFileOrResource("pr_template.html"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String [] getProgram() {
        String txt = txtEditor.getText().replaceAll("[\n ]+", " ");
        Pattern li = Pattern.compile("<li[^<]*</li>");
        Matcher m = li.matcher(txt);
        List<String> lines = new ArrayList<>();
        while (m.find()) {
            String group = m.group();
            String cmdStr = group.replaceAll("<[^>]+>", "").trim().toLowerCase();
            if (cmdStr.length() == 0)
                continue;
            lines.add(cmdStr);
        }
        return lines.toArray(new String[lines.size()]);
    }

    boolean compileProgram() {

        String [] program = getProgram();
        boolean success = true;
        String html = getTemplateHTML();

        String htmlFirst = html.substring(0, html.indexOf("<ol>")+4);
        String htmlLast  = html.substring(html.indexOf("</ol>"));
        String htmlList = "";
        probot.clear();
        try {
            int nesting = 0;
            for (String cmdStr : program) {
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
                htmlList += "<li class=\"" + color + "\">" + cmdStr + "</li>";
            }
            if (nesting > 0)
                success = false;
        } finally {
            txtEditor.setText(htmlFirst + htmlList + htmlLast);
        }
        return success;
    }

    void initLevel(int level) {
        int maxLevel = frame.getIntProperty("maxLevel", 0);
        probot.setLevel(level+1, levels.get(level));
        frame.setProperty("curLevel", level);
        probot.start();
        txtEditor.setText(getTemplateHTML());

        previous.setEnabled(level > 0);
        next.setEnabled(level < maxLevel);
        currentLevel.setText("  " + (level+1) + "  ");
        run.setEnabled(true);
        pause.setEnabled(false);
    }


}
