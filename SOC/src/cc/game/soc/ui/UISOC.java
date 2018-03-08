package cc.game.soc.ui;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.game.soc.core.BotNode;
import cc.game.soc.core.Card;
import cc.game.soc.core.CardType;
import cc.game.soc.core.CommodityType;
import cc.game.soc.core.DevelopmentArea;
import cc.game.soc.core.Dice;
import cc.game.soc.core.EventCard;
import cc.game.soc.core.ICardType;
import cc.game.soc.core.Island;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.Player;
import cc.game.soc.core.ProgressCardType;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.Route;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SpecialVictoryType;
import cc.game.soc.core.Tile;
import cc.game.soc.core.Trade;
import cc.game.soc.core.Vertex;
import cc.game.soc.core.VertexType;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

/**
 * Created by chriscaron on 2/22/18.
 */

public abstract class UISOC extends SOC implements MenuItem.Action {

    private static Logger log = LoggerFactory.getLogger(UISOC.class);

    private static UISOC instance = null;

    private final UIPlayerRenderer [] playerComponents;
    private final UIBoardRenderer boardRenderer;
    private final UIDiceRenderer diceRenderer;
    private final UIConsoleRenderer console;
    private final UIEventCardRenderer eventCardRenderer;
    private final UIBarbarianRenderer barbarianRenderer;
    private Object returnValue = null;
    private final Object waitObj = new Object();

    protected UISOC(UIPlayerRenderer [] playerComponents, UIBoardRenderer boardRenderer, UIDiceRenderer diceRenderer, UIConsoleRenderer console, UIEventCardRenderer eventCardRenderer, UIBarbarianRenderer barbarianRenderer) {
        if (instance != null)
            throw new RuntimeException();
        instance = this;
        this.playerComponents = playerComponents;
        this.boardRenderer = boardRenderer;
        this.diceRenderer = diceRenderer;
        this.eventCardRenderer = eventCardRenderer;
        this.barbarianRenderer = barbarianRenderer;
        this.console = console;
    }

    public static UISOC getInstance() {
        return instance;
    }

    public UIBoardRenderer getUIBoard() {
        return boardRenderer;
    }

    public void setReturnValue(Object o) {
        returnValue = o;
        synchronized (waitObj) {
            waitObj.notify();
        }
    }

    public final boolean isRunning() {
        return running;
    }

    public final MoveType chooseMoveMenu(Collection<MoveType> moves) {
        clearMenu();
        Iterator<MoveType> it = moves.iterator();
        while (it.hasNext()) {
            MoveType move = it.next();
            addMenuItem(CHOOSE_MOVE, move.getNiceText(), move.getHelpText(getRules()), move);
        }
        completeMenu();
        return waitForReturnValue(null);
    }

    protected final Player.RouteChoiceType chooseRouteType(){
        clearMenu();
        addMenuItem(CHOOSE_ROAD, "Roads", "View road options", Player.RouteChoiceType.ROAD_CHOICE);
        addMenuItem(CHOOSE_ROAD, "Ships", "View ship options", Player.RouteChoiceType.SHIP_CHOICE);
        completeMenu();
        return waitForReturnValue(null);
    }

    public GColor getPlayerColor(int playerNum) {
        UIPlayer p = (UIPlayer)getPlayerByPlayerNum(playerNum);
        if (p != null)
            return p.getColor();
        return GColor.BLACK;
    }

