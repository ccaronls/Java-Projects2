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
    AWTButton prev, next;
    final JLabel levelLabel = new JLabel();
    EZToggleButton lazer0;
    EZToggleButton lazer1;
    EZToggleButton lazer2;

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
                    Reflector.serializeToFile(levels, levelsFile);
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
            cellType.addButton(t.name(), t);
        }

        frame.add(rhs, BorderLayout.EAST);

        EZPanel lhs = new EZPanel();
        lhs.setLayout(new ButtonLayout(lhs));
        frame.add(lhs, BorderLayout.WEST);

        lhs.add(new AWTButton("Clear") {
            @Override
            protected void onAction() {
                grid.init(1, 1, Probot.Type.EM);
            }
        });

        lhs.add(new NumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Turns").build(new NumberPicker.Listener() {
            @Override
            public void onValueChanged(int oldValue, int newValue) {
                getLevel().numTurns = newValue;
            }
        }));


        prev = new AWTButton("<<") {
            @Override
            protected void onAction() {
                if (curLevel > 0) {
                    probot.setLevel(--curLevel, levels.get(curLevel));
                    updateAll();
                    repaint();
                }
            }
        };

        next = new AWTButton(">>") {
            @Override
            protected void onAction() {
                if (curLevel == levels.size()-1) {
                    levels.add(new Probot.Level());
                }
                probot.setLevel(++curLevel, levels.get(curLevel));
                updateAll();
                repaint();
            }
        };

        lazer0 = new EZToggleButton("Lazer 0") {
            @Override
            protected void onToggle(boolean on) {
                getLevel().lazers[0] = on;
                repaint();
            }
        };
        lazer1 = new EZToggleButton("Lazer 1") {
            @Override
            protected void onToggle(boolean on) {
                getLevel().lazers[1] = on;
                repaint();
            }
        };
        lazer2 = new EZToggleButton("Lazer 2") {
            @Override
            protected void onToggle(boolean on) {
                getLevel().lazers[2] = on;
                repaint();
            }
        };

        EZPanel top = new EZPanel(prev, levelLabel, next, lazer0, lazer1, lazer2);
        frame.add(top, BorderLayout.NORTH);
        updateAll();

        if (!frame.loadFromFile(new File("probotlevelbuilder.properties")))
            frame.centerToScreen(640, 640);
    }

    private Probot.Level getLevel() {
        return levels.get(curLevel);
    }

    private void updateAll() {
        levelLabel.setText("Level " + (curLevel+1) + " of " + levels.size());
        Probot.Level level = getLevel();
        lazer0.setSelected(level.lazers[0]);
        lazer1.setSelected(level.lazers[1]);
        lazer2.setSelected(level.lazers[2]);
        prev.setEnabled(curLevel > 0);
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
    protected void onClick() {
        if (pickCol < 0 || pickRow < 0) {
            return;
        }

        grid.ensureCapacity(pickCol+1, pickRow+1, Probot.Type.EM);
        getLevel().coins = grid.getGrid();

        Probot.Type t = this.cellType.getChecked();
        if (t == Probot.Type.EM || (t=grid.get(pickRow, pickCol)) == Probot.Type.EM) {
            grid.set(pickRow, pickCol, t = this.cellType.getChecked());
        } else {
            grid.set(pickRow, pickCol, t = Utils.incrementEnum(t, Probot.Type.values()));
        }

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
