package cc.game.soc.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.game.soc.core.Board;
import cc.game.soc.core.BotNode;
import cc.game.soc.core.Card;
import cc.game.soc.core.CardType;
import cc.game.soc.core.CommodityType;
import cc.game.soc.core.DevelopmentArea;
import cc.game.soc.core.Dice;
import cc.game.soc.core.EventCard;
import cc.game.soc.core.ICardType;
import cc.game.soc.core.ILocalized;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.Player;
import cc.game.soc.core.ProgressCardType;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.Route;
import cc.game.soc.core.RouteType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SpecialVictoryType;
import cc.game.soc.core.Tile;
import cc.game.soc.core.Trade;
import cc.game.soc.core.Vertex;
import cc.game.soc.core.VertexType;
import cc.lib.annotation.Keep;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Vector2D;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;
import cc.lib.net.GameServer;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 2/22/18.
 */

public abstract class UISOC extends SOC implements MenuItem.Action, GameServer.Listener, ClientConnection.Listener {

    private static Logger log = LoggerFactory.getLogger(UISOC.class);

    private static UISOC instance = null;

    public final GameServer server = new GameServer(getServerName(), NetCommon.PORT, NetCommon.VERSION, NetCommon.getCypher(), NetCommon.MAX_CONNECTIONS);

    private final UIPlayerRenderer [] playerComponents;
    private final UIBoardRenderer boardRenderer;
    private final UIDiceRenderer diceRenderer;
    private final UIConsoleRenderer console;
    private final UIEventCardRenderer eventCardRenderer;
    private final UIBarbarianRenderer barbarianRenderer;
    private Object returnValue = null;
    private final Object waitObj = new Object();

    protected UISOC(UIPlayerRenderer [] playerComponents, UIBoardRenderer boardRenderer, UIDiceRenderer diceRenderer, UIConsoleRenderer console, UIEventCardRenderer eventCardRenderer, UIBarbarianRenderer barbarianRenderer) {
        instance = this;
        server.addListener(this);
        this.playerComponents = playerComponents;
        this.boardRenderer = boardRenderer;
        this.diceRenderer = diceRenderer;
        this.eventCardRenderer = eventCardRenderer;
        this.barbarianRenderer = barbarianRenderer;
        this.console = console;
        MAX_PLAYERS = playerComponents.length;

        CANCEL = new MenuItem(getString("Cancel"), getString("Cancel current operation"), this);
        ACCEPT = new MenuItem(getString("Accept"), null, this);
        CHOOSE_MOVE = new MenuItem("--", null, this);
        CHOOSE_PLAYER = new MenuItem("--", null, this);
        CHOOSE_CARD = new MenuItem("--", null, this);
        CHOOSE_TRADE = new MenuItem("--", null, this);
        CHOOSE_SHIP = new MenuItem(getString("Ships"), getString("Show ship choices"), this);
        CHOOSE_ROAD = new MenuItem(getString("Roads"), getString("Show road choices"), this);
        SET_DICE = new MenuItem(getString("Set Dice"), getString("Click the dice to set value manually"), this);

    }

    public static UISOC getInstance() {
        return instance;
    }

    public UIBoardRenderer getUIBoard() {
        return boardRenderer;
    }

    @Keep
    public void setReturnValue(Object o) {
        returnValue = o;
        synchronized (waitObj) {
            waitObj.notify();
        }
    }

    public final boolean isRunning() {
        return running;
    }

    private SOC copy = null;

