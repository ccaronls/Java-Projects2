package cc.lib.monopoly;

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
import cc.lib.math.Bezier;
import cc.lib.math.CMath;
import cc.lib.math.Matrix3x3;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public abstract class UIMonopoly extends Monopoly {

    private final int MONEY_PAUSE = 1000;
    private final Object LOCK = this;
    private boolean gameRunning = false;
    private boolean gameStopped = true;

    protected abstract void repaint();

    protected abstract int getImageId(Piece p);

    protected abstract int getBoardImageId();

    @Override
    protected void onDiceRolled() {
        addAnimation(LOCK, new AAnimation<AGraphics>(2000) {

            long delay = 10;
            int die1 = 1+ Utils.rand()%6;
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
                synchronized (LOCK) {
                    LOCK.notify();
                }
            }
        }.start());
        Utils.waitNoThrow(LOCK, 3000);
        super.onDiceRolled();
    }

    @Override
    protected void onPlayerMove(int playerNum, int numSquares) {
        setSpriteAnim(getPlayer(playerNum).getPiece(), new JumpAnimation(playerNum, numSquares).start());
        Utils.waitNoThrow(LOCK, numSquares*600);
        super.onPlayerMove(playerNum, numSquares);
    }

    private void showMessage(final String title, final String txt) {
        addAnimation(board, new AAnimation<AGraphics>(4000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                final float border = 5;
                float width = Math.max(DIM/3, g.getTextWidth(txt) + border*2);
                String [] lines = g.generateWrappedLines(txt, width-border*2);
                //GDimension dim = //g.drawWrapStringOnBackground(DIM/2, DIM/2, DIM/3, Justify.CENTER, Justify.CENTER, txt, GColor.WHITE, 5);
                float txtHgt = g.getTextHeight();
                float height = txtHgt * lines.length;
                height += txtHgt + 4 * border;
                float x = DIM/2 - width/2;
                float y = DIM/2 - height/2;
                g.setColor(GColor.BLACK);
                g.drawFilledRect(x-border, y-border, width+border*2, height+border*2);
                g.setColor(GColor.WHITE);
                g.drawFilledRect(x, y, width, height);
                g.setColor(GColor.BLACK);
                y += border;
                x = DIM/2; // center
                g.drawJustifiedString(x, y, Justify.CENTER, Justify.TOP, title);
                y += txtHgt;
                y += border;
                g.drawLine(x-width/2, y, x+width/2, y, 4);
                y += border;
                for (String line : lines) {
                    g.drawJustifiedString(x, y, Justify.CENTER, Justify.TOP, line);
                    y += txtHgt;
                }
            }
        });
    }

    @Override
    protected void onPlayerDrawsChance(int playerNum, final CardActionType chance) {
        showMessage("Chance", chance.getDescription());
        super.onPlayerDrawsChance(playerNum, chance);
    }

    @Override
    protected void onPlayerDrawsCommunityChest(int playerNum, CardActionType commChest) {
        showMessage("Community Chest", commChest.getDescription());
        super.onPlayerDrawsCommunityChest(playerNum, commChest);
    }

    @Override
    protected void onPlayerGotPaid(int playerNum, int amt) {
        setSpriteAnim(playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), amt).start());
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerGotPaid(playerNum, amt);
    }

    @Override
    protected void onPlayerReceiveMoneyFromAnother(int playerNum, int giverNum, int amt) {
        setSpriteAnim(playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), amt).start());
        setSpriteAnim(giverNum, new MoneyAnim(getPlayer(giverNum).getMoney(), -amt).start());
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerReceiveMoneyFromAnother(playerNum, giverNum, amt);
    }

    @Override
    protected void onPlayerPayMoneyToKitty(int playerNum, int amt) {
        setSpriteAnim(playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start());
        setSpriteAnim(Square.FREE_PARKING, new MoneyAnim(getKitty(), amt).start());
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerPayMoneyToKitty(playerNum, amt);
    }

    @Override
    protected void onPlayerGoesToJail(int playerNum) {
        final Player p = getPlayer(playerNum);
        final GRectangle start = board.getPiecePlacement(playerNum, p.getSquare());
        final GRectangle end   = board.getPiecePlacementJail(playerNum);
        setSpriteAnim(getPlayer(playerNum).getPiece(), new AAnimation<Sprite>(2000) {

            final Bezier curve = new Bezier();

            @Override
            protected void onStarted() {
                MutableVector2D s = start.getTopLeft();
                MutableVector2D e = end.getTopLeft();
                curve.addPoint(s);
                MutableVector2D dv = e.min(s);
                MutableVector2D n = dv.norm();
                n.scaleEq(.3f);
                curve.addPoint(s.add(dv.scaledBy(.33f)).add(n));
                curve.addPoint(s.add(dv.scaledBy(.66f)).add(n));
                curve.addPoint(e);
            }

            @Override
            protected void draw(Sprite s, float position, float dt) {
                s.M.setTranslate(curve.getPointAt(position));
            }

            @Override
            protected void onDone() {
                synchronized (LOCK) {
                    LOCK.notify();
                }
            }
        }.start());
        Utils.waitNoThrow(LOCK, 5000);
        addAnimation(p, new JailedAnim(playerInfoWidth, playerInfoHeight).start());
        super.onPlayerGoesToJail(playerNum);
    }

    @Override
    protected void onPlayerOutOfJail(int playerNum) {
        addAnimation(getPlayer(playerNum), new JailedAnim(playerInfoWidth, playerInfoHeight).startReverse());
        super.onPlayerOutOfJail(playerNum);
    }

    @Override
    protected void onPlayerPaysRent(int playerNum, int renterNum, int amt) {
        spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start();
        spriteMap.get(renterNum).animation = new MoneyAnim(getPlayer(renterNum).getMoney(), amt).start();
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerPaysRent(playerNum, renterNum, amt);
    }

    @Override
    protected void onPlayerMortgaged(int playerNum, Square property, int amt) {
        spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), amt).start();
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerMortgaged(playerNum, property, amt);
    }

    @Override
    protected void onPlayerUnMortgaged(int playerNum, Square property, int amt) {
        spriteMap.get(playerNum).animation = new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start();
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
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

    final Map<Object, LinkedList<AAnimation>> animations = new HashMap<>();

    void addAnimation(Object where, AAnimation a) {
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

    void stopAnimations() {
        synchronized (animations) {
            animations.clear();
        }
        for (Sprite sprite : spriteMap.values()) {
            if (sprite.animation != null) {
                sprite.animation.stop();
                sprite.animation = null;
            }
        }
    }

    void setSpriteAnim(Object sprite, AAnimation<Sprite> anim) {
        Sprite s = spriteMap.get(sprite);
        s.animation = anim;
        repaint();
    }

    /**
     * Sprites solve the problem of rendering something that can be animated or static
     */
    abstract class Sprite {

        AAnimation<Sprite> animation = null;
        final Matrix3x3 M = Matrix3x3.newIdentity();
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
            if (isAnimating())
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
                    g.drawImage(getImageId(p), 0, 0, w, h);
                }
            });
        }

        // a players current money can be animated
        for (int i=0; i<Monopoly.MAX_PLAYERS; i++) {
            final int pNum = i;
            spriteMap.put(i, new Sprite() {
                @Override
                void draw(AGraphics g, float w, float h) {
                    final Player p = getPlayer(pNum);
                    int money = p.getMoney();
                    if (isAnimating()) {
                        money = data1;
                        float textHeight = g.getTextHeight();
                        String amt = data2 < 0 ? String.valueOf(data2) : "+" + data2;
                        animation.getPosition();
                        int dir = CMath.signOf(-data2);
                        Vector2D delta = new Vector2D(0, textHeight * animation.getPosition() * dir);
                        g.pushMatrix();
                        g.translate(0, textHeight*dir);
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
                if (getKitty() > 0 || isAnimating()) {
                    //GRectangle r = board.getSqaureBounds(Square.FREE_PARKING);
                    //Vector2D cntr = r.getCenter();
                    //g.setColor(GColor.GREEN);
                    GDimension dim = g.drawWrapString(0, 0, w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + getKitty());
                    g.setColor(GColor.TRANSLUSCENT_BLACK);
                    g.drawFilledRect(- dim.width/2 - 5, - dim.height/2 - 5, dim.width + 10, dim.height + 10);
                    int money = getKitty();
                    if (isAnimating()) {
                        money = data1;
                        float textHeight = g.getTextHeight();
                        String amt = data2 < 0 ? String.valueOf(data2) : "+" + data2;
                        animation.getPosition();
                        int dir = CMath.signOf(-data2);
                        Vector2D delta = new Vector2D(0, textHeight * animation.getPosition() * dir);
                        g.pushMatrix();
                        g.translate(0, textHeight*dir);
                        g.translate(delta);
                        g.setColor((data2 > 0 ? GColor.GREEN : GColor.RED).withAlpha(1f-animation.getPosition()));
                        g.drawJustifiedString(0, 0, Justify.RIGHT, Justify.CENTER, amt);
                        g.popMatrix();
                    }
                    g.setColor(color);
                    g.drawWrapString(0, 0, w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + money);
                }

            }
        });
    }

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
            drawJail(g, width, height, position);
        }

        @Override
        protected void onDone() {
            synchronized (LOCK) {
                LOCK.notify();
            }
        }
    }

    private void drawJail(AGraphics g, float w, float h, float position) {
        float barWidth = 5;
        float barSpacing = 20;
        float x = 5;
        while (x < w) {
            g.setColor(GColor.BLACK);
            g.drawFilledRect(x, 0, barWidth, h*position);
            x += barSpacing+barWidth;
        }
    }

    class MoneyAnim extends AAnimation<Sprite> {

        final int delta;
        final float startMoney;

        MoneyAnim(int startMoney, int amt) {
            super(2500);
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
            this.start = getPlayer(playerNum).getSquare();
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
                synchronized (LOCK) {
                    LOCK.notify();
                }
            }
        }
    }

    public synchronized void startGameThread() {
        if (gameRunning)
            return;
        gameRunning = true;
        gameStopped = false;
        new Thread() {
            public void run() {
                while (gameRunning && getWinner() < 0) {
                    runGame();
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
        synchronized (LOCK) {
            LOCK.notifyAll();
        }
        while (!gameStopped) {
            Utils.waitNoThrow(this, 50);
        }
    }

    void initPlayers(int num, Piece pc) {
        clear();
        PlayerUser user = new PlayerUser();
        user.setPiece(pc);
        addPlayer(user);
        for (int i=1; i<num; i++)
            addPlayer(new Player());
    }

    int W, H, DIM;
    Board board;
    float playerInfoWidth, playerInfoHeight;

    public void paint(APGraphics g, int mouseX, int mouseY) {
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
        Player p = getPlayer(playerNum);
        int pcId = getImageId(p.getPiece());
        float dim = w/2;
        playerInfoHeight = dim;
        float border = 5;
        g.drawImage(pcId, 0, 0, dim, dim);
        if (getCurrentPlayerNum()==playerNum) {
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
                drawJail(g, w, dim, 1);
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
        drawDice(g, getDie1(), getDie2());
        float w = W/3;
        for (int i=0; i<getNumPlayers(); i++) {
            g.pushMatrix();
            g.translate(w*i, DIM);
            drawPlayerInfo(g, i, w, H-DIM);
            g.popMatrix();
        }
        drawAnimations(g, this);
    }

    private void drawLandscape(APGraphics g, int mx, int my) {

        g.pushMatrix();
        g.translate(W/2-DIM/2, H/2-DIM/2);
        drawBoard(g);
        g.popMatrix();
        // draw dice in center of board
        drawDice(g, getDie1(), getDie2());
        float w = (W-DIM)/2;
        for (int i=0; i<getNumPlayers(); i++) {
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
        drawAnimations(g, this);
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
        g.drawImage(getBoardImageId(), 0, 0, DIM, DIM);
        {
            Sprite kitty = spriteMap.get(Square.FREE_PARKING);
            GRectangle r = board.getSqaureBounds(Square.FREE_PARKING);
            kitty.M.setTranslate(r.getCenter());
            kitty.color = GColor.GREEN;
            kitty.animateAndDraw(g, r.w, r.h);
        }
/*
        if (getKitty() > 0) {
            GRectangle r = board.getSqaureBounds(Square.FREE_PARKING);
            Vector2D cntr = r.getCenter();
            g.setColor(GColor.GREEN);
            GDimension dim = g.drawWrapString(cntr.X(), cntr.Y(), r.w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + getKitty());
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRect(cntr.X() - dim.width/2 - 5, cntr.Y() - dim.height/2 - 5, dim.width + 10, dim.height + 10);
            g.setColor(GColor.GREEN);
            g.drawWrapString(cntr.X(), cntr.Y(), r.w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + getKitty());
        }*/
        for (int i=0; i<getNumPlayers(); i++) {
            Player p = getPlayer(i);
            if (p.isEliminated())
                continue;
            int pcId = getImageId(p.getPiece());
            float targetDim = board.getPieceDimension();
            for (Card c : p.getCards()) {
                if (c.getProperty() == null)
                    continue;
                GRectangle r = board.getSqaureBounds(c.getProperty());
                float houseScale = Math.min(r.w, r.h)/10;
                switch (board.getsQuarePosition(c.getProperty())) {
                    case TOP:
                        g.drawImage(pcId, r.x+r.w/2-targetDim/2, r.y+r.h-targetDim/3, targetDim, targetDim);
                        drawHouses(g, new Vector2D(r.x+r.w/2, r.y+r.h-houseScale), 0, houseScale, c.getHouses());
                        break;
                    case RIGHT:
                        g.drawImage(pcId, r.x-targetDim*2/3, r.y+r.h/2-targetDim/2, targetDim, targetDim);
                        drawHouses(g, new Vector2D(r.x+houseScale, r.y+r.h/2), 270, houseScale, c.getHouses());
                        break;
                    case BOTTOM:
                        g.drawImage(pcId, r.x+r.w/2-targetDim/2, r.y-targetDim*2/3, targetDim, targetDim);
                        drawHouses(g, new Vector2D(r.x+r.w/2, r.y+houseScale), 0, houseScale, c.getHouses());
                        break;
                    case LEFT:
                        g.drawImage(pcId, r.x+r.w-targetDim/3, r.y+r.h/2-targetDim/2, targetDim, targetDim);
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
            GRectangle r = p.isInJail() ? board.getPiecePlacementJail(i) : board.getPiecePlacement(i, p.getSquare());
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
