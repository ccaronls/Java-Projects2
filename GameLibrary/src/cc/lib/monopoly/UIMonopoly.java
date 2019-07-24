package cc.lib.monopoly;

import java.util.ArrayList;
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
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public abstract class UIMonopoly extends Monopoly {

    private final static Logger log = LoggerFactory.getLogger(UIMonopoly.class);
    public final static float HOUSE_RADIUS = 1;
    public final static int MONEY_PAUSE = 1000;
    public final static int HOUSE_PAUSE = 2000;
    public final static float PADDING = 5;
    public final static GColor BOARD_COLOR = new GColor(0xFFD2E5D2);
    public final static GColor HOUSE_COLOR = GColor.GREEN.darkened(.6f);
    public final static GColor HOTEL_COLOR = GColor.RED;

    private final Object LOCK = this;
    private boolean gameRunning = false;
    private boolean gameStopped = true;
    public int W, H, DIM;
    private Board board;
    private float playerInfoWidth, playerInfoHeight;
    private final Map<String, LinkedList<AAnimation>> animations = new HashMap<>();
    private final Map<String, Sprite> spriteMap = new HashMap<>();

    public UIMonopoly() {
        initSprites();
    }

    /**
     *
     */
    public abstract void repaint();

    /**
     *
     * @param p
     * @return
     */
    public abstract int getImageId(Piece p);

    /**
     *
     * @return
     */
    public abstract int getBoardImageId();

    /**
     *
     * @param player
     * @param moves
     * @return
     */
    protected abstract MoveType showChooseMoveMenu(Player player, List<MoveType> moves);

    /**
     *
     * @param player
     * @param cards
     * @param type
     * @return
     */
    protected abstract Card showChooseCardMenu(Player player, List<Card> cards, Player.CardChoiceType type);

    /**
     *
     * @param player
     * @param trades
     * @return
     */
    protected abstract Trade showChooseTradeMenu(Player player, List<Trade> trades);

    /**
     *
     * @param user
     * @param sellable
     * @return
     */
    protected abstract boolean showMarkSellableMenu(PlayerUser user, List<Card> sellable);

    @Override
    protected void onDiceRolled() {
        addAnimation("GAME", new AAnimation<AGraphics>(2000) {

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
    protected void onPlayerMove(int playerNum, int numSquares, Square next) {
        setSpriteAnim(getPlayer(playerNum).getPiece().name(), new JumpAnimation(playerNum, numSquares).start());
        Utils.waitNoThrow(LOCK, numSquares*600);
        super.onPlayerMove(playerNum, numSquares, next);
    }

    private void showMessage(final String title, final String txt) {
        addAnimation("BOARD", new AAnimation<AGraphics>(4000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                final float border = 5;
                float width = DIM/3 + border*2;
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

            @Override
            protected void onDone() {
                synchronized (LOCK) {
                    LOCK.notify();
                }
            }
        }.start());
        Utils.waitNoThrow(LOCK, 5000);
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
        setSpriteAnim("PLAYER"+playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), amt).start());
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerGotPaid(playerNum, amt);
    }

    @Override
    protected void onPlayerReceiveMoneyFromAnother(int playerNum, int giverNum, int amt) {
        setSpriteAnim("PLAYER"+playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), amt).start());
        setSpriteAnim("PLAYER"+giverNum, new MoneyAnim(getPlayer(giverNum).getMoney(), -amt).start());
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerReceiveMoneyFromAnother(playerNum, giverNum, amt);
    }

    @Override
    protected void onPlayerPayMoneyToKitty(int playerNum, int amt) {
        setSpriteAnim("PLAYER"+playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start());
        setSpriteAnim(Square.FREE_PARKING.name(), new MoneyAnim(getKitty(), amt).start());
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerPayMoneyToKitty(playerNum, amt);
    }

    @Override
    protected void onPlayerGoesToJail(int playerNum) {
        final Player p = getPlayer(playerNum);
        final GRectangle start = board.getPiecePlacement(playerNum, p.getSquare());
        final GRectangle end   = board.getPiecePlacementJail(playerNum);
        addAnimation("PLAYER"+playerNum, new JailedAnim(playerInfoWidth, playerInfoHeight).start());
        setSpriteAnim(p.piece.name(), new AAnimation<Sprite>(2000) {

            final Bezier curve = new Bezier();

            @Override
            protected void onStarted() {
                MutableVector2D s = start.getTopLeft();
                MutableVector2D e = end.getTopLeft();
                curve.addPoint(s);
                MutableVector2D dv = e.sub(s);
                float len = dv.mag();
                MutableVector2D n = new MutableVector2D(0, len/3);
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
//        Utils.waitNoThrow(LOCK, 5000);
        super.onPlayerGoesToJail(playerNum);
    }

    @Override
    protected void onPlayerOutOfJail(final int playerNum) {
        setSpriteAnim("PLAYER"+playerNum, new AAnimation<Sprite>(500) {

            Vector2D start, end;

            @Override
            protected void onStarted() {
                start = board.getPiecePlacementJail(playerNum).getTopLeft();
                end   = board.getPiecePlacement(playerNum, Square.VISITING_JAIL).getTopLeft();
            }

            @Override
            protected void draw(Sprite g, float position, float dt) {
                g.M.translate(start.add(end.sub(start).scaledBy(position)));
            }
        }.start());
        addAnimation("PLAYER"+playerNum, new JailedAnim(playerInfoWidth, playerInfoHeight).startReverse());
        Utils.waitNoThrow(LOCK, 5000);
        super.onPlayerOutOfJail(playerNum);
    }

    @Override
    protected void onPlayerPaysRent(int playerNum, int renterNum, int amt) {
        setSpriteAnim("PLAYER"+playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start());
        setSpriteAnim("PLAYER"+renterNum, new MoneyAnim(getPlayer(renterNum).getMoney(), amt).start());
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerPaysRent(playerNum, renterNum, amt);
    }

    @Override
    protected void onPlayerMortgaged(int playerNum, Square property, int amt) {
        setSpriteAnim("PLAYER"+playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), amt).start());
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerMortgaged(playerNum, property, amt);
    }

    @Override
    protected void onPlayerUnMortgaged(int playerNum, Square property, int amt) {
        setSpriteAnim("PLAYER"+playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start());
        Utils.waitNoThrow(LOCK, MONEY_PAUSE);
        super.onPlayerUnMortgaged(playerNum, property, amt);
    }

    @Override
    protected void onPlayerPurchaseProperty(final int playerNum, final Square property) {
        addAnimation("GAME", new AAnimation<AGraphics>(5000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                float w = DIM/4;
                float h = w*3/2;
                float x = DIM/2-w/2;
                float y = DIM/2-h/2;
                g.pushMatrix();
                g.translate(x, y);
                drawPropertyCard(g, property, w, h);
                g.popMatrix();
                if (getElapsedTime() > 1000) {
                    g.setColor(GColor.RED);
                    int oldHgt = g.getTextHeight();
                    g.setTextHeight(h/20);
                    g.drawJustifiedString(DIM/2, h-10, Justify.CENTER, Justify.BOTTOM, "SOLD TO " + getPlayerName(playerNum));
                    g.setTextHeight(oldHgt);
                }
            }

            @Override
            protected void onDone() {
                setSpriteAnim("PLAYER"+playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), -property.getPrice()).start());
                /*
                addAnimation("BOARD", new AAnimation<AGraphics>(2000) {
                    @Override
                    protected void draw(AGraphics g, float position, float dt) {
                        GRectangle d = board.getSqaureBounds(property);
                        g.setColor(GColor.RED.withAlpha(1f-position));
                        g.drawWrapString(d.x+d.w/2, d.y+d.h/2, d.w * 3/4, Justify.CENTER, Justify.CENTER, "SOLD");
                    }

                    @Override
                    protected void onDone() {
                        synchronized (LOCK) {
                            LOCK.notify();
                        }
                    }
                }.start());*/
            }
        }.start());
        Utils.waitNoThrow(LOCK, 5000);
        super.onPlayerPurchaseProperty(playerNum, property);
    }

    @Override
    protected void onPlayerTrades(int buyer, int seller, Square property, int amount) {
        setSpriteAnim("PLAYER"+buyer, new MoneyAnim(getPlayer(buyer).getMoney(), -amount).start());
        setSpriteAnim("PLAYER"+seller, new MoneyAnim(getPlayer(seller).getMoney(), amount).start());
        super.onPlayerTrades(buyer, seller, property, amount);
    }

    @Override
    protected void onPlayerBoughtHouse(int playerNum, Square property, int amt) {
        addAnimation("GAME", new AAnimation<AGraphics>(HOUSE_PAUSE) {

            Vector2D v0, v1;

            @Override
            protected void onStarted() {
                super.onStarted();
                v0 = new Vector2D(DIM/2, DIM/2);
                v1 = board.getInnerEdge(property);
            }

            @Override
            protected void draw(AGraphics g, float position, float dt) {
                float x0 = DIM/2;
                float y0 = DIM/2;
                Vector2D v = v0.add(v1.sub(v0).scaledBy(position));
                float s = DIM/20;
                g.pushMatrix();
                g.translate(v);
                g.setColor(HOUSE_COLOR);
                g.scale(s*(1.0f-position));
                drawHouse(g);
                g.popMatrix();
            }

            @Override
            protected void onDone() {
                setSpriteAnim("PLAYER"+playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start());
                setSpriteAnim(property.name(), new ErectHouseAnim().start());
            }
        }.start());
        Utils.waitNoThrow(LOCK, HOUSE_PAUSE*2);
        super.onPlayerBoughtHouse(playerNum, property, amt);
    }

    @Override
    protected void onPlayerBoughtHotel(int playerNum, final Square property, int amt) {
        addAnimation("GAME", new AAnimation<AGraphics>(HOUSE_PAUSE) {

            Vector2D v0, v1;

            @Override
            protected void onStarted() {
                super.onStarted();
                v0 = new Vector2D(DIM/2, DIM/2);
                v1 = board.getInnerEdge(property);
            }

            @Override
            protected void draw(AGraphics g, float position, float dt) {
                Vector2D v = v0.add(v1.sub(v0).scaledBy(position));
                float sx = DIM/12;
                float sy = DIM/10;
                g.pushMatrix();
                g.translate(v);
                g.setColor(HOTEL_COLOR);
                g.scale(sx, sy);
                g.scale(1.0f-position);
                drawHouse(g);
                g.popMatrix();
            }

            @Override
            protected void onDone() {
                setSpriteAnim("PLAYER"+playerNum, new MoneyAnim(getPlayer(playerNum).getMoney(), -amt).start());
                setSpriteAnim(property.name(), new ErectHouseAnim().start());
            }
        }.start());
        Utils.waitNoThrow(LOCK, HOUSE_PAUSE*2);
        super.onPlayerBoughtHotel(playerNum, property, amt);
    }

    @Override
    protected void onPlayerBankrupt(int playerNum) {
        addAnimation("PLAYER" + playerNum, new AAnimation<AGraphics>(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                GRectangle rect = g.getClipRect();
                g.setColor(GColor.TRANSLUSCENT_BLACK);
                g.drawFilledRect(rect.x+((1-position)*rect.w/2), rect.y, rect.w*position, rect.h);
            }

            @Override
            protected void onDone() {
                synchronized (LOCK) {
                    LOCK.notify();
                }
            }
        }.start());
        Utils.waitNoThrow(LOCK, 2000);
        super.onPlayerBankrupt(playerNum);
    }

    @Override
    protected void onPlayerWins(int playerNum) {
        showMessage("WINNER", getPlayerName(playerNum) + " IS THE WINNER!");
        addAnimation("PLAYER" + playerNum, new AAnimation<AGraphics>(500, -1) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                GRectangle r = g.getClipRect();
                g.setColor(Utils.rand()%256, Utils.rand()%256, Utils.rand()%256, 255);
                g.drawRect(r, 3);
            }
        }.start());
        super.onPlayerWins(playerNum);
    }

    void addAnimation(String key, AAnimation<AGraphics> a) {
        if (!isGameRunning())
            return;
        synchronized (animations) {
            LinkedList<AAnimation> list = animations.get(key);
            if (list == null) {
                list = new LinkedList<>();
                animations.put(key, list);
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

    void setSpriteAnim(String key, AAnimation<Sprite> anim) {
        if (!isGameRunning())
            return;
        Sprite s = spriteMap.get(key);
        s.animation = anim;
        repaint();
    }

    public void onClick() {
    }

    public void startDrag() {

    }

    public void stopDrag() {

    }

    /**
     * Sprites solve the problem of rendering something that can be animated or static.
     * Transforms:
     * - position
     * - orientation
     * - scale
     * - color
     *
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

        /**
         * Draw the sprite at origin facing at angle 0. Translations will orient and move according
         * @param g
         * @param w
         * @param h
         */
        abstract void draw(AGraphics g, float w, float h);
    }

    void initSprites() {
        // the pieces on the board can be animated
        for (final Piece p : Piece.values()) {
            spriteMap.put(p.name(), new Sprite() {
                @Override
                void draw(AGraphics g, float w, float h) {
                    g.drawImage(getImageId(p), 0, 0, w, h);
                }
            });
        }

        // a players current money can be animated
        for (int i=0; i<Monopoly.MAX_PLAYERS; i++) {
            final int pNum = i;
            spriteMap.put("PLAYER"+i, new Sprite() {
                @Override
                void draw(AGraphics g, float w, float h) {
                    final Player p = getPlayer(pNum);
                    int money = p.getMoney();
                    if (animation != null) {
                        money = data1;
                        float textHeight = g.getTextHeight();
                        String amt = data2 < 0 ? String.valueOf(data2) : "+" + data2;
                        Vector2D delta = new Vector2D(0, textHeight * animation.getPosition());
                        g.pushMatrix();
                        g.translate(delta);
                        g.setColor((data2 > 0 ? Board.GREEN : Board.RED).withAlpha(animation.getPosition()));
                        g.drawJustifiedString(0, 0, Justify.RIGHT, Justify.CENTER, amt);
                        g.popMatrix();
                    }
                    g.setColor(color);
                    g.drawJustifiedString(0, 0, Justify.RIGHT, Justify.CENTER, "$"+money);
                }
            });
        }

        // The kitty can be animated
        spriteMap.put(Square.FREE_PARKING.name(), new Sprite() {
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

        // the property squares them selves can animate building houses
        for (final Square sq : Square.values()) {
            if (sq.isProperty()) {
                spriteMap.put(sq.name(), new Sprite() {
                    @Override
                    void draw(AGraphics g, float w, float h) {
                        GRectangle r = board.getSqaureBounds(sq);
                        final float houseScale = Math.min(r.w, r.h)/15;
                        g.pushMatrix();
                        int houses = data1;
                        Vector2D v = null;
                        int angle = 0;
                        switch (board.getSquarePosition(sq)) {
                            case TOP:
                                v = new Vector2D(r.x+r.w/2, r.y+r.h-houseScale);
                                angle = 0;//, houseScale, houses);
                                break;
                            case RIGHT:
                                v = new Vector2D(r.x+houseScale, r.y+r.h/2);
                                angle = 270;//, houseScale, houses);
                                break;
                            case BOTTOM:
                                v = new Vector2D(r.x+r.w/2, r.y+houseScale);
                                angle = 0;//, houseScale, houses);
                                break;
                            case LEFT:
                                v = new Vector2D(r.x+r.w-houseScale, r.y+r.h/2);
                                angle = 90;//, houseScale, houses);
                                break;
                        }
                        g.translate(v);
                        g.rotate(angle);
                        g.multMatrix(M);
                        if (isAnimating()) {
                            switch (houses) {
                                case 0: // draw one growing house
                                    g.setColor(HOUSE_COLOR);
                                    g.scale(houseScale * 2 * animation.getPosition());
                                    drawHouse(g);
                                    break;
                                case 1:
                                case 2:
                                case 3: // existing houses slide over while last one grows up
                                    g.scale(houseScale * 2);
                                    g.setColor(HOUSE_COLOR);
                                    g.translate(-HOUSE_RADIUS * (houses - 1) -HOUSE_RADIUS*animation.getPosition(), 0);
                                    for (int i = 0; i < houses; i++) {
                                        drawHouse(g);
                                        g.translate(HOUSE_RADIUS * 2, 0);
                                    }
                                    g.scale(animation.getPosition());
                                    drawHouse(g);
                                    break;

                                case 4: // existing houses dissovle/shrink while hotel is erected
                                    g.pushMatrix();
                                    g.scale(houseScale * 2, houseScale * 2 * (1.0f - animation.getPosition()));
                                    g.setColor(HOUSE_COLOR);
                                    g.translate(-HOUSE_RADIUS * (houses - 1), 0);
                                    for (int i = 0; i < houses; i++) {
                                        drawHouse(g);
                                        g.translate(HOUSE_RADIUS * 2, 0);
                                    }
                                    g.popMatrix();
                                    g.scale(animation.getPosition() * houseScale * 4);
                                    g.setColor(HOTEL_COLOR);
                                    drawHouse(g);
                                    break;
                            }
                        } else {
                            if (houses == 5) {
                                g.scale(houseScale * 4);
                                g.setColor(HOTEL_COLOR);
                                drawHouse(g);
                            } else if (houses < 5) {
                                g.setColor(HOUSE_COLOR);
                                g.scale(houseScale * 2);
                                g.translate(-HOUSE_RADIUS * (houses - 1), 0);
                                for (int i = 0; i < houses; i++) {
                                    drawHouse(g);
                                    g.translate(HOUSE_RADIUS * 2, 0);
                                }
                            } else {
                                Utils.unhandledCase(houses);
                            }
                        }
                        g.popMatrix();
                    }
                });
            }
        }
    }

    class ErectHouseAnim extends AAnimation<Sprite> {
        public ErectHouseAnim() {
            super(HOUSE_PAUSE);
        }

        @Override
        protected void draw(Sprite g, float position, float dt) {

        }
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
        int dir;
        Bezier curve;

        JumpAnimation(int playerNum, int jumps) {
            super(500);
            this.playerNum = playerNum;
            this.start = getPlayer(playerNum).getSquare();
            this.jumps = jumps;
            this.dir = CMath.signOf(jumps);
            curve = new Bezier();
            init();
        }

        void init() {
            GRectangle r0 = board.getPiecePlacement(playerNum, start);
            int steps = 1;
            if (jumps > 5) {
                steps = 5;
                start = Square.values()[(start.ordinal()+5) % NUM_SQUARES]; // make bigger steps when a long way to jump
            } else if (jumps < 0) {
                start = Utils.decrementValue(start, Square.values());
            } else {
                start = Utils.incrementValue(start, Square.values());
            }
            GRectangle r1 = board.getPiecePlacement(playerNum, start);
            curve.reset();
            curve.addPoint(r0.x, r0.y);
            float dx = r1.x-r0.x;
            float dy = r1.y-r0.y;
            curve.addPoint(r0.x + dx/3, Utils.clamp(r0.y + dy*0.33f + dx/6, 0, DIM));
            curve.addPoint(r0.x + dx*2/3, Utils.clamp(r0.y + dy*0.66f + dx/6, 0, DIM));
            curve.addPoint(r1.x, r1.y);
            jumps-=dir*steps;
        }

        @Override
        protected void draw(Sprite sp, float position, float dt) {
            sp.M.setTranslate(curve.getPointAt(position));
        }

        @Override
        protected void onDone() {
            if (jumps != 0) {
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
        log.debug("startGameThread");
        gameRunning = true;
        gameStopped = false;
        new Thread() {
            public void run() {
                log.debug("ENTER game thread");
                while (gameRunning && getWinner() < 0) {
                    try {
                        runGame();
                    } catch (Throwable t) {
                        onError(t);
                        break;
                    }
                    repaint();
                    Utils.waitNoThrow(this, 100);
                }
                log.debug("EXIT game thread");
                gameRunning = false;
                gameStopped = true;
            }
        }.start();

    }

    protected void onError(Throwable t) {
        throw new RuntimeException(t);
    }

    public final void stopGameThread() {
        if (gameStopped)
            return;
        log.debug("stopGameThread");
        gameRunning = false;
        stopAnimations();
        synchronized (LOCK) {
            LOCK.notifyAll();
        }
        while (!gameStopped) {
            Utils.waitNoThrow(this, 50);
        }
        log.debug("GAME THREAD STOPPED");
    }

    public final void initPlayers(int num, Piece pc) {
        clear();
        PlayerUser user = new PlayerUser();
        user.setPiece(pc);
        addPlayer(user);
        for (int i=1; i<num; i++)
            addPlayer(new Player());
    }

    public void paint(APGraphics g, int mouseX, int mouseY) {
        g.clearScreen(BOARD_COLOR);
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
        g.setColor(BOARD_COLOR);
        g.drawFilledRect(0, 0, w, h);
        g.setColor(GColor.BLACK);
        g.drawRect(0, 0, w, h);
        g.setClipRect(0, 0, w, h);
        playerInfoWidth = w;
        Player p = getPlayer(playerNum);
        if (p.getPiece() == null)
            return;
        int pcId = getImageId(p.getPiece());
        float dim = Math.min(w/2, h/4);
        playerInfoHeight = dim;
        float border = 5;
        g.drawImage(pcId, 0, 0, dim, dim);
        if (getCurrentPlayerNum()==playerNum) {
            g.setColor(GColor.CYAN);
            g.drawRect(0, 0, dim, dim, 2);
        }
        Sprite sp = spriteMap.get("PLAYER"+playerNum);
        sp.M.setTranslate(w-PADDING, dim/2);
        sp.color = GColor.BLACK;
        sp.animateAndDraw(g, 0, 0);

        if (p.isBankrupt()) {
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRect(0, 0, w, h);
            g.setColor(GColor.RED);
            g.drawWrapString(w/2, h/2, w, Justify.CENTER, Justify.CENTER, "BANKRUPT");
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
                String sqStr = Utils.getPrettyString(p.getSquare().name());
                while (Character.isDigit(sqStr.charAt(sqStr.length()-1))) {
                    sqStr = sqStr.substring(0, sqStr.length()-1);
                }
                sy += drawWrapStringOnBackground(g, 0, sy, w, sqStr, bkColor, txtColor, border);
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
        drawAnimations(g, "PLAYER"+playerNum);
        g.clearClip();
    }

    public float drawWrapStringOnBackground(AGraphics g, float x, float y, float width, String txt, GColor bkColor, GColor txtColor, float border) {
        GDimension d = g.drawWrapString(x+border, y+border, width-border*2, txt);
        g.setColor(bkColor);
        g.drawFilledRect(x, y, width, d.height+2*border);
        g.setColor(txtColor);
        g.drawWrapString(x+border, y+border, width-border*2, txt);
        return d.height + 2*border;
    }

    public void drawPropertyCard(AGraphics g, Square property, float w, float h) {
        final float padding = w/15;
        g.setColor(GColor.WHITE);
        g.drawFilledRect(0, 0, w, h);
        g.setColor(property.getColor());
        final float BANNER_HEIGHT = h/5;
        int oldTextHeight = g.getTextHeight();
        g.setTextHeight(BANNER_HEIGHT/3);
        g.drawFilledRect(0, 0, w, BANNER_HEIGHT);
        g.translate(padding, padding);
        w -= padding*2;
        g.setColor(chooseContrastColor(property.getColor()));
        g.drawWrapString(w/2, 0, w, Justify.CENTER, Justify.TOP, Utils.getPrettyString(property.name()));
        g.setTextHeight(oldTextHeight);
        g.translate(0, BANNER_HEIGHT);
        h -= padding*2 - BANNER_HEIGHT;
        g.setColor(GColor.BLACK);
        if (property.isProperty()) {
            g.drawWrapString(0, 0, w, String.format(
                    "PRICE $%d RENT $%d"
                    +"\n\nRent With:"
                    +"\n1 House $%d"
                    +"\n2 Houses $%d"
                    +"\n3 Houses $%d"
                    +"\n4 Houses $%d"
                    +"\nHotel $%d"
                    +"\n\nUpgrade $%d"
                    +"\nMORTGAGE: $%d"
                    ,property.getPrice(), property.getRent(0)
                    ,property.getRent(1)
                    ,property.getRent(2)
                    ,property.getRent(3)
                    ,property.getRent(4)
                    ,property.getRent(5)
                    ,property.getUnitPrice()
                    ,property.getMortgageValue(0)
            ));
        } else if (property.isUtility()) {
            g.drawWrapString(0, 0, w, String.format(
                    "PRICE $%d"
                    +"\n\nIf One Utility is owned then 4 times the amount shown on dice.\nIf two Utilities owned then 10 times the amount shown on Dice\n\nMortgage $%d", property.getPrice(), property.getMortgageValue(0)));
        } else if (property.isRailroad()) {
            g.drawWrapString(0, 0, w, String.format(
                    "PRICE $%d"
                    +"\n\nRENT $%d"
                    +"\nIf 2 R.R. Owned $%d"
                    +"\nIf 3 R.R. Owned $%d"
                    +"\nIf 4 R.R. Owned $%d"
                    +"\n\nMORTGAGE $%d"
                    ,property.getPrice()
                    ,Monopoly.RAILROAD_RENT * (1<<0)
                    ,Monopoly.RAILROAD_RENT * (1<<1)
                    ,Monopoly.RAILROAD_RENT * (1<<2)
                    ,Monopoly.RAILROAD_RENT * (1<<3)
                    ,property.getMortgageValue(0)
            ));
        } else {
            throw new AssertionError("Dont know how to draw: " + property);
        }
    }

    public GColor chooseContrastColor(GColor c) {
        float amt = c.getRed()+c.getBlue()+c.getGreen();
        amt *= c.getAlpha();
        if (amt > 1.5f)
            return GColor.BLACK;
        return GColor.WHITE;
    }

    private void drawPortrait(APGraphics g, int mx, int my) {
        drawBoard(g);
        drawDice(g, getDie1(), getDie2());
        float w = (W - (PADDING*(getNumPlayers()-1))) / getNumPlayers();
        for (int i=0; i<getNumPlayers(); i++) {
            g.pushMatrix();
            g.translate((w+PADDING)*i, DIM+PADDING);
            drawPlayerInfo(g, i, w, H-DIM-PADDING);
            g.popMatrix();
        }
        drawAnimations(g, "GAME");
    }

    private void drawLandscape(APGraphics g, int mx, int my) {

        g.pushMatrix();
        g.translate(W/2-DIM/2, H/2-DIM/2);
        drawBoard(g);
        g.popMatrix();
        // draw dice in center of board
        drawDice(g, getDie1(), getDie2());
        float w = (W-DIM)/2;
        float h1 = H;
        float h2 = H/2-PADDING/2;
        int numP = getNumPlayers();
        for (int i=0; i<getNumPlayers(); i++) {
            float h = h1;
            g.pushMatrix();
            switch (i) {
                case 0: if (numP == 4) h = h2; break; // top-left
                case 1: if (numP >= 3) h = h2; g.translate(W-w+PADDING, 0); break; // top-right
                case 2: h = h2; g.translate(W-w+PADDING, H/2+PADDING/2); break; // bottom-right
                case 3: h = h2; g.translate(0, H/2+PADDING); break; // bottom-left
            }
            drawPlayerInfo(g, i, w-PADDING, h);
            g.popMatrix();
        }
        drawAnimations(g, "GAME");
    }

    private void drawAnimations(AGraphics g, String key) {
        synchronized (animations) {
            LinkedList<AAnimation> list = animations.get(key);
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

    private final static Vector2D [] HOUSE_PTS = {
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



    // draw house with center at 0,0.
    public void drawHouse(AGraphics g) {
        GColor color = g.getColor();
        GColor roofL = color;
        GColor roofR = color.darkened(0.2f);
        GColor front = color.darkened(0.5f);

        g.setColor(front);
        g.begin();
        g.vertexArray(HOUSE_PTS[8], HOUSE_PTS[9], HOUSE_PTS[6], HOUSE_PTS[7]);
        g.drawQuadStrip();
        g.begin();
        g.setColor(roofL);
        g.vertexArray(HOUSE_PTS[0], HOUSE_PTS[1], HOUSE_PTS[2], HOUSE_PTS[3]);
        g.drawQuadStrip();
        g.begin();
        g.setColor(roofR);
        g.vertexArray(HOUSE_PTS[2], HOUSE_PTS[3], HOUSE_PTS[4], HOUSE_PTS[5]);
        g.drawQuadStrip();
        g.setColor(color); // restore color
    }

    private void drawBoard(AGraphics g) {
        g.drawImage(getBoardImageId(), 0, 0, DIM, DIM);
        {
            Sprite kitty = spriteMap.get(Square.FREE_PARKING.name());
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
            if (p.isBankrupt())
                continue;
            if (p.getPiece() == null)
                continue;
            int pcId = getImageId(p.getPiece());
            float targetDim = board.getPieceDimension();
            for (Card c : new ArrayList<>(p.getCards())) {
                if (c.getProperty() == null)
                    continue;
                GRectangle r = board.getSqaureBounds(c.getProperty());
                //float houseScale = Math.min(r.w, r.h)/15;
                switch (board.getSquarePosition(c.getProperty())) {
                    case TOP:
                        g.drawImage(pcId, r.x+r.w/2-targetDim/2, r.y+r.h-targetDim/3, targetDim, targetDim);
//                        drawHouses(g, new Vector2D(r.x+r.w/2, r.y+r.h-houseScale), 0, houseScale, c.getHouses());
                        break;
                    case RIGHT:
                        g.drawImage(pcId, r.x-targetDim*2/3, r.y+r.h/2-targetDim/2, targetDim, targetDim);
//                        drawHouses(g, new Vector2D(r.x+houseScale, r.y+r.h/2), 270, houseScale, c.getHouses());
                        break;
                    case BOTTOM:
                        g.drawImage(pcId, r.x+r.w/2-targetDim/2, r.y-targetDim*2/3, targetDim, targetDim);
//                        drawHouses(g, new Vector2D(r.x+r.w/2, r.y+houseScale), 0, houseScale, c.getHouses());
                        break;
                    case LEFT:
                        g.drawImage(pcId, r.x+r.w-targetDim/3, r.y+r.h/2-targetDim/2, targetDim, targetDim);
//                        drawHouses(g, new Vector2D(r.x+r.w-houseScale, r.y+r.h/2), 90, houseScale, c.getHouses());
                        break;
                }
                Sprite sp = spriteMap.get(c.property.name());
                if (sp != null) {
                    sp.data1 = c.getHouses();
                    //sp.M.setTranslate(r.x, r.y);
                    sp.animateAndDraw(g, r.w, r.h);
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
            {
                // draw player piece on the board
                GRectangle r = p.isInJail() ? board.getPiecePlacementJail(i) : board.getPiecePlacement(i, p.getSquare());
                Sprite sp = spriteMap.get(p.getPiece().name());
                sp.M.setTranslate(r.x, r.y);
                sp.animateAndDraw(g, r.w, r.h);
                //g.drawImage(pcId, r);
            }

            // Mark all sellable properties
            for (Trade t : p.getTrades()) {
                Vector2D v = board.getInnerEdge(t.getCard().property);
                float offset = board.getPieceDimension();
                //GRectangle r = board.getPiecePlacement(getPlayerNum(t.getTrader()), t.getCard().property);
                Board.Position pos = board.getSquarePosition(t.getCard().property);
                g.setColor(GColor.YELLOW);
                switch (pos) {
                    case TOP:
                        g.drawJustifiedStringOnBackground(v.X(), v.Y() + offset, Justify.CENTER, Justify.TOP, "$" + t.getPrice(), GColor.TRANSLUSCENT_BLACK, 5, 5);
                        break;
                    case RIGHT:
                        g.drawJustifiedStringOnBackground(v.X()-offset, v.Y(), Justify.RIGHT, Justify.CENTER, "$" + t.getPrice(), GColor.TRANSLUSCENT_BLACK, 5, 5);
                        break;
                    case BOTTOM:
                        g.drawJustifiedStringOnBackground(v.X(), v.Y()-offset, Justify.CENTER, Justify.BOTTOM, "$" + t.getPrice(), GColor.TRANSLUSCENT_BLACK, 5, 5);
                        break;
                    case LEFT:
                        g.drawJustifiedStringOnBackground(v.X()+offset, v.Y(), Justify.LEFT, Justify.CENTER, "$" + t.getPrice(), GColor.TRANSLUSCENT_BLACK, 5, 5);
                        break;
                    default:
                        Utils.unhandledCase(pos);
                }
            }
        }
        drawAnimations(g, "BOARD");
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
                Utils.unhandledCase(numDots);// && "Invalid die");
                break;
        }
        g.drawPoints();
        g.setPointSize(oldDotSize);
    }

    public final boolean isGameRunning() {
        return gameRunning;
    }
}
