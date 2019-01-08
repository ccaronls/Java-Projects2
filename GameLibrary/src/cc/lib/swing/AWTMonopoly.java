package cc.lib.swing;

import java.io.File;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.monopoly.Card;
import cc.lib.monopoly.Monopoly;
import cc.lib.monopoly.MoveType;
import cc.lib.monopoly.Piece;
import cc.lib.monopoly.Player;

public class AWTMonopoly extends AWTComponent {
    private final static Logger log = LoggerFactory.getLogger(AWTMonopoly.class);

    public static void main(String[] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        instance = new AWTMonopoly();
    }

    final EZFrame frame;
    final Monopoly monopoly = new Monopoly();
    File saveFile = new File("monopoly.save");
    static AWTMonopoly instance;

    AWTMonopoly() {
        setMouseEnabled(true);
        setPadding(5);
        frame = new EZFrame("Monopoly") {
            @Override
            protected void onWindowClosing() {
                if (gameRunning)
                    monopoly.trySaveToFile(saveFile);
            }

            @Override
            protected void onMenuItemSelected(String menu, String subMenu) {
                switch (menu) {
                    case "File":
                        switch (subMenu) {
                            case "New Game":
                                frame.showListChooserDialog(new OnListItemChoosen() {
                                    @Override
                                    public void itemChoose(final int item) {
                                        frame.showListChooserDialog(new OnListItemChoosen() {
                                            @Override
                                            public void itemChoose(int pc) {
                                                initPlayers(item + 2, Piece.values()[pc]);
                                                startGameThread();
                                            }
                                        }, "Choose Piece", Utils.toStringArray(Piece.values()));

                                    }
                                }, "How Many Players?", "2", "3", "4");
                                break;
                            case "Resume":
                                if (monopoly.tryLoadFromFile(saveFile)) {
                                    startGameThread();
                                }
                                break;
                        }
                }
            }
        };

        frame.addMenuBarMenu("File", "New Game", "Resume");
        frame.add(this);

        if (!frame.loadFromFile(new File("monopoly.properties")))
            frame.centerToScreen(800, 600);

    }

    boolean gameRunning = false;

    synchronized void startGameThread() {
        if (gameRunning)
            return;
        gameRunning = true;
        new Thread() {
            public void run() {
                while (gameRunning && monopoly.getWinner() < 0) {
                    monopoly.runGame();
                    repaint();
                }
                gameRunning = false;
            }
        }.start();

    }

    void initPlayers(int num, Piece pc) {
        monopoly.clear();
        User user = new User();
        user.setPiece(pc);
        monopoly.addPlayer(user);
        for (int i=1; i<num; i++)
            monopoly.addPlayer(new Player());
    }


    public static class User extends Player {
        @Override
        public Card chooseCard(List<Card> cards) {
            return super.chooseCard(cards);
        }

        @Override
        public MoveType chooseMove(List<MoveType> options) {

            return super.chooseMove(options);
        }
    }

    int numImagesLoaded = 0;

    enum Images {
        board, car, dog, iron, ship, shoe, thimble, tophat, wheelbarrow
    }

    int [] ids = new int[Images.values().length];

    @Override
    protected void init(AWTGraphics g) {
        for (int i=0; i<ids.length; i++) {
            ids[i] = g.loadImage("images/" + Images.values()[i].name() + ".png");
            numImagesLoaded++;
        }
    }

    @Override
    protected float getInitProgress() {
        return (float)numImagesLoaded / ids.length;
    }

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        final int W = g.getViewportWidth();
        final int H = g.getViewportHeight();

        final int DIM = Math.min(W, H);

        g.drawImage(ids[Images.board.ordinal()], W/2-DIM/2, H/2-DIM/2, DIM, DIM);
        for (int i=0; i<monopoly.getNumPlayers(); i++) {

        }
    }
}