    public Vertex chooseVertex(final Collection<Integer> vertexIndices, final int playerNum, final Player.VertexChoice choice) {
        clearMenu();
        getUIBoard().setPickHandler(new PickHandler() {

            @Override
            public void onPick(UIBoardRenderer b, int pickedValue) {
                b.setPickHandler(null);
                setReturnValue(getBoard().getVertex(pickedValue));
            }

            @Override
            public void onHighlighted(UIBoardRenderer b, APGraphics g, int highlightedIndex) {
                Vertex v = getBoard().getVertex(highlightedIndex);
                g.setColor(getPlayerColor(getCurPlayerNum()));
                switch (choice) {
                    case SETTLEMENT:
                        b.drawSettlement(g, v, v.getPlayer(), true);
                        break;
                    case CITY:
                        b.drawCity(g, v, v.getPlayer(), true);
                        break;
                    case CITY_WALL:
                        b.drawWalledCity(g, v, v.getPlayer(), true);
                        break;
                    case KNIGHT_DESERTER:
                    case KNIGHT_DISPLACED:
                    case KNIGHT_MOVE_POSITION:
                    case KNIGHT_TO_MOVE:
                    case OPPONENT_KNIGHT_TO_DISPLACE:
                        b.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel(), v.getType().isKnightActive(), false);
                        g.setColor(GColor.RED);
                        b.drawCircle(g, v);
                        break;
                    case KNIGHT_TO_ACTIVATE:
                        b.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel(), true, true);
                        break;
                    case KNIGHT_TO_PROMOTE:
                        b.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel() + 1, v.getType().isKnightActive(), true);
                        break;
                    case NEW_KNIGHT:
                        b.drawKnight(g, v, v.getPlayer(), 1, false, true);
                        g.setColor(GColor.RED);
                        b.drawCircle(g, v);
                        break;
                    case POLITICS_METROPOLIS:
                        b.drawMetropolisPolitics(g, v, v.getPlayer(), true);
                        break;
                    case SCIENCE_METROPOLIS:
                        b.drawMetropolisScience(g, v, v.getPlayer(), true);
                        break;
                    case TRADE_METROPOLIS:
                        b.drawMetropolisTrade(g, v, v.getPlayer(), true);
                        break;
                    case PIRATE_FORTRESS:
                        b.drawPirateFortress(g, v, true);
                        break;
                    case OPPONENT_STRUCTURE_TO_ATTACK:
                        b.drawVertex(g, v, v.getType(), v.getPlayer(), true);
                        break;
                }

            }

            @Override
            public void onDrawPickable(UIBoardRenderer b, APGraphics g, int index) {
                Vertex v = getBoard().getVertex(index);
                GColor color = getPlayerColor(getCurPlayerNum()).withAlpha(120);
                g.setColor(color);
                switch (choice) {
                    case SETTLEMENT:
                        b.drawSettlement(g, v, 0, false);
                        break;
                    case CITY:
                        b.drawCity(g, v, 0, false);
                        break;
                    case CITY_WALL:
                        b.drawWalledCity(g, v, 0, false);
                        break;
                    case KNIGHT_DESERTER:
                    case KNIGHT_DISPLACED:
                    case KNIGHT_MOVE_POSITION:
                    case KNIGHT_TO_MOVE:
                    case OPPONENT_KNIGHT_TO_DISPLACE:
                        b.drawKnight(g, v, 0, v.getType().getKnightLevel(), v.getType().isKnightActive(), false);
                        g.setColor(GColor.YELLOW);
                        b.drawCircle(g, v);
                        g.setColor(color);
                        break;
                    case KNIGHT_TO_ACTIVATE:
                        b.drawKnight(g, v, 0, v.getType().getKnightLevel(), true, false);
                        break;
                    case KNIGHT_TO_PROMOTE:
                        b.drawKnight(g, v, 0, v.getType().getKnightLevel() + 1, v.getType().isKnightActive(), false);
                        break;
                    case NEW_KNIGHT:
                        b.drawKnight(g, v, 0, 1, false, false);
                        g.setColor(GColor.YELLOW);
                        b.drawCircle(g, v);
                        g.setColor(color);
                        break;
                    case POLITICS_METROPOLIS:
                        b.drawMetropolisPolitics(g, v, 0, false);
                        break;
                    case SCIENCE_METROPOLIS:
                        b.drawMetropolisScience(g, v, 0, false);
                        break;
                    case TRADE_METROPOLIS:
                        b.drawMetropolisTrade(g, v, 0, false);
                        break;
                    case PIRATE_FORTRESS:
                        b.drawPirateFortress(g, v, false);
                        break;
                    case OPPONENT_STRUCTURE_TO_ATTACK:
                        g.setColor(getPlayerColor(v.getPlayer()).withAlpha(120));
                        b.drawVertex(g, v, v.getType(), 0, false);
                        break;
                }
            }

