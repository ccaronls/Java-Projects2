package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
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

    final JEditorPane txtEditor = new JEditorPane();
    //final JTextPane txtEditor = new JTextPane();
    final AWTFrame frame;
    final JButton run, pause, stop, clear, quit, next, previous, help;
    final JLabel currentLevelNum = new AWTLabel("-", 1, 14, true);
    final JLabel currentLevelTitle = new AWTLabel("-", 1, 16, true);
    final List<Level> levels = new ArrayList<>();

    AWTProbot() throws Exception {
        final File settingsDir = FileUtils.getOrCreateSettingsDirectory(getClass());
        final File lbSettingsDir = FileUtils.getOrCreateSettingsDirectory(AWTProbotLevelBuilder.class);
        final File propertiesFile = new File(settingsDir, "awtprobot.properties");

        setMouseEnabled(false);

        txtEditor.setEditable(true);
        txtEditor.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 0), 5));

        txtEditor.setContentType("text/html");
        JScrollPane scrollPane = new JScrollPane(txtEditor);

        InputMap iMap = txtEditor.getInputMap();
        ActionMap aMap = txtEditor.getActionMap();
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        aMap.put("enter", new HTMLEditorKit.InsertHTMLTextAction("Bullets", "<li></li>",HTML.Tag.OL,HTML.Tag.LI) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                // get the position of the cursor
                String [] lines = getProgram(true);
                int cursor = txtEditor.getCaretPosition();
                int lineNum = 0;
                for (int c = cursor; lineNum<lines.length; ) {
                    c -= Math.max(1, lines[lineNum].length());
                    if (c <= 0)
                        break;
                    lineNum++;
                }

                super.actionPerformed(ae);
                try {
                    FileUtils.stringToFile(txtEditor.getText(), new File("/tmp/x.html"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // *****************************************************************************************
                // Annoying Hack: for some reason the editor adds 2 list items, so remove the extra one made
                // *****************************************************************************************

                String html = txtEditor.getText();
                System.out.println("Cursor is on lineNum: " + lineNum);
                //cursor -= lines.length; // position includes newlines which we strip off
                System.out.println("cursor=" + cursor);
                System.out.println("program=" + Arrays.toString(lines));
                // convert the cursor pos inrendered coords to a char in the html string.
                // For example, a cursor pos of 0 maps to Everything up to the end of the first <li>
                html = html.replaceAll("[\n\t ]+", " ");
                int left = html.lastIndexOf("<li>")+4;
                int right = html.lastIndexOf("</li>")+5;
                System.out.println(html);
                System.out.println(Utils.getRepeatingChars(' ', left-1) + "^" + Utils.getRepeatingChars(' ', right-left-1) + "^");

                html = html.substring(0, left) + html.substring(right);
                //txtEditor.setText(html);
            }
        });

        HTMLDocument doc = (HTMLDocument)txtEditor.getDocument();
        HTMLEditorKit kit = (HTMLEditorKit)txtEditor.getEditorKit();

        kit.setDefaultCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        clear = new AWTButton(ImageIO.read(FileUtils.openFileOrResource("clear_x.png"))) {
            @Override
            protected void onAction() {
                txtEditor.setText(getTemplateHTML());
                txtEditor.grabFocus();
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
        help = new AWTButton("HELP") {
            @Override
            protected void onAction() {
                try {
                    frame.showMessageDialogWithHTMLContent("Commands", FileUtils.inputStreamToString(FileUtils.openFileOrResource("pr_help.html")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        AWTPanel topButtons = new AWTPanel(previous, currentLevelNum, next, help);
        AWTPanel bottomButtons = new AWTPanel(run, pause, stop, clear);
        AWTPanel left = new AWTPanel(new BorderLayout());

        left.add(scrollPane, BorderLayout.CENTER);
        left.add(topButtons, BorderLayout.NORTH);
        left.add(bottomButtons, BorderLayout.SOUTH);
        frame = new AWTFrame("Probot") {
            @Override
            protected void onWindowResized(int w, int h) {
                super.onWindowResized(w, h);
                //txtEditor.grabFocus();
                txtEditor.grabFocus();
            }

            @Override
            protected void onWindowClosing() {
                try {
                    FileUtils.stringToFile(txtEditor.getText(), new File(settingsDir, "program.html"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        AWTPanel cntr = new AWTPanel(new BorderLayout());
        cntr.addTop(currentLevelTitle);
        cntr.add(this, BorderLayout.CENTER);
        frame.add(cntr, BorderLayout.CENTER);
        frame.add(left, BorderLayout.WEST);
        File lbLevelsFile = new File(lbSettingsDir, "levels_backup.txt");

        levels.addAll(Reflector.deserializeFromFile(lbLevelsFile));
        frame.setPropertiesFile(propertiesFile);
        int curLevel = frame.getIntProperty("curLevel", 0);

        initLevel(curLevel);
        String html = null;
        try {
            html = FileUtils.fileToString(new File(settingsDir, "program.html"));
        } catch (FileNotFoundException e) {
            // dont care
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (html == null)
            html = getTemplateHTML();
        txtEditor.setText(html);
        frame.centerToScreen(640, 480);
        txtEditor.grabFocus();
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
            String [] program = getProgram(false);
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
            initLevel(curLevel+1);
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
            return FileUtils.inputStreamToString(FileUtils.openFileOrResource("pr_template.html")).replaceAll(">[\n\t ]+<", "><");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String [] getProgram(boolean includeEmptyLines) {
        String txt = txtEditor.getText().replaceAll("[\n ]+", " ");
        Pattern li = Pattern.compile("<li[^<]*</li>");
        Matcher m = li.matcher(txt);
        List<String> lines = new ArrayList<>();
        while (m.find()) {
            String group = m.group();
            String cmdStr = group.replaceAll("<[^>]+>", "").trim().toLowerCase();
            if (!includeEmptyLines && cmdStr.length() == 0)
                continue;
            lines.add(cmdStr);
        }
        return lines.toArray(new String[lines.size()]);
    }

    boolean compileProgram() {

        String [] program = getProgram(false);
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
                    case "loopend":
                        if (nesting > 0) {
                            probot.add(new Command(CommandType.LoopEnd, 1));
                            break;
                        }
                    case "loop":
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

    void initLevel(int levelNum) {
        if (levelNum > levels.size()-1)
            levelNum = 0;
        Level level = levels.get(levelNum);
        int maxLevel = frame.getIntProperty("maxLevel", 0);
        if (maxLevel < levelNum) {
            maxLevel = levelNum;
            if (!Utils.isEmpty(level.info)) {
                frame.showMessageDialog(level.label, level.info);
            }
        }
        currentLevelTitle.setText(level.label);
        probot.setLevel(levelNum +1, levels.get(levelNum));
        frame.setProperty("curLevel", levelNum);
        probot.clear();
        probot.start();
        txtEditor.setText(getTemplateHTML());
        previous.setEnabled(levelNum > 0);
        next.setEnabled(levelNum < maxLevel);
        currentLevelNum.setText("  " + (levelNum +1) + "  ");
        run.setEnabled(true);
        pause.setEnabled(false);
    }


}