    @Override
    public void runGame() {
        if (server.isConnected()) {
            if (copy == null) {
                copy = new SOC();
                copy.copyFrom(this);
            }
            completeMenu();
            super.runGame();
            try {
                String diff = copy.diff(this);
                if (!diff.isEmpty()) {
                    server.broadcastCommand(new GameCommand(NetCommon.SVR_TO_CL_UPDATE).setArg("diff", diff));
                    copy.merge(diff);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            super.runGame();
        }
    }

    public final MoveType chooseMoveMenu(Collection<MoveType> moves) {
        clearMenu();
        Iterator<MoveType> it = moves.iterator();
        while (it.hasNext()) {
            MoveType move = it.next();
            addMenuItem(CHOOSE_MOVE, move.getName(), move.getHelpText(getRules()), move);
        }
        completeMenu();
        return waitForReturnValue(null);
    }

    protected final Player.RouteChoiceType chooseRouteType(){
        clearMenu();
        addMenuItem(CHOOSE_ROAD, Player.RouteChoiceType.ROAD_CHOICE);
        addMenuItem(CHOOSE_SHIP, Player.RouteChoiceType.SHIP_CHOICE);
        completeMenu();
        return waitForReturnValue(null);
    }

    public GColor getPlayerColor(int playerNum) {
        UIPlayer p = (UIPlayer)getPlayerByPlayerNum(playerNum);
        if (p != null)
            return p.getColor();
        return GColor.DARK_GRAY;
    }

    public Integer chooseVertex(final Collection<Integer> vertexIndices, final Player.VertexChoice choice, final Integer knightToMove) {
        clearMenu();
        addMenuItem(ACCEPT);
        getUIBoard().setPickHandler(new PickHandler() {

            @Override
            public void onPick(UIBoardRenderer b, int pickedValue) {
                b.setPickHandler(null);
                setReturnValue(pickedValue);
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
                    case OPPONENT_KNIGHT_TO_DISPLACE: {
                        int knightLevel = v.getType().getKnightLevel();
                        boolean active = v.getType().isKnightActive();
                        if (knightToMove != null) {
                            Vertex vv = getBoard().getVertex(knightToMove);
                            knightLevel = vv.getType().getKnightLevel();
                            active = vv.getType().isKnightActive();
                        }
                        b.drawKnight(g, v, v.getPlayer(), knightLevel, active, false);
                        g.setColor(GColor.RED);
                        b.drawCircle(g, v);
                        break;
                    }
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
                GColor color = getPlayerColor(getCurPlayerNum()).withAlpha(RenderConstants.pickableAlpha);
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
                    case OPPONENT_KNIGHT_TO_DISPLACE: {
                        int knightLevel = v.getType().getKnightLevel();
                        boolean active = v.getType().isKnightActive();
                        if (knightToMove != null) {
                            Vertex vv = getBoard().getVertex(knightToMove);
                            knightLevel = vv.getType().getKnightLevel();
                            active = vv.getType().isKnightActive();
                        }
                        b.drawKnight(g, v, 0, knightLevel, active, false);
                        g.setColor(GColor.YELLOW);
                        b.drawCircle(g, v);
                        g.setColor(color);
                        break;
                    }
                    case NEW_KNIGHT:
                        b.drawKnight(g, v, 0, 1, false, false);
                        break;
                    case KNIGHT_TO_MOVE:
                    case KNIGHT_TO_ACTIVATE:
                    case KNIGHT_TO_PROMOTE:
                        b.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel(), v.getType().isKnightActive(), false);
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
                        g.setColor(getPlayerColor(v.getPlayer()).withAlpha(RenderConstants.pickableAlpha));
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

    public Integer chooseRoute(final Collection<Integer> edges, final Player.RouteChoice choice, final Route shipToMove) {
        clearMenu();
        addMenuItem(ACCEPT);
        final RouteType shipType = shipToMove == null ? RouteType.SHIP : shipToMove.getType();
        assert(shipType.isVessel);
        if (shipToMove != null) {
            shipToMove.setType(RouteType.OPEN);
        }
        getUIBoard().setPickHandler(new PickHandler() {

            @Override
            public void onPick(UIBoardRenderer b, int pickedValue) {
                b.setPickHandler(null);
                if (shipToMove != null) {
                    shipToMove.setType(shipType);
                }
                setReturnValue(pickedValue);
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
                        b.drawVessel(g, shipType, route, true);
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
                g.setColor(getPlayerColor(getCurPlayerNum()).withAlpha(RenderConstants.pickableAlpha));
                switch (choice) {
                    case OPPONENT_ROAD_TO_ATTACK:
                        g.setColor(getPlayerColor(route.getPlayer()).withAlpha(RenderConstants.pickableAlpha));
                        b.drawEdge(g, route, route.getType(), 0, false);
                        break;
                    case ROAD:
                    case ROUTE_DIPLOMAT:
                        b.drawRoad(g, route, false);
                        break;
                    case SHIP:
                        b.drawVessel(g, shipType, route, false);
                        break;
                    case SHIP_TO_MOVE:
                        b.drawEdge(g, route, route.getType(), 0, false);
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

    public Integer chooseTile(final Collection<Integer> tiles, final Player.TileChoice choice) {
        clearMenu();
        addMenuItem(ACCEPT);
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
                setReturnValue(pickedValue);
            }

            @Override
            public void onHighlighted(UIBoardRenderer b, APGraphics g, int highlightedIndex) {
                Tile t = getBoard().getTile(highlightedIndex);
                switch (choice) {
                    case INVENTOR:
                        g.setColor(GColor.YELLOW);
                        b.drawTileOutline(g, t, RenderConstants.thickLineThickness);
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
                switch (choice) {

                    case ROBBER:
                    case PIRATE:
                        if (t.isWater())
                            b.drawPirate(g, t, GColor.TRANSLUSCENT_BLACK);
                        else
                            b.drawRobber(g, t, GColor.LIGHT_GRAY.withAlpha(RenderConstants.pickableAlpha));
                        break;
                    case INVENTOR:
                        g.setColor(GColor.RED);
                        g.drawFilledCircle(t.getX(), t.getY(), g.getTextHeight()*2+10);
                        if (t.getDieNum() > 0)
                            b.drawCellProductionValue(g, t.getX(), t.getY(), t.getDieNum());
                        break;
                    case MERCHANT:
                        g.setColor(getPlayerColor(getCurPlayerNum()).withAlpha(RenderConstants.pickableAlpha));
                        b.drawMerchant(g, t, 0);
                        break;
                }
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
            String str = trade.getType().getName() + " X " + trade.getAmount();
            addMenuItem(CHOOSE_TRADE, str, null, trade);
        }
        completeMenu();
        return waitForReturnValue(null);
    }

    public final Integer choosePlayerMenu(Collection<Integer> players, Player.PlayerChoice mode) {
        clearMenu();
        for (int num : players) {
            Player player = getPlayerByPlayerNum(num);
            switch (mode) {
                case PLAYER_FOR_DESERTION: {
                    int numKnights = getBoard().getNumKnightsForPlayer(player.getPlayerNum());
                    addMenuItem(CHOOSE_PLAYER, getString("%1$s X %2$d Knights", player.getName(), numKnights), null, num);
                    break;
                }
                case PLAYER_TO_SPY_ON:
                    addMenuItem(CHOOSE_PLAYER, getString("%1$s X %2$d Progress Cards", player.getName(), player.getUnusedCardCount(CardType.Progress)), null, num);
                    break;
                default:
                    System.err.println("ERROR: Unhandled case '" + mode + "'");
                case PLAYER_TO_FORCE_HARBOR_TRADE:
                case PLAYER_TO_GIFT_CARD:
                case PLAYER_TO_TAKE_CARD_FROM:
                    addMenuItem(CHOOSE_PLAYER, getString("%1$s X %2$d Cards", player.getName(), player.getTotalCardsLeftInHand()), null, num);
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
            addMenuItem(CHOOSE_MOVE, ((ILocalized)choice).getName(), null, choice);
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

    protected abstract String getServerName();

    protected abstract void addMenuItem(MenuItem item, String title, String helpText, Object extra);

    public abstract void clearMenu();

    public abstract void redraw();

    public void refreshComponents() {
        UIDiceRenderer r = getDiceRenderer();
        r.setDice(getDice());
        eventCardRenderer.setEventCard(getTopEventCard());
        barbarianRenderer.setDistance(getBarbarianDistance());
    }

    public BotNode chooseOptimalPath(BotNode optimal, List<BotNode> leafs) {
        return optimal;
    }

    public final void addMenuItem(MenuItem item) {
        addMenuItem(item, item.title, item.helpText, null);
    }

    public final void addMenuItem(MenuItem item, Object extra) {
        addMenuItem(item, item.title, item.helpText, extra);
    }

    public void completeMenu() {
        if (canCancel()) {
            addMenuItem(CANCEL);
        }
    }

    UIDiceRenderer getDiceRenderer() {
        return getRules().isEnableEventCards() ? eventCardRenderer.diceComps :diceRenderer;
    }

    public boolean getSetDiceMenu(List<Dice> die, int num) {
        clearMenu();
        UIDiceRenderer r = getDiceRenderer();
        r.setDice(die);
        r.setPickableDice(num);
        addMenuItem(SET_DICE);
        completeMenu();
        int [] result = (int[])waitForReturnValue(null);
        if (result != null) {
            for (int i=0; i<result.length; i++) {
                die.get(i).setNum(result[i], true);
            }
            r.setPickableDice(0);
            return true;
        }
        return false;
    }

    private boolean running = false;

    public final synchronized void startGameThread() {
        log.debug("Entering thread");
        assert(!running);
        running = true;
        copy = null;
        refreshComponents();
        new Thread() {
            @Override
            public void run() {
                try {
                    while (running) {
                        long enterTime = System.currentTimeMillis();
                        runGame();
                        if (running) {
                            redraw();
                            long exitTime = System.currentTimeMillis();
                            //make sure we take at least 1 second in between rungame calls
                            int dt = (int)(exitTime - enterTime);
                            if (dt > 0 && dt < 1000) {
                                synchronized (waitObj) {
                                    waitObj.wait(dt);
                                }
                            }
                        }
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
        boardRenderer.reset();
        console.clear();
    }

    protected void onRunError(Throwable e) {
        e.printStackTrace();
    }

    // in game options
    public final MenuItem CANCEL;
    public final MenuItem ACCEPT;
    public final MenuItem CHOOSE_MOVE;
    public final MenuItem CHOOSE_PLAYER;
    public final MenuItem CHOOSE_CARD;
    public final MenuItem CHOOSE_TRADE;
    public final MenuItem CHOOSE_SHIP;
    public final MenuItem CHOOSE_ROAD;
    public final MenuItem SET_DICE;

    @Override
    public void onAction(MenuItem item, Object extra) {
        if (item == CANCEL) {
            boardRenderer.setPickHandler(null);
            cancel();
            notifyWaitObj();
        } else if (item == SET_DICE) {
            returnValue = getDiceRenderer().getPickedDiceNums();
            notifyWaitObj();
        } else if (item == ACCEPT) {
            if (boardRenderer.isPicked()) {
                clearMenu();
                boardRenderer.acceptPicked();
            }
        } else {
            clearMenu();
            returnValue = extra;
            notifyWaitObj();
        }
    }

    public final void notifyWaitObj() {
        synchronized (waitObj) {
            waitObj.notify();
        }
    }

    @Override
    @Keep
    public void printinfo(int playerNum, String txt) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, txt);
        super.printinfo(playerNum, txt);
        if (console != null)
            console.addText(getPlayerColor(playerNum), txt);
    }

    @Override
    @Keep
    protected void onDiceRolled(List<Dice> dice) {
        log.debug("onDiceRolled: " + dice);
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, dice);
        UIDiceRenderer dr = getDiceRenderer();
        List<Dice> copy = Reflector.deepCopy(dice);
        dr.spinDice(3000, copy);
        log.debug("Set dice: " + dice);
        dr.setDice(dice);
        super.onDiceRolled(dice);
    }

    @Override
    @Keep
    protected void onEventCardDealt(EventCard card) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, card);
        eventCardRenderer.setEventCard(card);
        super.onEventCardDealt(card);
    }

    @Override
    @Keep
    protected void onBarbariansAdvanced(int distanceAway) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, distanceAway);
        barbarianRenderer.setDistance(distanceAway);
        super.onBarbariansAdvanced(distanceAway);
    }

    @Override
    @Keep
    protected void onBarbariansAttack(int catanStrength, int barbarianStrength, String [] playerStatus) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, catanStrength, barbarianStrength, playerStatus);
        barbarianRenderer.onBarbarianAttack(catanStrength, barbarianStrength, playerStatus);
        StringBuffer str = new StringBuffer(getString("Barbarian Attack!\n\nBarbarian Strength %1$d\nCatan Strength %2$d\n", barbarianStrength, catanStrength));
        for (Player p : getPlayers()) {
            str.append(p.getName()).append(" ").append(playerStatus[p.getPlayerNum()]).append("\n");
        }
        showOkPopup(getString("Barbarian Attack"), str.toString());
        super.onBarbariansAttack(catanStrength, barbarianStrength, playerStatus);
        boardRenderer.getComponent().redraw();
    }

    private UIPlayerRenderer getRendererForPlayerNum(int playerNum) {
        for (UIPlayerRenderer pr : playerComponents) {
            if (pr.playerNum == playerNum)
                return pr;
        }
        log.error("No player component for playerNum: %d", playerNum);
        return playerComponents[0];
    }

    protected final void addCardAnimation(final int playerNum, final String text) {

        final UIPlayerRenderer comp = getRendererForPlayerNum(playerNum);

        final float cardHeight = boardRenderer.getComponent().getHeight()/5;
        final float cardWidth = cardHeight*2/3;

        final Vector2D compPt = comp.getComponent().getViewportLocation();
        final Vector2D boardPt = boardRenderer.getComponent().getViewportLocation();

        final Vector2D dv = compPt.sub(boardPt);
        final float spacing = cardWidth/4;

        // center the card vertically against its player getComponent()
        float _y = compPt.getY() - boardPt.getY() + comp.getComponent().getHeight()/2 - cardHeight/2;
        final float W = comp.numCardAnimations * cardWidth + spacing - (comp.numCardAnimations-1) * spacing;
        final float x = boardPt.X() < compPt.getX() ? boardRenderer.getComponent().getWidth() - cardWidth - W : W;

        if (_y < 0) {
            _y = 0;
        } else if (_y + cardHeight > boardRenderer.getComponent().getHeight()) {
            _y = boardRenderer.getComponent().getHeight() - cardHeight;
        }

        final float y = _y;
        final GColor color = ((UIPlayer)getPlayerByPlayerNum(playerNum)).getColor();

        comp.numCardAnimations++;
        boardRenderer.addAnimation(true, new AAnimation<AGraphics>(4000) {
            public void draw(AGraphics g, float position, float dt) {
                float alpha = 1;
                if (getElapsedTime() < 500) {
                    alpha = (float)getElapsedTime()/500;
                } else if (getDuration() > 3500) {
                    alpha = (float)(4000-getElapsedTime())/500;
                }
                boardRenderer.drawCard(color, g, text, x, y, cardWidth, cardHeight, alpha);
            }

            @Override
            public void onDone() {
                comp.numCardAnimations--;
            }

        }, false);
        Utils.waitNoThrow(this, 500);
    }

    @Override
    @Keep
    protected void onCardPicked(final int playerNum, final Card card) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, card);
        String txt = card.getCardType().getName();
        Player player = getPlayerByPlayerNum(playerNum);
        if (((UIPlayer)player).isInfoVisible()) {
            txt = card.getName();
        }
        addCardAnimation(playerNum, txt);
        super.onCardPicked(playerNum, card);
    }

    @Override
    @Keep
    protected void onDistributeResources(int player, final ResourceType type, final int amount) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, type, amount);
        addCardAnimation(player, type.getName() + "\nX " + amount);
        super.onDistributeResources(player, type, amount);
    }

    @Override
    @Keep
    protected void onDistributeCommodity(final int player, final CommodityType type, final int amount) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, type, amount);
        addCardAnimation(player, type.getName() + "\nX " + amount);
        super.onDistributeCommodity(player, type, amount);
    }

    @Override
    @Keep
    protected void onProgressCardDistributed(int player, ProgressCardType type) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, type);
        String txt = type.getName();
        if (!((UIPlayer)getPlayerByPlayerNum(player)).isInfoVisible()) {
            txt = CardType.Progress.getName();
        }
        addCardAnimation(player, txt);
        super.onProgressCardDistributed(player, type);
    }