            @Override
            public void onDrawOverlay(UIBoardRenderer b, APGraphics g) {
            }

            @Override
            public boolean isPickableIndex(UIBoardRenderer b, int index) {
                return vertexIndices.contains(index);
            }

            @Override
            public PickMode getPickMode() {
                return PickMode.PM_VERTEX;
            }
        });
        completeMenu();
        return waitForReturnValue(null);
    }

    public Route chooseRoute(final Collection<Integer> edges, final Player.RouteChoice choice) {
        clearMenu();
        getUIBoard().setPickHandler(new PickHandler() {

            @Override
            public void onPick(UIBoardRenderer b, int pickedValue) {
                b.setPickHandler(null);
                setReturnValue(getBoard().getRoute(pickedValue));
            }

            @Override
            public void onHighlighted(UIBoardRenderer b, APGraphics g, int highlightedIndex) {
                Route route = getBoard().getRoute(highlightedIndex);
                g.setColor(getPlayerColor(getCurPlayerNum()));
                switch (choice) {
                    case OPPONENT_ROAD_TO_ATTACK:
                        g.setColor(getPlayerColor(route.getPlayer()));
                        b.drawEdge(g, route, route.getType(), 0, true);
                        break;
                    case ROAD:
                    case ROUTE_DIPLOMAT:
                        b.drawRoad(g, route, true);
                        break;
                    case SHIP:
                        b.drawShip(g, route, true);
                        break;
                    case SHIP_TO_MOVE:
                        b.drawEdge(g, route, route.getType(), getCurPlayerNum(), true);
                        break;
                    case UPGRADE_SHIP:
                        b.drawWarShip(g, route, true);
                        break;
                }
            }

            @Override
            public void onDrawPickable(UIBoardRenderer b, APGraphics g, int index) {
                Route route = getBoard().getRoute(index);
                g.setColor(getPlayerColor(getCurPlayerNum()).withAlpha(120));
                switch (choice) {
                    case OPPONENT_ROAD_TO_ATTACK:
                        g.setColor(getPlayerColor(route.getPlayer()).withAlpha(120));
                        b.drawEdge(g, route, route.getType(), 0, false);
                        break;
                    case ROAD:
                    case ROUTE_DIPLOMAT:
                        b.drawRoad(g, route, false);
                        break;
                    case SHIP:
                        b.drawShip(g, route, false);
                        break;
                    case SHIP_TO_MOVE:
                        b.drawEdge(g, route, route.getType(), getCurPlayerNum(), true);
                        break;
                    case UPGRADE_SHIP:
                        b.drawWarShip(g, route, false);
                        break;
                }
            }

            @Override
            public void onDrawOverlay(UIBoardRenderer b, APGraphics g) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean isPickableIndex(UIBoardRenderer b, int index) {
                return edges.contains(index);
            }

            @Override
            public PickMode getPickMode() {
                return PickMode.PM_EDGE;
            }
        });
        completeMenu();
        return waitForReturnValue(null);
    }

    public Tile chooseTile(final Collection<Integer> tiles, final Player.TileChoice choice) {
        clearMenu();
        final Tile robberTile = getBoard().getRobberTile();
        final int merchantTileIndex = getBoard().getMerchantTileIndex();
        final int merchantTilePlayer = getBoard().getMerchantPlayer();
        getBoard().setRobber(-1);
        getBoard().setMerchant(-1, 0);
        getUIBoard().setPickHandler(new PickHandler() {

            @Override
            public void onPick(UIBoardRenderer b, int pickedValue) {
                b.setPickHandler(null);
                getBoard().setRobberTile(robberTile);
                getBoard().setMerchant(merchantTileIndex, merchantTilePlayer);
                setReturnValue(getBoard().getTile(pickedValue));
            }

            @Override
            public void onHighlighted(UIBoardRenderer b, APGraphics g, int highlightedIndex) {
                Tile t = getBoard().getTile(highlightedIndex);
                switch (choice) {
                    case INVENTOR:
                        g.setColor(GColor.YELLOW);
                        b.drawTileOutline(g, t, 4);
                        break;
                    case MERCHANT:
                        b.drawMerchant(g, t, getCurPlayerNum());
                        break;
                    case ROBBER:
                    case PIRATE:
                        if (t.isWater())
                            b.drawPirate(g, t);
                        else
                            b.drawRobber(g, t);
                        break;
                }
            }

            @Override
            public void onDrawPickable(UIBoardRenderer b, APGraphics g, int index) {
                Tile t = getBoard().getTile(index);
                //g.setColor(Color.RED);
                //bc.drawTileOutline(g, t, 1)
                MutableVector2D v = g.transform(t);
                g.setColor(GColor.RED);
                g.drawFilledCircle(v.Xi(), v.Yi(), UIBoardRenderer.TILE_CELL_NUM_RADIUS+10);
                if (t.getDieNum() > 0)
                    b.drawCellProductionValue(g, v.Xi(), v.Yi(), t.getDieNum(), UIBoardRenderer.TILE_CELL_NUM_RADIUS);//drawTileOutline(g, cell, borderThickness);
            }

            @Override
            public void onDrawOverlay(UIBoardRenderer b, APGraphics g) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean isPickableIndex(UIBoardRenderer b, int index) {
                return tiles.contains(index);
            }

            @Override
            public PickMode getPickMode() {
                return PickMode.PM_TILE;
            }
        });
        completeMenu();
        return waitForReturnValue(null);        

    }

    public final Trade chooseTradeMenu(Collection<Trade> trades){
        clearMenu();
        Iterator<Trade> it = trades.iterator();
        while (it.hasNext()) {
            Trade trade = it.next();
            String str = trade.getType().name() + " X " + trade.getAmount();
            addMenuItem(CHOOSE_TRADE, str, null, trade);
        }
        completeMenu();
        return waitForReturnValue(null);
    }

    public final Player choosePlayerMenu(Collection<Integer> players, Player.PlayerChoice mode) {
        clearMenu();
        for (int num : players) {
            Player player = getPlayerByPlayerNum(num);
            switch (mode) {
                case PLAYER_FOR_DESERTION: {
                    int numKnights = getBoard().getNumKnightsForPlayer(player.getPlayerNum());
                    addMenuItem(CHOOSE_PLAYER, player.getName() + " X " + numKnights + " Knights", null, player);
                    break;
                }
                case PLAYER_TO_SPY_ON:
                    addMenuItem(CHOOSE_PLAYER, player.getName() + " X " + player.getUnusedCardCount(CardType.Progress) + " Progress Cards", null, player);
                    break;
                default:
                    System.err.println("ERROR: Unhandled case '" + mode + "'");
                case PLAYER_TO_FORCE_HARBOR_TRADE:
                case PLAYER_TO_GIFT_CARD:
                case PLAYER_TO_TAKE_CARD_FROM:
                    addMenuItem(CHOOSE_PLAYER, player.getName() + " X " + player.getTotalCardsLeftInHand() + " Cards", null, player);
                    break;
            }
        }
        completeMenu();
        return waitForReturnValue(null);
    }

    public final Card chooseCardMenu(Collection<Card> cards) {
        clearMenu();
        for (Card type : cards) {
            addMenuItem(CHOOSE_CARD, type.getName(), type.getHelpText(getRules()), type);
        }
        completeMenu();
        return waitForReturnValue(null);
    }

    public final <T extends Enum<T>> T chooseEnum(List<T> choices) {
        clearMenu();
        Iterator<T> it = choices.iterator();
        while (it.hasNext()) {
            Enum<T> choice= it.next();
            addMenuItem(CHOOSE_MOVE, choice.name(), null, choice);
        }
        completeMenu();
        return waitForReturnValue(null);
    }

    @SuppressWarnings("unchecked")
    public <T> T waitForReturnValue(T defaultValue) {
        returnValue = defaultValue;
        Utils.waitNoThrow(waitObj, -1);
        return (T)returnValue;
    }

    protected abstract void addMenuItem(MenuItem item, String title, String helpText, Object extra);

    public abstract void clearMenu();

    public abstract void redraw();

    public BotNode chooseOptimalPath(BotNode optimal, List<BotNode> leafs) {
        return optimal;
    }

    protected final void addMenuItem(MenuItem item) {
        addMenuItem(item, item.title, item.helpText, null);
    }

    public void completeMenu() {
        if (canCancel()) {
            addMenuItem(CANCEL);
        }
    }

    @Override
    protected void onDiceRolled(Dice ... dice) {
        UIDiceRenderer dr = getRules().isEnableEventCards() ? eventCardRenderer.diceComps :diceRenderer;

        dr.spinDice(3000, dice);
        dr.setDice(getDice());
    }

    @Override
    protected void onEventCardDealt(EventCard card) {
        eventCardRenderer.setEventCard(card);
    }

    public boolean getSetDiceMenu(Dice[] die, int num) {
        clearMenu();
        diceRenderer.setDice(die);
        diceRenderer.setPickableDice(num);
        addMenuItem(SET_DICE);
        completeMenu();
        int [] result = (int[])waitForReturnValue(null);
        if (result != null) {
            for (int i=0; i<result.length; i++) {
                die[i].setNum(result[i]);
            }
            diceRenderer.setPickableDice(0);
            return true;
        }
        return false;
    }

    private boolean running = false;

    public final synchronized void startGameThread() {
        log.debug("Entering thread");
        assert(!running);
        running = true;
        diceRenderer.setDice(getDice());
        eventCardRenderer.setEventCard(getTopEventCard());
        barbarianRenderer.setDistance(getBarbarianDistance());
        new Thread() {
            @Override
            public void run() {
                try {
                    while (running) {
                        runGame();
                        if (running)
                            redraw();
                    }
                } catch (Throwable e) {
                    onRunError(e);
                }
                running = false;
                log.debug("Exiting thread");

            }
        }.start();
    }

    public final synchronized void stopRunning() {
        if (running) {
            running = false;
            notifyWaitObj();
        }
        setReturnValue(null);
    }

    protected void onRunError(Throwable e) {
        e.printStackTrace();
    }

    // in game options
    public final MenuItem CANCEL = new MenuItem("Cancel", "Cancel current operation", new MenuItem.Action() {
        @Override
        public void onAction(MenuItem item, Object extra) {
            boardRenderer.setPickHandler(null);
            cancel();
            notifyWaitObj();
        }
    });
    public final MenuItem CHOOSE_MOVE = new MenuItem("--", null, this);
    public final MenuItem CHOOSE_GIVEUP_CARD = new MenuItem("--", null, this);
    public final MenuItem CHOOSE_PLAYER = new MenuItem("--", null, this);
    public final MenuItem CHOOSE_CARD = new MenuItem("--", null, this);
    public final MenuItem CHOOSE_TRADE = new MenuItem("--", null, this);
    public final MenuItem CHOOSE_SHIP = new MenuItem("Ships", "Show ship choices", this);
    public final MenuItem CHOOSE_ROAD = new MenuItem("Roads", "Show road choices", this);
    public final MenuItem SET_DICE = new MenuItem("Set Dice", "Click the dice to set value manually", new MenuItem.Action() {
        @Override
        public void onAction(MenuItem item, Object extra) {
            returnValue = diceRenderer.getPickedDiceNums();
            notifyWaitObj();
        }
    });

    @Override
    public void onAction(MenuItem item, Object extra) {
        clearMenu();
        returnValue = extra;
        notifyWaitObj();
    }

    public final void notifyWaitObj() {
        synchronized (waitObj) {
            waitObj.notify();
        }
    }

    @Override
    public void printinfo(int playerNum, String txt) {
        super.printinfo(playerNum, txt);
        if (console != null)
            console.addText(getPlayerColor(playerNum), txt);
    }

    @Override
    protected void onBarbariansAdvanced(int distanceAway) {
        barbarianRenderer.setDistance(distanceAway);
    }

    @Override
    protected void onBarbariansAttack(int catanStrength, int barbarianStrength, String [] playerStatus) {
        StringBuffer str = new StringBuffer("Barbarian Attack!\n\nBarbarian Strength ")
                .append(barbarianStrength)
                .append("\nCatan Strength ")
                .append(catanStrength)
                .append("\n");
        for (Player p : getPlayers()) {
            str.append(p.getName()).append(" ").append(playerStatus[p.getPlayerNum()]).append("\n");
        }
        showOkPopup("Barbarian Attack", str.toString());
    }

    protected final void addCardAnimation(final Player player, final String text) {

        final UIPlayerRenderer comp = playerComponents[player.getPlayerNum()];
        final List<AAnimation<AGraphics>> cardsList = comp.animations;

        final float cardHeight = comp.component.getHeight()/5;
        final float cardWidth = cardHeight*2/3;

        final Vector2D compPt = comp.component.getViewportLocation();
        final Vector2D boardPt = boardRenderer.component.getViewportLocation();

        final Vector2D dv = compPt.sub(boardPt);

//        final int y = board.getY() + dy; // duh
        final float W = cardsList.size() * cardWidth + (cardsList.size()+1) * cardWidth/3;
//        final int x = board.getX() + dx > 0 ? board.getWidth() - W - cardWidth : W;
//        final int

        final int animTime = 3000;//GUI.instance.getProps().getIntProperty("anim.card.tm", 3000);

        final float x = dv.X() - W - cardWidth;
        final float y = dv.Y();

        boardRenderer.addAnimation(new AAnimation<AGraphics>(animTime) {
            public void draw(AGraphics g, float position, float dt) {
                boardRenderer.drawCard(((UIPlayer)player).getColor(), g, text, x, y, cardWidth, cardHeight);
            }

            @Override
            public void onDone() {
                //playerRowCardNum[player.getPlayerNum()]--;
                synchronized (cardsList) {
                    cardsList.remove(this);
                }
            }

            @Override
            public void onStarted() {
                synchronized (cardsList) {
                    cardsList.add(this);
                }
            }

        }, false);
    }

    @Override
    protected void onCardPicked(final Player player, final Card card) {
        String txt = "";
        if (((UIPlayer)player).isInfoVisible()) {
            Pattern splitter = Pattern.compile("[A-Z][a-z0-9]*");
            Matcher matcher = splitter.matcher(card.getName());
            while (matcher.find()) {
                if (txt.length() > 0) {
                    txt += " ";
                }
                txt += matcher.group();
            }
        }
        addCardAnimation(player, txt);
    }

    @Override
    protected void onDistributeResources(final Player player, final ResourceType type, final int amount) {
        addCardAnimation(player, type.name() + "\nX " + amount);
    }

    @Override
    protected void onDistributeCommodity(final Player player, final CommodityType type, final int amount) {
        addCardAnimation(player, type.name() + "\nX " + amount);
    }

    @Override
    protected void onProgressCardDistributed(Player player, ProgressCardType type) {
        String txt = type.name();
        if (!((UIPlayer)player).isInfoVisible()) {
            txt = "Progress";
        }
        addCardAnimation(player, txt);
    }

    @Override
    protected void onSpecialVictoryCard(Player player, SpecialVictoryType type) {
        addCardAnimation(player, type.name());
    }
