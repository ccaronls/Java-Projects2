package cc.game.soc.ui;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import cc.game.soc.core.BotNode;
import cc.game.soc.core.Card;
import cc.game.soc.core.CardType;
import cc.game.soc.core.Dice;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.Player;
import cc.game.soc.core.Route;
import cc.game.soc.core.SOC;
import cc.game.soc.core.Tile;
import cc.game.soc.core.Trade;
import cc.game.soc.core.Vertex;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;

/**
 * Created by chriscaron on 2/22/18.
 */

public abstract class UISOC extends SOC implements MenuItem.Action {

    private static Logger log = LoggerFactory.getLogger(UISOC.class);

    private static UISOC instance = null;

    private final UIBoardRenderer boardComp;
    private final UIDiceRenderer[] diceComps;
    private Object returnValue = null;
    private final Object waitObj = new Object();

    protected UISOC(UIProperties properties, UIBoardRenderer comp, UIDiceRenderer[] diceComps) {
        if (instance != null)
            throw new RuntimeException();
        instance = this;
        this.properties = properties;
        this.boardComp = comp;
        this.diceComps = diceComps;
    }

    public static UISOC getInstance() {
        return instance;
    }

    public UIBoardRenderer getUIBoard() {
        return boardComp;
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

    private final UIProperties properties;

    public final UIProperties getProps() {
        return properties;
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
        try {
            synchronized (waitObj) {
                waitObj.wait();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
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
        //menu.add(new JSeparator());

        if (canCancel()) {
            addMenuItem(CANCEL);
        } else {
            //addMenuItem(CANCEL);
            //menu.add(new JLabel(""));
        }
        /*
        addMenuItem(SHOW_RULES);
        addMenuItem(BUILDABLES_POPUP);
        addMenuItem(REWIND_GAME);
        addMenuItem(QUIT);
        */
    }

    @Override
    protected void onDiceRolled(Dice...  dice) {
        spinDice(dice);
    }

    private  void spinDice(Dice ... dieToSpin) {
        float diceSpinTimeSeconds = getProps().getFloatProperty("gui.diceSpinTimeSeconds", 3);
        clearMenu();
        int delay = 10;
        long startTime = System.currentTimeMillis();
        while (true) {
            long curTime = System.currentTimeMillis();
            if (curTime - startTime > diceSpinTimeSeconds*1000)
                break;
            for (int i=0; i<dieToSpin.length; i++) {
                if (dieToSpin[i] == null)
                    continue;
                diceComps[i].setType(dieToSpin[i].getType());
                diceComps[i].setDie(Utils.rand() % 6 + 1);
            }
            try {
                Thread.sleep(delay);
            } catch (Exception e) {}
            delay += 20;
        }
    }

    public boolean getSetDiceMenu(Dice[] die, int num) {
        clearMenu();
        diceComps[0].setDicePickerEnabled(true);
        diceComps[1].setDicePickerEnabled(true);
        addMenuItem(SET_DICE);
        completeMenu();
        int [] result = (int[])waitForReturnValue(null);
        if (result != null) {
            for (int i=0; i<result.length; i++) {
                die[i].setNum(result[i]);
            }
            return true;
        }
        return false;
    }

    private boolean running = false;

    public final synchronized void startGameThread() {
        log.debug("Entering thread");
        assert(!running);
        running = true;
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
            boardComp.setPickHandler(null);
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
            returnValue = new int[] { diceComps[0].getDie(), diceComps[1].getDie() };
            diceComps[0].setDicePickerEnabled(false);
            diceComps[1].setDicePickerEnabled(false);
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
}
