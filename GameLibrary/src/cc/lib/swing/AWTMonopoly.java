package cc.lib.swing;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
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
import cc.lib.monopoly.CardActionType;
import cc.lib.monopoly.Monopoly;
import cc.lib.monopoly.MoveType;
import cc.lib.monopoly.Piece;
import cc.lib.monopoly.Player;
import cc.lib.monopoly.Square;
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
    final Monopoly monopoly = new Monopoly() {
        @Override
        protected void onDiceRolled() {
            addAnimation(new AAnimation<AGraphics>(2000) {

                long delay = 10;

                @Override
                protected void draw(AGraphics g, float position, float dt) {

                    drawDice(g, 1+Utils.rand()%6, 1+Utils.rand()%6);

                    new Thread() {
                        public void run() {
                            Utils.waitNoThrow(this, delay);
                            delay += 20;
                            repaint();
                        }
                    }.start();
                }

                @Override
                public void onDone() {
                    synchronized (monopoly) {
                        monopoly.notifyAll();
                    }
                }
            });
            Utils.waitNoThrow(monopoly, 3000);
            super.onDiceRolled();
        }

        @Override
        protected void onPlayerMove(int playerNum, int numSquares) {
            super.onPlayerMove(playerNum, numSquares);
        }

        @Override
        protected void onPlayerDrawsChance(int playerNum, CardActionType chance) {
            super.onPlayerDrawsChance(playerNum, chance);
        }

        @Override
        protected void onPlayerDrawsCommunityChest(int playerNum, CardActionType commChest) {
            super.onPlayerDrawsCommunityChest(playerNum, commChest);
        }

        @Override
        protected void onPlayerGotPaid(int playerNum, int amt) {
            super.onPlayerGotPaid(playerNum, amt);
        }

        @Override
        protected void onPlayerReceiveMoneyFromAnother(int playerNum, int giverNum, int amt) {
            super.onPlayerReceiveMoneyFromAnother(playerNum, giverNum, amt);
        }

        @Override
        protected void onPlayerPayMoneyToKitty(int playerNum, int amt) {
            super.onPlayerPayMoneyToKitty(playerNum, amt);
        }

        @Override
        protected void onPlayerGoesToJail(int playerNum) {
            super.onPlayerGoesToJail(playerNum);
        }

        @Override
        protected void onPlayerOutOfJail(int playerNum) {
            super.onPlayerOutOfJail(playerNum);
        }

        @Override
        protected void onPlayerPaysRent(int playerNum, int renterPlayer, int amt) {
            super.onPlayerPaysRent(playerNum, renterPlayer, amt);
        }

        @Override
        protected void onPlayerMortgaged(int playerNum, Square property, int amt) {
            super.onPlayerMortgaged(playerNum, property, amt);
        }

        @Override
        protected void onPlayerUnMortgaged(int playerNum, Square property, int amt) {
            super.onPlayerUnMortgaged(playerNum, property, amt);
        }

        @Override
        protected void onPlayerPurchaseProperty(int playerNum, Square property) {
            super.onPlayerPurchaseProperty(playerNum, property);
        }

        @Override
        protected void onPlayerBoughtHouse(int playerNum, Square property, int amt) {
            super.onPlayerBoughtHouse(playerNum, property, amt);
        }

        @Override
        protected void onPlayerBoughtHotel(int playerNum, Square property, int amt) {
            super.onPlayerBoughtHotel(playerNum, property, amt);
        }
    };
    File saveFile = new File("monopoly.save");
    static AWTMonopoly instance;
    LinkedList<AAnimation> animations = new LinkedList<>();

    void addAnimation(AAnimation a) {
        synchronized (animations) {
            animations.add(a);
        }
        a.start();
        repaint();
    }

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
                                                stopGameThread();
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

        if (monopoly.tryLoadFromFile(saveFile))
            startGameThread();

    }

    boolean gameRunning = false;
    boolean gameStopped = true;

    synchronized void startGameThread() {
        if (gameRunning)
            return;
        gameRunning = true;
        gameStopped = false;
        new Thread() {
            public void run() {
                while (gameRunning && monopoly.getWinner() < 0) {
                    monopoly.runGame();
                    repaint();
                }
                gameRunning = false;
                gameStopped = true;
            }
        }.start();

    }

    void stopGameThread() {
        if (gameStopped)
            return;
        gameRunning = false;
        synchronized (monopoly) {
            monopoly.notifyAll();
        }
        while (!gameStopped) {
            Utils.waitNoThrow(this, 50);
        }
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
                    synchronized (instance.monopoly) {
                        instance.monopoly.notify();
                    }
                }

                @Override
                public void cancelled() {
                    instance.monopoly.cancel();
                    synchronized (instance.monopoly) {
                        instance.monopoly.notify();
                    }
                }
            }, "Choose Card " + getPiece(), Utils.toStringArray(cards));
            Utils.waitNoThrow(instance.monopoly, -1);
            return result[0];
        }

        @Override
        public MoveType chooseMove(final List<MoveType> options) {
            final MoveType [] result = new MoveType[1];
            instance.frame.showListChooserDialog(new EZFrame.OnListItemChoosen() {
                @Override
                public void itemChoose(int index) {
                    result[0] = options.get(index);
                    synchronized (instance.monopoly) {
                        instance.monopoly.notify();
                    }
                }

                @Override
                public void cancelled() {
                    instance.monopoly.cancel();
                    synchronized (instance.monopoly) {
                        instance.monopoly.notify();
                    }
                }
            }, "Choose Move " + getPiece(), Utils.toStringArray(options.toArray(new MoveType[options.size()])));
            Utils.waitNoThrow(instance.monopoly, -1);
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
        g.setTextHeight(16);
        g.setTextStyles(AGraphics.TextStyle.BOLD);
        if (W > H) {
            drawLandscape(g, mouseX, mouseY);
        } else {
            drawPortrait(g, mouseX, mouseY);
        }
    }

    private void drawPlayerInfo(APGraphics g, Player p, float w, float h) {
        int pcId = pieceMap.get(p.getPiece());
        float dim = w/2;
        float border = 5;
        g.drawImage(pcId, 0, 0, dim, dim);
        g.setColor(GColor.BLACK);
        g.drawJustifiedString(w, dim/2, Justify.RIGHT, Justify.CENTER, "$"+p.getMoney());
        if (p.isEliminated()) {
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRect(0, 0, w, h);
            g.setColor(GColor.RED);
            g.drawWrapString(w/2, h/2, w, Justify.CENTER, Justify.CENTER, "ELIMINATED");
        } else {
            float sy = dim;
            GColor bkColor = GColor.TRANSPARENT;
            GColor txtColor = GColor.BLACK;
            if (p.getSquare().canPurchase()) {
                bkColor = p.getSquare().getColor();
                txtColor = chooseContrastColor(bkColor);
            }
            sy += drawWrapStringOnBackground(g, 0, sy, w, Utils.getPrettyString(p.getSquare().name()), bkColor, txtColor, border);
            int num;
            if ((num = p.getNumGetOutOfJailFreeCards()) > 0) {
                g.setColor(GColor.BLACK);
                sy += drawWrapStringOnBackground(g, 0, sy, w,"Get out of Jail FREE x " + num, GColor.WHITE, GColor.BLACK, border);
            }
            if ((num = p.getNumRailroads()) > 0) {
                g.setColor(GColor.BLACK);
                sy += drawWrapStringOnBackground(g, 0, sy, w,"Railroads x " + num, GColor.BLACK, GColor.WHITE, border);
            }
            if ((num = p.getNumUtilities()) > 0) {
                g.setColor(GColor.BLACK);
                sy += drawWrapStringOnBackground(g, 0, sy, w,"Utilities x " + num, GColor.WHITE, GColor.BLACK, border);
            }
            Map<GColor, List<Card>> map = p.getPropertySets();
            for (Map.Entry<GColor, List<Card>> e : map.entrySet()) {
                String txt = String.format("%d of %d", e.getValue().size(), e.getValue().get(0).getProperty().getNumForSet());
                bkColor = e.getKey();
                txtColor = chooseContrastColor(bkColor);
                sy += drawWrapStringOnBackground(g, 0, sy, w,txt, bkColor, txtColor, border);
            }
        }
    }

    private float drawWrapStringOnBackground(AGraphics g, float x, float y, float width, String txt, GColor bkColor, GColor txtColor, float border) {
        GDimension d = g.drawWrapString(x+border, y+border, width-border*2, txt);
        g.setColor(bkColor);
        g.drawFilledRect(x, y, width, d.height+2*border);
        g.setColor(txtColor);
        g.drawWrapString(x+border, y+border, width-border*2, txt);
        return d.height + 2*border;
    }

    private GColor chooseContrastColor(GColor c) {
        float amt = c.getRed()+c.getBlue()+c.getGreen();
        amt *= c.getAlpha();
        if (amt > 1.5f)
            return GColor.BLACK;
        return GColor.WHITE;
    }

    private void drawPortrait(APGraphics g, int mx, int my) {
        drawBoard(g);
        drawDice(g, monopoly.getDie1(), monopoly.getDie2());
        float w = W/3;
        for (int i=0; i<monopoly.getNumPlayers(); i++) {
            g.pushMatrix();
            g.translate(w*i, DIM);
            drawPlayerInfo(g, monopoly.getPlayer(i), w, H-DIM);
            g.popMatrix();
        }
    }

    private void drawLandscape(APGraphics g, int mx, int my) {

        g.pushMatrix();
        g.translate(W/2-DIM/2, H/2-DIM/2);
        drawBoard(g);
        g.popMatrix();
        // draw dice in center of board
        drawDice(g, monopoly.getDie1(), monopoly.getDie2());
        float w = (W-DIM)/2;
        for (int i=0; i<monopoly.getNumPlayers(); i++) {
            g.pushMatrix();
            switch (i) {
                case 0: break;
                case 1: g.translate(W-w, 0); break;
                case 2: g.translate(0, H/2); break;
                case 3: g.translate(W-w, H/2); break;
            }
            drawPlayerInfo(g, monopoly.getPlayer(i), w, H/2);
            g.popMatrix();
        }

        synchronized (animations) {
            Iterator<AAnimation> it = animations.iterator();
            while (it.hasNext()) {
                AAnimation<AGraphics> a = it.next();
                a.update(g);
                if (a.isDone())
                    it.remove();
            }
        }
    }

    // draw house with center at 0,0.
    public void drawHouse(AGraphics g) {
        GColor front = g.getColor();
        GColor roof = front.lightened(0.5f);
        GColor side = front.darkened(0.2f);

        Vector2D [] pts = {
                new Vector2D(-1, -2),
                new Vector2D(1, -2),
                new Vector2D(-2, 0),
                new Vector2D(.5f, 0),
                new Vector2D(-2, 2),
                new Vector2D(.5f, 2),
                new Vector2D(2, 1),
                new Vector2D(2, -1),
        };

        g.setColor(front);
        g.begin();
        g.vertexArray(pts[2], pts[3], pts[4], pts[5]);
        g.drawQuadStrip();
        g.begin();
        g.setColor(side);
        g.vertexArray(pts[1], pts[3], pts[5], pts[6], pts[7]);
        g.drawTriangleFan();
        g.begin();
        g.setColor(roof);
        g.vertexArray(pts[0], pts[1], pts[2], pts[3]);
        g.drawQuadStrip();
        g.setColor(front); // restore color
    }

    public void drawBoard(AGraphics g) {
        g.drawImage(ids[Images.board.ordinal()], 0, 0, DIM, DIM);
        Board b = new Board(DIM);
        if (monopoly.getKitty() > 0) {
            GRectangle r = b.getSqaureBounds(Square.FREE_PARKING);
            Vector2D cntr = r.getCenter();
            g.setColor(GColor.GREEN);
            GDimension dim = g.drawWrapString(cntr.X(), cntr.Y(), r.w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + monopoly.getKitty());
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRect(cntr.X() - dim.width/2 - 5, cntr.Y() - dim.height/2 - 5, dim.width + 10, dim.height + 10);
            g.setColor(GColor.GREEN);
            g.drawWrapString(cntr.X(), cntr.Y(), r.w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + monopoly.getKitty());
        }
        for (int i=0; i<monopoly.getNumPlayers(); i++) {
            Player p = monopoly.getPlayer(i);
            if (p.isEliminated())
                continue;
            int pcId = pieceMap.get(p.getPiece());
            float targetDim = b.getPieceDimension();
            for (Card c : p.getCards()) {
                if (c.getProperty() == null)
                    continue;
                GRectangle r = b.getSqaureBounds(c.getProperty());
                float houseScale = Math.min(r.w, r.h)/10;
                switch (b.getsQuarePosition(c.getProperty())) {
                    case TOP:
                        g.drawImage(pcId, r.x+r.w/2-targetDim/2, r.y+r.h, targetDim, targetDim);
                        drawHouses(g, new Vector2D(r.x+r.w/2, r.y+r.h-houseScale), 0, houseScale, c.getHouses());
                        break;
                    case RIGHT:
                        g.drawImage(pcId, r.x-targetDim, r.y+r.h/2-targetDim/2, targetDim, targetDim);
                        drawHouses(g, new Vector2D(r.x+houseScale, r.y+r.h/2), 270, houseScale, c.getHouses());
                        break;
                    case BOTTOM:
                        g.drawImage(pcId, r.x+r.w/2-targetDim/2, r.y-targetDim, targetDim, targetDim);
                        drawHouses(g, new Vector2D(r.x+r.w/2, r.y+houseScale), 0, houseScale, c.getHouses());
                        break;
                    case LEFT:
                        g.drawImage(pcId, r.x+r.w, r.y+r.h/2-targetDim/2, targetDim, targetDim);
                        drawHouses(g, new Vector2D(r.x+r.w-houseScale, r.y+r.h/2), 90, houseScale, c.getHouses());
                        break;
                }
                if (c.isMortgaged()) {
                    g.setColor(GColor.TRANSLUSCENT_BLACK);
                    g.drawFilledRect(r);
                    g.setColor(GColor.RED);
                    Vector2D v = r.getCenter();
                    g.drawWrapString(v.getX(), v.getY(), r.w, Justify.CENTER, Justify.CENTER,
                            "MORTGAGED");
                }
            }
            // draw player piece of the board
            GRectangle r = b.getPiecePlacement(i, p.getSquare());
            g.drawImage(pcId, r);
        }

    }

    private void drawHouses(AGraphics g, Vector2D cntr, float angle, float scale, int num) {
        g.pushMatrix();
        g.translate(cntr);
        g.rotate(angle);
        if (num == 5) {
            g.scale(scale * 1.5f);
            g.setColor(GColor.RED);
            drawHouse(g);
        } else {
            g.setColor(GColor.GREEN);
            g.scale(scale);
            g.translate(-1.25f*(num-1), 0);
            for (int i=0; i<num; i++) {
                drawHouse(g);
                g.translate(2.5f, 0);
            }
        }
        g.popMatrix();
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