/*
    @SuppressWarnings("serial")
    @Override
    protected void onGameOver(final Player winner) {
        PopupButton button = new PopupButton("OK") {
            public boolean doAction() {
                gui.quitToMainMenu();
                synchronized (this) {
                    notify();
                }
                return true;
            }
        };
        gui.showPopup("A WINNER!", "Player " + winner.getPlayerNum() + "\n Wins!", button);
        try {
            synchronized (button) {
                button.wait();
            }
        } catch (Exception e) {
            //gui.quitToMainMenu();
        }
    }
*/
    @Override
    protected void onLargestArmyPlayerUpdated(final Player oldPlayer, final Player newPlayer, final int armySize) {
        if (newPlayer != null)
            addCardAnimation(newPlayer, "Largest Army");
        if (oldPlayer != null)
            addCardAnimation(oldPlayer, "Largest Army Lost!");
    }

    @Override
    protected void onLongestRoadPlayerUpdated(final Player oldPlayer, final Player newPlayer, final int maxRoadLen) {
        if (newPlayer != null)
            addCardAnimation(newPlayer, "Longest Road");
        if (oldPlayer != null)
            addCardAnimation(oldPlayer, "Longest Road Lost!");
    }

    @Override
    protected void onHarborMasterPlayerUpdated(Player oldPlayer, Player newPlayer, int harborPts) {
        if (newPlayer != null)
            addCardAnimation(newPlayer, "Harbor Master");
        if (oldPlayer != null)
            addCardAnimation(oldPlayer, "Harbor Master Lost!");
    }

    @Override
    protected void onMonopolyCardApplied(final Player taker, final Player giver, final ICardType<?> type, final int amount) {
        addCardAnimation(giver, type.name() + "\n- " + amount);
        addCardAnimation(taker, type.name() + "\n+ " + amount);
    }

    @Override
    protected void onPlayerPointsChanged(final Player player, final int changeAmount) {
    }

    @Override
    protected void onTakeOpponentCard(final Player taker, final Player giver, final Card card) {
        addCardAnimation(giver, card.getName() + "\n-1");
        addCardAnimation(taker, card.getName() + "\n+1");
    }

    @Override
    protected void onPlayerRoadLengthChanged(Player p, int oldLen, int newLen) {
        if (oldLen > newLen)
            addCardAnimation(p, "Route Reduced!\n" + "-" + (oldLen - newLen));
        else
            addCardAnimation(p, "Route Increased!\n" + "+" + (newLen - oldLen));
    }

    @Override
    protected void onTradeCompleted(Player player, Trade trade) {
        addCardAnimation(player, "Trade\n" + trade.getType() + "\n -" + trade.getAmount());
    }

    @Override
    protected void onPlayerDiscoveredIsland(Player player, Island island) {
        addCardAnimation(player, "Island " + island.getNum() + "\nDiscovered!");
    }

    @Override
    protected void onDiscoverTerritory(Player player, Tile tile) {
        addCardAnimation(player, "Territory\nDiscovered");
    }


    @Override
    protected void onMetropolisStolen(Player loser, Player stealer, DevelopmentArea area) {
        addCardAnimation(loser, "Metropolis\n" + area.name() + "\nLost!");
        addCardAnimation(stealer, "Metropolis\n" + area.name() + "\nStolen!");
    }

    @Override
    protected void onTilesInvented(Player player, final Tile tile0, final Tile tile1) {
        boardRenderer.startTilesInventedAnimation(tile0, tile1);
    }

    @Override
    protected void onPlayerShipUpgraded(Player p, Route r) {
    }

    @Override
    protected void onPirateSailing(final int fromTile, final int toTile) {
        boardRenderer.addAnimation(new UIAnimation(800) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                Vector2D v = Vector2D.newTemp(getBoard().getTile(fromTile)).scaledBy(1-position).add(Vector2D.newTemp(getBoard().getTile(toTile)).scaledBy(position));
                boardRenderer.drawPirate(g, v);
            }

        }, true);
    }

    @Override
    protected void onCardLost(Player p, Card c) {
        addCardAnimation(p, c.getName() + "\n-1");
    }

    @Override
    protected void onPirateAttack(Player p, int playerStrength, int pirateStrength) {
        StringBuffer str = new StringBuffer("Pirates attack " + p.getName())
                .append("\nPlayer Strength " + playerStrength)
                .append("\nPirate Stength " + pirateStrength)
                .append("\n");
        showOkPopup("Pirate Attack", str.toString());
    }

    /**
     * Show a popup and block until a button is pressed and return the index of the button pressed
     *
     * @param title
     * @param message
     * @return
     */
    protected abstract void showOkPopup(String title, String message);

    @Override
    protected void onPlayerConqueredPirateFortress(Player p, Vertex v) {
        StringBuffer str = new StringBuffer("Player " + p.getName() + " has conquered the fortress!");
        showOkPopup("Pirate Attack", str.toString());
    }

    @Override
    protected void onPlayerAttacksPirateFortress(Player p, int playerHealth, int pirateHealth) {
        String result = null;
        if (playerHealth > pirateHealth)
            result = "Player damages the fortress";
        else if (playerHealth < pirateHealth)
            result = "Player loses battle and 2 ships";
        else
            result = "Battle is a draw.  Player lost a ship";
        StringBuffer str = new StringBuffer(p.getName() + " attackes the pirate fortress!")
                .append("\nPlayer Strength " + playerHealth)
                .append("\nPirate Stength " + pirateHealth)
                .append("\n")
                .append("Result: " + result + "\n");
        showOkPopup("Pirate Attack", str.toString());
    }

    @Override
    protected void onAqueduct(Player p) {
        addCardAnimation(p, "Aqueduct Ability!");
    }

    @Override
    protected void onPlayerAttackingOpponent(Player attacker, Player victim, String attackingWhat, int attackerScore, int victimScore) {
        String message = attacker.getName() + " is attacking " + victim.getName() + "'s " + attackingWhat + "\n"
                + attacker.getName() + "'s score : " + attackerScore + "\n"
                + victim.getName() + "'s score : " + victimScore;
        showOkPopup("Player Attack!", message);
    }

    @Override
    protected void onRoadDestroyed(Route r, Player destroyer, Player victim) {
        addCardAnimation(victim, "Road Destroyed!");
    }

    @Override
    protected void onStructureDemoted(Vertex v, VertexType newType, Player destroyer, Player victim) {
        addCardAnimation(victim, v.getType().getNiceName() + " Reduced to " + newType.getNiceName());
    }

