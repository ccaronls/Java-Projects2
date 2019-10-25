package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.probot.*;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public class AWTProbotLevelBuilder extends AWTComponent {

    public static void main(String [] args) throws Exception {
        Utils.DEBUG_ENABLED=true;
        new AWTProbotLevelBuilder();
    }

    final AWTFrame frame;
    final AWTRadioButtonGroup<Type> cellType;
    final AWTButton prev1, next1, prev10, next10;
    final JLabel levelNumLabel = new JLabel();
    final AWTToggleButton lazer0;
    final AWTToggleButton lazer1;
    final AWTToggleButton lazer2;
    final AWTNumberPicker npTurns, npJumps, npLoops;
    final AWTEditText levelLabel;
    final JTextPane info = new JTextPane();

    final Probot probot = new Probot() {
        @Override
        protected float getStrokeWidth() {
            return 5;
        }
    };
    final ArrayList<Level> levels = new ArrayList<>();
    int curLevel = 0;
    final File BACKUP_FILE;

    final String LEVEL_FILE = "levelsFile";

    AWTProbotLevelBuilder() throws Exception {
        final File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        BACKUP_FILE = new File(settings, "levels_backup.txt");
        frame = new AWTFrame() {

            @Override
            protected void onWindowClosing() {
                try {
                    Reflector.serializeToFile(levels, BACKUP_FILE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        {
            if (BACKUP_FILE.isFile()) {
                try {
                    levels.addAll(Reflector.deserializeFromFile(BACKUP_FILE));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (levels.size() == 0) {
                levels.add(new Level());
            }
        }

        setMouseEnabled(true);

        frame.add(this);
        frame.addMenuBarMenu("File", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                switch (ev.getActionCommand()) {
                    case "New": {
                        frame.setProperty(LEVEL_FILE, null);
                        levels.clear();
                        levels.add(new Level());
                        updateAll();
                        break;
                    }
                    case "Open": {
                        File toOpen = frame.showFileOpenChooser("Open Levels File", null);
                        if (toOpen != null) {
                            frame.log.info("Opening '%s'", toOpen);
                            try {
                                List<Level> newLevels = Reflector.deserializeFromFile(toOpen);
                                frame.setProperty(LEVEL_FILE, toOpen.getAbsolutePath());
                                levels.clear();
                                levels.addAll(newLevels);
                                updateAll();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                    case "Save":
                        try {
                            String levelsStr  = frame.getProperties().getProperty(LEVEL_FILE);
                            frame.log.info("Saving to '%s'",levelsStr);
                            if (levelsStr  == null) {
                                File levelsFile = frame.showFileSaveChooser("Choose File to save", null, null);
                                if (levelsFile != null) {
                                    Reflector.serializeToFile(levels, levelsFile);
                                    frame.setProperty(LEVEL_FILE, levelsFile.getAbsolutePath());
                                }
                            } else {
                                Reflector.serializeToFile(levels, new File(levelsStr));
                            }
                            updateAll();
                        } catch (Exception e) {
                            frame.setProperty(LEVEL_FILE, null);
                            e.printStackTrace();
                        }
                        break;
                    case "Save as": {
                        File levelsFile = null;
                        String str = frame.getProperties().getProperty(LEVEL_FILE);
                        if (str != null) {
                            levelsFile = new File(str);
                        }
                        File newFile = frame.showFileSaveChooser("Choose File to save", null, levelsFile);
                        if (newFile != null) {
                           frame.log.info("Saving to '%d'", newFile);
                            try {
                                Reflector.serializeToFile(levels, newFile);
                                frame.setProperty(LEVEL_FILE, newFile.getAbsolutePath());
                                updateAll();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        }, "New", "Open", "Save", "Save as");

        AWTPanel rhs = new AWTPanel(0, 1);
        cellType = new AWTRadioButtonGroup<Type>(rhs) {
            @Override
            protected void onChange(Type extra) {

            }
        };
        for (Type t : Type.values()) {
            cellType.addButton(t.displayName, t);
        }

        frame.add(rhs, BorderLayout.EAST);


        AWTPanel lhs = new AWTPanel();
        lhs.setLayout(new AWTButtonLayout(lhs));
        frame.add(lhs, BorderLayout.WEST);
        info.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                levels.get(curLevel).info = info.getText();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                levels.get(curLevel).info = info.getText();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                levels.get(curLevel).info = info.getText();
            }
        });
        lhs.add(new AWTButton("Clear") {
            @Override
            protected void onAction() {
                grid.init(1, 1, Type.EM);
                getLevel().coins = grid.getGrid();
                AWTProbotLevelBuilder.this.repaint();
            }
        });

        lhs.add(new AWTButton("Reload"){
            @Override
            protected void onAction() {
                if (BACKUP_FILE.isFile()) {
                    try {
                        levels.clear();
                        levels.addAll(Reflector.deserializeFromFile(BACKUP_FILE));
                        updateAll();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        lhs.add(new AWTButton("Insert\nBefore") {
            @Override
            protected void onAction() {
                levels.add(curLevel, new Level());
                probot.setLevel(curLevel, levels.get(curLevel));
                updateAll();
                AWTProbotLevelBuilder.this.repaint();
            }
        });

        lhs.add(npTurns = new AWTNumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Turns").build((int oldValue, int newValue) -> getLevel().numTurns = newValue ));
        lhs.add(npLoops = new AWTNumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Loops").build((int oldValue, int newValue) -> getLevel().numLoops = newValue ));
        lhs.add(npJumps = new AWTNumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Jumps").build((int oldValue, int newValue) -> getLevel().numJumps = newValue ));

        lhs.add(lazer0 = new AWTToggleButton("Lazer 0") {
            @Override
            protected void onToggle(boolean on) {
                getLevel().lazers[0] = on;
                probot.setLazerEnabled(0, on);
                AWTProbotLevelBuilder.this.repaint();
            }
        });
        lhs.add(lazer1 = new AWTToggleButton("Lazer 1") {
            @Override
            protected void onToggle(boolean on) {
                getLevel().lazers[1] = on;
                probot.setLazerEnabled(1, on);
                AWTProbotLevelBuilder.this.repaint();
            }
        });
        lhs.add(lazer2 = new AWTToggleButton("Lazer 2") {
            @Override
            protected void onToggle(boolean on) {
                getLevel().lazers[2] = on;
                probot.setLazerEnabled(2, on);
                AWTProbotLevelBuilder.this.repaint();
            }
        });
        info.setPreferredSize(new Dimension(100, 300));
        lhs.add(new JScrollPane(info));

        prev1 = new AWTButton("<") {
            @Override
            protected void onAction() {
                if (curLevel > 0) {
                    probot.setLevel(--curLevel, levels.get(curLevel));
                    updateAll();
                    AWTProbotLevelBuilder.this.repaint();
                }
            }
        };

        next1 = new AWTButton(">") {
            @Override
            protected void onAction() {
                if (curLevel == levels.size()-1) {
                    levels.add(new Level());
                }
                probot.setLevel(++curLevel, levels.get(curLevel));
                updateAll();
                AWTProbotLevelBuilder.this.repaint();
            }
        };

        prev10 = new AWTButton("<<") {
            @Override
            protected void onAction() {
                if (curLevel > 0) {
                    curLevel = Math.max(0, curLevel-10);
                    probot.setLevel(curLevel, levels.get(curLevel));
                    updateAll();
                    AWTProbotLevelBuilder.this.repaint();
                }
            }
        };

        next10 = new AWTButton(">>") {
            @Override
            protected void onAction() {
                if (curLevel < levels.size()-1) {
                    curLevel = Math.min(curLevel+10, levels.size()-1);
                    probot.setLevel(curLevel, levels.get(curLevel));
                    updateAll();
                    AWTProbotLevelBuilder.this.repaint();
                }
            }
        };

        levelLabel = new AWTEditText("", 32) {
            @Override
            protected void onTextChanged(String newText) {
                getLevel().label = newText;
            }
        };

        AWTPanel top = new AWTPanel(levelLabel, prev10, prev1, levelNumLabel, next1, next10);
        frame.add(top, BorderLayout.NORTH);

        if (!frame.loadFromFile(new File(settings, "probotlevelbuilder.properties")))
            frame.centerToScreen(800, 640);

        grid.setGrid(getLevel().coins);
        updateAll();

    }

    private Level getLevel() {
        return levels.get(curLevel);
    }

    private void updateAll() {
        levelNumLabel.setText("Level " + (curLevel+1) + " of " + levels.size());
        Level level = getLevel();
        grid.setGrid(level.coins);
        lazer0.setSelected(level.lazers[0]);
        lazer1.setSelected(level.lazers[1]);
        lazer2.setSelected(level.lazers[2]);
        prev1.setEnabled(curLevel > 0);
        prev10.setEnabled(curLevel > 0);
        next10.setEnabled(curLevel < levels.size()-1);
        npJumps.setValue(level.numJumps);
        npLoops.setValue(level.numLoops);
        npTurns.setValue(level.numTurns);
        levelLabel.setText(level.label);
        info.setText(level.info);
        String fileName = frame.getStringProperty(LEVEL_FILE, null);
        frame.setTitle(fileName == null ? "<UNNAMED>" : fileName);
    }

    int pickCol = -1;
    int pickRow = -1;

    Grid<Type> grid = new Grid() {
        @Override
        protected Type[][] build(int rows, int cols) {
            return new Type[rows][cols];
        }
    };

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {

        if (levels.size() == 0)
            return;
        // draw a grid

        final int cellDim = Math.min(g.getViewportWidth(), g.getViewportHeight()) / 20;
        final int viewWidth = g.getViewportWidth();
        final int viewHeight = g.getViewportHeight();

        probot.setLevel(curLevel, getLevel());
        probot.draw(g, grid.getCols()*cellDim, grid.getRows()*cellDim);
        g.setColor(GColor.WHITE);

        for (int i=0; i<=cellDim; i++) {
            g.drawLine(0, i*cellDim, viewWidth, i*cellDim);
            g.drawLine(i*cellDim, 0, i*cellDim, viewHeight);
        }

        pickCol = mouseX / cellDim;
        pickRow = mouseY / cellDim;

        //Utils.println("paint mx=" + mouseX + " my=" + mouseY);

        g.setColor(GColor.RED);

        g.drawRect(pickCol * cellDim, pickRow *cellDim, cellDim, cellDim);


    }

    @Override
    protected void onKeyPressed(VKKey key) {
        if (!grid.isValid(pickCol, pickRow))
            return;

        Type t = grid.get(pickRow, pickRow);
        Type [] values = {};

        switch (t) {

            case EM:
            case DD:
                values = Utils.toArray(Type.EM, Type.DD);
                break;
            case SE:
            case SS:
            case SW:
            case SN:
                values = Utils.toArray(Type.SE, Type.SS, Type.SW, Type.SN);
                break;
            case LH0:
            case LV0:
            case LH1:
            case LV1:
            case LH2:
            case LV2:
                values = Utils.toArray(Type.LH0, Type.LV0, Type.LH1, Type.LV1, Type.LH2, Type.LV2);
                break;
            case LB0:
            case LB1:
            case LB2:
            case LB:
                values = Utils.toArray(Type.LB0, Type.LB1, Type.LB2, Type.LB);
                break;
        }

        switch (key) {
            case VK_LEFT:
                t = Utils.decrementValue(t, values);
                break;
            case VK_RIGHT:
                t = Utils.incrementValue(t, values);
                break;
        }

        grid.set(pickRow, pickCol, t);
        repaint();
    }

    @Override
    protected void onClick() {
        if (pickCol < 0 || pickRow < 0) {
            return;
        }

        grid.ensureCapacity(pickRow+1, pickCol+1, Type.EM);
        getLevel().coins = grid.getGrid();

        Type t = this.cellType.getChecked();
        grid.set(pickRow, pickCol, t);

        repaint();
    }

    void drawGuy(AWTGraphics g, int x, int y, int radius, Direction dir) {
        g.drawFilledCircle(x, y, radius);
        g.setColor(GColor.BLACK);
        switch (dir) {
            case Right:
                g.drawLine(x, y, x+radius, y, 4);
                break;
            case Down:
                g.drawLine(x, y, x, y+radius, 4);
                break;
            case Left:
                g.drawLine(x, y, x-radius, y, 4);
                break;
            case Up:
                g.drawLine(x, y, x, y-radius, 4);
                break;
        }
    }
}
