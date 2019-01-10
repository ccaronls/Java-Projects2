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
import cc.lib.math.Bezier;
import cc.lib.math.CMath;
import cc.lib.math.Matrix3x3;
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
                int die1 = 1+Utils.rand()%6;
                int die2 = 1+Utils.rand()%6;

                @Override
                protected void onStarted() {
                    new Thread() {
                        public void run() {
                            while (!isDone()) {
                                Utils.waitNoThrow(this, delay);
                                delay += 20;
                                die1 = 1 + Utils.rand()%6;
                                die2 = 1 + Utils.rand()%6;
                            }
                        }
                    }.start();
                }

                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    drawDice(g, die1, die2);
                }

                @Override
                public void onDone() {
                    synchronized (monopoly) {
                        monopoly.notify();
                    }
                }
            }.start(), monopoly);
            Utils.waitNoThrow(monopoly, 3000);
            super.onDiceRolled();
        }

        @Override
        protected void onPlayerMove(int playerNum, int numSquares) {
            spriteMap.get(getPlayer(playerNum).getPiece()).animation = new JumpAnimation(playerNum, numSquares).start();
            Utils.waitNoThrow(monopoly, numSquares*600);
            super.onPlayerMove(playerNum, numSquares);
        }

        @Override
        protected void onPlayerDrawsChance(int playerNum, CardActionType chance) {
            frame.showMessageDialog("Chance", chance.getDescription());
            super.onPlayerDrawsChance(playerNum, chance);
        }

        @Override
        protected void onPlayerDrawsCommunityChest(int playerNum, CardActionType commChest) {
            frame.showMessageDialog("Community Chest", commChest.getDescription());
            super.onPlayerDrawsCommunityChest(playerNum, commChest);
        }

        @Override
        protected void onPlayerGotPaid(int playerNum, int amt) {
            spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), amt).start();
            Utils.waitNoThrow(monopoly, 700);
            super.onPlayerGotPaid(playerNum, amt);
        }

        @Override
        protected void onPlayerReceiveMoneyFromAnother(int playerNum, int giverNum, int amt) {
            spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), amt).start();
            spriteMap.get(giverNum).animation = new MoneyAnim(getPlayer(giverNum).getMoney(), -amt).start();
            Utils.waitNoThrow(monopoly, 700);
            super.onPlayerReceiveMoneyFromAnother(playerNum, giverNum, amt);
        }

        @Override
        protected void onPlayerPayMoneyToKitty(int playerNum, int amt) {
            spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start();
            spriteMap.get(Square.FREE_PARKING).animation = new MoneyAnim(monopoly.getKitty(), amt).start();
            super.onPlayerPayMoneyToKitty(playerNum, amt);
        }

        @Override
        protected void onPlayerGoesToJail(int playerNum) {
            addAnimation(new JailedAnim(playerInfoWidth, playerInfoHeight).start(), getPlayer(playerNum));
            Utils.waitNoThrow(monopoly, 5000);
            super.onPlayerGoesToJail(playerNum);
        }

        @Override
        protected void onPlayerOutOfJail(int playerNum) {
            addAnimation(new JailedAnim(playerInfoWidth, playerInfoHeight).startReverse(), getPlayer(playerNum));
            Utils.waitNoThrow(monopoly, 5000);
            super.onPlayerOutOfJail(playerNum);
        }

        @Override
        protected void onPlayerPaysRent(int playerNum, int renterNum, int amt) {
            spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start();
            spriteMap.get(renterNum).animation = new MoneyAnim(getPlayer(renterNum).getMoney(), amt).start();
            Utils.waitNoThrow(monopoly, 700);
            super.onPlayerPaysRent(playerNum, renterNum, amt);
        }

        @Override
        protected void onPlayerMortgaged(int playerNum, Square property, int amt) {
            spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), amt).start();
            Utils.waitNoThrow(monopoly, 700);
            super.onPlayerMortgaged(playerNum, property, amt);
        }

        @Override
        protected void onPlayerUnMortgaged(int playerNum, Square property, int amt) {
            spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start();
            Utils.waitNoThrow(monopoly, 700);
            super.onPlayerUnMortgaged(playerNum, property, amt);
        }

        @Override
        protected void onPlayerPurchaseProperty(int playerNum, Square property) {
            spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), -property.getPrice()).start();
            super.onPlayerPurchaseProperty(playerNum, property);
        }

        @Override
        protected void onPlayerBoughtHouse(int playerNum, Square property, int amt) {
            spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start();
            super.onPlayerBoughtHouse(playerNum, property, amt);
        }

        @Override
        protected void onPlayerBoughtHotel(int playerNum, Square property, int amt) {
            spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start();
            super.onPlayerBoughtHotel(playerNum, property, amt);
        }
    };
    File saveFile = new File("monopoly.save");
    static AWTMonopoly instance;
    Map<Object, LinkedList<AAnimation>> animations = new HashMap<>();
    //LinkedList<AAnimation> animations = new LinkedList<>();

    void addAnimation(AAnimation a, Object where) {
        synchronized (animations) {
            LinkedList<AAnimation> list = animations.get(where);
            if (list == null) {
                list = new LinkedList<>();
                animations.put(where, list);
            }
            list.add(a);
        }
        repaint();
    }

    abstract class Sprite {

        AAnimation<Sprite> animation = null;
        Matrix3x3 M = Matrix3x3.newIdentity();
        GColor color = GColor.BLACK;
        int data1, data2;

        final void animateAndDraw(AGraphics g, float w, float h) {
            if (isAnimating()) {
                animation.update(this);
            }
            g.pushMatrix();
            g.multMatrix(M);
            GColor c = g.getColor();
            g.setColor(color);
            draw(g, w, h);
            g.setColor(c);
            g.popMatrix();
            if (!isAnimating())
                repaint();
        }

        final boolean isAnimating() {
            return animation != null && !animation.isDone();
        }

        abstract void draw(AGraphics g, float w, float h);
    }

    final Map<Object, Sprite> spriteMap = new HashMap<>();

    void initSprites() {
        // the pieces on the board can be animated
        for (final Piece p : Piece.values()) {
            spriteMap.put(p, new Sprite() {
                @Override
                void draw(AGraphics g, float w, float h) {
                    g.drawImage(pieceMap.get(p), 0, 0, w, h);
                }
            });
        }

        // a players current money can be animated
        for (int i=0; i<Monopoly.MAX_PLAYERS; i++) {
            final int pNum = i;
            spriteMap.put(i, new Sprite() {
                @Override
                void draw(AGraphics g, float w, float h) {
                    final Player p = monopoly.getPlayer(pNum);
                    int money = p.getMoney();
                    if (isAnimating()) {
                        money = data1;
                        float textHeight = g.getTextHeight();
                        String amt = data2 < 0 ? String.valueOf(data2) : "+" + data2;
                        animation.getPosition();
                        Vector2D delta = new Vector2D(0, textHeight * CMath.signOf(data2) * animation.getPosition());
                        g.pushMatrix();
                        g.translate(delta);
                        g.setColor((data2 > 0 ? GColor.GREEN : GColor.RED).withAlpha(1f-animation.getPosition()));
                        g.drawJustifiedString(0, 0, Justify.RIGHT, Justify.CENTER, amt);
                        g.popMatrix();
                    }
                    g.setColor(color);
                    g.drawJustifiedString(0, 0, Justify.RIGHT, Justify.CENTER, "$"+money);
                }
            });
        }

        // The kitty can be animated
        spriteMap.put(Square.FREE_PARKING, new Sprite() {
            @Override
            void draw(AGraphics g, float w, float h) {
                if (monopoly.getKitty() > 0) {
                    //GRectangle r = board.getSqaureBounds(Square.FREE_PARKING);
                    //Vector2D cntr = r.getCenter();
                    //g.setColor(GColor.GREEN);
                    GDimension dim = g.drawWrapString(0, 0, w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + monopoly.getKitty());
                    g.setColor(GColor.TRANSLUSCENT_BLACK);
                    g.drawFilledRect(- dim.width/2 - 5, - dim.height/2 - 5, dim.width + 10, dim.height + 10);
                    g.setColor(color);
                    g.drawWrapString(0, 0, w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + monopoly.getKitty());
                }

            }
        });
    }