//    @Override
//    protected void onShouldSaveGame() {
//        FileUtils.backupFile(gui.saveGameFile.getAbsolutePath(), 10);
//        save(gui.saveGameFile.getAbsolutePath());
//    }

    @Override
    protected void onExplorerPlayerUpdated(Player oldPlayer, Player newPlayer, int harborPts) {
        if (oldPlayer != null)
            addCardAnimation(oldPlayer, "Explorer Lost!");
        addCardAnimation(newPlayer, "Explorer Gained!");
    }

    @Override
    protected void onPlayerKnightDestroyed(Player player, Vertex knight) {
        addFloatingTextAnimation((UIPlayer)player, knight, "Knight\nDestroyed");
    }

    @Override
    protected void onPlayerKnightDemoted(Player player, Vertex knight) {
        addFloatingTextAnimation((UIPlayer)player, knight, "Demoted to\n" + knight.getType().getNiceName());
    }

    void addFloatingTextAnimation(final UIPlayer p, final IVector2D v, final String msg) {
        boardRenderer.addAnimation(new UIAnimation(2000) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                g.setColor(p.getColor());
                g.pushMatrix();
                g.translate(v);
                g.translate(0, boardRenderer.getKnightRadius()*5);
                g.translate(0, boardRenderer.getKnightRadius()*10*position);
                MutableVector2D mv = new MutableVector2D();
                g.transform(mv);
                g.drawJustifiedString(mv.Xi(), mv.Yi(), Justify.CENTER, Justify.CENTER, msg);
                g.popMatrix();
            }

        }, true);

    }

    @Override
    protected void onPlayerKnightPromoted(Player player, final Vertex knight) {
        addFloatingTextAnimation((UIPlayer)player, knight, "Promoted to\n" + knight.getType().getNiceName());
    }

    @Override
    protected void onPlayerCityDeveloped(Player p, DevelopmentArea area) {
        addCardAnimation(p, area.name() + "\n\n" + area.levelName[p.getCityDevelopment(area)]);
    }

    @Override
    protected void onRoadDamaged(Route r, Player destroyer, Player victim) {
        super.onRoadDamaged(r, destroyer, victim);
    }

    @Override
    protected void onShouldSaveGame() {
        super.onShouldSaveGame();
    }

    @Override
    protected void onPlayerShipComandeered(Player taker, Route shipTaken) {
        super.onPlayerShipComandeered(taker, shipTaken);
    }

    @Override
    protected void onPlayerShipDestroyed(Route r) {
        super.onPlayerShipDestroyed(r);
    }


}
