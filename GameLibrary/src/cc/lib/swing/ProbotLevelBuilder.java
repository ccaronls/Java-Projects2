package cc.lib.swing;

import java.io.File;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.probot.Probot;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

import javax.swing.*;
import java.awt.*;

import java.util.ArrayList;

public class ProbotLevelBuilder extends AWTComponent {

    public static void main(String [] args) {
        Utils.DEBUG_ENABLED=true;
        new ProbotLevelBuilder();
    }

    EZFrame frame;
    AWTRadioButtonGroup<Probot.Type> cellType;
    AWTButton prev1, next1, prev10, next10;
    final JLabel levelNumLabel = new JLabel();
    EZToggleButton lazer0;
    EZToggleButton lazer1;
    EZToggleButton lazer2;
    NumberPicker npTurns, npJumps, npLoops;
    AWTEditText levelLabel;

    File levelsFile = new File("Robots/assets/levels.txt");
    Probot probot = new Probot() {
        @Override
        protected float getStrokeWidth() {
            return 5;
        }
    };
    ArrayList<Probot.Level> levels = new ArrayList<>();
    int curLevel = 0;

    ProbotLevelBuilder() {

        try {
            if (!levelsFile.isFile()) {
                levelsFile.createNewFile();
            } else {
                levels = Reflector.deserializeFromFile(levelsFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (levels.size() == 0) {
            levels.add(new Probot.Level());
        }

        grid.setGrid(getLevel().coins);

        frame = new EZFrame() {

            @Override
            protected void onWindowClosing() {
                try {
                    Reflector.serializeToFile(levels, new File("levels_backup.txt"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        setMouseEnabled(true);

        frame.add(this);

        EZPanel rhs = new EZPanel(0, 1);
        cellType = new AWTRadioButtonGroup<Probot.Type>(rhs) {
            @Override
            protected void onChange(Probot.Type extra) {

            }
        };
        for (Probot.Type t : Probot.Type.values()) {
            cellType.addButton(t.displayName, t);
        }

        frame.add(rhs, BorderLayout.EAST);

        EZPanel lhs = new EZPanel();
        lhs.setLayout(new ButtonLayout(lhs));
        frame.add(lhs, BorderLayout.WEST);

        lhs.add(new AWTButton("Clear") {
            @Override
            protected void onAction() {
                grid.init(1, 1, Probot.Type.EM);
                getLevel().coins = grid.getGrid();
                ProbotLevelBuilder.this.repaint();
            }
        });
        lhs.add(new AWTButton("Save") {
            @Override
            protected void onAction() {
                try {
                    Reflector.serializeToFile(levels, levelsFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        lhs.add(new AWTButton("Insert\nBefore") {
            @Override
            protected void onAction() {
                levels.add(curLevel, new Probot.Level());
                probot.setLevel(curLevel, levels.get(curLevel));
                updateAll();
                ProbotLevelBuilder.this.repaint();
            }
        });

        lhs.add(npTurns = new NumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Turns").build(new NumberPicker.Listener() {
            @Override
            public void onValueChanged(int oldValue, int newValue) {
                getLevel().numTurns = newValue;
            }
        }));

        lhs.add(npLoops = new NumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Loops").build(new NumberPicker.Listener() {
            @Override
            public void onValueChanged(int oldValue, int newValue) {
                getLevel().numLoops = newValue;
            }
        }));
        lhs.add(npJumps = new NumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Jumps").build(new NumberPicker.Listener() {
            @Override
            public void onValueChanged(int oldValue, int newValue) {
                getLevel().numJumps = newValue;
            }
        }));

        lhs.add(lazer0 = new EZToggleButton("Lazer 0") {
            @Override
            protected void onToggle(boolean on) {
                getLevel().lazers[0] = on;
                probot.setLazerEnabled(0, on);
                ProbotLevelBuilder.this.repaint();
            }
        });
        lhs.add(lazer1 = new EZToggleButton("Lazer 1") {
            @Override
            protected void onToggle(boolean on) {
                getLevel().lazers[1] = on;
                probot.setLazerEnabled(1, on);
                ProbotLevelBuilder.this.repaint();
            }
        });
        lhs.add(lazer2 = new EZToggleButton("Lazer 2") {
            @Override
            protected void onToggle(boolean on) {
                getLevel().lazers[2] = on;
                probot.setLazerEnabled(2, on);
                ProbotLevelBuilder.this.repaint();
            }
        });

        prev1 = new AWTButton("<") {
            @Override
            protected void onAction() {
                if (curLevel > 0) {
                    probot.setLevel(--curLevel, levels.get(curLevel));
                    updateAll();
                    ProbotLevelBuilder.this.repaint();
                }
            }
        };

        next1 = new AWTButton(">") {
            @Override
            protected void onAction() {
                if (curLevel == levels.size()-1) {
                    levels.add(new Probot.Level());
                }
                probot.setLevel(++curLevel, levels.get(curLevel));
                updateAll();
                ProbotLevelBuilder.this.repaint();
            }
        };

        prev10 = new AWTButton("<<") {
            @Override
            protected void onAction() {
                if (curLevel > 0) {
                    curLevel = Math.max(0, curLevel-10);
                    probot.setLevel(curLevel, levels.get(curLevel));
                    updateAll();
                    ProbotLevelBuilder.this.repaint();
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
                    ProbotLevelBuilder.this.repaint();
                }
            }
        };

        levelLabel = new AWTEditText("", 32) {
            @Override
            protected void onTextChanged(String newText) {
                getLevel().label = newText;
            }
        };

        EZPanel top = new EZPanel(levelLabel, prev10, prev1, levelNumLabel, next1, next10);
        frame.add(top, BorderLayout.NORTH);
        updateAll();

        if (!frame.loadFromFile(new File("probotlevelbuilder.properties")))
            frame.centerToScreen(800, 640);
    }

    private Probot.Level getLevel() {
        return levels.get(curLevel);
    }

    private void updateAll() {
        levelNumLabel.setText("Level " + (curLevel+1) + " of " + levels.size());
        Probot.Level level = getLevel();
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
    }

    int pickCol = -1;
    int pickRow = -1;

    Grid<Probot.Type> grid = new Grid() {
        @Override
        protected Probot.Type[][] build(int rows, int cols) {
            return new Probot.Type[rows][cols];
        }
    };

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {

        // draw a grid

        final int cellDim = Math.min(g.getViewportWidth(), g.getViewportHeight()) / 20;
        final int viewWidth = g.getViewportWidth();
        final int viewHeight = g.getViewportHeight();

        probot.init(getLevel());
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
        if (pickCol < 0 || pickRow < 0)
            return;

        Probot.Type t = grid.get(pickCol, pickRow);
        Probot.Type [] values = {};

        switch (t) {

            case EM:
            case DD:
                values = Utils.toArray(Probot.Type.EM, Probot.Type.DD);
                break;
            case SE:
            case SS:
            case SW:
            case SN:
                values = Utils.toArray(Probot.Type.SE, Probot.Type.SS, Probot.Type.SW, Probot.Type.SN);
                break;
            case LH0:
            case LV0:
            case LH1:
            case LV1:
            case LH2:
            case LV2:
                values = Utils.toArray(Probot.Type.LH0, Probot.Type.LV0, Probot.Type.LH1, Probot.Type.LV1, Probot.Type.LH2, Probot.Type.LV2);
                break;
            case LB0:
            case LB1:
            case LB2:
            case LB:
                values = Utils.toArray(Probot.Type.LB0, Probot.Type.LB1, Probot.Type.LB2, Probot.Type.LB);
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

        grid.ensureCapacity(pickRow+1, pickCol+1, Probot.Type.EM);
        getLevel().coins = grid.getGrid();

        Probot.Type t = this.cellType.getChecked();
        grid.set(pickRow, pickCol, t);

        switch (t) {
            case SE:
            case SN:
            case SW:
            case SS:
                // make sure only 1 instance of a start point
                for (int i=0; i<grid.getRows(); i++) {
                    for (int ii=0; ii<grid.getCols(); ii++) {
                        if (i==pickRow && ii==pickCol)
                            continue;
                        switch (grid.get(i, ii)) {
                            case SE:
                            case SN:
                            case SW:
                            case SS:
                                grid.set(i, ii, Probot.Type.EM);
                        }
                    }
                }
        }


        repaint();
    }

    void drawGuy(AWTGraphics g, int x, int y, int radius, Probot.Direction dir) {
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