    @Override
    @Keep
    protected void onSpecialVictoryCard(int player, SpecialVictoryType type) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, type);
        addCardAnimation(player, type.getName());
        super.onSpecialVictoryCard(player, type);
    }

    @Override
    @Keep
    protected void onLargestArmyPlayerUpdated(final int oldPlayer, final int newPlayer, final int armySize) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, oldPlayer, newPlayer, armySize);
        if (newPlayer > 0)
            addCardAnimation(newPlayer, getString("Largest Army"));
        if (oldPlayer > 0)
            addCardAnimation(oldPlayer, getString("Largest Army Lost!"));
        super.onLargestArmyPlayerUpdated(oldPlayer, newPlayer, armySize);
    }

    @Override
    @Keep
    protected void onLongestRoadPlayerUpdated(final int oldPlayer, final int newPlayer, final int maxRoadLen) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, oldPlayer, newPlayer, maxRoadLen);
        if (newPlayer > 0)
            addCardAnimation(newPlayer, getString("Longest Road"));
        if (oldPlayer > 0)
            addCardAnimation(oldPlayer, getString("Longest Road Lost!"));
        super.onLongestRoadPlayerUpdated(oldPlayer, newPlayer, maxRoadLen);
    }

    @Override
    @Keep
    protected void onHarborMasterPlayerUpdated(int oldPlayer, int newPlayer, int harborPts) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, oldPlayer, newPlayer, harborPts);
        if (newPlayer > 0)
            addCardAnimation(newPlayer, getString("Harbor Master"));
        if (oldPlayer > 0)
            addCardAnimation(oldPlayer, getString("Harbor Master Lost!"));
        super.onHarborMasterPlayerUpdated(oldPlayer, newPlayer, harborPts);
    }

    @Override
    @Keep
    protected void onMonopolyCardApplied(final int taker, final int giver, final ICardType<?> type, final int amount) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, taker, giver, type, amount);
        addCardAnimation(giver, type.getName() + "\n- " + amount);
        addCardAnimation(taker, type.getName() + "\n+ " + amount);
        super.onMonopolyCardApplied(taker, giver, type, amount);
    }

    @Override
    @Keep
    protected void onPlayerPointsChanged(final int player, final int changeAmount) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, changeAmount);
        super.onPlayerPointsChanged(player, changeAmount);
    }

    @Override
    @Keep
    protected void onTakeOpponentCard(final int taker, final int giver, final Card card) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, taker, giver, card);
        addCardAnimation(giver, card.getName() + "\n-1");
        addCardAnimation(taker, card.getName() + "\n+1");
        super.onTakeOpponentCard(taker, giver, card);
    }

    @Override
    @Keep
    protected void onPlayerRoadLengthChanged(int p, int oldLen, int newLen) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, p, oldLen, newLen);
        if (oldLen > newLen)
            addCardAnimation(p, getString("Route Reduced!\n-%d", oldLen - newLen));
        else
            addCardAnimation(p, getString("Route Increased!\n+%d", newLen - oldLen));
        super.onPlayerRoadLengthChanged(p, oldLen, newLen);
    }

    @Override
    @Keep
    protected void onCardsTraded(int player, Trade trade) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, trade);
        addCardAnimation(player, getString("Trade\n %1$s\n -%2$d", trade.getType() , trade.getAmount()));
        super.onCardsTraded(player, trade);
    }

    @Override
    @Keep
    protected void onPlayerDiscoveredIsland(int player, int island) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, island);
        addCardAnimation(player, getString("Island %s\nDiscovered!", island));
        super.onPlayerDiscoveredIsland(player, island);
    }

    @Override
    @Keep
    protected void onDiscoverTerritory(int player, int tile) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, tile);
        addCardAnimation(player, getString("Territory\nDiscovered"));
        boardRenderer.clearCached();
        super.onDiscoverTerritory(player, tile);
    }


    @Override
    @Keep
    protected void onMetropolisStolen(int loser, int stealer, DevelopmentArea area) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, loser, stealer, area);
        addCardAnimation(loser, getString("Metropolis\n%s\nLost!", area.getName()));
        addCardAnimation(stealer, getString("Metropolis\n%s\nLost!", area.getName()));
        super.onMetropolisStolen(loser, stealer, area);
    }

    @Override
    @Keep
    protected void onTilesInvented(int player, final int tile0, final int tile1) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, tile0, tile1);
        boardRenderer.clearCached();
        boardRenderer.startTilesInventedAnimation(getBoard().getTile(tile0), getBoard().getTile(tile1));
        boardRenderer.clearCached();
        super.onTilesInvented(player, tile0, tile1);
    }

    @Override
    @Keep
    protected void onPlayerShipUpgraded(int playerNum, int routeIndex) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, routeIndex);
        super.onPlayerShipUpgraded(playerNum, routeIndex);
    }

    @Override
    @Keep
    protected void onPirateSailing(final int fromTile, final int toTile) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, fromTile, toTile);
        boardRenderer.addAnimation(new UIAnimation(800) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                Vector2D v = Vector2D.newTemp(getBoard().getTile(fromTile)).scaledBy(1-position).add(Vector2D.newTemp(getBoard().getTile(toTile)).scaledBy(position));
                boardRenderer.drawPirate(g, v);
            }

        }, true);
        super.onPirateSailing(fromTile, toTile);
    }

    @Override
    @Keep
    protected void onCardLost(int playerNum, Card c) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, c);
        addCardAnimation(playerNum, c.getName() + "\n-1");
        super.onCardLost(playerNum, c);
    }

    @Override
    @Keep
    protected void onPirateAttack(int playerNum, int playerStrength, int pirateStrength) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, playerStrength, pirateStrength);
        showOkPopup(getString("Pirate Attack"), getString("Pirates attack %1$s\nPlayer Strength %2$d\nPirate Strength %3$d\n", getPlayerByPlayerNum(playerNum).getName(), playerStrength, pirateStrength));
        super.onPirateAttack(playerNum, playerStrength,pirateStrength);
    }

    /**
     * Show a popup and block until a button is pressed and return the index of the button pressed
     *
     * @param title
     * @param message
     * @return
     */
    protected abstract void showOkPopup(String title, String message);

    protected abstract String showChoicePopup(String title, List<String> choices);

    @Override
    @Keep
    protected void onPlayerConqueredPirateFortress(int p, int v) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, p, v);
        showOkPopup(getString("Pirate Attack"), getString("Player %s has conquered the fortress!", getPlayerByPlayerNum(p).getName()));
        super.onPlayerConqueredPirateFortress(p, v);
    }

    @Override
    @Keep
    protected void onPlayerAttacksPirateFortress(int p, int playerHealth, int pirateHealth) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, p, playerHealth, pirateHealth);
        String result = null;
        if (playerHealth > pirateHealth)
            result = getString("Player damages the fortress");
        else if (playerHealth < pirateHealth)
            result = getString("Player loses battle and 2 ships");
        else
            result = getString("Battle is a draw.  Player lost a ship");
        showOkPopup(getString("Pirate Attack"), getString("%1$s attacks the pirate fortress!\nPlayer Strength %2$d\nPirate Strength %3$d\nResult:\n%4$s",
                getPlayerByPlayerNum(p).getName(), playerHealth, pirateHealth, result));
        super.onPlayerAttacksPirateFortress(p, playerHealth,pirateHealth);
    }

    @Override
    @Keep
    protected void onAqueduct(int playerNum) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum);
        addCardAnimation(playerNum, getString("Aqueduct Ability!"));
        super.onAqueduct(playerNum);
    }

    @Override
    @Keep
    protected void onPlayerAttackingOpponent(int attackerNum, int victimNum, String attackingWhat, int attackerScore, int victimScore) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, attackerNum, victimNum, attackingWhat, attackerScore, victimNum);
        Player attacker = getPlayerByPlayerNum(attackerNum);
        Player victim = getPlayerByPlayerNum(victimNum);
        showOkPopup(getString("Player Attack!"), getString("%1$s is attacking %2$s's %3$s\n%4$s's score : %5$d\n%6$s's score : %7$d", attacker.getName(), victim.getName(), attackingWhat, attacker.getName(), attackerScore, victim.getName(), victimScore));
        super.onPlayerAttackingOpponent(attackerNum, victimNum, attackingWhat, attackerScore, victimScore);
    }

    @Override
    @Keep
    protected void onRoadDestroyed(int rIndex, int destroyer, int victim) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, rIndex, destroyer, victim);
        addCardAnimation(victim, getString("Road Destroyed"));
        super.onRoadDestroyed(rIndex, destroyer, victim);
    }

    @Override
    @Keep
    protected void onStructureDemoted(int vIndex, VertexType newType, int destroyer, int victim) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, vIndex, newType, destroyer, victim);
        addCardAnimation(victim, getString("%1$s Reduced to %2$s", getBoard().getVertex(vIndex).getType().getName(), newType.getName()));
        super.onStructureDemoted(vIndex, newType, destroyer, victim);
    }

    @Override
    @Keep
    protected void onExplorerPlayerUpdated(int oldPlayer, int newPlayer, int harborPts) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, oldPlayer, newPlayer, harborPts);
        if (oldPlayer > 0)
            addCardAnimation(oldPlayer, getString("Explorer Lost!"));
        addCardAnimation(newPlayer, getString("Explorer Gained!"));
        super.onExplorerPlayerUpdated(oldPlayer, newPlayer, harborPts);
    }

    @Override
    @Keep
    protected void onPlayerKnightDestroyed(int player, int knight) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, knight);
        addFloatingTextAnimation((UIPlayer)getPlayerByPlayerNum(player), getBoard().getVertex(knight), getString("Knight\nDestroyed"));
        super.onPlayerKnightDestroyed(player, knight);
    }

    @Override
    @Keep
    protected void onPlayerKnightDemoted(int player, int knightIndex) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, knightIndex);
        Vertex knight = getBoard().getVertex(knightIndex);
        addFloatingTextAnimation((UIPlayer)getPlayerByPlayerNum(player), knight, getString("Demoted to\n%s", knight.getType().getName()));
        super.onPlayerKnightDemoted(player, knightIndex);
    }

    void addFloatingTextAnimation(final UIPlayer p, final IVector2D v, final String msg) {
        boardRenderer.addAnimation(new UIAnimation(5000) {

            GDimension dim = null;

            @Override
            public void draw(AGraphics g, float position, float dt) {

                final float width = boardRenderer.getComponent().getWidth();
                final float height = boardRenderer.getComponent().getHeight();
                final float margin = RenderConstants.textMargin;
                final float m2 = margin*2;

                if (dim == null) {
                    dim = g.getTextDimension(msg, width);
                }

                // draw a rectangle in either upper half of screen or lower half of screen
                // with an arrow pointing to the vertex to be modified.

                Vector2D mv = g.transform(v);

                g.pushMatrix();
                g.setIdentity();
                g.setColor(GColor.LIGHT_GRAY);
                if (mv.getY() < height/2) {
                    // lower half
                    g.drawFilledRect(width/2-dim.width/2-margin, height*2/3, dim.width+m2, dim.height+m2);
                    // arrow
                    g.begin();
                    g.vertex(width/2-margin, height*2/3);
                    g.vertex(width/2+margin, height*2/3);
                    g.vertex(mv);
                    g.drawTriangles();
                    g.end();
                    g.translate(width/2, height*2/3+margin);
                } else {
                    // upper half
                    float hgt = height/3-dim.height-m2;
                    g.drawFilledRect(width/2-dim.width/2-margin, hgt, dim.width+m2, dim.height+m2);
                    // arrow
                    g.begin();
                    g.vertex(width/2-margin, hgt+g.getTextHeight());
                    g.vertex(width/2+margin, hgt+g.getTextHeight());
                    g.vertex(mv);
                    g.drawTriangles();
                    g.end();
                    g.translate(width/2, height/3-dim.height-margin);
                }

                if (getTimeRemaining() < 1000) {
                    g.setColor(p.getColor().withAlpha(0.001f * getTimeRemaining()));
                } else {
                    g.setColor(p.getColor());
                }
                g.drawJustifiedString(0, 0, Justify.CENTER, Justify.TOP, msg);
                g.popMatrix();
            }

        }, false);

    }

    @Override
    @Keep
    protected void onPlayerKnightPromoted(int player, final int knightIndex) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, knightIndex);
        Vertex knight = getBoard().getVertex(knightIndex);
        addFloatingTextAnimation((UIPlayer)getPlayerByPlayerNum(player), knight, getString("Promoted to\n%s", knight.getType().getName()));
        super.onPlayerKnightPromoted(player, knightIndex);
    }

    @Override
    @Keep
    protected void onPlayerCityDeveloped(int p, DevelopmentArea area) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, p, area);
        addCardAnimation(p, area.getName() + "\n\n" + area.getLevelName(getPlayerByPlayerNum(p).getCityDevelopment(area)));
        super.onPlayerCityDeveloped(p, area);
    }

    @Override
    @Keep
    protected void onRoadDamaged(int r, int destroyer, int victim) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, r, destroyer, victim);
        Route road = getBoard().getRoute(r);
        addFloatingTextAnimation((UIPlayer)getPlayerByPlayerNum(road.getPlayer()), getBoard().getRouteMidpoint(road), getString("Road Damaged.\nCannot build another\nuntil it is repaired"));
        super.onRoadDamaged(r, destroyer, victim);
    }

    @Override
    @Keep
    protected void onPlayerShipComandeered(int taker, int shipTaken) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, taker, shipTaken);
        Route ship = getBoard().getRoute(shipTaken);
        Player tPlayer = getPlayerByPlayerNum(taker);
        addFloatingTextAnimation((UIPlayer)getPlayerByPlayerNum(ship.getPlayer()), getBoard().getRouteMidpoint(ship), getString("Ship Commandeered by %s", tPlayer.getName()));
        super.onPlayerShipComandeered(taker, shipTaken);
    }

    @Override
    @Keep
    protected void onPlayerShipDestroyed(int r) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, r);
        Route ship = getBoard().getRoute(r);
        addFloatingTextAnimation((UIPlayer)getPlayerByPlayerNum(ship.getPlayer()), getBoard().getRouteMidpoint(ship), getString("Ship Destroyed"));
        super.onPlayerShipDestroyed(r);
    }


    @Override
    @Keep
    protected void onGameOver(final int winnerNum) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, winnerNum);
        getUIBoard().addAnimation(new AAnimation<AGraphics>(700, -1, true) {

            @Override
            protected void draw(AGraphics g, float position, float dt) {
                UIPlayer player = (UIPlayer)getPlayerByPlayerNum(winnerNum);
                String txt = getString("%s WINS!!!", player.getName());
                float width = g.getTextWidth(txt);
                float ratio = g.getViewportWidth()/(width*2);
                float oldHgt = g.getTextHeight();
                float targetHeight = ratio * oldHgt;
                float minHeight = targetHeight * 0.8f;
                float maxHeight = targetHeight * 1.2f;
                g.setTextHeight(minHeight + (maxHeight-minHeight)*position);
                g.setColor(player.getColor());
                g.pushMatrix();
                g.setIdentity();
                g.translate(g.getViewportWidth()/2, g.getViewportHeight()/2);
                g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER,txt);
                g.popMatrix();
                g.setTextHeight(oldHgt);
            }
        }, false);
        running = false;
        super.onGameOver(winnerNum);
    }

    @Override
    public void onConnected(ClientConnection conn) {
        UIPlayer player = null;
        for (Player p : getPlayers()) {
            if (!(p instanceof UIPlayerUser)) {
                UIPlayer uip = (UIPlayer)p;
                if (uip.connection == null) {
                    uip.connect(conn);
                    player = uip;
                } else {
                    if (uip.connection.isConnected() && uip.connection.getName().equals(conn.getName()))
                        return; // already connected
                }
            }
        }
        if (player == null) {
            if (isRunning()) {
                conn.disconnect(getString("Game Full"));
            } else if (getNumPlayers() >= getRules().getMaxPlayers()) {
                conn.disconnect(getString("Game Full"));
            } else {
                // made it here so it means we were not able to assign, so add a new player!
                player = new UIPlayer();
                player.setColor(getAvailableColors().values().iterator().next());
                player.connect(conn);
                addPlayer(player);
            }
        }

        if (player != null) {
            conn.addListener(this);
            try {
                conn.sendCommand(new GameCommand(NetCommon.SVR_TO_CL_INIT)
                        .setArg("numPlayers", getNumPlayers())
                        .setArg("playerNum", player.getPlayerNum())
                        .setArg("soc", this))
                        ;
            } catch (Exception e) {
                e.printStackTrace();
            }
            printinfo(0, getString("Player %s has joined", conn.getName()));
        }

    }

    @Override
    public void onReconnection(ClientConnection conn) {

    }

    @Override
    public void onClientDisconnected(ClientConnection conn) {
        printinfo(0, getString("Player %s has disconnected", conn.getName()));
    }

    @Override
    public void onCommand(ClientConnection c, GameCommand cmd) {
    }

    @Override
    public void onDisconnected(ClientConnection c, String reason) {

    }

    @Override
    public void onCancelled(ClientConnection c, String id) {
        cancel();
    }

    /**
     *
     * @param board
     */
    public final void setBoard(Board board) {
        super.setBoard(board);
        boardRenderer.getComponent().redraw();
    }

    public boolean isAITuningEnabled() {
        return false;
    }

    public static Map<String, GColor> getAllColors() {
        HashMap<String, GColor> colors = new HashMap<>();
        colors.put("RED", GColor.RED);
        colors.put("GREEN", GColor.GREEN);
        colors.put("BLUE", GColor.BLUE.lightened(.2f));
        colors.put("YELLOW", GColor.YELLOW);
        colors.put("ORANGE", GColor.ORANGE);
        colors.put("PINK", GColor.PINK);
        return colors;
    }

    public Map<String, GColor> getAvailableColors() {
        Map<String, GColor> colors = getAllColors();

        for (int i=0; i<getNumPlayers(); i++) {
            GColor color = getPlayerColor(i);
            for (Map.Entry<String, GColor> e : colors.entrySet()) {
                if (e.getValue().equals(color)) {
                    colors.remove(e.getKey());
                    break;
                }
            }
        }

        return colors;
    }

    private long getAnimTime() {
        return 1500;
    }

    void startStructureAnimation(final int playerNum, final Vertex vertex, final VertexType type) {
        if (vertex == null)
            return;
        final UIBoardRenderer board = UISOC.getInstance().getUIBoard();
        vertex.setOpen();
        board.addAnimation(new UIAnimation(getAnimTime()) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                g.setColor(getPlayerColor(playerNum));
                g.pushMatrix();
                g.translate(vertex);
                g.translate(0, (1-position)*board.getStructureRadius()*5);
                g.scale(1, position);
                switch (type) {
                    case SETTLEMENT:
                        board.drawSettlement(g, Vector2D.ZERO, playerNum, false);
                        break;
                    case CITY:
                        board.drawCity(g, Vector2D.ZERO, playerNum, false);
                        break;
                    case WALLED_CITY:
                        board.drawWalledCity(g, Vector2D.ZERO, playerNum, false);
                        break;
                    case METROPOLIS_POLITICS:
                        board.drawMetropolisPolitics(g, Vector2D.ZERO, playerNum, false);
                        break;
                    case METROPOLIS_SCIENCE:
                        board.drawMetropolisScience(g, Vector2D.ZERO, playerNum, false);
                        break;
                    case METROPOLIS_TRADE:
                        board.drawMetropolisTrade(g, Vector2D.ZERO, playerNum, false);
                        break;
                }
                g.popMatrix();
            }
        }, true);

    }

    void startMoveShipAnimation(final int playerNum, final Route source, final Route target, final SOC soc) {
        if (source == null || target == null || soc == null)
            return;
        final RouteType shipType = source.getType();
        source.setType(RouteType.OPEN);
        final UIBoardRenderer comp = ((UISOC)soc).getUIBoard();
        comp.addAnimation(new UIAnimation(getAnimTime()) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                g.setColor(getPlayerColor(playerNum));
                g.pushMatrix();
                //render.translate(mp);
                //render.scale(1, position);
                Vector2D startV = soc.getBoard().getRouteMidpoint(source);
                Vector2D endV   = soc.getBoard().getRouteMidpoint(target);
                Vector2D curV   = startV.add(endV.sub(startV).scaledBy(position));

                float startAng  = comp.getEdgeAngle(source);
                float endAng    = comp.getEdgeAngle(target);
                float curAng    = startAng + (endAng - startAng) * position;

                comp.drawVessel(g, shipType, curV, Math.round(curAng), false);
                g.popMatrix();
            }
        }, true);
    }

    void startBuildShipAnimation(final int playerNum, final Route edge, final SOC soc) {
        if (edge == null || soc == null)
            return;

        final UIBoardRenderer comp = ((UISOC)soc).getUIBoard();
        comp.addAnimation(new UIAnimation(getAnimTime()) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                g.setColor(getPlayerColor(playerNum));
                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, position);
                comp.drawShip(g, edge, false);
                g.popMatrix();
            }
        }, true);
    }

    void startUpgradeShipAnimation(final int playerNum, final Route ship) {
        if (ship == null)
            return;
        final UIBoardRenderer comp = UISOC.getInstance().getUIBoard();
        ship.setType(RouteType.OPEN);
        comp.addAnimation(new UIAnimation(2000) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                g.setColor(getPlayerColor(playerNum));
                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, 1.0f - position);
                comp.drawShip(g, ship, false);
                g.popMatrix();

                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, position);
                comp.drawWarShip(g, ship, false);
                g.popMatrix();
            }
        }, true);
    }

    void startRoadAnimation(final int playerNum, final Route edge, final SOC soc) {
        if (edge == null || soc == null)
            return;
        final UIBoardRenderer comp = UISOC.getInstance().getUIBoard();
        if (edge != null) {
            comp.addAnimation(new UIAnimation(getAnimTime()) {

                final Vertex A = soc.getBoard().getVertex(edge.getFrom());
                final Vertex B = soc.getBoard().getVertex(edge.getTo());

                @Override
                public void draw(AGraphics g, float position, float dt) {
                    Vertex from, to;
                    if (A.getPlayer() == playerNum || soc.getBoard().isVertexAdjacentToPlayerRoad(edge.getFrom(), playerNum)) {
                        from=A; to=B;
                    } else {
                        from=B; to=A;
                    }
                    float dx = (to.getX() - from.getX()) * position;
                    float dy = (to.getY() - from.getY()) * position;
                    g.begin();
                    g.vertex(from);
                    g.vertex(from.getX() + dx, from.getY() + dy);
                    g.setColor(getPlayerColor(playerNum));
                    g.drawLines(RenderConstants.thickLineThickness);
                }
            }, true);
        }
    }

    void startKnightAnimation(final int playerNum, final Vertex vertex) {
        final UIBoardRenderer comp = UISOC.getInstance().getUIBoard();
        comp.addAnimation(new UIAnimation(getAnimTime()) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                g.setColor(getPlayerColor(playerNum));
                g.pushMatrix();
                g.translate(vertex.getX(), position * (vertex.getY()));
                g.scale((2f-position) * (float)Math.cos((1-position)*20), (2f-position));
                comp.drawKnight(g, Vector2D.ZERO, playerNum, 1, false, false);
                g.popMatrix();
            }
        }, true);
    }

    void startMoveKnightAnimation(final int playerNum, final Vertex fromVertex, final Vertex toVertex) {
        final UIBoardRenderer comp = UISOC.getInstance().getUIBoard();
        final VertexType knightType = fromVertex.getType();
        fromVertex.setOpen();
        comp.addAnimation(new UIAnimation(getAnimTime()) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                g.setColor(getPlayerColor(playerNum));
                g.pushMatrix();
                IVector2D pos = Vector2D.newTemp(fromVertex).add(Vector2D.newTemp(toVertex).sub(fromVertex).scaledBy(position));
                g.translate(pos);
                comp.drawKnight(g, Vector2D.ZERO, playerNum, knightType.getKnightLevel(), knightType.isKnightActive(), false);
                g.popMatrix();
            }
        }, true);
    }

    @Override
    protected final void onVertexChosen(int playerNum, Player.VertexChoice mode, Integer vIndex, Integer v2) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, mode, vIndex, v2);
        Vertex v = getBoard().getVertex(vIndex);
        if (v == null)
            return;
        switch (mode) {
            case CITY:
                startStructureAnimation(playerNum, v, VertexType.CITY);
                break;
            case CITY_WALL:
                startStructureAnimation(playerNum, v, VertexType.WALLED_CITY);
                break;
            case KNIGHT_DESERTER:
                break;
            case KNIGHT_DISPLACED:
            case KNIGHT_MOVE_POSITION:
                if (v2 != null)
                    startMoveKnightAnimation(playerNum, getBoard().getVertex(v2), v);
                break;
            case NEW_KNIGHT:
                startKnightAnimation(playerNum, v);
                break;
            case KNIGHT_TO_ACTIVATE:
                break;
            case KNIGHT_TO_MOVE:
                break;
            case KNIGHT_TO_PROMOTE:
                break;
            case OPPONENT_KNIGHT_TO_DISPLACE:
                break;
            case POLITICS_METROPOLIS:
                startStructureAnimation(playerNum, v, VertexType.METROPOLIS_POLITICS);
                break;
            case SCIENCE_METROPOLIS:
                startStructureAnimation(playerNum, v, VertexType.METROPOLIS_SCIENCE);
                break;
            case SETTLEMENT:
                startStructureAnimation(playerNum, v, VertexType.SETTLEMENT);
                break;
            case TRADE_METROPOLIS:
                startStructureAnimation(playerNum, v, VertexType.METROPOLIS_TRADE);
                break;
            case PIRATE_FORTRESS:
                break;
            case OPPONENT_STRUCTURE_TO_ATTACK:
                break;
        }
    }

    @Override
    protected final void onRouteChosen(int playerNum, Player.RouteChoice mode, Integer routeIndex, Integer shipToMove) {
        server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, mode, routeIndex, shipToMove);
        Route route = getBoard().getRoute(routeIndex);
        switch (mode)
        {
            case ROAD:
                startRoadAnimation(playerNum, route, this);
                break;
            case ROUTE_DIPLOMAT:
                break;
            case SHIP:
                if (shipToMove != null) {
                    startMoveShipAnimation(playerNum, getBoard().getRoute(shipToMove), route, this);
                } else {
                    startBuildShipAnimation(playerNum, route, this);
                }
                break;
            case SHIP_TO_MOVE:
                break;
            case UPGRADE_SHIP:
                startUpgradeShipAnimation(playerNum, route);
                break;
            case OPPONENT_ROAD_TO_ATTACK:
            case OPPONENT_SHIP_TO_ATTACK:
                break;
        }
    }


}
