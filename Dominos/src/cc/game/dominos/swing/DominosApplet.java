package cc.game.dominos.swing;

import java.io.File;

import cc.game.dominos.core.*;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZButton;
import cc.lib.swing.EZFrame;
import cc.lib.swing.EZPanel;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DominosApplet extends AWTComponent {

    public static void main(String [] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        new DominosApplet();
    }

    final EZFrame frame;

    DominosApplet() {
        setMouseEnabled(true);
        frame = new EZFrame("Dominos") {
            protected void onWindowClosing() {
                dominos.trySaveToFile(saveFile);
            }

            @Override
            protected void onMenuItemSelected(String menu, String subMenu) {
                switch (subMenu) {
                    case "New Game":
                        showNewGamePopup();
                        break;
                }
            }
        };
        frame.addMenuBarMenu("File", "New Game");
        frame.add(this);
        /*
        try {
            dominos.loadFromFile(saveFile);
        } catch (Exception e) {
            e.printStackTrace();
            dominos.initGame(9, 150,0);
            dominos.setNumPlayers(4);
            dominos.startNewGame();
        }
*/
        if (!frame.loadFromFile(new File("dominos.properties")))
            frame.centerToScreen(800, 600);
//        dominos.startGameThread();
        dominos.initGame(6, 150, 0);
        dominos.startNewGame();
        dominos.getBoard().startShuffleAnimation(dominos.getPool());
    }

    interface OnChoiceListener {
        void choiceMade(int choice);
    }

    JPanel makeRadioGroup(int choice, final OnChoiceListener listener, int ... buttons) {
        EZPanel panel = new EZPanel(new GridLayout(1, 0));
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listener.choiceMade(Integer.parseInt(e.getActionCommand()));
            }
        };
        ButtonGroup group = new ButtonGroup();
        for (int i  :buttons) {
            String s = String.valueOf(i);
            JRadioButton b = new JRadioButton(s);
            b.setSelected(i==choice);
            b.addActionListener(al);
            b.setActionCommand(s);
            group.add(b);
            panel.add(b);
        }
        return panel;
    }

    JPanel makeRadioGroup(int choice, final OnChoiceListener listener, String ... buttons) {
        EZPanel panel = new EZPanel(new GridLayout(1, 0));
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listener.choiceMade(Integer.parseInt(e.getActionCommand()));
            }
        };
        ButtonGroup group = new ButtonGroup();
        int i = 0;
        for (String s  :buttons) {
            JRadioButton b = new JRadioButton(s);
            b.setSelected(i==choice);
            b.addActionListener(al);
            b.setActionCommand(String.valueOf(i++));
            group.add(b);
            panel.add(b);
        }
        return panel;
    }

    int numPlayersChoice = 4;
    int numPipsChoice = 6;
    int maxScoreChoice = 150;
    int difficultyChoice = 0;

    void showNewGamePopup() {
        numPlayersChoice = dominos.getNumPlayers();
        numPipsChoice = dominos.getMaxPips();
        maxScoreChoice = dominos.getMaxScore();
        difficultyChoice = dominos.getDifficulty();

        final JPanel numPlayers = makeRadioGroup(numPlayersChoice, new OnChoiceListener() {
                @Override
                public void choiceMade(int choice) {
                    numPlayersChoice = choice;
                }
            }, 2, 3, 4);
        JPanel numPips = makeRadioGroup(numPipsChoice, new OnChoiceListener() {
                @Override
                public void choiceMade(int choice) {
                    numPipsChoice = choice;
                }
            }, 6, 9, 12);
        JPanel maxPts = makeRadioGroup(maxScoreChoice, new OnChoiceListener() {
                @Override
                public void choiceMade(int choice) {
                    maxScoreChoice = choice;
                }
            }, 150, 200, 250);
        JPanel difficulty = makeRadioGroup(difficultyChoice, new OnChoiceListener() {
                @Override
                public void choiceMade(int choice) {
                    difficultyChoice = choice;
                }
            }, "Easy", "Medium", "Hard");
        EZFrame popup = new EZFrame();
        EZPanel panel = new EZPanel(new GridLayout(0, 1),
                numPlayers,
                numPips,
                maxPts,
                difficulty
                );
        EZPanel buttons = new EZPanel(new FlowLayout(),
                new EZButton("Cancel", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        popup.closePopup(frame);
                    }
                }),
                new EZButton("Start", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dominos.stopGameThread();
                        dominos.initGame(numPipsChoice, maxScoreChoice, difficultyChoice);
                        dominos.setNumPlayers(numPlayersChoice);
                        dominos.startNewGame();
                        dominos.startGameThread();
                        popup.closePopup(frame);
                    }
                }));
        EZPanel root = new EZPanel(new BorderLayout());
        root.add(new JLabel("Game Setup"), BorderLayout.NORTH);
        root.add(panel, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        popup.setContentPane(root);
        popup.showAsPopup(frame);
    }

    final File saveFile = new File("dominos.save");
    final Dominos dominos = new Dominos() {

        @Override
        public void redraw() {
            repaint();
        }

        @Override
        protected void onMenuClicked() {
            showNewGamePopup();
        }
    };

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        dominos.draw(g, mouseX, mouseY);
    }

    @Override
    protected void startDrag(int x, int y) {
        dominos.startDrag();
    }

    @Override
    protected void stopDrag() {
        dominos.stopDrag();
    }

    @Override
    protected void onClick() {
        dominos.onClick();
    }

}
