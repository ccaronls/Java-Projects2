package cc.lib.swing;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Vector2D;
import cc.lib.monopoly.Board;
import cc.lib.monopoly.Card;
import cc.lib.monopoly.Monopoly;
import cc.lib.monopoly.MoveType;
import cc.lib.monopoly.Piece;
import cc.lib.monopoly.Player;
import cc.lib.utils.Reflector;

public class AWTMonopoly extends AWTComponent {
    private final static Logger log = LoggerFactory.getLogger(AWTMonopoly.class);

    public static void main(String[] args) {
        Reflector.registerClass(User.class);
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

                                            @Override
                                            public void cancelled() {}
                                        }, "Choose Piece", Utils.toStringArray(Piece.values()));

                                    }
                                    @Override
                                    public void cancelled() {}
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
        public Card chooseCard(final List<Card> cards) {
            final Card [] result = new Card[1];
            instance.frame.showListChooserDialog(new EZFrame.OnListItemChoosen() {
                @Override
                public void itemChoose(int index) {
                    result[0] = cards.get(index);
                    synchronized (User.this) {
                        User.this.notify();
                    }
                }

                @Override
                public void cancelled() {
                    synchronized (User.this) {
                        User.this.notify();
                    }
                }
            }, "Choose Card " + getPiece(), Utils.toStringArray(cards));
            Utils.waitNoThrow(User.this, -1);
            return result[0];
        }

        @Override
        public MoveType chooseMove(final List<MoveType> options) {
            final MoveType [] result = new MoveType[1];
            instance.frame.showListChooserDialog(new EZFrame.OnListItemChoosen() {
                @Override
                public void itemChoose(int index) {
                    result[0] = options.get(index);
                    synchronized (User.this) {
                        User.this.notify();
                    }
                }

                @Override
                public void cancelled() {
                    synchronized (User.this) {
                        User.this.notify();
                    }
                }
            }, "Choose Move " + getPiece(), Utils.toStringArray(options.toArray(new MoveType[options.size()])));
            Utils.waitNoThrow(User.this, -1);
            return result[0];
        }
    }

    int numImagesLoaded = 0;

    enum Images {
        board, car, dog, iron, ship, shoe, thimble, tophat, wheelbarrow;
    }

    int [] ids = new int[Images.values().length];
    Map<Piece, Integer> pieceMap = new HashMap<>();

    @Override
    protected void init(AWTGraphics g) {
        for (int i=0; i<ids.length; i++) {
            ids[i] = g.loadImage("images/" + Images.values()[i].name() + ".png");
            numImagesLoaded++;
        }
        pieceMap.put(Piece.BOAT, Images.ship.ordinal());
        pieceMap.put(Piece.CAR, Images.car.ordinal());
        pieceMap.put(Piece.DOG, Images.dog.ordinal());
        pieceMap.put(Piece.HAT, Images.tophat.ordinal());
        pieceMap.put(Piece.IRON, Images.iron.ordinal());
        pieceMap.put(Piece.SHOE, Images.shoe.ordinal());
        pieceMap.put(Piece.THIMBLE, Images.thimble.ordinal());
        pieceMap.put(Piece.WHEELBARROW, Images.wheelbarrow.ordinal());
    }

    @Override
    protected float getInitProgress() {
        return (float)numImagesLoaded / ids.length;
    }


    int W, H, DIM;

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        W = g.getViewportWidth();
        H = g.getViewportHeight();
        DIM = Math.min(W, H);

        g.drawImage(ids[Images.board.ordinal()], W/2-DIM/2, H/2-DIM/2, DIM, DIM);
        Board b = new Board(DIM);

        /*
        g.pushMatrix();
        g.translate(W/2-DIM/2, H/2-DIM/2);
        g.setTextHeight(8);
        g.setTextStyles(AGraphics.TextStyle.BOLD);
        for (Square sq : Square.values()) {
            GRectangle rect = b.getSqaureBounds(sq);
            g.setColor(GColor.YELLOW);
            g.drawRect(rect, 3);
            g.setColor(GColor.RED);
            g.drawString(sq.name(), rect.getCenter().X(), rect.getCenter().Y());
        }
        g.popMatrix();
*/
        // draw dice in center of board
        drawDice(g, monopoly.getDie1(), monopoly.getDie2());

        for (int i=0; i<monopoly.getNumPlayers(); i++) {
            Player p = monopoly.getPlayer(i);
            float sx=0, sy=0;
            Justify justify = Justify.LEFT;
            int pcId = pieceMap.get(p.getPiece());
            float targetHeight = b.getPieceDimension();
            float targetWidth = targetHeight;
            switch (i) {
                case 0:
                    g.drawImage(pcId, W/2-DIM/2-targetWidth, 0, targetWidth, targetHeight);
                    break;
                case 1:
                    g.drawImage(pcId, W/2+DIM/2, 0, targetWidth, targetHeight);
                    sx = W;
                    justify = Justify.RIGHT;
                    break;
                case 2:
                    g.drawImage(pcId, W/2-DIM/2-targetWidth, H/2, targetWidth, targetHeight);
                    sy = H/2;
                    break;
                case 3:
                    g.drawImage(pcId, W/2+DIM/2, H/2, targetWidth, targetHeight);
                    sy = H/2;
                    sx = W;
                    justify = Justify.RIGHT;
                    break;
            }
            String txt = "$" + p.getMoney() + "\n" + Utils.getPrettyString(p.getSquare().name());
            if (p.isInJail()) {
                txt += "\nJAILED";
            }
            int border = 5;
            GDimension d = g.drawJustifiedString(sx, sy, justify, txt);
            sy += d.height+border;
            Map<GColor, List<Card>> sets = p.getPropertySets();
            for (Map.Entry<GColor, List<Card>> e : sets.entrySet()) {
                txt = String.format("%d of %d", e.getValue().size(), e.getValue().get(0).getProperty().getNumForSet());
                d = g.drawJustifiedStringOnBackground(sx, sy, justify, Justify.TOP, txt, e.getKey(), 3);
                sy += d.height;
            }
            // Draw the players piece of the board
            for (Card c : p.getCards()) {
                if (c.isGetOutOfJail()) {
                    txt = "Get out\nof Jail";
                    g.setColor(GColor.BLACK);
                    d = g.drawJustifiedStringOnBackground(sx, sy, justify, Justify.TOP, txt, GColor.WHITE, 3);
                    sy += d.height;
                }
                if (c.getProperty() == null)
                    continue;
                g.pushMatrix();
                g.translate(W/2-DIM/2, H/2-DIM/2);
                GRectangle r = b.getSqaureBounds(c.getProperty());
                Vector2D cntr = b.getInnerEdge(c.getProperty());
                Justify vert = Justify.CENTER;
                Justify horz = Justify.CENTER;
                switch (c.getProperty().ordinal()/Monopoly.NUM_SQUARES) {
                    case 0: // bottom
                        vert = Justify.BOTTOM; break;
                    case 1: // left
                        horz = Justify.LEFT; break;
                    case 2: // top
                        vert = Justify.TOP; break;
                    case 3: // right
                        horz = Justify.RIGHT; break;
                }
                g.setTextHeight(14);
                g.setColor(GColor.BLUE);
                g.drawJustifiedStringOnBackground(cntr.X(), cntr.Y(), horz, vert, p.getPiece().name(), GColor.TRANSLUSCENT_BLACK, 5);
                if (c.isMortgaged()) {
                    g.setColor(GColor.TRANSLUSCENT_BLACK);
                    g.drawFilledRect(r);
                    g.setColor(GColor.RED);
                    g.setTextHeight(14);
                    Vector2D v = r.getCenter();
                    g.drawJustifiedString(v.getX(), v.getY(), Justify.CENTER, Justify.CENTER,
                            "MORTGAGED");
                }
                g.popMatrix();
            }
            // draw player piece of the board
            g.pushMatrix();
            g.translate(W/2-DIM/2, H/2-DIM/2);
            GRectangle r = b.getPiecePlacement(i, p.getSquare());
            g.drawImage(pcId, r);
            g.popMatrix();

        }
    }

    public void drawDice(AGraphics g, int die1, int die2) {
        int dieDim = DIM / 10;
        int padding = dieDim/4;
        if (die1 > 0 && die2 > 0) {
            g.pushMatrix();
            g.translate(W/2-padding/2-dieDim, H/4);
            drawDie(g, dieDim, GColor.WHITE, GColor.BLACK, die1);
            g.popMatrix();
            g.pushMatrix();
            g.translate(W/2+padding/2, H/4);
            drawDie(g, dieDim, GColor.WHITE, GColor.BLACK, die2);
            g.popMatrix();
        }
    }

    public void drawDie(AGraphics g, float dim, GColor dieColor, GColor dotColor, int numDots) {
        g.setColor(dieColor);
        float arc = dim/4;
        g.drawFilledRoundedRect(0, 0, dim, dim, arc);
        g.setColor(dotColor);
        float dd2 = dim/2;
        float dd4 = dim/4;
        float dd34 = (dim*3)/4;
        float dotSize = dim/8;
        float oldDotSize = g.setPointSize(dotSize);
        g.begin();
        switch (numDots) {
            case 1:
                g.vertex(dd2, dd2);
                break;
            case 2:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                break;
            case 3:
                g.vertex(dd4, dd4);
                g.vertex(dd2, dd2);
                g.vertex(dd34, dd34);
                break;
            case 4:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                break;
            case 5:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                g.vertex(dd2, dd2);
                break;
            case 6:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                g.vertex(dd4, dd2);
                g.vertex(dd34, dd2);
                break;
            default:
                assert(false);// && "Invalid die");
                break;
        }
        g.drawPoints();
        g.setPointSize(oldDotSize);
    }

}