/*
    class JumpAnimation extends AAnimation<AGraphics> {

        Square start;
        Piece p;
        int jumps;
        int playerNum;
        Bezier curve;

        JumpAnimation(int playerNum, int jumps) {
            super(500);
            this.playerNum = playerNum;
            this.start = monopoly.getPlayer(playerNum).getSquare();
            this.jumps = jumps;
            this.p = monopoly.getPlayer(playerNum).getPiece();
            curve = new Bezier();
            init();
        }

        void init() {
            Player p = monopoly.getPlayer(playerNum);
            GRectangle r0 = board.getPiecePlacement(playerNum, start);
            start = Utils.incrementValue(start, Square.values());
            GRectangle r1 = board.getPiecePlacement(playerNum, start);
            curve.reset();
            curve.addPoint(r0.x, r0.y);
            float dx = r1.x-r0.x;
            float dy = r1.y-r0.y;
            curve.addPoint(r0.x + dx/3, r0.y + dx/6);
            curve.addPoint(r0.x + dx*2/3, r0.y + dx/6);
            curve.addPoint(r1.x, r1.y);
            jumps--;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            g.pushMatrix();
            g.translate(curve.getPointAt(position));
            float dim = board.getPieceDimension();
            g.drawImage(pieceMap.get(p), 0, 0, dim, dim);
            g.popMatrix();
            repaint();
        }

        @Override
        protected void onDone() {
            if (jumps > 0) {
                init();
                start();
            } else {
                synchronized (monopoly) {
                    monopoly.notifyAll();
                }
            }
        }
    }*/

    class JailedAnim extends AAnimation<AGraphics> {
        final float width;
        final float height;
        JailedAnim(float width, float height) {
            super(2000);
            this.width = width;
            this.height = height;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            float barWidth = 5;
            float barSpacing = 20;
            float x = 5;
            while (x < width) {
                g.setColor(GColor.BLACK);
                g.drawFilledRect(x, 0, barWidth, height*position);
                x += width;
                x += barSpacing;
            }
        }

        @Override
        protected void onDone() {
            synchronized (monopoly) {
                monopoly.notify();
            }
        }
    }

    class MoneyAnim extends AAnimation<Sprite> {

        final int delta;
        final float startMoney;

        MoneyAnim(int startMoney, int amt) {
            super(1500);
            this.delta = amt;
            this.startMoney = startMoney;
        }

        @Override
        protected void draw(Sprite g, float position, float dt) {
            g.data1 = Math.round(startMoney + position * delta);
            g.data2 = delta;
        }
    }

    class JumpAnimation extends AAnimation<Sprite> {

        Square start;
        int jumps;
        int playerNum;
        Bezier curve;

        JumpAnimation(int playerNum, int jumps) {
            super(500);
            this.playerNum = playerNum;
            this.start = monopoly.getPlayer(playerNum).getSquare();
            this.jumps = jumps;
            curve = new Bezier();
            init();
        }

        void init() {
            GRectangle r0 = board.getPiecePlacement(playerNum, start);
            start = Utils.incrementValue(start, Square.values());
            GRectangle r1 = board.getPiecePlacement(playerNum, start);
            curve.reset();
            curve.addPoint(r0.x, r0.y);
            float dx = r1.x-r0.x;
            float dy = r1.y-r0.y;
            curve.addPoint(r0.x + dx/3, r0.y + dx/6);
            curve.addPoint(r0.x + dx*2/3, r0.y + dx/6);
            curve.addPoint(r1.x, r1.y);
            jumps--;
        }

        @Override
        protected void draw(Sprite sp, float position, float dt) {
            sp.M.setTranslate(curve.getPointAt(position));
        }

        @Override
        protected void onDone() {
            if (jumps > 0) {
                init();
                start();
            } else {
                synchronized (monopoly) {
                    monopoly.notify();
                }
            }
        }
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

        initSprites();
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
                    if (instance.monopoly.canCancel())
                        instance.monopoly.cancel();
                    else
                        instance.stopGameThread();
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
                    if (instance.monopoly.canCancel())
                        instance.monopoly.cancel();
                    else
                        instance.stopGameThread();
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
    Board board;
    float playerInfoWidth, playerInfoHeight;

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        W = g.getViewportWidth();
        H = g.getViewportHeight();
        DIM = Math.min(W, H);
        board = new Board(DIM);
        g.setTextHeight(16);
        g.setTextStyles(AGraphics.TextStyle.BOLD);
        if (W > H) {
            drawLandscape(g, mouseX, mouseY);
        } else {
            drawPortrait(g, mouseX, mouseY);
        }
    }

    private void drawPlayerInfo(APGraphics g, int playerNum, float w, float h) {
        playerInfoWidth = w;
        Player p = monopoly.getPlayer(playerNum);
        int pcId = pieceMap.get(p.getPiece());
        float dim = w/2;
        playerInfoHeight = dim;
        float border = 5;
        g.drawImage(pcId, 0, 0, dim, dim);
        if (monopoly.getCurrentPlayerNum()==playerNum) {
            g.setColor(GColor.CYAN);
            g.drawRect(0, 0, dim, dim, 2);
        }
        Sprite sp = spriteMap.get(playerNum);
        sp.M.setTranslate(w, dim/2);
        sp.color = GColor.BLACK;
        sp.animateAndDraw(g, 0, 0);

        if (p.isEliminated()) {
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRect(0, 0, w, h);
            g.setColor(GColor.RED);
            g.drawWrapString(w/2, h/2, w, Justify.CENTER, Justify.CENTER, "ELIMINATED");
        } else {
            float sy = dim;
            GColor bkColor = GColor.TRANSPARENT;
            GColor txtColor = GColor.BLACK;
            if (p.isInJail()) {
                // TODO: draw 'BARS'
                sy += drawWrapStringOnBackground(g, 0, sy, w, "IN JAIL", bkColor, txtColor, border);
            } else {
                if (p.getSquare().canPurchase()) {
                    bkColor = p.getSquare().getColor();
                    txtColor = chooseContrastColor(bkColor);
                }
                sy += drawWrapStringOnBackground(g, 0, sy, w, Utils.getPrettyString(p.getSquare().name()), bkColor, txtColor, border);
            }
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
        drawAnimations(g, p);
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
            drawPlayerInfo(g, i, w, H-DIM);
            g.popMatrix();
        }
        drawAnimations(g, monopoly);
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
            drawPlayerInfo(g, i, w, H/2);
            g.popMatrix();
        }
        drawAnimations(g, monopoly);
    }

    private void drawAnimations(AGraphics g, Object where) {
        synchronized (animations) {
            LinkedList<AAnimation> list = animations.get(where);
            if (list == null)
                return;
            Iterator<AAnimation> it = list.iterator();
            while (it.hasNext()) {
                AAnimation<AGraphics> a = it.next();
                g.pushMatrix();
                a.update(g);
                g.popMatrix();
                if (a.isDone())
                    it.remove();
            }
            if (list.size() > 0)
                repaint();
        }
    }

    final static float HOUSE_RADIUS = 1;

    // draw house with center at 0,0.
    public void drawHouse(AGraphics g) {
        GColor color = g.getColor();
        GColor roofL = color;
        GColor roofR = color.darkened(0.2f);
        GColor front = color.darkened(0.5f);

        Vector2D [] pts = {
                new Vector2D(-1, -0.8f),
                new Vector2D(-1, .6f),
                new Vector2D(0, -1),
                new Vector2D(0, .2f),
                new Vector2D(1, -0.8f),
                new Vector2D(1, .6f),
                new Vector2D(-0.8f, 1),
                new Vector2D(.8f, 1),
                new Vector2D(-.8f, 0),
                new Vector2D(.8f, 0),
        };

        g.setColor(front);
        g.begin();
        g.vertexArray(pts[8], pts[9], pts[6], pts[7]);
        g.drawQuadStrip();
        g.begin();
        g.setColor(roofL);
        g.vertexArray(pts[0], pts[1], pts[2], pts[3]);
        g.drawQuadStrip();
        g.begin();
        g.setColor(roofR);
        g.vertexArray(pts[2], pts[3], pts[4], pts[5]);
        g.drawQuadStrip();
        g.setColor(color); // restore color
    }

    public void drawBoard(AGraphics g) {
        g.drawImage(ids[Images.board.ordinal()], 0, 0, DIM, DIM);
        {
            Sprite kitty = spriteMap.get(Square.FREE_PARKING);
            GRectangle r = board.getSqaureBounds(Square.FREE_PARKING);
            kitty.M.setTranslate(r.getCenter());
            kitty.color = GColor.GREEN;
            kitty.animateAndDraw(g, r.w, r.h);
        }
/*
        if (monopoly.getKitty() > 0) {
            GRectangle r = board.getSqaureBounds(Square.FREE_PARKING);
            Vector2D cntr = r.getCenter();
            g.setColor(GColor.GREEN);
            GDimension dim = g.drawWrapString(cntr.X(), cntr.Y(), r.w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + monopoly.getKitty());
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRect(cntr.X() - dim.width/2 - 5, cntr.Y() - dim.height/2 - 5, dim.width + 10, dim.height + 10);
            g.setColor(GColor.GREEN);
            g.drawWrapString(cntr.X(), cntr.Y(), r.w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + monopoly.getKitty());
        }*/
        for (int i=0; i<monopoly.getNumPlayers(); i++) {
            Player p = monopoly.getPlayer(i);
            if (p.isEliminated())
                continue;
            int pcId = pieceMap.get(p.getPiece());
            float targetDim = board.getPieceDimension();
            for (Card c : p.getCards()) {
                if (c.getProperty() == null)
                    continue;
                GRectangle r = board.getSqaureBounds(c.getProperty());
                float houseScale = Math.min(r.w, r.h)/10;
                switch (board.getsQuarePosition(c.getProperty())) {
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
            GRectangle r = board.getPiecePlacement(i, p.getSquare());
            Sprite sp = spriteMap.get(p.getPiece());
            sp.M.setTranslate(r.x, r.y);
            sp.animateAndDraw(g, r.w, r.h);
            //g.drawImage(pcId, r);
        }
        drawAnimations(g, board);
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
            g.translate(-HOUSE_RADIUS*(num-1), 0);
            for (int i=0; i<num; i++) {
                drawHouse(g);
                g.translate(HOUSE_RADIUS*2, 0);
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