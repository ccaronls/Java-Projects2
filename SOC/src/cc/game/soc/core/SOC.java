package cc.game.soc.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import cc.game.soc.core.Player.EnumChoice;
import cc.game.soc.core.Player.PlayerChoice;
import cc.game.soc.core.Player.RouteChoice;
import cc.game.soc.core.Player.RouteChoiceType;
import cc.game.soc.core.Player.TileChoice;
import cc.game.soc.core.Player.VertexChoice;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.CMath;
import cc.lib.utils.Reflector;

/**
 * SOC Core business logic
 * 
 * Add player instances then call runGame() until isGameOver()
 * Make sure to initialize the board otherwise a compltly random one will get generated
 * 
 * @author Chris Caron
 * 
 */
public class SOC extends Reflector<SOC> implements StringResource {

    protected final static Logger log = LoggerFactory.getLogger(SOC.class);

    public static int MAX_PLAYERS = 6;

    public static final int NUM_RESOURCE_TYPES = ResourceType.values().length;
    public static final int NUM_DEVELOPMENT_CARD_TYPES = DevelopmentCardType.values().length;
    public static final int NUM_DEVELOPMENT_AREA_TYPES = DevelopmentArea.values().length; // DONT REMOVE. I know this is redundant, but it prevents a null ptr in DevelopementArea.commodity
    public static final int NUM_COMMODITY_TYPES = CommodityType.values().length;
    public static final int NUM_DEVELOPMENT_AREAS = DevelopmentArea.values().length;

    public interface UndoAction {
        void undo();
    }

    static {
        addField(StackItem.class, "state");
        //addField(StackItem.class, "data");
        //addField(StackItem.class, "options");
    }

    public static class StackItem extends Reflector<StackItem> {

        public StackItem() {
            this(null, null, null, null, null);
        }

        public StackItem(State state, UndoAction action, Collection<Integer> intOptions, Collection<?> xtraOptions, Object data) {
            this.state = state;
            this.action = action;
            this.data = data;
            this.intOptions = intOptions;
            this.xtraOptions = xtraOptions;
        }

//    	public StackItem(State state, UndoAction action) {
//            this(state, action, 0);
//        }

        final State state;
        final UndoAction action;
        final Object data;
        final Collection<Integer> intOptions;
        final Collection<?> xtraOptions;

        public String toString() {
            return state.name() + (data == null ? "" : "(" + data + ")");
        }
    }

    ;

    static {
        addAllFields(SOC.class);
    }

    private final Player[] mPlayers = new Player[MAX_PLAYERS];
    private int mCurrentPlayer;
    private int mNumPlayers;
    private final LinkedList<Integer> mDice = new LinkedList<>(); // compute the next 100 die rolls to support rewind with consistent die rolls
    private final Stack<DiceType[]> mDiceConfigStack = new Stack<>();
    private final Stack<StackItem> mStateStack = new Stack();
    private final List<Card> mDevelopmentCards = new ArrayList<>();
    private List<Card>[] mProgressCards;
    private final List<EventCard> mEventCards = new ArrayList<>();
    private Board mBoard;
    private Rules mRules;
    private int mBarbarianDistance = -1; // CAK
    private final int[] mMetropolisPlayer = new int[NUM_DEVELOPMENT_AREA_TYPES];
    private int mBarbarianAttackCount = 0;
    private boolean mGameOver = false;

    public final State getState() {
        return mStateStack.peek().state;
    }

    @SuppressWarnings("unchecked")
    private final <T> T getStateData() {
        return (T) mStateStack.peek().data;
    }

    private final Collection<Integer> getStateOptions() {
        return mStateStack.peek().intOptions;
    }

    @SuppressWarnings("unchecked")
    private final <T> Collection<T> getStateExtraOptions() {
        return (Collection<T>) mStateStack.peek().xtraOptions;
    }

    private UndoAction getUndoAction() {
        return mStateStack.peek().action;
    }

    private Player getSpecialVictoryPlayer(SpecialVictoryType card) {
        Player player = null;
        for (Player p : getPlayers()) {
            int num = p.getCardCount(card);
            Utils.assertTrue (num == 0 || num == 1);
            if (num > 0) {
                Utils.assertTrue (player == null);
                player = p;
            }
        }
        return player;
    }

    /**
     * Get the playernum with the longest road
     *
     * @return
     */
    public Player getLongestRoadPlayer() {
        return getSpecialVictoryPlayer(SpecialVictoryType.LongestRoad);
    }

    /**
     * Get the playernum with the largest ary
     *
     * @return
     */
    public Player getLargestArmyPlayer() {
        return getSpecialVictoryPlayer(SpecialVictoryType.LargestArmy);
    }

    /**
     * @return
     */
    public Player getHarborMaster() {
        return getSpecialVictoryPlayer(SpecialVictoryType.HarborMaster);
    }

    /**
     * @return
     */
    public Player getExplorer() {
        return getSpecialVictoryPlayer(SpecialVictoryType.Explorer);
    }

    /**
     * Get number of attached players
     *
     * @return
     */
    public int getNumPlayers() {
        return mNumPlayers;
    }

    /**
     * Get the current player number.  If game not in progress then return 0.
     *
     * @return
     */
    public int getCurPlayerNum() {
        Utils.assertTrue (mCurrentPlayer >= 0);
        if (mCurrentPlayer < 0)
            return 0;

        return mPlayers[mCurrentPlayer].getPlayerNum();
    }

    /**
     * @param playerNum
     */
    public void setCurrentPlayer(int playerNum) {
        for (int i = 0; i < mNumPlayers; i++) {
            if (mPlayers[i].getPlayerNum() == playerNum) {
                mCurrentPlayer = i;
                break;
            }
        }
    }

    private Dice getDie(int index) {
        return getDice().get(index);
    }

    /**
     * Get the dice.
     *
     * @return
     */
    public List<Dice> getDice() {
        initDice();
        DiceType[] types = getDiceConfig();
        List<Dice> die = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            die.add(new Dice(mDice.get(i), types[i]));
        }
        return die;
    }

    private DiceType[] getDiceConfig() {
        return mDiceConfigStack.peek();
    }

    private void pushDiceConfig(DiceType... types) {
        mDiceConfigStack.add(types);
    }

    private void popDiceConfig() {
        mDiceConfigStack.pop();
    }

    /**
     * Get sum of both 6 sided die
     *
     * @return
     */
    public int getProductionNum() {
        EventCard card = getTopEventCard();
        if (card != null) {
            return card.getProduction();
        }
        List<Dice> dice = getDice();
        int num = 0;
        for (Dice d : dice) {
            switch (d.getType()) {
                case Event:
                    break;
                case RedYellow:
                case WhiteBlack:
                case YellowRed:
                    num += d.getNum();
                    break;
            }
        }

        return num;
    }

    /**
     * @return
     */
    public Board getBoard() {
        if (mBoard == null) {
            mBoard = new Board();
            mBoard.generateDefaultBoard();
        }
        return mBoard;
    }

    /**
     * @return
     */
    public Player getCurPlayer() {
        return mPlayers[mCurrentPlayer];
    }

    /**
     * Return player num who has existing metropolis or 0 if not owned
     *
     * @param area
     * @return
     */
    public int getMetropolisPlayer(DevelopmentArea area) {
        return mMetropolisPlayer[area.ordinal()];
    }

    void setMetropolisPlayer(DevelopmentArea area, int playerNum) {
        mMetropolisPlayer[area.ordinal()] = playerNum;
    }

    /**
     * @return
     */
    public Tile getRobberCell() {
        return getBoard().getTile(getBoard().getRobberTileIndex());
    }

    /**
     *
     */
    public SOC() {
        mBarbarianDistance = -1; // CAK
        mBarbarianAttackCount = 0;
    }

    public SOC(SOC other) {
        for (int i=0; i<other.mNumPlayers; i++) {
            mPlayers[i] = other.mPlayers[i].shallowCopy();
        }
        Utils.copyElems(mPlayers, other.mPlayers);
        mDice.addAll(other.mDice);
        mDiceConfigStack.addAll(other.mDiceConfigStack);
        mStateStack.addAll(other.mStateStack);
        mDevelopmentCards.addAll(other.mDevelopmentCards);
        mEventCards.addAll(other.mEventCards);
        mBarbarianDistance = other.mBarbarianDistance;
        Utils.copyElems(mMetropolisPlayer, other.mMetropolisPlayer);
        mBarbarianAttackCount = other.mBarbarianAttackCount;
        mBoard = other.mBoard.shallowCopy();
        mRules = other.mRules;
    }

    /**
     * Get the interface for getting game settings.  Never returns null
     *
     * @return
     */
    public final Rules getRules() {
        if (mRules == null) {
            mRules = new Rules();
        }
        return mRules;
    }

    /**
     * @param rules
     */
    public final void setRules(Rules rules) {
        this.mRules = rules;
    }

    /**
     * Resets game but keeps the players
     */
    private void reset() {
        for (int i = 0; i < getNumPlayers(); i++) {
            if (mPlayers[i] != null)
                mPlayers[i].reset();
        }
        mStateStack.clear();
        mEventCards.clear();
        mDice.clear();
        mDiceConfigStack.clear();
        mBoard.reset();
        Arrays.fill(mMetropolisPlayer, 0);
        mBarbarianAttackCount = 0;
        mGameOver = false;
    }

    /**
     * Resets and removes all the players
     */
    public void clear() {
        reset();
        for (Player p : mPlayers) {
            if (p != null) {
                p.reset();
            }
        }
        Arrays.fill(mPlayers, null);
        mNumPlayers = 0;
        mCurrentPlayer = -1;
    }

    @SuppressWarnings("unchecked")
    private void initDeck() {
        mDevelopmentCards.clear();
        if (getRules().isEnableCitiesAndKnightsExpansion()) {
            mProgressCards = new List[NUM_DEVELOPMENT_AREA_TYPES];
            for (int i = 0; i < NUM_DEVELOPMENT_AREA_TYPES; i++)
                mProgressCards[i] = new ArrayList<Card>();
            for (ProgressCardType p : ProgressCardType.values()) {
                if (!p.isEnabled(getRules()))
                    continue;
                for (int i = 0; i < p.deckOccurances; i++) {
                    mProgressCards[p.type.ordinal()].add(new Card(p, CardStatus.USABLE));
                }
            }
            for (int i = 0; i < mProgressCards.length; i++)
                Utils.shuffle(mProgressCards[i]);
        } else {
            for (DevelopmentCardType d : DevelopmentCardType.values()) {
                switch (d) {
                    case Monopoly:
                    case RoadBuilding:
                    case YearOfPlenty:
                    case Victory: {
                        for (int i = 0; i < d.deckOccurances; i++)
                            mDevelopmentCards.add(new Card(d, CardStatus.USABLE));
                        break;
                    }
                    case Soldier: {
                        if (getRules().isEnableRobber()) {
                            for (int i = 0; i < d.deckOccurances; i++)
                                mDevelopmentCards.add(new Card(d, CardStatus.USABLE));
                        }
                        break;
                    }
                    case Warship: {
                        if (isPirateAttacksEnabled() || getRules().isEnableWarShipBuildable()) {
                            for (int i = 0; i < d.deckOccurances; i++)
                                mDevelopmentCards.add(new Card(d, CardStatus.USABLE));
                        }
                        break;
                    }
                    default:
                        throw new SOCException("Unhandled case " + d);
                }
            }
            Utils.shuffle(mDevelopmentCards);
        }

    }

    private void initEventCards() {
        mEventCards.clear();
        for (EventCardType e : EventCardType.values()) {
            for (int p : e.production) {
                mEventCards.add(new EventCard(e, p));
            }
        }
        Utils.shuffle(mEventCards);
    }

    /**
     * @return
     */
    public final Player getNeutralPlayer() {
        for (Player p : getPlayers()) {
            if (p.isNeutralPlayer())
                return p;
        }
        return null;
    }

    /**
     * @param board
     */
    public void setBoard(Board board) {
        mBoard = board;
    }

    /**
     * @param playerNum range is [1-numPlayers] inclusive
     * @return null if player num out of range, the player with num otherwise
     */
    public Player getPlayerByPlayerNum(int playerNum) {
        if (playerNum < 1 || playerNum > MAX_PLAYERS)
            return null;
        return mPlayers[playerNum - 1];
    }

    public final void setPlayer(Player p, int playerNum) {
        Utils.assertTrue (playerNum > 0 && playerNum <= MAX_PLAYERS);
        mPlayers[playerNum - 1] = p;
        p.setPlayerNum(playerNum);
    }

    // package access for unit tests
    void pushStateFront(State state) {
        pushStateFront(state, null, null, null);
    }

    void pushStateFront(State state, Object data) {
        pushStateFront(state, data, null, null);
    }

    private void pushStateFront(State state, Object data, Collection<Integer> options) {
        //log.debug("Push state: " + state);
        //mStateStack.add(new StackItem(state, null, options, null, data));
        pushStateFront(state, data, options, null);
    }

    private void pushStateFront(State state, Object data, Collection<Integer> options, UndoAction action) {
        //log.debug("Push state: " + state);
        //mStateStack.add(new StackItem(state, action, options, null, data));
        pushStateFront(state, data, options, null, action);
    }

    // states are managed in a FIFO stack
    private void pushStateFront(State state, Object data, Collection<Integer> options, Collection<?> xtraOptions, UndoAction action) {
        log.debug("Push state: " + state);
        mStateStack.add(new StackItem(state, action, options, xtraOptions, data));
    }

    /**
     * Override to enable/disable some logging.  Base version always returns true.
     *
     * @return
     */
    public boolean isDebugEnabled() {
        return true;
    }

    /**
     * @param player
     */
    public final void addPlayer(Player player) {
        if (mNumPlayers == MAX_PLAYERS)
            throw new SOCException("Too many players");

        if (mNumPlayers == getRules().getMaxPlayers())
            throw new SOCException("Max players already added.");

        mPlayers[mNumPlayers++] = player;

        if (player.getPlayerNum() == 0)
            player.setPlayerNum(mNumPlayers);
        log.debug("AddPlayer num = " + player.getPlayerNum() + " " + player.getClass().getSimpleName());
    }

    private void incrementCurPlayer(int num) {
        int nextPlayer = (mCurrentPlayer + mNumPlayers + num) % mNumPlayers;
        log.debug("Increment player [" + num + "] positions, was " + getCurPlayer().getPlayerNum() + ", now " + mPlayers[nextPlayer].getPlayerNum());
        mCurrentPlayer = nextPlayer;
    }

    private void dumpStateStack() {
        if (mStateStack.size() == 0) {
            log.warn("State Stack Empty");
        } else {
            StringBuffer buf = new StringBuffer();
            for (StackItem s : mStateStack) {
                buf.append(s.state).append(", ");
            }
            log.debug(buf.toString());
        }
    }

    /*
     *
     */
    private void popState() {
        log.debug("Popping state " + getState());
        Utils.assertTrue (mStateStack.size() > 0);
        mStateStack.pop();
//		log.debug("Setting state to " + (getState()));
    }

    // package access for unit tests
    //void setDice(List<Dice> dice) {
    //    this.di

    private void initDice() {
        while (mDice.size() < 100) {
            mDice.addLast(Utils.rand() % 6 + 1);
        }
    }

    void clearDiceStack() {
        mDice.clear();
    }

    private List<Dice> nextDice() {
        if (mDice.size() > 0)
            mDice.removeFirst();
        initDice();
        return getDice();
    }

    public final int nextDie() {
        initDice();
        return mDice.removeFirst();
    }

    /**
     *
     */
    public void rollDice() {
        List<Dice> dice = nextDice();
        onDiceRolledPrivate(dice);
    }

    public final EventCard getTopEventCard() {
        if (mEventCards.size() > 0) {
            return mEventCards.get(0);
        }
        return null;
    }

    private void processEventCard() {
        EventCard next = getTopEventCard();
        switch (next.getType()) {
            case CalmSea: {
                // player with most harbors get to pick a resource card
                int[] harborCount = new int[MAX_PLAYERS + 1];
                int most = 0;
                for (Player p : getPlayers()) {
                    int num = 0;
                    boolean[] used = new boolean[getBoard().getNumTiles()];
                    for (int vIndex : mBoard.getStructuresForPlayer(p.getPlayerNum())) {
                        Vertex v = getBoard().getVertex(vIndex);
                        for (int i = 0; i < v.getNumTiles(); i++) {
                            int tIndex = v.getTile(i);
                            if (used[tIndex])
                                continue;
                            used[tIndex] = true;
                            Tile t = getBoard().getTile(tIndex);
                            switch (t.getType()) {
                                case PORT_BRICK:
                                case PORT_MULTI:
                                case PORT_ORE:
                                case PORT_SHEEP:
                                case PORT_WHEAT:
                                case PORT_WOOD:
                                    num++;
                                    break;
                                default:
                                    break;

                            }
                        }
                    }
                    harborCount[p.getPlayerNum()] = num;
                    printinfo(getString("%1$s has %2$d harbors", p.getName(), num));
                    if (num > most) {
                        most = num;
                    }
                }
                if (most > 0) {
                    for (int i = 1; i < getNumPlayers() + 1; i++) {
                        if (harborCount[i] == most) {
                            printinfo(getString("%s has most harbors and gets to pick a resource card", getPlayerByPlayerNum(i).getName()));
                            pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                            pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
                            pushStateFront(State.SET_PLAYER, i);
                        }
                    }
                }
                break;
            }
            case Conflict: {
                Player LAP = getLargestArmyPlayer();
                if (LAP == null) {
                    int[] armySize = new int[getNumPlayers() + 1];
                    int maxSize = 0;
                    for (Player p : getPlayers()) {
                        int size = 0;
                        if (getRules().isEnableCitiesAndKnightsExpansion()) {
                            size = mBoard.getKnightLevelForPlayer(p.getPlayerNum(), true, false);
                        } else {
                            size = p.getArmySize(mBoard);
                        }
                        armySize[p.getPlayerNum()] = size;
                        maxSize = Math.max(size, maxSize);
                    }

                    for (int i = 1; i < armySize.length; i++) {
                        if (armySize[i] == maxSize) {
                            if (LAP == null)
                                LAP = getPlayerByPlayerNum(i);
                            else {
                                // there is a tie, s no event
                                LAP = null;
                                break;
                            }
                        }
                    }
                }
                if (LAP != null) {
                    printinfo(getString("%s Has largest army and gets to take a resource card from another", LAP.getName()));
                    pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                    pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeOpponents(this, LAP.getPlayerNum()), null);
                    pushStateFront(State.SET_PLAYER, LAP.getPlayerNum());
                } else {
                    printinfo(getString("No single player with largest army so event cancelled"));
                }
                break;
            }
            case Earthquake: {
                for (Player p : getPlayers()) {
                    if (p.getCardCount(SpecialVictoryType.DamagedRoad) > 0)
                        continue;
                    List<Route> routes = getBoard().getRoutesOfType(p.getPlayerNum(), RouteType.ROAD, RouteType.DAMAGED_ROAD);
                    if (routes.size() > 0) {
                        Route r = Utils.randItem(routes);
                        p.addCard(SpecialVictoryType.DamagedRoad);
                        r.setType(RouteType.DAMAGED_ROAD);
                    }
                }
                break;
            }
            case Epidemic: {
                // cities only produce 1 resource.  handled in distribute resources
                break;
            }
            case GoodNeighbor: {
                // each player gives player to their left a resource or commodity
                pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                for (int i = 1; i <= mNumPlayers; i++) {
                    int left = i + 1;
                    if (left > mNumPlayers)
                        left = 1;

                    pushStateFront(State.CHOOSE_GIFT_CARD, getPlayerByPlayerNum(left));
                    pushStateFront(State.SET_PLAYER, i);
                }
                break;
            }
            case NeighborlyAssistance: {
                // winning player gives up a resource to another
                Player p = computePlayerWithMostVictoryPoints(this);
                if (p != null) {
                    List<Card> cards = p.getGiftableCards();
                    if (cards.size() > 0) {
                        printinfo(getString("%s must give a resource card to another player of their choice", p.getName()));
                        pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                        pushStateFront(State.CHOOSE_OPPONENT_FOR_GIFT_CARD, null, computeOpponents(this, p.getPlayerNum()), cards, null);
                        pushStateFront(State.SET_PLAYER, p.getPlayerNum());
                    }
                }
                break;
            }
            case NoEvent:
                break;
            case PlentifulYear: {
                // each player draws a resource from pile
                for (int i = 0; i < getNumPlayers(); i++) {
                    pushStateFront(State.NEXT_PLAYER);
                    pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
                }
                break;
            }
            case RobberAttack: {
                // same as rolling a seven.  handled by processDice
                break;
            }
            case RobberFlees: {
                List<Integer> tiles = getBoard().getTilesOfType(TileType.DESERT);
                if (tiles.size() > 0) {
                    getBoard().setRobber(tiles.get(0));
                } else {
                    getBoard().setRobber(-1);
                }
                break;
            }
            case Tournament: { // TODO: verify non-cak rules
                int[] numKnights = new int[getNumPlayers() + 1];
                int most = 0;
                for (int i = 1; i <= getNumPlayers(); i++) {
                    int num = 0;
                    if (getRules().isEnableCitiesAndKnightsExpansion())
                        num = getBoard().getNumKnightsForPlayer(i);
                    else
                        num = getPlayerByPlayerNum(i).getArmySize(mBoard);
                    numKnights[i] = num;
                    if (num > most) {
                        most = num;
                    }
                }
                if (most > 0) {
                    pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                    for (int i = 1; i < numKnights.length; i++) {
                        if (numKnights[i] == most) {
                            pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
                            pushStateFront(State.SET_PLAYER, i);
                        }
                    }
                }
                break;
            }
            case TradeAdvantage: {
                Player LRP = getLongestRoadPlayer();
                if (LRP == null) {
                    int[] roadLength = new int[getNumPlayers() + 1];
                    int maxSize = 0;
                    for (Player p : getPlayers()) {
                        int len = roadLength[p.getPlayerNum()] = p.getRoadLength();
                        maxSize = Math.max(len, maxSize);
                    }

                    for (int i = 1; i < roadLength.length; i++) {
                        if (roadLength[i] == maxSize) {
                            if (LRP == null)
                                LRP = getPlayerByPlayerNum(i);
                            else {
                                // there is a tie, s no event
                                printinfo(getString("No event when 2 or more players have the same size army"));
                                LRP = null;
                                break;
                            }
                        }
                    }
                }
                if (LRP != null) {
                    printinfo(getString("%s has longest road and gets to take a card from another", LRP.getName()));
                    pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                    pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeOpponents(this, LRP.getPlayerNum()), null);
                    pushStateFront(State.SET_PLAYER, LRP.getPlayerNum());
                } else {
                    printinfo(getString("No single player with the longest road so event cancelled"));
                }
                break;
            }
        }
    }

    /**
     * Called for Every resource bundle a player recieves.
     * Called once for each player, for each resource.
     * default method does nothing.
     *
     * @param playerNum
     * @param type
     * @param amount
     */
    protected void onDistributeResources(int playerNum, ResourceType type, int amount) {
    }

    /**
     * @param playerNum
     * @param type
     * @param amount
     */
    protected void onDistributeCommodity(int playerNum, CommodityType type, int amount) {
    }

    private void distributeResources(int diceRoll) {
        // collect info to be displayed at the end
        int[][] resourceInfo = new int[NUM_RESOURCE_TYPES][];
        for (int i = 0; i < resourceInfo.length; i++) {
            resourceInfo[i] = new int[getNumPlayers() + 1];
        }
        int[][] commodityInfo = new int[NUM_COMMODITY_TYPES][];
        for (int i = 0; i < commodityInfo.length; i++) {
            commodityInfo[i] = new int[getNumPlayers() + 1];
        }
        if (diceRoll > 0)
            printinfo(getString("Distributing resources for num %d", diceRoll));

        boolean epidemic = false;
        if (getTopEventCard() != null) {
            epidemic = getTopEventCard().getType() == EventCardType.Epidemic;
        }

        final boolean[] playerDidRecieveResources = new boolean[getNumPlayers() + 1];

        // visit all the cells with dice as their num
        for (int i = 0; i < mBoard.getNumTiles(); i++) {

            Tile cell = mBoard.getTile(i);
            if (!cell.isDistributionTile())
                continue;
            Utils.assertTrue(cell.getDieNum() != 0);
            if (mBoard.getRobberTileIndex() == i)
                continue; // apply the robber

            if (diceRoll > 0 && cell.getDieNum() != diceRoll)
                continue;

            // visit each of the adjacent verts to this cell and
            // add to any player at the vertex, some resource of
            // type cell.resource
            for (int vIndex : cell.getAdjVerts()) {
                Vertex vertex = mBoard.getVertex(vIndex);
                if (vertex.getPlayer() > 0 && vertex.isStructure()) {
                    Player p = getPlayerByPlayerNum(vertex.getPlayer());
                    Utils.assertTrue (p != null);
                    if (cell.getType() == TileType.GOLD) {
                        // set to original player
                        printinfo(getString("%s has struck Gold!", p.getName()));
                        pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                        if (getRules().isEnableCitiesAndKnightsExpansion()) {
                            pushStateFront(State.DRAW_RESOURCE_OR_COMMODITY_NOCANCEL);
                        } else {
                            pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
                        }
                        if (vertex.isCity()) {
                            pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
                        }
                        playerDidRecieveResources[p.getPlayerNum()] = true;
                        // set to player that needs choose a resource
                        pushStateFront(State.SET_PLAYER, vertex.getPlayer());
                    } else if (getRules().isEnableCitiesAndKnightsExpansion()) {

                        int numPerCity = getRules().getNumResourcesForCity();
                        int numPerSet = getRules().getNumResourcesForSettlement();

                        if (vertex.isCity()) {

                            if (epidemic) {
                                resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += numPerCity / 2;
                                p.incrementResource(cell.getResource(), numPerCity / 2);
                            } else {
                                if (cell.getCommodity() == null) {
                                    resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += numPerCity;
                                    p.incrementResource(cell.getResource(), numPerCity);
                                } else {
                                    int numComm = numPerCity / 2;
                                    int numRes = numPerCity - numComm;
                                    resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += numRes;
                                    p.incrementResource(cell.getResource(), numRes);
                                    commodityInfo[cell.getCommodity().ordinal()][vertex.getPlayer()] += numComm;
                                    p.incrementResource(cell.getCommodity(), numComm);
                                }
                            }
                        } else if (vertex.isStructure()) {
                            resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += numPerSet;
                            p.incrementResource(cell.getResource(), numPerSet);
                        }
                    } else {
                        int num = getRules().getNumResourcesForSettlement();
                        if (!epidemic && vertex.isCity())
                            num = getRules().getNumResourcesForCity();
                        resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += num;
                        p.incrementResource(cell.getResource(), num);
                    }
                }
            }
        }

        for (Player p : getPlayers()) {
            String msg = "";
            for (ResourceType r : ResourceType.values()) {
                int amount = resourceInfo[r.ordinal()][p.getPlayerNum()];
                if (amount > 0) {
                    msg += (msg.length() == 0 ? "" : ", ") + amount + " X " + r.getName();
                    this.onDistributeResources(p.getPlayerNum(), r, amount);
                }
            }

            for (CommodityType c : CommodityType.values()) {
                int amount = commodityInfo[c.ordinal()][p.getPlayerNum()];
                if (amount > 0) {
                    msg += (msg.length() == 0 ? "" : ", ") + amount + " X " + c.getName();
                    this.onDistributeCommodity(p.getPlayerNum(), c, amount);
                }
            }

            if (msg.length() > 0) {
                printinfo(getString("%1$s gets %2$s", p.getName(), msg));
            } else if (!playerDidRecieveResources[p.getPlayerNum()] && p.hasAqueduct()) {
                printinfo(getString("%s applying Aqueduct ability", p.getName()));
                onAqueduct(p.getPlayerNum());
                pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
                pushStateFront(State.SET_PLAYER, p.getPlayerNum());
            }
        }
    }

    /**
     * CValled when a player gets to apply their aqueduct special ability.
     *
     * @param playerNum
     */
    protected void onAqueduct(int playerNum) {
    }

    /**
     * Print a game info.  Base version writes to stdout.
     * All messages originate from @see runRame
     *
     * @param playerNum
     * @param txt
     */
    public void printinfo(int playerNum, String txt) {
        if (!Utils.isEmpty(txt)) {
            if (playerNum > 0) {
                Player p = getPlayerByPlayerNum(playerNum);
                if (p != null) {
                    log.info("%s: %s", p.getName(), txt);
                    return;
                }
            }
            log.info(txt);
        }
    }

    private void printinfo(String txt) {
        printinfo(getCurPlayerNum(), txt);
    }

    /**
     * Compute the point the player should have based on the board and relevant SOC values.
     * The player's point field is not changed.
     *
     * @param player
     * @return
     */
    static public int computePointsForPlayer(Player player, Board board, SOC soc) {
        int numPts = player.getSpecialVictoryPoints();
        // count cities and settlements
        boolean[] islands = new boolean[board.getNumIslands() + 1];
        for (int i = 0; i < board.getNumAvailableVerts(); i++) {
            Vertex vertex = board.getVertex(i);
            if (vertex.getPlayer() == player.getPlayerNum()) {
                numPts += vertex.getPointsValue(soc.getRules());
                for (Tile t : board.getTilesAdjacentToVertex(vertex)) {
                    if (t.getIslandNum() > 0) {
                        islands[t.getIslandNum()] = true;
                    }
                }
            }
        }
        for (boolean b : islands) {
            if (b) {
                numPts += soc.getRules().getPointsIslandDiscovery();
            }
        }

        if (board.getNumVertsOfType(0, VertexType.PIRATE_FORTRESS) == 0 ||
                player.getCardCount(SpecialVictoryType.CapturePirateFortress) > 0) {
            int victoryPts = player.getUsableCardCount(DevelopmentCardType.Victory);
            if (numPts + victoryPts >= soc.getRules().getPointsForWinGame()) {
                numPts += victoryPts;
            }
        }
        return numPts;
    }

    /**
     * Called when a players point change (for better or worse).  default method does nothing.
     *
     * @param playerNum
     * @param changeAmount
     */
    protected void onPlayerPointsChanged(int playerNum, int changeAmount) {
    }

    private void updatePlayerPoints() {
        for (int i = 0; i < mNumPlayers; i++) {
            Player p = mPlayers[i];
            int newPoints = computePointsForPlayer(p, mBoard, this);
            if (newPoints != p.getPoints()) {
                this.onPlayerPointsChanged(p.getPlayerNum(), newPoints - p.getPoints());
                p.setPoints(newPoints);
            }
        }
    }

    private void onDiceRolledPrivate(List<Dice> dice) {
        String rolled = "";
        for (Dice d : dice) {
            if (d == null)
                continue;
            switch (d.getType()) {
                case Event:
                    if (rolled.length() > 0)
                        rolled += ", ";
                    rolled += DiceEvent.fromDieNum(d.getNum());
                    break;
                case RedYellow:
                case WhiteBlack:
                case YellowRed:
                    if (rolled.length() > 0)
                        rolled += ", ";
                    rolled += String.valueOf(d.getNum());
                    break;
            }
        }
        printinfo(getString("Rolled %s", rolled));
        onDiceRolled(dice);
    }

    /**
     * Called immediately after a die roll.  Base method does nothing.
     *
     * @param dice
     */
    protected void onDiceRolled(List<Dice> dice) {
    }

    /**
     * Called immediately after event card dealt.  Base method does nothing.
     *
     * @param card
     */
    protected void onEventCardDealt(EventCard card) {
    }

    /**
     * Called when a player picks a development card from the deck.
     * default method does nothing.
     *
     * @param playerNum
     * @param card
     */
    protected void onCardPicked(int playerNum, Card card) {
    }

    private void pickDevelopmentCardFromDeck() {
        // add up the total chance
        if (mDevelopmentCards.size() <= 0) {
            initDeck();
        }
        Card picked = mDevelopmentCards.remove(0);
        picked.setUnusable();
        getCurPlayer().addCard(picked);
        printinfo(getString("%1$s picked a %2$s card", getCurPlayer().getName(), picked));
        this.onCardPicked(getCurPlayerNum(), picked);
    }

    /**
     * Called when a player takes a card from another due to soldier.  default method does nothing.
     *
     * @param takerNum
     * @param giverNum
     * @param card
     */
    protected void onTakeOpponentCard(int takerNum, int giverNum, Card card) {
    }

    protected Player newNeutralPlayer() {
        throw new SOCException("Not implemented");
    }

    private void takeOpponentCard(Player taker, Player giver) {
        Utils.assertTrue (giver != taker);
        Card taken = giver.removeRandomUnusedCard();
        taker.addCard(taken);
        printinfo(getString("%1$s taking a %2$s card from Player %3$s", taker.getName(), taken.getName(), giver.getName()));
        onTakeOpponentCard(taker.getPlayerNum(), giver.getPlayerNum(), taken);
    }

    public void initGame() {
        // setup
        if (mNumPlayers < getRules().getMinPlayers())
            throw new SOCException("Too few players " + mNumPlayers + " is too few of " + getRules().getMinPlayers());

        if (getRules().isCatanForTwo()) {
            if (getNeutralPlayer() == null) {
                addPlayer(newNeutralPlayer());
            }
        }

        clearDiceStack();
        getBoard().reset();
        getBoard().assignRandom();
        reset();

        if (getRules().isEnableCitiesAndKnightsExpansion()) {

            if (getRules().isEnableEventCards()) {
                pushDiceConfig(DiceType.RedYellow, DiceType.Event);
            } else {
                pushDiceConfig(DiceType.YellowRed, DiceType.RedYellow, DiceType.Event);
            }
            mBarbarianDistance = getRules().getBarbarianStepsToAttack();
        } else {
            if (!getRules().isEnableEventCards())
                pushDiceConfig(DiceType.WhiteBlack, DiceType.WhiteBlack);
            else
                pushDiceConfig();
            mBarbarianDistance = -1;
        }

        initDice();
        initDeck();

        if (isPirateAttacksEnabled()) {
            getBoard().setPirate(getBoard().getPirateRouteStartTile());
        }

        for (int vIndex : getBoard().getVertIndicesOfType(0, VertexType.PIRATE_FORTRESS)) {
            getBoard().getVertex(vIndex).setPirateHealth(getRules().getPirateFortressHealth());
        }

        mCurrentPlayer = Utils.randRange(0, getNumPlayers() - 1);

        pushStateFront(State.START_ROUND);
        pushStateFront(State.DEAL_CARDS);

        // first player picks last
        pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
        if (getRules().isEnableCitiesAndKnightsExpansion())
            pushStateFront(State.POSITION_CITY_NOCANCEL);
        else
            pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);

        // player picks in reverse order
        for (int i = mNumPlayers - 1; i > 0; i--) {
            if (getRules().isCatanForTwo()) {
                pushStateFront(State.POSITION_NEUTRAL_ROAD_NOCANCEL);
                pushStateFront(State.POSITION_NEUTRAL_SETTLEMENT_NOCANCEL);
            }

            pushStateFront(State.PREV_PLAYER);
            pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
            if (getRules().isEnableCitiesAndKnightsExpansion())
                pushStateFront(State.POSITION_CITY_NOCANCEL);
            else
                pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);
        }

        pushStateFront(State.CLEAR_FORCED_SETTLEMENTS);

        // the last player picks again
        pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
        pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);

        // players pick in order
        for (int i = 0; i < mNumPlayers - 1; i++) {
            if (getRules().isCatanForTwo()) {
                pushStateFront(State.POSITION_NEUTRAL_ROAD_NOCANCEL);
                pushStateFront(State.POSITION_NEUTRAL_SETTLEMENT_NOCANCEL);
            }

            pushStateFront(State.NEXT_PLAYER);
            pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
            pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);
        }
    }

    /**
     * @return
     */
    public boolean isGameOver() {
        return mGameOver;
    }

    private boolean checkGameOver() {
        if (mGameOver)
            return true;
        for (int i = 1; i <= getNumPlayers(); i++) {
            Player player = getPlayerByPlayerNum(i);
            int pts = player.getPoints();
            if (mBoard.getNumVertsOfType(0, VertexType.PIRATE_FORTRESS) == 0 || player.getCardCount(SpecialVictoryType.CapturePirateFortress) > 0) {
                if (player.getPoints() >= getRules().getPointsForWinGame()) {
                    onGameOver(player.getPlayerNum());
                    mGameOver = true;
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Card> createCards(ICardType<?>[] values, CardStatus status) {
        List<Card> cards = new ArrayList<Card>();
        for (ICardType<?> c : values) {
            cards.add(new Card(c, status));
        }
        return cards;
    }

    public static List<Integer> computeHarborTradePlayers(Player trader, SOC soc) {
        List<Integer> players = new ArrayList<Integer>();
        if (trader.getCardCount(CardType.Resource) == 0)
            return players;
        for (int pNum : players) {
            Player p = soc.getPlayerByPlayerNum(pNum);
            int numCommodity = p.getCardCount(CardType.Commodity);
            if (numCommodity > 0) {
                players.add(p.getPlayerNum());
            }
        }
        return players;
    }

    /**
     * Find all knights that are NOT owned by playerNum but ARE on a route of playerNum
     *
     * @param playerNum
     * @param b
     * @return
     */
    public static List<Integer> computeIntrigueKnightsVertexIndices(int playerNum, Board b) {
        List<Integer> verts = new ArrayList<Integer>();
        for (int vIndex = 0; vIndex < b.getNumAvailableVerts(); vIndex++) {
            Vertex v = b.getVertex(vIndex);
            if (v.isKnight() && v.getPlayer() != playerNum && b.isVertexAdjacentToPlayerRoute(vIndex, playerNum)) {
                verts.add(vIndex);
            }
        }
        return verts;
    }

    public static List<Integer> computeInventorTileIndices(Board b, SOC soc) {
        int[] values = null;
        if (soc.getRules().isUnlimitedInventorTiles()) {
            values = new int[]{2, 3, 4, 5, 6, 8, 9, 10, 11, 12};
        } else {
            values = new int[]{3, 4, 5, 9, 10, 11};
        }
        ;
        List<Integer> tiles = new ArrayList<Integer>();
        for (int tIndex = 0; tIndex < b.getNumTiles(); tIndex++) {
            Tile t = b.getTile(tIndex);
            switch (t.getType()) {
                case FIELDS:
                case FOREST:
                case GOLD:
                case HILLS:
                case MOUNTAINS:
                case PASTURE:
                    if (Arrays.binarySearch(values, t.getDieNum()) >= 0) {
                        tiles.add(tIndex);
                    }
                    break;
                default:
                    //
            }
        }
        return tiles;
    }

    /**
     * Return true when it is valid to call run()
     *
     * @return
     */
    public boolean canRun() {
        return this.mStateStack.size() > 0;
    }

    private boolean runGameCheck() {
        if (mBoard == null) {
            throw new SOCException("No board, cannot run game");
        }

        if (!mBoard.isReady()) {
            throw new SOCException("Board not initialized, cannot run game");
        }

        if (mNumPlayers < 2) {
            throw new SOCException("Not enought players, cannot run game");
        }

        int i;

        // test that the players are numbered correctly
        for (i = 1; i <= mNumPlayers; i++) {
            if (getPlayerByPlayerNum(i) == null)
                throw new SOCException("Cannot find player '" + i + "' of '" + mNumPlayers + "' cannot run game");
        }

        if (mStateStack.isEmpty())
            initGame();

        Utils.assertTrue (!mStateStack.isEmpty());

        updatePlayerPoints();
        if (checkGameOver()) {
            return false;
        }

        return true;
    }

    /**
     * A game processing step.  Typically this method is called from a unique thread.
     *
     * @return true if run is valid, false otherwise
     */
    public void runGame() {

        //Vertex knightVertex = null;

        if (!runGameCheck())
            return;

        //dumpStateStack();

        String msg = "Processing state : " + getState();
        if (getStateData() != null) {
            msg += " data=" + getStateData();
        }
        if (getStateOptions() != null) {
            msg += " options=" + getStateOptions();
        }
        if (getStateExtraOptions() != null) {
            msg += " xtraOpts=" + getStateExtraOptions();
        }

        log.debug(msg);

        try {

            Collection<Integer> options = getStateOptions();
            Collection<Card> cards = null;

            final State state = getState();
            switch (state) {

                case DEAL_CARDS: // transition state
                    popState();
                    distributeResources(0);
                    break;

                case PROCESS_DICE:
                    popState();
                    processDice();
                    break;

                case PROCESS_PIRATE_ATTACK:
                    popState();
                    processPirateAttack();
                    break;

                case CHOOSE_PIRATE_FORTRESS_TO_ATTACK: {
                    printinfo(getString("%s choose fortress to attack", getCurPlayer().getName()));
                    Integer v = getCurPlayer().chooseVertex(this, options, VertexChoice.PIRATE_FORTRESS, null);
                    if (v != null) {
                        Utils.assertContains(v, options);
                        onVertexChosen(getCurPlayerNum(), VertexChoice.PIRATE_FORTRESS, v, null);
                        popState();
                        if (getRules().isAttackPirateFortressEndsTurn())
                            popState();
                        processPlayerAttacksPirateFortress(getCurPlayer(), v);
                    }
                    break;
                }

                case POSITION_NEUTRAL_SETTLEMENT_NOCANCEL:
                    options = computeSettlementVertexIndices(this, getNeutralPlayer().getPlayerNum(), mBoard);
                    printinfo(getString("%s place settlement", getCurPlayer().getName()));
                case POSITION_SETTLEMENT_CANCEL: // wait state
                case POSITION_SETTLEMENT_NOCANCEL: { // wait state
                    if (options == null) {
                        printinfo(getString("%s place settlement", getCurPlayer().getName()));
                        options = computeSettlementVertexIndices(this, getCurPlayerNum(), mBoard);
                    }
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.SETTLEMENT, null);

                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);
                        onVertexChosen(getCurPlayerNum(), VertexChoice.SETTLEMENT, vIndex, null);
                        popState();

                        Vertex v = getBoard().getVertex(vIndex);
                        printinfo(getString("%1$s placed a settlement on vertex %2$s", getCurPlayer().getName(), vIndex));
                        Utils.assertTrue (v.getPlayer() == 0);
                        v.setPlayerAndType(getCurPlayerNum(), VertexType.SETTLEMENT);
                        updatePlayerRoadsBlocked(vIndex);

                        // need to re-eval the road lengths for all players that the new settlement
                        // may have affected.  Get the players to update first to avoid dups.
                        boolean[] playerNumsToCompute = new boolean[getNumPlayers() + 1];
                        for (int ii = 0; ii < v.getNumAdjacentVerts(); ii++) {
                            int eIndex = mBoard.getRouteIndex(vIndex, v.getAdjacentVerts()[ii]);
                            if (eIndex >= 0) {
                                Route e = mBoard.getRoute(eIndex);
                                if (e.getPlayer() > 0)
                                    playerNumsToCompute[e.getPlayer()] = true;
                            }
                        }

                        for (int ii = 1; ii < playerNumsToCompute.length; ii++) {
                            if (playerNumsToCompute[ii]) {
                                Player p = getPlayerByPlayerNum(ii);
                                int len = mBoard.computeMaxRouteLengthForPlayer(ii, getRules().isEnableRoadBlock());
                                p.setRoadLength(len);
                            }
                        }

                        updateLongestRoutePlayer();
                        checkForDiscoveredIsland(vIndex);
                        if (getRules().isEnableHarborMaster()) {
                            int newPts = 0;
                            for (Tile t : getBoard().getTilesAdjacentToVertex(v)) {
                                if (t.isPort()) {
                                    newPts += 1;
                                }
                            }
                            if (newPts > 0) {
                                int pts = getCurPlayer().getHarborPoints() + newPts;
                                getCurPlayer().setHarborPoints(pts);
                                updateHarborMasterPlayer();
                            }
                        }
                    }
                    break;
                }

                case POSITION_ROAD_OR_SHIP_CANCEL:
                case POSITION_ROAD_OR_SHIP_NOCANCEL: {
                    printinfo(getString("%s position road or ship", getCurPlayer().getName()));
                    if (getRules().isEnableSeafarersExpansion()) {

                        // this state reserved for choosing between roads or ships to place
                        List<Integer> shipOptions = computeShipRouteIndices(this, getCurPlayerNum(), mBoard);
                        List<Integer> roadOptions = computeRoadRouteIndices(getCurPlayerNum(), mBoard);
                        if (shipOptions.size() > 0 && roadOptions.size() > 0) {
                            RouteChoiceType type = getCurPlayer().chooseRouteType(this);
                            final State saveState = getState();
                            popState();
                            if (type != null) {
                                // allow player to back out to this menu to switch their choice if they want
                                switch (type) {
                                    case ROAD_CHOICE:
                                        pushStateFront(State.POSITION_ROAD_CANCEL, null, roadOptions, new UndoAction() {
                                            @Override
                                            public void undo() {
                                                pushStateFront(saveState);
                                            }
                                        });
                                        break;
                                    case SHIP_CHOICE:
                                        pushStateFront(State.POSITION_SHIP_CANCEL, null, shipOptions, new UndoAction() {

                                            @Override
                                            public void undo() {
                                                pushStateFront(saveState);
                                            }
                                        });
                                        break;
                                }
                            }
                        } else if (shipOptions.size() > 0) {
                            popState();
                            if (canCancel()) {
                                pushStateFront(State.POSITION_SHIP_CANCEL);
                            } else {
                                pushStateFront(State.POSITION_SHIP_NOCANCEL);
                            }
                        } else if (roadOptions.size() > 0) {
                            popState();
                            if (canCancel()) {
                                pushStateFront(State.POSITION_ROAD_CANCEL);
                            } else {
                                pushStateFront(State.POSITION_ROAD_NOCANCEL);
                            }
                        } else {
                            popState(); // no road or ship choices?  hmmmmmmmm
                        }
                    } else {
                        popState();
                        pushStateFront(State.POSITION_ROAD_NOCANCEL);
                    }
                    break;
                }

                case POSITION_NEUTRAL_ROAD_NOCANCEL:
                    options = computeRoadRouteIndices(getNeutralPlayer().getPlayerNum(), getBoard());
                    printinfo(getString("%s place road", getCurPlayer().getName()));
                case POSITION_ROAD_NOCANCEL: // wait state
                case POSITION_ROAD_CANCEL: {// wait state

                    if (options == null) {
                        options = computeRoadRouteIndices(getCurPlayerNum(), mBoard);
                        printinfo(getString("%s place road", getCurPlayer().getName()));
                    }
                    Utils.assertTrue (!Utils.isEmpty(options));

                    Integer edgeIndex = getCurPlayer().chooseRoute(this, options, RouteChoice.ROAD, null);

                    if (edgeIndex != null) {
                        Utils.assertContains(edgeIndex, options);

                        onRouteChosen(getCurPlayerNum(), RouteChoice.ROAD, edgeIndex, null);
                        Route edge = getBoard().getRoute(edgeIndex);
                        Utils.assertTrue (edge.getType() == RouteType.OPEN);
                        Utils.assertTrue (edge.getPlayer() == 0);
                        Utils.assertTrue (edge.isAdjacentToLand());
                        printinfo(getString("%1$s placing a road on edge %2$s", getCurPlayer().getName(), edgeIndex));
                        getBoard().setPlayerForRoute(edge, getCurPlayerNum(), RouteType.ROAD);
                        popState();
                        processRouteChange(getCurPlayer(), edge);
                    }
                    break;
                }

                case POSITION_SHIP_NOCANCEL:
                case POSITION_SHIP_AND_LOCK_CANCEL:
                case POSITION_SHIP_CANCEL: {
                    Integer shipToMove = getStateData();
                    printinfo(getString("%s place ship", getCurPlayer().getName()));
                    if (options == null) {
                        options = computeShipRouteIndices(this, getCurPlayerNum(), mBoard);
                    }
                    final RouteType shipType = shipToMove == null ? RouteType.SHIP : mBoard.getRoute(shipToMove).getType();
                    Utils.assertTrue (shipType.isVessel);
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer edgeIndex = getCurPlayer().chooseRoute(this, options, RouteChoice.SHIP, shipToMove);
                    if (edgeIndex != null) {
                        Utils.assertContains(edgeIndex, options);

                        onRouteChosen(getCurPlayerNum(), RouteChoice.SHIP, edgeIndex, shipToMove);
                        popState();
                        Route edge = getBoard().getRoute(edgeIndex);
                        Utils.assertTrue (edge.getPlayer() == 0);
                        Utils.assertTrue (edge.isAdjacentToWater());
                        edge.setLocked(true);
                        printinfo(getString("%1$s placing a ship on edge %2$s", getCurPlayer().getName(), edgeIndex));
                        getBoard().setPlayerForRoute(edge, getCurPlayerNum(), shipType);
                        if (shipToMove != null) {
                            getBoard().setRouteOpen(getBoard().getRoute(shipToMove));
                        }
                        processRouteChange(getCurPlayer(), edge);
                        if (getState() == State.POSITION_SHIP_AND_LOCK_CANCEL) {
                            for (Route toLock : getBoard().getRoutesOfType(getCurPlayerNum(), RouteType.SHIP, RouteType.WARSHIP)) {
                                toLock.setLocked(true);
                            }
                        }
                    }
                    break;
                }

                case UPGRADE_SHIP_CANCEL: {
                    printinfo(getString("%s upgrade one of your ships", getCurPlayer().getName()));
                    if (options == null) {
                        options = getBoard().getRoutesIndicesOfType(getCurPlayerNum(), RouteType.SHIP);
                    }
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer edgeIndex = getCurPlayer().chooseRoute(this, options, RouteChoice.UPGRADE_SHIP, null);
                    if (edgeIndex != null) {
                        Utils.assertContains(edgeIndex, options);
                        onRouteChosen(getCurPlayerNum(), RouteChoice.UPGRADE_SHIP, edgeIndex, null);
                        Route edge = getBoard().getRoute(edgeIndex);
                        Utils.assertTrue (edge.getPlayer() == getCurPlayerNum());
                        edge.setType(RouteType.WARSHIP);
                        popState();
                        for (Tile t : getBoard().getRouteTiles(edge)) {
                            if (t == getBoard().getPirateTile()) {
                                List<Integer> opts = computePirateTileIndices(this, mBoard);
                                if (opts.size() > 0)
                                    pushStateFront(State.POSITION_PIRATE_NOCANCEL, null, opts);
                            }
                        }
                        updateLargestArmyPlayer();
                    }
                    break;
                }

                case CHOOSE_SHIP_TO_MOVE:
                    printinfo(getString("%s choose ship to move", getCurPlayer().getName()));
                    if (options == null) {
                        options = computeOpenRouteIndices(getCurPlayerNum(), mBoard, false, true);
                    }

                    Utils.assertTrue (!Utils.isEmpty(options));
                    final Integer shipIndex = getCurPlayer().chooseRoute(this, options, RouteChoice.SHIP_TO_MOVE, null);
                    if (shipIndex != null) {
                        Utils.assertContains(shipIndex, options);
                        onRouteChosen(getCurPlayerNum(), RouteChoice.SHIP_TO_MOVE, shipIndex, null);
                        final Route ship = getBoard().getRoute(shipIndex);
                        Utils.assertTrue (ship.getType().isVessel);
                        Utils.assertTrue (ship.getPlayer() == getCurPlayerNum());
                        popState();
                        final RouteType saveType = ship.getType();
                        ship.setType(RouteType.OPEN);
                        List<Integer> moveShipOptions = computeShipRouteIndices(this, getCurPlayerNum(), getBoard());
                        moveShipOptions.remove((Object) shipIndex);
                        ship.setType(saveType);
                        pushStateFront(State.POSITION_SHIP_AND_LOCK_CANCEL, shipIndex, moveShipOptions, new UndoAction() {
                            @Override
                            public void undo() {
                                ship.setType(saveType);
                            }
                        });
                    }
                    break;

                case POSITION_CITY_NOCANCEL:
                    if (options == null) {
                        options = computeSettlementVertexIndices(this, getCurPlayerNum(), mBoard);
                    }
                case POSITION_CITY_CANCEL: { // wait state
                    printinfo(getString("%s place city", getCurPlayer().getName()));
                    if (options == null) {
                        options = computeCityVertxIndices(getCurPlayerNum(), mBoard);
                    }

                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.CITY, null);

                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);
                        onVertexChosen(getCurPlayerNum(), VertexChoice.CITY, vIndex, null);
                        popState();
                        Vertex v = getBoard().getVertex(vIndex);
                        v.setPlayerAndType(getCurPlayerNum(), VertexType.CITY);
                        printinfo(getString("%1$s placing a city at vertex %2$s", getCurPlayer().getName(), vIndex));
                        if (getRules().isEnableHarborMaster()) {
                            int newPts = 0;
                            for (Tile t : getBoard().getTilesAdjacentToVertex(v)) {
                                if (t.isPort()) {
                                    newPts += v.getType() == VertexType.SETTLEMENT ? 1 : 2;
                                }
                            }
                            if (newPts > 0) {
                                int pts = getCurPlayer().getHarborPoints() + newPts;
                                getCurPlayer().setHarborPoints(pts);
                                updateHarborMasterPlayer();
                            }
                        }

//						checkForOpeningStructure(vIndex);
                    }
                    break;
                }

                case CHOOSE_CITY_FOR_WALL: {
                    printinfo(getString("%s choose city to protect with wall", getCurPlayer().getName()));
                    if (options == null) {
                        options = computeCityWallVertexIndices(getCurPlayerNum(), mBoard);
                    }

                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.CITY_WALL, null);

                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);
                        onVertexChosen(getCurPlayerNum(), VertexChoice.CITY_WALL, vIndex, null);
                        popState();

                        Vertex v = getBoard().getVertex(vIndex);
                        printinfo(getString("%1$s placing a city at vertex %2$s", getCurPlayer().getName(), vIndex));
                        v.setPlayerAndType(getCurPlayerNum(), VertexType.WALLED_CITY);
                    }
                    break;

                }

                case CHOOSE_METROPOLIS: {
                    printinfo(getString("%s choose city to upgrade to Metropolis", getCurPlayer().getName()));
                    if (options == null) {
                        options = computeMetropolisVertexIndices(getCurPlayerNum(), mBoard);
                    }

                    Utils.assertTrue (!Utils.isEmpty(options));
                    DevelopmentArea area = getStateData();
                    Utils.assertTrue (area != null);
                    Integer vIndex = null;
                    vIndex = getCurPlayer().chooseVertex(this, options, area.choice, null);
                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);
                        onVertexChosen(getCurPlayerNum(), area.choice, vIndex, null);
                        popState();
                        Vertex v = getBoard().getVertex(vIndex);
                        setMetropolisPlayer(area, getCurPlayerNum());
                        printinfo(getString("%1$s is building a %2$s Metropolis", getCurPlayer().getName(), area));
                        v.setPlayerAndType(getCurPlayerNum(), area.vertexType);
                    }
                    break;
                }

                case NEXT_PLAYER: // transition state
                    incrementCurPlayer(1);
                    popState();
                    break;

                case PREV_PLAYER: // transition state
                    incrementCurPlayer(-1);
                    popState();
                    break;

                case START_ROUND: { // transition state
                    printinfo(getString("Begin round"));
                    Utils.assertTrue (mStateStack.size() == 1); // this should always be the start
                    onShouldSaveGame();
                    List<MoveType> moves = null;
                    if (getRules().isEnableEventCards()) {
                        moves = Arrays.asList(MoveType.DEAL_EVENT_CARD);
                    } else {
                        if (getCurPlayer().getCardCount(ProgressCardType.Alchemist) > 0) {
                            moves = Arrays.asList(MoveType.ALCHEMIST_CARD, MoveType.ROLL_DICE);
                        } else {
                            moves = Arrays.asList(MoveType.ROLL_DICE);
                        }
                    }
                    pushStateFront(State.PLAYER_TURN_NOCANCEL, moves);
                    break;
                }

                case PROCESS_NEUTRAL_PLAYER: {
                    List<MoveType> moves = null;
                    if (getRules().isEnableEventCards()) {
                        moves = Arrays.asList(MoveType.DEAL_EVENT_CARD);
                    } else {
                        moves = Arrays.asList(MoveType.ROLL_DICE);
                    }
                    pushStateFront(State.PLAYER_TURN_NOCANCEL, moves);
                    break;
                }

                case INIT_PLAYER_TURN: { // transition state
                    // update any unusable cards in that players hand to be usable
                    if (getCurPlayer().getMerchantFleetTradable() != null) {
                        getCurPlayer().setMerchantFleetTradable(null);
                        putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.MerchantFleet));
                    }

                    getCurPlayer().setCardsUsable(CardType.Development, true);

                    // lock all player routes that are not open ended.
                    // If we dont do this the AI will find too many move ship choices.
                    for (Route r : mBoard.getRoutesForPlayer(getCurPlayerNum())) {
                        r.setLocked(!mBoard.isRouteOpenEnded(r));
                    }
                    popState();
                    pushStateFront(State.PLAYER_TURN_NOCANCEL);
                    break;
                }

                case PLAYER_TURN_NOCANCEL: { // wait state
                    printinfo(getString("%s choose move", getCurPlayer().getName()));
                    List<MoveType> moves = getStateData();
                    if (Utils.isEmpty(moves)) {
                        moves = computeMoves(getCurPlayer(), mBoard, this);
                        log.debug("computeMoves: %s", moves);
                    }
                    Utils.assertTrue (!Utils.isEmpty(moves));
                    MoveType move = getCurPlayer().chooseMove(this, moves);
                    if (move != null) {
                        Utils.assertContains(move, moves);
                        processMove(move);
                    }
                    break;
                }

                case SHOW_TRADE_OPTIONS: { // wait state
                    printinfo(getString("%s select trade option", getCurPlayer().getName()));
                    List<Trade> trades = getStateData();
                    if (trades == null) {
                        trades = computeTrades(getCurPlayer(), mBoard);
                    }
                    Utils.assertTrue (!Utils.isEmpty(trades));
                    final Trade trade = getCurPlayer().chooseTradeOption(this, trades);
                    if (trade != null) {
                        Utils.assertContains(trade, trades);
                        printinfo(getString("%1$s trades %2$s X %3$s", getCurPlayer().getName(), trade.getType(), trade.getAmount()));
                        getCurPlayer().incrementResource(trade.getType(), -trade.getAmount());
                        onCardsTraded(getCurPlayerNum(), trade);
                        popState();
                        UndoAction action = new UndoAction() {
                            public void undo() {
                                getCurPlayer().incrementResource(trade.getType(), trade.getAmount());
                            }
                        };
                        if (getRules().isEnableCitiesAndKnightsExpansion()) {
                            pushStateFront(State.DRAW_RESOURCE_OR_COMMODITY_CANCEL, null, null, action);
                        } else {
                            pushStateFront(State.DRAW_RESOURCE_CANCEL, null, null, action);
                        }
                    }
                    break;
                }

                case POSITION_ROBBER_OR_PIRATE_CANCEL:
                case POSITION_ROBBER_OR_PIRATE_NOCANCEL:
                    if (options == null) {
                        printinfo(getString("%s place robber or pirate", getCurPlayer().getName()));
                        options = computeRobberTileIndices(this, mBoard);
                        options.addAll(computePirateTileIndices(this, mBoard));
                    }
                case POSITION_ROBBER_CANCEL: // wait state
                case POSITION_ROBBER_NOCANCEL: { // wait state
                    if (options == null) {
                        printinfo(getString("%s place robber", getCurPlayer().getName()));
                        options = computeRobberTileIndices(this, mBoard);
                    }
                    if (options.size() == 0) {
                        mBoard.setRobber(-1);
                        mBoard.setPirate(-1);
                        popState();
                    } else {
                        Integer cellIndex = getCurPlayer().chooseTile(this, options, TileChoice.ROBBER);

                        if (cellIndex != null) {
                            Utils.assertContains(cellIndex, options);
                            popState();
                            Tile cell = getBoard().getTile(cellIndex);
                            if (cell.isWater()) {
                                printinfo(getString("%1$s placing robber on cell %2$s", getCurPlayer().getName(), cellIndex));
                                mBoard.setPirate(cellIndex);
                                pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeRobberTakeOpponentCardOptions(getCurPlayer(), getBoard(), true), null);
                            } else {
                                printinfo(getString("%1$s placing robber on cell %2$s", getCurPlayer().getName(), cellIndex));
                                mBoard.setRobber(cellIndex);
                                pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeRobberTakeOpponentCardOptions(getCurPlayer(), getBoard(), false), null);
                            }
                        }
                    }
                    break;
                }
                case POSITION_PIRATE_CANCEL: // wait state
                case POSITION_PIRATE_NOCANCEL: { // wait state
                    printinfo(getString("%s place pirate", getCurPlayer().getName()));
                    if (options == null) {
                        options = computePirateTileIndices(this, mBoard);
                    }
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer cellIndex = getCurPlayer().chooseTile(this, options, TileChoice.PIRATE);

                    if (cellIndex != null) {
                        Utils.assertContains(cellIndex, options);

                        popState();
                        Tile cell = getBoard().getTile(cellIndex);
                        if (cell.isWater()) {
                            printinfo(getString("%1$s placing pirate on cell %2$s", getCurPlayer().getName(), cellIndex));
                            mBoard.setPirate(cellIndex);
                            pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeRobberTakeOpponentCardOptions(getCurPlayer(), getBoard(), true), null);
                        } else {
                            printinfo(getString("%1$s placing robber on cell %2$s", getCurPlayer().getName(), cellIndex));
                            mBoard.setRobber(cellIndex);
                            pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeRobberTakeOpponentCardOptions(getCurPlayer(), getBoard(), false), null);
                        }
                    }
                    break;
                }

                case CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM: // wait state
                    printinfo(getString("%s choose opponent to take card from", getCurPlayer().getName()));
                    Utils.assertTrue (options != null);
                    if (options.size() == 0) {
                        popState();
                    } else {
                        Integer playerNum = getCurPlayer().choosePlayer(this, options, PlayerChoice.PLAYER_TO_TAKE_CARD_FROM);
                        if (playerNum != null) {
                            Utils.assertContains(playerNum, options);

                            Player player = getPlayerByPlayerNum(playerNum);
                            Utils.assertTrue (player != getCurPlayer());
                            Utils.assertTrue (player.getPlayerNum() > 0);
                            takeOpponentCard(getCurPlayer(), player);
                            popState();
                        }
                    }
                    break;

                case CHOOSE_OPPONENT_FOR_GIFT_CARD: {
                    printinfo(getString("%s choose opponent for gift", getCurPlayer().getName()));
                    Integer playerNum = getCurPlayer().choosePlayer(this, getStateOptions(), PlayerChoice.PLAYER_TO_GIFT_CARD);
                    if (playerNum != null) {
                        Utils.assertContains(playerNum, getStateOptions());

                        Player player = getPlayerByPlayerNum(playerNum);
                        cards = getStateExtraOptions();
                        Utils.assertTrue (!Utils.isEmpty(cards));
                        popState();
                        pushStateFront(State.CHOOSE_GIFT_CARD, player, null, cards, null);
                    }
                    break;
                }

                case CHOOSE_RESOURCE_MONOPOLY: { // wait state
                    printinfo(getString("%s choose resource to monopolize", getCurPlayer().getName()));
                    ResourceType type = getCurPlayer().chooseEnum(this, EnumChoice.RESOURCE_MONOPOLY, ResourceType.values());
                    if (type != null) {
                        processMonopoly(type);
                        popState();
                    }
                    break;
                }

                case SETUP_GIVEUP_CARDS: {
                    popState();
                    pushStateFront(State.NEXT_PLAYER);
                    pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                    for (int ii = 0; ii < mNumPlayers; ii++) {
                        Player cur = mPlayers[(ii + mCurrentPlayer) % mNumPlayers];
                        int numCards = cur.getTotalCardsLeftInHand();
                        if (numCards > getRules().getMaxSafeCardsForPlayer(cur.getPlayerNum(), mBoard)) {
                            int numCardsToSurrender = numCards / 2;
                            printinfo(getString("%1$s must give up %2$d of %3$d cards", cur.getName(), numCardsToSurrender, numCards));
                            for (int i = 0; i < numCardsToSurrender; i++)// (numCardsToSurrender > 0)
                                pushStateFront(State.GIVE_UP_CARD, numCardsToSurrender);
                            pushStateFront(State.SET_PLAYER, cur.getPlayerNum());
                        }
                    }
                    break;
                }

                case GIVE_UP_CARD: { // wait state
                    int numToGiveUp = getStateData();
                    cards = getStateExtraOptions();
                    if (cards == null) {
                        cards = getCurPlayer().getUnusedCards();
                    }
                    Utils.assertTrue (!Utils.isEmpty(cards));
                    printinfo(getString("%1$s Give up one of %2$d cards", getCurPlayer().getName(), numToGiveUp));
                    Card card = getCurPlayer().chooseCard(this, cards, Player.CardChoice.GIVEUP_CARD);
                    if (card != null) {
                        Utils.assertContains(card, cards);

                        getCurPlayer().removeCard(card);
                        popState();
                    }
                    break;
                }

                case DRAW_RESOURCE_OR_COMMODITY_NOCANCEL:
                case DRAW_RESOURCE_OR_COMMODITY_CANCEL:
                    if (Utils.isEmpty(cards)) {
                        printinfo(getString("%s draw a resource or commodity", getCurPlayer().getName()));
                        cards = new ArrayList<>();
                        for (ResourceType t : ResourceType.values()) {
                            cards.add(new Card(t, CardStatus.USABLE));
                        }
                        for (CommodityType c : CommodityType.values()) {
                            cards.add(new Card(c, CardStatus.USABLE));
                        }
                    }
                    // fallthrough
                case DRAW_RESOURCE_NOCANCEL:
                case DRAW_RESOURCE_CANCEL: { // wait state
                    if (Utils.isEmpty(cards)) {
                        printinfo(getString("%s draw a resource", getCurPlayer().getName()));
                        cards = new ArrayList<>();
                        for (ResourceType t : ResourceType.values()) {
                            cards.add(new Card(t, CardStatus.USABLE));
                        }
                    }
                    Card card = getCurPlayer().chooseCard(this, cards, Player.CardChoice.RESOURCE_OR_COMMODITY);
                    if (card != null) {
                        Utils.assertContains(card, cards);

                        printinfo(getString("%1$s draws a %2$s card", getCurPlayer().getName(), card.getName()));
                        onCardPicked(getCurPlayerNum(), card);
                        getCurPlayer().addCard(card);
                        popState();
                    }
                    break;
                }

                case SET_PLAYER: {
                    int playerNum = getStateData();
                    log.debug("Setting player to " + playerNum);
                    setCurrentPlayer(playerNum);
                    popState();
                    break;
                }

                case SET_VERTEX_TYPE: {
                    Object[] data = getStateData();
                    int vIndex = (Integer) data[0];
                    VertexType type = (VertexType) data[1];
                    mBoard.getVertex(vIndex).setPlayerAndType(getCurPlayerNum(), type);
                    popState();
                    break;
                }

                case CHOOSE_KNIGHT_TO_ACTIVATE: {
                    printinfo(getString("%s choose knight to activate", getCurPlayer().getName()));
                    if (Utils.isEmpty(options)) {
                        options = mBoard.getVertIndicesOfType(getCurPlayerNum(), VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
                    }
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.KNIGHT_TO_ACTIVATE, null);
                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);

                        onVertexChosen(getCurPlayerNum(), VertexChoice.KNIGHT_TO_ACTIVATE, vIndex, null);
                        Vertex v = getBoard().getVertex(vIndex);
                        getCurPlayer().adjustResourcesForBuildable(BuildableType.ActivateKnight, -1);
                        v.activateKnight();
                        popState();
                    }
                    break;
                }

                case CHOOSE_KNIGHT_TO_PROMOTE: {
                    printinfo(getString("%s choose knight to promote", getCurPlayer().getName()));
                    if (Utils.isEmpty(options)) {
                        options = computePromoteKnightVertexIndices(getCurPlayer(), mBoard);
                    }
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.KNIGHT_TO_PROMOTE, null);
                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);

                        onVertexChosen(getCurPlayerNum(), VertexChoice.KNIGHT_TO_PROMOTE, vIndex, null);
                        Vertex v = getBoard().getVertex(vIndex);
                        Utils.assertTrue (v.isKnight());
                        onPlayerKnightPromoted(getCurPlayerNum(), vIndex);
                        v.setPlayerAndType(getCurPlayerNum(), v.getType().promotedType());
                        popState();
                    }
                    break;
                }

                case CHOOSE_KNIGHT_TO_MOVE: {
                    printinfo(getString("%s choose knight to move", getCurPlayer().getName()));
                    Utils.assertTrue (!Utils.isEmpty(options));
                    final Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.KNIGHT_TO_MOVE, null);
                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);

                        onVertexChosen(getCurPlayerNum(), VertexChoice.KNIGHT_TO_MOVE, vIndex, null);
                        popState();
                        pushStateFront(State.POSITION_KNIGHT_CANCEL, vIndex, computeKnightMoveVertexIndices(this, vIndex, mBoard));
                    }
                    break;
                }

                case POSITION_DISPLACED_KNIGHT: {
                    printinfo(getString("%s position your displaced knight", getCurPlayer().getName()));
                    Vertex knight = getStateData();
                    VertexType displacedKnight = knight.getType();
                    Integer knightIndex = getBoard().getVertexIndex(knight);
                    Utils.assertTrue (displacedKnight.isKnight());
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.KNIGHT_DISPLACED, knightIndex);
                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);

                        onVertexChosen(getCurPlayerNum(), VertexChoice.KNIGHT_DISPLACED, vIndex, knightIndex);
                        Vertex v = getBoard().getVertex(vIndex);
                        knight.setOpen();
                        v.setPlayerAndType(getCurPlayerNum(), displacedKnight);
                        popState();
                    }
                    break;
                }

                case POSITION_NEW_KNIGHT_CANCEL:
                case POSITION_KNIGHT_NOCANCEL:
                case POSITION_KNIGHT_CANCEL: {
                    printinfo(getString("%s position knight", getCurPlayer().getName()));
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer sourceKnight = getStateData();
                    VertexType knight = VertexType.BASIC_KNIGHT_INACTIVE;
                    if (sourceKnight != null) {
                        knight = getBoard().getVertex(sourceKnight).getType();
                    }

                    Utils.assertTrue (knight.isKnight());
                    VertexChoice choice = state == State.POSITION_NEW_KNIGHT_CANCEL ? VertexChoice.NEW_KNIGHT : VertexChoice.KNIGHT_MOVE_POSITION;
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, choice, sourceKnight);
                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);

                        onVertexChosen(getCurPlayerNum(), choice, vIndex, sourceKnight);
                        if (sourceKnight != null) {
                            getBoard().getVertex(sourceKnight).setOpen();
                        }
                        Vertex v = getBoard().getVertex(vIndex);
                        popState();

                        // see if we are chasing away the robber/pirate
                        for (int i = 0; i < v.getNumTiles(); i++) {
                            int tIndex = v.getTile(i);
                            if (tIndex == mBoard.getRobberTileIndex()) {
                                printinfo(getString("%s has chased away the robber!", getCurPlayer().getName()));
                                pushStateFront(State.POSITION_ROBBER_NOCANCEL);
                            } else if (tIndex == mBoard.getPirateTileIndex()) {
                                printinfo(getString("%s has chased away the pirate!", getCurPlayer().getName()));
                                pushStateFront(State.POSITION_PIRATE_NOCANCEL);
                            }

                        }

                        if (v.getPlayer() != 0) {
                            Utils.assertTrue (v.isKnight());
                            Utils.assertTrue (v.getType().getKnightLevel() < knight.getKnightLevel());
                            options = computeDisplacedKnightVertexIndices(this, vIndex, mBoard);
                            if (options.size() > 0) {
                                pushStateFront(State.SET_VERTEX_TYPE, new Object[]{vIndex, knight});
                                pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                                pushStateFront(State.POSITION_DISPLACED_KNIGHT, v, options);
                                pushStateFront(State.SET_PLAYER, v.getPlayer());
                                break; // exit out early. SET_VERTEX_TYPE will assign after displaced knight is moved.
                            }
                        } else {
                            Utils.assertTrue (v.getType() == VertexType.OPEN);
                        }
                        v.setPlayerAndType(getCurPlayerNum(), knight);
                        updatePlayerRoadsBlocked(vIndex);

                    }
                    break;
                }

                case CHOOSE_PROGRESS_CARD_TYPE: {
                    printinfo(getString("%s draw a progress card", getCurPlayer().getName()));
                    DevelopmentArea area = getCurPlayer().chooseEnum(this, EnumChoice.DRAW_PROGRESS_CARD, DevelopmentArea.values());
                    if (area != null && mProgressCards[area.ordinal()].size() > 0) {
                        Card dealt = mProgressCards[area.ordinal()].remove(0);
                        getCurPlayer().addCard(dealt);
                        onCardPicked(getCurPlayerNum(), dealt);
                        popState();
                    }
                    break;
                }

                // Crane Card
                case CHOOSE_CITY_IMPROVEMENT: {
                    printinfo(getString("%s choose development area", getCurPlayer().getName()));
                    Collection<DevelopmentArea> ops = getStateExtraOptions();
                    Utils.assertTrue (!Utils.isEmpty(ops));
                    DevelopmentArea[] areas = ops.toArray(new DevelopmentArea[ops.size()]);
                    DevelopmentArea area = getCurPlayer().chooseEnum(this, EnumChoice.CRANE_CARD_DEVELOPEMENT, areas);
                    if (area != null) {
                        Utils.assertContains(area, Arrays.asList(areas));
                        popState();
                        processCityImprovement(getCurPlayer(), area, 1);
                    }
                    break;
                }

                case CHOOSE_KNIGHT_TO_DESERT: {
                    printinfo(getString("%s choose a knight for desertion", getCurPlayer().getName()));
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.KNIGHT_DESERTER, null);
                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);

                        onVertexChosen(getCurPlayerNum(), VertexChoice.KNIGHT_DESERTER, vIndex, null);
                        int newPlayerNum = getStateData();
                        popState();
                        options = computeNewKnightVertexIndices(newPlayerNum, getBoard());
                        if (options.size() > 0) {
                            pushStateFront(State.POSITION_KNIGHT_NOCANCEL, vIndex, options);
                        }
                        pushStateFront(State.SET_PLAYER, newPlayerNum);
                    }
                    break;
                }

                case CHOOSE_PLAYER_FOR_DESERTION: {
                    printinfo(getString("%s choose player for desertion", getCurPlayer().getName()));
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer playerNum = getCurPlayer().choosePlayer(this, options, PlayerChoice.PLAYER_FOR_DESERTION);
                    if (playerNum != null) {
                        Utils.assertContains(playerNum, options);

                        popState();
                        List<Integer> knights = getBoard().getKnightsForPlayer(playerNum);
                        Utils.assertTrue (knights.size() > 0);
                        putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Deserter));
                        pushStateFront(State.CHOOSE_KNIGHT_TO_DESERT, getCurPlayerNum(), knights);
                        pushStateFront(State.SET_PLAYER, playerNum);
                    }
                    break;
                }

                case CHOOSE_DIPLOMAT_ROUTE: {
                    printinfo(getString("%s choose diplomat route", getCurPlayer().getName()));
                    Utils.assertTrue (!Utils.isEmpty(options));
                    final Integer rIndex = getCurPlayer().chooseRoute(this, options, RouteChoice.ROUTE_DIPLOMAT, null);
                    if (rIndex != null) {
                        Utils.assertContains(rIndex, options);

                        onRouteChosen(getCurPlayerNum(), RouteChoice.ROUTE_DIPLOMAT, rIndex, null);
                        final Route r = getBoard().getRoute(rIndex);
                        popState();
                        final Card card = getCurPlayer().removeCard(ProgressCardType.Diplomat);
                        putCardBackInDeck(card);
                        if (r.getPlayer() == getCurPlayerNum()) {
                            mBoard.setRouteOpen(r);
                            options = computeRoadRouteIndices(getCurPlayerNum(), mBoard);
                            options.remove((Object) mBoard.getRouteIndex(r)); // remove the edge we just removed
                            pushStateFront(State.POSITION_ROAD_CANCEL, null, options, new UndoAction() {
                                @Override
                                public void undo() {
                                    mBoard.setPlayerForRoute(r, getCurPlayerNum(), RouteType.ROAD);
                                    processRouteChange(getCurPlayer(), r);
                                    removeCardFromDeck(card);
                                    getCurPlayer().addCard(card);
                                }
                            });
                        } else {
                            int playerNum = r.getPlayer();
                            mBoard.setRouteOpen(r);
                            processRouteChange(getPlayerByPlayerNum(playerNum), r);
                        }
                    }
                    break;
                }

                case CHOOSE_HARBOR_PLAYER: {
                    if (Utils.isEmpty(options) || getCurPlayer().getCardCount(CardType.Resource) == 0) {
                        popState();
                        break;
                    }
                    printinfo(getString("%s choose player for harbor trade", getCurPlayer().getName()));
                    final Integer playerNum = getCurPlayer().choosePlayer(this, options, PlayerChoice.PLAYER_TO_FORCE_HARBOR_TRADE);
                    if (playerNum != null) {
                        Utils.assertContains(playerNum, options);

                        Player p = getPlayerByPlayerNum(playerNum);
                        Card card = getStateData();
                        if (card != null) {
                            getCurPlayer().removeCard(card);
                            putCardBackInDeck(card);
                        }
                        options.remove(p);
                        popState();
//						pushStateFront(State.CHOOSE_HARBOR_PLAYER, null, options);
                        pushStateFront(State.CHOOSE_HARBOR_RESOURCE, p, options);
                    }
                    break;
                }

                case CHOOSE_HARBOR_RESOURCE: {
                    printinfo(getString("%s choose a harbor resource", getCurPlayer().getName()));
                    List<Card> resourceCards = getCurPlayer().getCards(CardType.Resource);
                    Card card = getCurPlayer().chooseCard(this, resourceCards, Player.CardChoice.EXCHANGE_CARD);
                    if (card != null) {
                        Utils.assertContains(card, resourceCards);
                        Player exchanging = getStateData();
                        popState();
                        printinfo(getString("%1$s gives a %2$s to %3$s", getCurPlayer().getName(), card.getName(), exchanging.getName()));
                        Card exchanged = getCurPlayer().removeCard(card);
                        onCardLost(getCurPlayerNum(), exchanged);

                        exchanging.addCard(exchanged);
                        onCardPicked(exchanging.getPlayerNum(), exchanged);

                        pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                        pushStateFront(State.EXCHANGE_CARD, getCurPlayer(), null, exchanging.getCards(CardType.Commodity), null);
                        pushStateFront(State.SET_PLAYER, exchanging.getPlayerNum());
                    }
                    break;
                }

                case EXCHANGE_CARD: {
                    printinfo(getString("%s choose card for exchange", getCurPlayer().getName()));
                    cards = getStateExtraOptions();
                    Utils.assertTrue (!Utils.isEmpty(cards));
                    Card card = getCurPlayer().chooseCard(this, cards, Player.CardChoice.EXCHANGE_CARD);
                    if (card != null) {
                        Utils.assertContains(card, cards);

                        getCurPlayer().removeCard(card);
                        onCardLost(getCurPlayerNum(), card);
                        Player exchanging = getStateData();
                        exchanging.addCard(card);
                        onCardPicked(exchanging.getPlayerNum(), card);
                        popState();
                    }
                    break;
                }

                case CHOOSE_OPPONENT_KNIGHT_TO_DISPLACE: {
                    printinfo(getString("%s choose opponent knight to displace", getCurPlayer().getName()));
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.OPPONENT_KNIGHT_TO_DISPLACE, null);
                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);

                        onVertexChosen(getCurPlayerNum(), VertexChoice.OPPONENT_KNIGHT_TO_DISPLACE, vIndex, null);
                        Vertex v = getBoard().getVertex(vIndex);
                        options = computeDisplacedKnightVertexIndices(this, vIndex, mBoard);
                        options.remove((Object) vIndex);
                        putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Intrigue));
                        popState();
                        if (options.size() > 0) {
                            pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                            pushStateFront(State.POSITION_DISPLACED_KNIGHT, v, options);
                            pushStateFront(State.SET_PLAYER, v.getPlayer());
                        } else {
                            v.setOpen();
                        }
                    }
                    break;
                }

                case CHOOSE_TILE_INVENTOR: {
                    printinfo(getString("%s choose tile", getCurPlayer().getName()));
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer tileIndex = getCurPlayer().chooseTile(this, options, TileChoice.INVENTOR);
                    if (tileIndex != null) {
                        Utils.assertContains(tileIndex, options);

                        Integer firstTileIndex = getStateData();
                        popState();
                        if (firstTileIndex == null) {
                            options.remove(tileIndex);
                            pushStateFront(State.CHOOSE_TILE_INVENTOR, tileIndex, options);
                        } else {
                            // swap em
                            Tile secondTile = getBoard().getTile(tileIndex);
                            Tile firstTile = getBoard().getTile(firstTileIndex);
                            int t = firstTile.getDieNum();
                            firstTile.setDieNum(secondTile.getDieNum());
                            secondTile.setDieNum(t);
                            onTilesInvented(getCurPlayerNum(), firstTileIndex, tileIndex);
                            putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Inventor));
                        }
                    }
                    break;
                }

                case CHOOSE_PLAYER_MASTER_MERCHANT: {
                    printinfo(getString("%s choose player to take a card from", getCurPlayer().getName()));
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer playerNum = getCurPlayer().choosePlayer(this, options, PlayerChoice.PLAYER_TO_TAKE_CARD_FROM);
                    if (playerNum != null) {
                        Utils.assertContains(playerNum, options);
                        Player p = getPlayerByPlayerNum(playerNum);
                        putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.MasterMerchant));
                        cards = p.getCards(CardType.Commodity);
                        cards.addAll(p.getCards(CardType.Resource));
                        popState();
                        for (int i = 0; i < Math.min(2, cards.size()); i++) {
                            pushStateFront(State.TAKE_CARD_FROM_OPPONENT, p);
                        }
                    }
                    break;
                }


                case TAKE_CARD_FROM_OPPONENT: {
                    printinfo(getString("%s draw a card from opponents hand", getCurPlayer().getName()));
                    Player p = getStateData();
                    cards = getStateExtraOptions();
                    if (Utils.isEmpty(cards)) {
                        cards = p.getCards(CardType.Commodity);
                        cards.addAll(p.getCards(CardType.Resource));
                    }
                    Card c = getCurPlayer().chooseCard(this, cards, Player.CardChoice.OPPONENT_CARD);
                    if (c != null) {
                        Utils.assertContains(c, cards);
                        p.removeCard(c);
                        getCurPlayer().addCard(c);
                        onTakeOpponentCard(getCurPlayerNum(), p.getPlayerNum(), c);
                        popState();
                    }
                    break;
                }

                case POSITION_MERCHANT: {
                    printinfo(getString("%s position the merchant", getCurPlayer().getName()));
                    if (Utils.isEmpty(options)) {
                        options = computeMerchantTileIndices(this, getCurPlayerNum(), mBoard);
                    }
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer tIndex = getCurPlayer().chooseTile(this, options, TileChoice.MERCHANT);
                    if (tIndex != null) {
                        Utils.assertContains(tIndex, options);
                        if (mBoard.getMerchantPlayer() > 0) {
                            Player p = getPlayerByPlayerNum(mBoard.getMerchantPlayer());
                            Card c = p.removeCard(SpecialVictoryType.Merchant);
                            onCardLost(p.getPlayerNum(), c);
                        }

                        putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Merchant));
                        mBoard.setMerchant(tIndex, getCurPlayerNum());
                        getCurPlayer().addCard(SpecialVictoryType.Merchant);
                        onSpecialVictoryCard(getCurPlayerNum(), SpecialVictoryType.Merchant);
                        popState();
                    }
                    break;
                }

                case CHOOSE_RESOURCE_FLEET: {
                    printinfo(getString("%s choose card type for trade", getCurPlayer().getName()));
                    cards = getStateExtraOptions();
                    Utils.assertTrue (!Utils.isEmpty(cards));
                    Card c = getCurPlayer().chooseCard(this, cards, Player.CardChoice.FLEET_TRADE);
                    if (c != null) {
                        Utils.assertContains(c, cards);
                        getCurPlayer().getCard(ProgressCardType.MerchantFleet).setUsed();
                        getCurPlayer().setMerchantFleetTradable(c);
                        popState();
                    }
                    break;
                }

                case CHOOSE_PLAYER_TO_SPY_ON: {
                    printinfo(getString("%s choose player to spy on", getCurPlayer().getName()));
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer playerNum = getCurPlayer().choosePlayer(this, options, PlayerChoice.PLAYER_TO_SPY_ON);
                    if (playerNum != null) {
                        Utils.assertContains(playerNum, options);
                        Player p = getPlayerByPlayerNum(playerNum);
                        putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Spy));
                        popState();
                        cards = p.getUnusedCards(CardType.Progress);
                        if (cards.size() > 0) {
                            pushStateFront(State.TAKE_CARD_FROM_OPPONENT, p, null, cards, null);
                        }
                    }
                    break;
                }

                case CHOOSE_TRADE_MONOPOLY: {
                    printinfo(getString("%s choose commodity for monopoly", getCurPlayer().getName()));
                    CommodityType c = getCurPlayer().chooseEnum(this, EnumChoice.COMMODITY_MONOPOLY, CommodityType.values());
                    if (c != null) {
                        popState();
                        putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.TradeMonopoly));
                        for (Player p : getPlayers()) {
                            if (p != getCurPlayer()) {
                                int num = p.getCardCount(c);
                                if (num > 0) {
                                    p.removeCards(c, 1);
                                    getCurPlayer().incrementResource(c, 1);
                                    onTakeOpponentCard(getCurPlayerNum(), p.getPlayerNum(), new Card(c, CardStatus.USABLE));
                                }
                            }
                        }
                    }
                    break;
                }

                case CHOOSE_GIFT_CARD: {
                    printinfo(getString("%s choose card to give up", getCurPlayer().getName()));
                    cards = getStateExtraOptions();
                    if (Utils.isEmpty(cards)) {
                        cards = computeGiftCards(this, getCurPlayer());
                    }
                    Card c = getCurPlayer().chooseCard(this, cards, Player.CardChoice.GIVEUP_CARD);
                    if (c != null) {
                        Utils.assertContains(c, cards);
                        Player taker = getStateData();
                        getCurPlayer().removeCard(c);
                        taker.addCard(c);
                        printinfo(getString("%1$s gives a %2$s card to %3$s", getCurPlayer().getName(), c.getName(), taker.getName()));
                        onTakeOpponentCard(taker.getPlayerNum(), getCurPlayerNum(), c);
                        popState();
                    }
                    break;
                }

                case CHOOSE_ROAD_TO_ATTACK: {
                    Utils.assertTrue (getRules().getKnightScoreToDestroyRoad() > 0);
                    printinfo(getString("%s choose road to attack", getCurPlayer().getName()));
                    if (Utils.isEmpty(options)) {
                        options = computeAttackableRoads(this, getCurPlayerNum(), getBoard());
                    }
                    Utils.assertTrue (!Utils.isEmpty(options));
                    Integer rIndex = getCurPlayer().chooseRoute(this, options, RouteChoice.OPPONENT_ROAD_TO_ATTACK, null);
                    if (rIndex != null) {
                        Utils.assertContains(rIndex, options);
                        onRouteChosen(getCurPlayerNum(), RouteChoice.OPPONENT_ROAD_TO_ATTACK, rIndex, null);
                        Route r = getBoard().getRoute(rIndex);
                        Utils.assertTrue (r.getPlayer() > 0);
                        Utils.assertTrue (r.getType().isRoad);
                        popState();
                        pushStateFront(State.ROLL_DICE_ATTACK_ROAD, r);
                    }
                    break;
                }

                case ROLL_DICE_ATTACK_ROAD: {
                    Route r = getStateData();
                    Utils.assertTrue (r != null);
                    Player victim = getPlayerByPlayerNum(r.getPlayer());
                    printinfo(getString("%1$s is attacking %2$s's road", getCurPlayer().getName(), victim.getName()));
                    pushDiceConfig(DiceType.WhiteBlack);
                    rollDice();
                    int routeIndex = mBoard.getRouteIndex(r);
                    AttackInfo<RouteType> info = computeAttackRoad(routeIndex, this, mBoard, getCurPlayerNum());
                    //score += computeAttackerScoreAgainstRoad(r, getCurPlayerNum(), getBoard());
                    onPlayerAttackingOpponent(getCurPlayerNum(), victim.getPlayerNum(), getString("Road"), info.knightStrength + getDie(0).getNum(), info.minScore);
                    if (getDie(0).getNum() > info.minScore - info.knightStrength) {
                        switch (info.destroyedType) {
                            case DAMAGED_ROAD:
                                printinfo(getString("%s has damaged the road", getCurPlayer().getName()));
                                onRoadDamaged(routeIndex, getCurPlayerNum(), victim.getPlayerNum());
                                r.setType(RouteType.DAMAGED_ROAD);
                                break;
                            case OPEN:
                                printinfo(getString("%s has destroyed the road", getCurPlayer().getName()));
                                onRoadDestroyed(routeIndex, getCurPlayerNum(), victim.getPlayerNum());
                                getBoard().setRouteOpen(r);
                                processRouteChange(victim, r);
                                break;
                            default:
                                Utils.assertTrue (false);
                        }
                        updateLongestRoutePlayer();
                    } else {
                        printinfo(getString("%s failed to destroy the road", getCurPlayer().getName()));
                    }
                    popDiceConfig();
                    popState();
                    break;
                }

                case CHOOSE_STRUCTURE_TO_ATTACK: {
                    printinfo(getString("%s choose structure to attack", getCurPlayer().getName()));
                    if (Utils.isEmpty(options)) {
                        options = computeAttackableStructures(this, getCurPlayerNum(), getBoard());
                    }
                    Integer vIndex = getCurPlayer().chooseVertex(this, options, VertexChoice.OPPONENT_STRUCTURE_TO_ATTACK, null);
                    if (vIndex != null) {
                        Utils.assertContains(vIndex, options);
                        onVertexChosen(getCurPlayerNum(), VertexChoice.OPPONENT_STRUCTURE_TO_ATTACK, vIndex, null);
                        Vertex v = getBoard().getVertex(vIndex);
                        popState();
                        pushStateFront(State.ROLL_DICE_ATTACK_STRUCTURE, v);
                    }
                    break;
                }
                case ROLL_DICE_ATTACK_STRUCTURE: {
                    // find all active knights adjacent to structure (may need to be on own road)
                    // subtract their combined levels from the min roll needed for successful attack
                    // if the die roll is > then this value, then the structure gets reduced by a level
                    // if die roll <= to value then all knights are reduced by a level
                    Vertex v = getStateData();
                    int vIndex = mBoard.getVertexIndex(v);
                    Utils.assertTrue (v != null);
                    Player victim = getPlayerByPlayerNum(v.getPlayer());
                    AttackInfo<VertexType> info = computeStructureAttack(vIndex, this, mBoard, getCurPlayerNum());
                    printinfo(getString("%1$s is attacking %2$s's %3$s with knight strength %4$s", getCurPlayer().getName(), victim.getName(), v.getType().getName(), info.knightStrength));
                    pushDiceConfig(DiceType.WhiteBlack);
                    rollDice();
                    onPlayerAttackingOpponent(getCurPlayerNum(), victim.getPlayerNum(), v.getType().getName(), info.knightStrength + getDie(0).getNum(), info.minScore);

                    int diff = getDie(0).getNum() + info.knightStrength - info.minScore;
                    if (diff > 0) {
                        // knights win
                        if (info.destroyedType == VertexType.OPEN) {
                            printinfo(getString("%s's Settlement destroyed!", victim.getName()));
                            v.setOpen();
                            updatePlayerRoadsBlocked(vIndex);
                        } else {
                            printinfo(getString("%1$s's %2$s reduced to %3$s", victim.getName(), v.getType().getName(), info.destroyedType.getName()));
                            v.setPlayerAndType(v.getPlayer(), info.destroyedType);
                        }
                    } else if (diff == 0) {
                        // draw no change on either side
                        printinfo(getString("Attack was a draw"));
                    } else {
                        // victim wins
                        printinfo(getString("%s defends themselves from the attack!", victim.getName()));
                        for (int kIndex : info.attackingKnights) {
                            Vertex knight = getBoard().getVertex(kIndex);
                            switch (knight.getType()) {
                                case BASIC_KNIGHT_INACTIVE:
                                    printinfo(getString("%s's knight lost!", getCurPlayer().getName()));
                                    onPlayerKnightDestroyed(getCurPlayerNum(), kIndex);
                                    knight.setOpen();
                                    break;
                                case MIGHTY_KNIGHT_INACTIVE:
                                case STRONG_KNIGHT_INACTIVE:
                                    printinfo(getString("%s's knight demoted!", getCurPlayer().getName()));
                                    onPlayerKnightDemoted(getCurPlayerNum(), kIndex);
                                    knight.demoteKnight();
                                    break;
                                default:
                                    Utils.assertTrue (false); // unhandled case
                            }
                        }
                    }
                    popDiceConfig();
                    popState();
                    break;
                }

                case CHOOSE_SHIP_TO_ATTACK: {
                    printinfo(getString("%s choose ship to attack", getCurPlayer().getName()));
                    if (Utils.isEmpty(options)) {
                        options = computeAttackableShips(this, getCurPlayerNum(), getBoard());
                    }
                    Integer rIndex = getCurPlayer().chooseRoute(this, options, RouteChoice.OPPONENT_SHIP_TO_ATTACK, null);
                    onRouteChosen(getCurPlayerNum(), RouteChoice.OPPONENT_SHIP_TO_ATTACK, rIndex, null);
                    if (rIndex != null) {
                        Utils.assertContains(rIndex, options);
                        Route r = getBoard().getRoute(rIndex);
                        popState();
                        pushStateFront(State.ROLL_DICE_ATTACK_SHIP, r);
                    }
                    break;
                }

                case ROLL_DICE_ATTACK_SHIP: {
                    Route attacking = getStateData();
                    Utils.assertTrue (attacking != null);
                    pushDiceConfig(DiceType.WhiteBlack);
                    Player victim = getPlayerByPlayerNum(attacking.getPlayer());
                    int dieToWin = computeDiceToWinAttackShip(mBoard, getCurPlayerNum(), attacking.getPlayer());
                    printinfo(getString("%1$s needs a %2$s or better to win.", getCurPlayer().getName(), dieToWin));
                    rollDice();
                    int score = getDie(0).getNum();
                    onPlayerAttackingOpponent(getCurPlayerNum(), victim.getPlayerNum(), getString("Ship"), score, dieToWin - 1);
                    if (score >= dieToWin) {
                        printinfo(getString("%1$s has attacked %2$s and commandeered their ship!", getCurPlayer().getName(), getPlayerByPlayerNum(attacking.getPlayer()).getName()));
                        onPlayerShipComandeered(getCurPlayerNum(), getBoard().getRouteIndex(attacking));
                        mBoard.setPlayerForRoute(attacking, getCurPlayerNum(), attacking.getType());
                    } else {
                        for (int i : mBoard.getRouteIndicesAdjacentToRoute(attacking)) {
                            Route r = mBoard.getRoute(i);
                            if (r.getPlayer() == getCurPlayerNum() && r.getType() == RouteType.WARSHIP) {
                                printinfo(getString("%1$s has defended their ship from %2$s's attack.  Warship destroyed.", getPlayerByPlayerNum(attacking.getPlayer()).getName(), getCurPlayer().getName()));
                                onPlayerShipDestroyed(i);
                                mBoard.setRouteOpen(r);
                                break;
                            }
                        }
                    }
                    popDiceConfig();
                    popState();
                    break;
                }

                case CLEAR_FORCED_SETTLEMENTS: {
                    for (int vIndex : mBoard.getVertIndicesOfType(0, VertexType.OPEN_SETTLEMENT))
                        mBoard.getVertex(vIndex).setOpen();

                    popState();
                    break;
                }
            }

        } finally {
            //if (Profiler.ENABLED) Profiler.pop("SOC::runGame[" + state + "]");
            //if (Profiler.ENABLED) Profiler.pop("SOC::runGame");
        }
    }

    public static int computeDiceToWinAttackShip(Board b, int attackerNum, int defenderNum) {
        // if roll is 1,2,3 then attacker lost and loses their ship
        // if roll is 4,5,6 then attacker won and the attacked ship becomes property of attacker
        // the midpoint is shifted by the difference in the number of cities of each player
        int attackingCities = b.getCitiesForPlayer(attackerNum).size();
        int defenderCities = b.getCitiesForPlayer(defenderNum).size();
        int dieToWin = (4 - (attackingCities - defenderCities));
        return dieToWin;
    }

    static class AttackInfo<T> {
        List<Integer> attackingKnights = new ArrayList<>(); // which knights involved in the attack.  They will have been set to inactive post processing
        int knightStrength;
        int minScore;
        T destroyedType;
    }

    /**
     * Returns an attack info structure.  The attackingKnights will be all knights involved in the attack. They will have been
     * active prior to this call and inactive after leaving.  Caller will need to re-activate knight s if they wish to undo.
     *
     * @param vIndex
     * @param soc
     * @param board
     * @param attackerPlayerNum
     * @return
     */
    static AttackInfo<VertexType> computeStructureAttack(int vIndex, SOC soc, Board board, int attackerPlayerNum) {
        Vertex v = board.getVertex(vIndex);
        AttackInfo<VertexType> info = new AttackInfo<VertexType>();
        for (int i = 0; i < v.getNumAdjacentVerts(); i++) {
            int vIndex2 = v.getAdjacentVerts()[i];
            Vertex v2 = board.getVertex(vIndex2);
            if (v2.isActiveKnight() && v2.getPlayer() == attackerPlayerNum) {
                if (!soc.getRules().isEnableKnightExtendedMoves()) {
                    Route r = board.getRoute(vIndex, vIndex2);
                    if (r.getType() != RouteType.ROAD || r.getPlayer() != attackerPlayerNum)
                        continue;
                }
                info.attackingKnights.add(vIndex2);
                info.knightStrength += v2.getType().getKnightLevel();
                v2.deactivateKnight();
            }
        }

        switch (v.getType()) {
            case CITY:
                info.minScore = soc.getRules().getKnightScoreToDestroyCity();
                info.destroyedType = VertexType.SETTLEMENT;
                break;
            case METROPOLIS_POLITICS:
            case METROPOLIS_SCIENCE:
            case METROPOLIS_TRADE:
                info.minScore = soc.getRules().getKnightScoreToDestroyMetropolis();
                info.destroyedType = VertexType.CITY;
                break;
            case SETTLEMENT:
                info.minScore = soc.getRules().getKnightScoreToDestroySettlement();
                info.destroyedType = VertexType.OPEN;
                break;
            case WALLED_CITY:
                info.minScore = soc.getRules().getKnightScoreToDestroyWalledCity();
                info.destroyedType = VertexType.CITY;
                break;
            default:
                Utils.assertTrue (false); // unhandled case
        }
        return info;
    }

    static AttackInfo<RouteType> computeAttackRoad(int routeIndex, SOC soc, Board b, int attackerPlayerNum) {
        AttackInfo<RouteType> info = new AttackInfo<RouteType>();
        Route r = b.getRoute(routeIndex);
        Vertex v0 = b.getVertex(r.getFrom());
        Vertex v1 = b.getVertex(r.getTo());
        if (v0.isActiveKnight() && v0.getPlayer() == attackerPlayerNum) {
            info.knightStrength += v0.getType().getKnightLevel();
            v0.deactivateKnight();
            info.attackingKnights.add(r.getFrom());
        }
        if (v1.isActiveKnight() && v1.getPlayer() == attackerPlayerNum) {
            info.knightStrength += v1.getType().getKnightLevel();
            v1.deactivateKnight();
            info.attackingKnights.add(r.getTo());
        }

        switch (r.getType()) {
            case ROAD:
                info.destroyedType = RouteType.DAMAGED_ROAD;
                break;
            case DAMAGED_ROAD:
                info.destroyedType = RouteType.OPEN;
                break;
            default:
                Utils.assertTrue (false);
        }
        info.minScore = soc.getRules().getKnightScoreToDestroyRoad();
        return info;
    }

    /**
     * @param attackerNum
     * @param victimNum
     * @param attackingWhat
     * @param attackerScore
     * @param victimScore
     */
    protected void onPlayerAttackingOpponent(int attackerNum, int victimNum, String attackingWhat, int attackerScore, int victimScore) {
    }

    /**
     * @param routeIndex
     * @param destroyerNum
     * @param victimNum
     */
    protected void onRoadDestroyed(int routeIndex, int destroyerNum, int victimNum) {
    }

    /**
     * @param routeIndex
     * @param destroyerNum
     * @param victimNum
     */
    protected void onRoadDamaged(int routeIndex, int destroyerNum, int victimNum) {
    }

    /**
     * Called when a player structure has been attacked and as a result has lost a level.  Settlements get demoted to open vertex.
     *
     * @param vIndex
     * @param newType
     * @param destroyerNum
     * @param victimNum
     */
    protected void onStructureDemoted(int vIndex, VertexType newType, int destroyerNum, int victimNum) {
    }

    /**
     * @param soc
     * @param playerNum
     * @param b
     * @return
     */
    public static List<Integer> computeAttackableRoads(SOC soc, int playerNum, Board b) {
        if (soc.getRules().getKnightScoreToDestroyRoad() <= 0)
            return Collections.emptyList();
        List<Integer> attackableroads = new ArrayList<Integer>();
        for (int rIndex : b.getRoutesIndicesOfType(0, RouteType.ROAD, RouteType.DAMAGED_ROAD)) {
            Route r = b.getRoute(rIndex);
            if (r.getPlayer() == playerNum)
                continue;
            Vertex v = b.getVertex(r.getFrom());
            if (v.isActiveKnight() && v.getPlayer() == playerNum) {
                attackableroads.add(rIndex);
                continue;
            }
            v = b.getVertex(r.getTo());
            if (v.isActiveKnight() && v.getPlayer() == playerNum) {
                attackableroads.add(rIndex);
            }
        }
        return attackableroads;
    }

    /**
     * @param soc
     * @param playerNum
     * @param b
     * @return
     */
    public static List<Integer> computeAttackableShips(SOC soc, int playerNum, Board b) {
        HashSet<Integer> ships = new HashSet<Integer>();
        for (Route r : b.getRoutesOfType(playerNum, RouteType.WARSHIP)) {
            for (int rIndex : b.getRouteIndicesAdjacentToVertex(r.getFrom())) {
                Route rr = b.getRoute(rIndex);
                if (rr.getPlayer() != playerNum && rr.getPlayer() > 0 && rr.getType() == RouteType.SHIP) {
                    // attackable ship
                    ships.add(rIndex);
                }
            }
        }
        return new ArrayList<Integer>(ships);
    }

    /**
     * Attackable structures are those adjacent to a knight and on a road owned by the knight's player.
     *
     * @param soc
     * @param playerNum
     * @param b
     * @return
     */
    public static List<Integer> computeAttackableStructures(SOC soc, int playerNum, Board b) {
        HashSet<Integer> verts = new HashSet<Integer>();
        for (int vIndex : b.getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE)) {
            Vertex v = b.getVertex(vIndex);
            for (int i = 0; i < v.getNumAdjacentVerts(); i++) {
                int vIndex2 = v.getAdjacentVerts()[i];
                Vertex v2 = b.getVertex(vIndex2);
                if (!v2.isStructure() || v2.getPlayer() == playerNum)
                    continue;
                if (!soc.getRules().isEnableKnightExtendedMoves()) {
                    Route r = b.getRoute(vIndex, vIndex2);
                    if (r.getType() != RouteType.ROAD || r.getPlayer() != playerNum)
                        continue;
                }

                switch (v2.getType()) {
                    case CITY:
                        if (soc.getRules().getKnightScoreToDestroyCity() > 0)
                            verts.add(vIndex2);
                        break;
                    case METROPOLIS_POLITICS:
                    case METROPOLIS_SCIENCE:
                    case METROPOLIS_TRADE:
                        if (soc.getRules().getKnightScoreToDestroyMetropolis() > 0)
                            verts.add(vIndex2);
                        break;
                    case SETTLEMENT:
                        if (soc.getRules().getKnightScoreToDestroySettlement() > 0)
                            verts.add(vIndex2);
                        break;
                    case WALLED_CITY:
                        if (soc.getRules().getKnightScoreToDestroyWalledCity() > 0)
                            verts.add(vIndex2);
                        break;
                    default:
                        break;

                }
            }
        }
        return new ArrayList<Integer>(verts);
    }
	/*
	static int processAttackStructure(Vertex v, int attackerNum, Board b) {
		int score = 0;
		for (int i=0; i<v.getNumAdjacentVerts(); i++) {
			int vIndex = v.getAdjacentVerts()[i];
			Vertex v2 = b.getVertex(vIndex);
			if (v2.isActiveKnight()) {
				score += v2.getType().getKnightLevel();
				v2.deactivateKnight();
			}
		}
		return score;
	}*/

    private void processRouteChange(Player p, Route edge) {
        int len = mBoard.computeMaxRouteLengthForPlayer(p.getPlayerNum(), getRules().isEnableRoadBlock());
        p.setRoadLength(len);
        updateLongestRoutePlayer();
        if (edge != null && edge.getPlayer() > 0) {
            checkForDiscoveredNewTerritory(edge.getFrom());
            checkForDiscoveredNewTerritory(edge.getTo());
        }
    }

    /**
     * @param playerNum
     * @param vertexIndex
     */
    protected void onPlayerConqueredPirateFortress(int playerNum, int vertexIndex) {
    }

    /**
     * @param playerNum
     * @param playerHealth
     * @param pirateHealth
     */
    protected void onPlayerAttacksPirateFortress(int playerNum, int playerHealth, int pirateHealth) {
    }

    private void processPlayerAttacksPirateFortress(Player p, int vIndex) {
        Vertex v = getBoard().getVertex(vIndex);
        printinfo(getString("%s is attacking a pirate fortress", p.getName()));
        int playerHealth = getBoard().getRoutesOfType(getCurPlayerNum(), RouteType.WARSHIP).size();
        pushDiceConfig(DiceType.WhiteBlack);
        Dice pirateHealth = getDie(0);
        pirateHealth.roll();
        onDiceRolledPrivate(Utils.asList(pirateHealth));
        onPlayerAttacksPirateFortress(p.getPlayerNum(), playerHealth, pirateHealth.getNum());
        if (playerHealth > pirateHealth.getNum()) {
            // player wins
            Utils.assertTrue (v.getPirateHealth() > 0);
            int h = v.getPirateHealth() - 1;
            v.setPirateHealth(h);
            if (h <= 0) {
                printinfo(getString("%s has conquered a pirate fortress!", p.getName()));
                v.setOpen();
                onPlayerConqueredPirateFortress(getCurPlayerNum(), vIndex);
                v.setPlayerAndType(getCurPlayerNum(), VertexType.SETTLEMENT);
                p.addCard(SpecialVictoryType.CapturePirateFortress);
            } else {
                printinfo(getString("%1$s won and reduced the fortress health to %2$d", p.getName(), h));
            }
        } else if (playerHealth == pirateHealth.getNum()) {
            // lose ship adjacent to the fortress
            printinfo(getString("%s's attack results in a draw.  Player loses 1 ship", p.getName()));
            getBoard().removeShipsClosestToVertex(vIndex, p.getPlayerNum(), 1);
            processRouteChange(p, null);
        } else {
            // lose the 2 ships closest to the fortress
            printinfo(getString("%s's attack results in a loss.  Player loses 2 ships", p.getName()));
            getBoard().removeShipsClosestToVertex(vIndex, p.getPlayerNum(), 2);
            processRouteChange(p, null);
        }
        popDiceConfig();
    }

    /**
     * Not recommended to use as this function modifies player and this data.
     * call runGame until returns true to process
     *
     * @param move
     */
    public final void processMove(MoveType move) {
        log.debug("processMove: %s", move);
        printinfo(getString("%1$s choose move %2$s", getCurPlayer().getName(), move.getName()));
        switch (move) {
            case ROLL_DICE:
                if (getRules().isCatanForTwo()) {
                    pushStateFront(State.PROCESS_NEUTRAL_PLAYER);
                }
            case ROLL_DICE_NEUTRAL_PLAYER:
                rollDice();
                popState();
                pushStateFront(State.PROCESS_DICE);
                if (isPirateAttacksEnabled()) {
                    pushStateFront(State.PROCESS_PIRATE_ATTACK);
                }
                break;

            case DEAL_EVENT_CARD:
                if (getRules().isCatanForTwo()) {
                    pushStateFront(State.PROCESS_NEUTRAL_PLAYER);
                }
            case DEAL_EVENT_CARD_NEUTRAL_PLAYER:
                if (mEventCards.size() > 1) {
                    mEventCards.remove(0);
                } else {
                    initEventCards();
                }
                onEventCardDealt(getTopEventCard());
                if (getRules().isEnableCitiesAndKnightsExpansion()) {
                    List<Dice> dice = nextDice();
                    for (Dice d : nextDice()) {
                        d.roll();
                    }
                    onDiceRolledPrivate(dice);
                }
                popState();
                processDice();
                processEventCard();
                break;

            case REPAIR_ROAD: {
                getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, -1);
                getCurPlayer().removeCard(SpecialVictoryType.DamagedRoad);
                for (Route r : getBoard().getRoutesForPlayer(getCurPlayerNum())) {
                    if (r.getType() == RouteType.DAMAGED_ROAD) {
                        r.setType(RouteType.ROAD);
                        break;
                    }
                }
                break;
            }

            case ATTACK_PIRATE_FORTRESS: {
                pushStateFront(State.CHOOSE_PIRATE_FORTRESS_TO_ATTACK, null, computeAttackablePirateFortresses(getBoard(), getCurPlayer()), null);
                break;
            }

            case BUILD_ROAD:
                getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, -1);
                pushStateFront(State.POSITION_ROAD_CANCEL, null, null, new UndoAction() {
                    public void undo() {
                        getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, 1);
                    }
                });
                break;

            case BUILD_SHIP:
                getCurPlayer().adjustResourcesForBuildable(BuildableType.Ship, -1);
                pushStateFront(State.POSITION_SHIP_CANCEL, null, null, new UndoAction() {
                    public void undo() {
                        getCurPlayer().adjustResourcesForBuildable(BuildableType.Ship, 1);
                    }
                });
                break;

            case BUILD_WARSHIP:
                getCurPlayer().adjustResourcesForBuildable(BuildableType.Warship, -1);
                pushStateFront(State.UPGRADE_SHIP_CANCEL, null, null, new UndoAction() {
                    public void undo() {
                        getCurPlayer().adjustResourcesForBuildable(BuildableType.Warship, 1);
                    }
                });
                break;

            case MOVE_SHIP:
                pushStateFront(State.CHOOSE_SHIP_TO_MOVE);
                break;

            case BUILD_SETTLEMENT:
                getCurPlayer().adjustResourcesForBuildable(BuildableType.Settlement, -1);
                pushStateFront(State.POSITION_SETTLEMENT_CANCEL, null, null, new UndoAction() {
                    public void undo() {
                        getCurPlayer().adjustResourcesForBuildable(BuildableType.Settlement, 1);
                    }
                });
                break;

            case BUILD_CITY:
                getCurPlayer().adjustResourcesForBuildable(BuildableType.City, -1);
                pushStateFront(State.POSITION_CITY_CANCEL, null, null, new UndoAction() {
                    public void undo() {
                        getCurPlayer().adjustResourcesForBuildable(BuildableType.City, 1);
                    }
                });
                break;

            case DRAW_DEVELOPMENT:
                getCurPlayer().adjustResourcesForBuildable(BuildableType.Development, -1);
                pickDevelopmentCardFromDeck();
                break;

            case MONOPOLY_CARD: {
                final Card removed = getCurPlayer().removeUsableCard(DevelopmentCardType.Monopoly);
                putCardBackInDeck(removed);
                pushStateFront(State.CHOOSE_RESOURCE_MONOPOLY, null, null, new UndoAction() {
                    public void undo() {
                        getCurPlayer().addCard(removed);
                        removeCardFromDeck(removed);
                    }
                });
                break;
            }

            case YEAR_OF_PLENTY_CARD: {
                final Card removed = getCurPlayer().removeUsableCard(DevelopmentCardType.YearOfPlenty);
                putCardBackInDeck(removed);
                mDevelopmentCards.add(removed);
                pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
                getCurPlayer().setCardsUsable(CardType.Development, false);
                pushStateFront(State.DRAW_RESOURCE_CANCEL, null, null, new UndoAction() {
                    public void undo() {
                        getCurPlayer().addCard(removed);
                        removeCardFromDeck(removed);
                        getCurPlayer().setCardsUsable(CardType.Development, true);
                        popState();
                    }
                });
                break;
            }

            case ROAD_BUILDING_CARD: {
                final Card removed = getRules().isEnableCitiesAndKnightsExpansion() ?
                        getCurPlayer().removeUsableCard(ProgressCardType.RoadBuilding) : getCurPlayer().removeUsableCard(DevelopmentCardType.RoadBuilding);
                putCardBackInDeck(removed);
                pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
                getCurPlayer().setCardsUsable(CardType.Development, false);
                pushStateFront(State.POSITION_ROAD_OR_SHIP_CANCEL, null, null, new UndoAction() {
                    public void undo() {
                        getCurPlayer().addCard(removed);
                        removeCardFromDeck(removed);
//        				getCurPlayer().setCardsUsable(CardType.Development, true);
                        popState(); // pop an extra state since we push the NOCANCEL
                    }
                });
                break;
            }

            case BISHOP_CARD: {
                final Card removed = getCurPlayer().removeCard(ProgressCardType.Bishop);
                putCardBackInDeck(removed);
                pushStateFront(getRules().isEnableSeafarersExpansion() ? State.POSITION_ROBBER_OR_PIRATE_CANCEL : State.POSITION_ROBBER_CANCEL, null, null, new UndoAction() {

                    @Override
                    public void undo() {
                        removeCardFromDeck(removed);
                        getCurPlayer().addCard(removed);
                    }
                });
                break;
            }

            case WARSHIP_CARD: {
                Collection<Integer> options = getBoard().getRoutesIndicesOfType(getCurPlayerNum(), RouteType.SHIP);
                if (options.size() > 0) {
                    final Card card = getCurPlayer().removeUsableCard(DevelopmentCardType.Warship);
                    putCardBackInDeck(card);
                    pushStateFront(State.UPGRADE_SHIP_CANCEL, null, options, new UndoAction() {

                        @Override
                        public void undo() {
                            removeCardFromDeck(card);
                            getCurPlayer().addCard(card);
                        }
                    });
                }
                break;
            }

            case SOLDIER_CARD: {
                final Card used = getCurPlayer().getUsableCard(DevelopmentCardType.Soldier);
                used.setUsed();
                updateLargestArmyPlayer();
                getCurPlayer().setCardsUsable(CardType.Development, false);
                pushStateFront(getRules().isEnableSeafarersExpansion() ? State.POSITION_ROBBER_OR_PIRATE_CANCEL : State.POSITION_ROBBER_CANCEL, null, null, new UndoAction() {
                    public void undo() {
                        used.setUsable();
                        updateLargestArmyPlayer();
                        getCurPlayer().setCardsUsable(CardType.Development, true);
                    }
                });
                break;
            }

            case ALCHEMIST_CARD: {
                List<Dice> dice = nextDice();
                if (getCurPlayer().setDice(this, dice, 2)) {
                    putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Alchemist));
                    Dice ry = getDiceOfType(DiceType.YellowRed, dice);
                    Dice yr = getDiceOfType(DiceType.RedYellow, dice);
                    Dice ev = getDiceOfType(DiceType.Event, dice);
                    ev.roll();
                    popState();
                    onDiceRolledPrivate(Utils.asList(ry, yr, ev));
                    printinfo(getString("%1$s applied Alchemist card on dice %2$d, %2$d, %4$s", getCurPlayer().getName(), ry.getNum(), yr.getNum(), DiceEvent.fromDieNum(ev.getNum()).getName()));
                    processDice();
                }
                break;
            }

            case TRADE:
                pushStateFront(State.SHOW_TRADE_OPTIONS);
                break;

            case BUILD_CITY_WALL: {
                getCurPlayer().adjustResourcesForBuildable(BuildableType.CityWall, -1);
                pushStateFront(State.CHOOSE_CITY_FOR_WALL, null, null, new UndoAction() {
                    @Override
                    public void undo() {
                        getCurPlayer().adjustResourcesForBuildable(BuildableType.CityWall, 1);
                    }
                });
                break;
            }

            case ACTIVATE_KNIGHT: {
                Utils.assertTrue (getCurPlayer().canBuild(BuildableType.ActivateKnight));
                pushStateFront(State.CHOOSE_KNIGHT_TO_ACTIVATE);
                break;
            }
            case HIRE_KNIGHT: {
                Collection<Integer> options = computeNewKnightVertexIndices(getCurPlayerNum(), mBoard);
                if (options.size() > 0) {
                    getCurPlayer().adjustResourcesForBuildable(BuildableType.Knight, -1);
                    pushStateFront(State.POSITION_NEW_KNIGHT_CANCEL, null, options, new UndoAction() {
                        @Override
                        public void undo() {
                            getCurPlayer().adjustResourcesForBuildable(BuildableType.Knight, 1);
                        }
                    });
                }
                break;
            }
            case MOVE_KNIGHT: {
                Collection<Integer> options = computeMovableKnightVertexIndices(this, getCurPlayerNum(), mBoard);
                if (options.size() > 0) {
                    pushStateFront(State.CHOOSE_KNIGHT_TO_MOVE, null, options, null);
                }
                break;
            }
            case PROMOTE_KNIGHT: {
                getCurPlayer().adjustResourcesForBuildable(BuildableType.PromoteKnight, -1);
                pushStateFront(State.CHOOSE_KNIGHT_TO_PROMOTE, null, null, new UndoAction() {
                    @Override
                    public void undo() {
                        getCurPlayer().adjustResourcesForBuildable(BuildableType.PromoteKnight, 1);
                    }
                });
                break;
            }

            case IMPROVE_CITY_POLITICS: {
                processCityImprovement(getCurPlayer(), DevelopmentArea.Politics, 0);
                break;
            }

            case IMPROVE_CITY_SCIENCE: {
                processCityImprovement(getCurPlayer(), DevelopmentArea.Science, 0);
                break;
            }

            case IMPROVE_CITY_TRADE: {
                processCityImprovement(getCurPlayer(), DevelopmentArea.Trade, 0);
                break;
            }

            case CONTINUE:
                popState();
                break;

            case CRANE_CARD: {
                // build a city improvement for 1 commodity less than normal
                List<DevelopmentArea> options = computeCraneCardImprovements(getCurPlayer());
                if (options.size() > 0) {
                    final Card card = getCurPlayer().removeCard(ProgressCardType.Crane);
                    putCardBackInDeck(card);
                    pushStateFront(State.CHOOSE_CITY_IMPROVEMENT, null, null, options, new UndoAction() {

                        @Override
                        public void undo() {
                            removeCardFromDeck(card);
                            getCurPlayer().addCard(card);
                        }
                    });
                }
                break;
            }

            case DESERTER_CARD: {
                // replace an opponents knight with one of your own
                List<Integer> knightOptions = SOC.computeNewKnightVertexIndices(getCurPlayerNum(), mBoard);
                if (knightOptions.size() > 0) {
                    List<Integer> players = computeDeserterPlayers(this, mBoard, getCurPlayer());
                    if (players.size() > 0) {
                        pushStateFront(State.CHOOSE_PLAYER_FOR_DESERTION, null, players);
                    }
                }
                break;
            }
            case DIPLOMAT_CARD: {
                List<Integer> allOpenRoutes = computeDiplomatOpenRouteIndices(this, mBoard);
                if (allOpenRoutes.size() > 0) {
                    pushStateFront(State.CHOOSE_DIPLOMAT_ROUTE, null, allOpenRoutes, null);
                }
                break;
            }
            case ENGINEER_CARD: {
                // build a city wall for free
                List<Integer> cities = computeCityWallVertexIndices(getCurPlayerNum(), mBoard);
                if (cities.size() > 0) {
                    final Card card = getCurPlayer().removeCard(ProgressCardType.Engineer);
                    putCardBackInDeck(card);
                    pushStateFront(State.CHOOSE_CITY_FOR_WALL, null, cities, new UndoAction() {
                        @Override
                        public void undo() {
                            getCurPlayer().addCard(card);
                            removeCardFromDeck(card);
                        }
                    });
                }
                break;
            }
            case HARBOR_CARD: {
                // You may force each of the other players to make a special trade.
                // You may offer each opponent any 1 Resource Card from your hand. He must exchange it for any
                // 1 Commodity Card from his hand of his choice, if he has any.
                // You must have a resource card to trade and they must have a commodity card to trade
                // You can skip a player if you wish
                List<Integer> players = computeHarborTradePlayers(getCurPlayer(), this);
                if (players.size() > 0) {
                    Card card = getCurPlayer().getCard(ProgressCardType.Harbor);
                    Utils.assertTrue (card != null);
                    pushStateFront(State.CHOOSE_HARBOR_PLAYER, card, players);
                }
                break;
            }
            case INTRIGUE_CARD: {
                // displace an opponents knight that is on your road without moving your own knight
                List<Integer> verts = computeIntrigueKnightsVertexIndices(getCurPlayerNum(), mBoard);
                if (verts.size() > 0) {
                    pushStateFront(State.CHOOSE_OPPONENT_KNIGHT_TO_DISPLACE, null, verts, null);
                }
                break;
            }
            case INVENTOR_CARD: {
                // switch tile tokens of users choice
                pushStateFront(State.CHOOSE_TILE_INVENTOR, null, computeInventorTileIndices(mBoard, this), null);
                break;
            }
            case IRRIGATION_CARD: {
                // player gets 2 wheat for each structure on a field
                int numGained = 2 * computeNumStructuresAdjacentToTileType(getCurPlayerNum(), mBoard, TileType.FIELDS);
                putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Irrigation));
                if (numGained > 0) {
                    getCurPlayer().incrementResource(ResourceType.Wheat, numGained);
                    onDistributeResources(getCurPlayerNum(), ResourceType.Wheat, numGained);
                }
                break;
            }
            case MASTER_MERCHANT_CARD: {
                // take 2 resource or commodity cards from another player who has more victory pts than you
                List<Integer> players = computeMasterMerchantPlayers(this, getCurPlayer());
                if (players.size() > 0) {
                    pushStateFront(State.CHOOSE_PLAYER_MASTER_MERCHANT, null, players, null);
                }
                break;
            }
            case MEDICINE_CARD: {
                // upgrade to city for cheaper
                if (getCurPlayer().getCardCount(ResourceType.Ore) >= 2 && getCurPlayer().getCardCount(ResourceType.Wheat) >= 1) {
                    List<Integer> settlements = mBoard.getSettlementsForPlayer(getCurPlayerNum());
                    if (settlements.size() > 0) {
                        getCurPlayer().incrementResource(ResourceType.Ore, -2);
                        getCurPlayer().incrementResource(ResourceType.Wheat, -1);
                        final Card card = getCurPlayer().removeCard(ProgressCardType.Medicine);
                        putCardBackInDeck(card);
                        pushStateFront(State.POSITION_CITY_CANCEL, null, settlements, new UndoAction() {
                            @Override
                            public void undo() {
                                removeCardFromDeck(card);
                                getCurPlayer().addCard(card);
                                getCurPlayer().incrementResource(ResourceType.Ore, 2);
                                getCurPlayer().incrementResource(ResourceType.Wheat, 1);
                            }
                        });
                    }
                }
                break;
            }
            case MERCHANT_CARD: {
                pushStateFront(State.POSITION_MERCHANT);
                break;
            }
            case MERCHANT_FLEET_CARD: {
                List<Card> tradableCards = computeMerchantFleetCards(getCurPlayer());
                if (tradableCards.size() > 0) {
                    pushStateFront(State.CHOOSE_RESOURCE_FLEET, null, null, tradableCards, null);
                }
                break;
            }
            case MINING_CARD: {
                // player gets 2 ore for each structure on a field
                int numGained = 2 * computeNumStructuresAdjacentToTileType(getCurPlayerNum(), mBoard, TileType.MOUNTAINS);
                if (numGained > 0) {
                    getCurPlayer().incrementResource(ResourceType.Ore, numGained);
                    onDistributeResources(getCurPlayerNum(), ResourceType.Ore, numGained);
                    putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Mining));
                }
                break;
            }
            case RESOURCE_MONOPOLY_CARD: {
                final Card remove = getCurPlayer().removeCard(ProgressCardType.ResourceMonopoly);
                putCardBackInDeck(remove);
                pushStateFront(State.CHOOSE_RESOURCE_MONOPOLY, null, null, new UndoAction() {
                    @Override
                    public void undo() {
                        removeCardFromDeck(remove);
                        getCurPlayer().addCard(remove);
                    }
                });
                break;
            }
            case SABOTEUR_CARD: {
                List<Integer> sabotagePlayers = computeSaboteurPlayers(this, getCurPlayerNum());
                boolean done = false;
                pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                for (int pNum : sabotagePlayers) {
                    Player p = getPlayerByPlayerNum(pNum);
                    int num = p.getUnusedCardCount() / 2;
                    if (num > 0) {
                        done = true;
                        for (int i = 0; i < num; i++) {
                            pushStateFront(State.GIVE_UP_CARD, num);
                        }
                        pushStateFront(State.SET_PLAYER, pNum);
                    }
                }
                if (done) {
                    putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Saboteur));
                }
                break;
            }
            case SMITH_CARD: {
                // promote 2 knights for free
                final List<Integer> knights = computePromoteKnightVertexIndices(getCurPlayer(), mBoard);
                if (knights.size() > 0) {
                    final Card removed = getCurPlayer().removeCard(ProgressCardType.Smith);
                    putCardBackInDeck(removed);
                    if (knights.size() > 1) {
                        // remember the knights we have so that we can revert
                        final HashMap<Integer, VertexType> currentKnights = new HashMap<>();
                        for (int k : knights) {
                            currentKnights.put(k, mBoard.getVertex(k).getType());
                        }

                        // this one we be chosen second
                        pushStateFront(State.CHOOSE_KNIGHT_TO_PROMOTE, null, null, new UndoAction() {

                            @Override
                            public void undo() {
                                for (int k : knights) {
                                    mBoard.getVertex(k).setType(currentKnights.get(k));
                                }
                                getCurPlayer().addCard(removed);
                                removeCardFromDeck(removed);
                            }
                        });
                    }
                    // this one will be chosen first
                    pushStateFront(State.CHOOSE_KNIGHT_TO_PROMOTE, null, knights, new UndoAction() {

                        @Override
                        public void undo() {
                            if (knights.size() > 1)
                                popState();

                            getCurPlayer().addCard(removed);
                            removeCardFromDeck(removed);
                        }
                    });
                }
                break;
            }
            case SPY_CARD: {
                // steal a players progress cards
                List<Integer> players = computeSpyOpponents(this, getCurPlayerNum());
                if (players.size() > 0) {
                    pushStateFront(State.CHOOSE_PLAYER_TO_SPY_ON, null, players, null);
                }
                break;
            }
            case TRADE_MONOPOLY_CARD: {
                pushStateFront(State.CHOOSE_TRADE_MONOPOLY);
                break;
            }
            case WARLORD_CARD: {
                // Activate all knights
                List<Integer> knights = computeWarlordVertices(mBoard, getCurPlayerNum());
                if (knights.size() > 0) {
                    for (int vIndex : knights) {
                        mBoard.getVertex(vIndex).activateKnight();
                    }
                    putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Warlord));
                }
                break;
            }
            case WEDDING_CARD: {
                pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                List<Integer> opponents = computeWeddingOpponents(this, getCurPlayer());
                if (opponents.size() > 0) {
                    putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Wedding));
                    for (int num : opponents) {
                        Player p = getPlayerByPlayerNum(num);
                        // automatically give
                        List<Card> cards = p.getCards(CardType.Commodity);
                        cards.addAll(p.getCards(CardType.Resource));
                        pushStateFront(State.CHOOSE_GIFT_CARD, getCurPlayer(), opponents);
                        if (cards.size() > 1)
                            pushStateFront(State.CHOOSE_GIFT_CARD, getCurPlayer());
                        pushStateFront(State.SET_PLAYER, p.getPlayerNum());

                    }
                }
                break;
            }

            case KNIGHT_ATTACK_ROAD: {
                pushStateFront(State.CHOOSE_ROAD_TO_ATTACK);
                break;
            }

            case KNIGHT_ATTACK_STRUCTURE: {
                pushStateFront(State.CHOOSE_STRUCTURE_TO_ATTACK);
                break;
            }

            case ATTACK_SHIP: {
                pushStateFront(State.CHOOSE_SHIP_TO_ATTACK);
                break;
            }
        }
    }

    public static List<Integer> computeSpyOpponents(SOC soc, int playerNum) {
        List<Integer> players = new ArrayList<Integer>();
        for (Player p : soc.getPlayers()) {
            if (p.getPlayerNum() != playerNum) {
                if (p.getUnusedCardCount(CardType.Progress) > 0) {
                    players.add(p.getPlayerNum());
                }
            }
        }
        return players;
    }

    public static int computeNumStructuresAdjacentToTileType(int playerNum, Board b, TileType type) {
        int num = 0;
        for (Tile t : b.getTiles()) {
            if (t.getType() != type)
                continue;
            for (Vertex v : b.getTileVertices(t)) {
                if (v.isStructure() && v.getPlayer() == playerNum) {
                    num++;
                }
            }
        }
        return num;
    }

    public static List<Integer> computeMasterMerchantPlayers(SOC soc, Player player) {
        List<Integer> players = new ArrayList<Integer>();
        for (Player p : soc.getPlayers()) {
            if (p.getPlayerNum() == player.getPlayerNum())
                continue;
            if (p.getPoints() > player.getPoints()) {
                if (p.getCardCount(CardType.Commodity) > 0 || p.getCardCount(CardType.Resource) > 0)
                    players.add(p.getPlayerNum());
            }
        }
        return players;
    }

    private void putCardBackInDeck(Card card) {
        switch (card.getCardType()) {
            case Development:
                mDevelopmentCards.add(card);
                break;
            case Progress: {
                int index = ProgressCardType.values()[card.getTypeOrdinal()].type.ordinal();
                mProgressCards[index].add(card);
                break;
            }
            case Commodity:
            case Resource: // ignore, these are infinite
                break;
            default:
                throw new SOCException("Should not happen");
        }
    }

    private void removeCardFromDeck(Card card) {
        boolean success = false;
        switch (card.getCardType()) {
            case Development:
                success = mDevelopmentCards.remove(card);
                break;
            case Progress: {
                int index = ProgressCardType.values()[card.getTypeOrdinal()].type.ordinal();
                success = mProgressCards[index].remove(card);
                break;
            }
            case Commodity:
            case Resource: // ignore, these are infinite
                success = true;
                break;
            default:
                throw new SOCException("Should not happen");
        }
        Utils.assertTrue (success);
    }

    /**
     * Called when a trade completed as event for the user to handle if they wish
     * base method does nothing.
     *
     * @param playerNum
     * @param trade
     */
    protected void onCardsTraded(int playerNum, Trade trade) {
    }

    /**
     * Called when a player has discovered a new island for app to add any logic they want.
     *
     * @param playerNum
     * @param islandIndex
     */
    protected void onPlayerDiscoveredIsland(int playerNum, int islandIndex) {
    }

    /**
     * Executed when game is in a good state for saving.
     */
    protected void onShouldSaveGame() {
    }

    /**
     * @param playerNum
     * @param routeIndex
     */
    protected void onPlayerShipUpgraded(int playerNum, int routeIndex) {
    }

    /**
     * Called when a players ship get taken over by another
     *
     * @param takerNum
     * @param shipTakenRouteIndex
     */
    protected void onPlayerShipComandeered(int takerNum, int shipTakenRouteIndex) {
    }

    /**
     * Called when a player ship is destroyed during an attack.
     *
     * @param routeIndex
     */
    protected void onPlayerShipDestroyed(int routeIndex) {
    }

    private void givePlayerSpecialVictoryCard(Player player, SpecialVictoryType card) {
        Player current = getSpecialVictoryPlayer(card);
        if (current != player) {
            if (current != null)
                current.removeCard(card);
            if (player != null)
                player.addCard(card);
        }
    }

    /**
     * Set the current largest army player
     *
     * @param player
     */
    public void setLargestArmyPlayer(Player player) {
        givePlayerSpecialVictoryCard(player, SpecialVictoryType.LargestArmy);
    }

    /**
     * Set the current longest road player
     *
     * @param player
     */
    public void setLongestRoadPlayer(Player player) {
        givePlayerSpecialVictoryCard(player, SpecialVictoryType.LongestRoad);
    }

    /**
     * @param player
     */
    public void setHarborMasterPlayer(Player player) {
        givePlayerSpecialVictoryCard(player, SpecialVictoryType.HarborMaster);
    }

    /**
     * @param player
     */
    public void setExplorer(Player player) {
        givePlayerSpecialVictoryCard(player, SpecialVictoryType.Explorer);
    }

    /**
     * Return true when it is legal for a player to cancel from their current Move.
     *
     * @return
     */
    public boolean canCancel() {
        if (mStateStack.empty())
            return false;
        return getState().canCancel;
    }

    /**
     * @return
     */
    public boolean isPirateAttacksEnabled() {
        return getRules().isEnableSeafarersExpansion() && getBoard().getPirateRouteStartTile() >= 0;
    }

    /**
     * Typically this operation causes the game to revert a state.
     */
    public void cancel() {
        if (!canCancel()) {
            log.error("Calling cancel when cancel not allowed");
            return;
        }

        UndoAction undoAction = getUndoAction();
        popState();
        if (undoAction != null) {
            undoAction.undo();
        }

    }

    /**
     * @param playerNum
     * @param tileIndex
     */
    protected void onDiscoverTerritory(int playerNum, int tileIndex) {
    }

    private void checkForDiscoveredNewTerritory(int vIndex) {
        int[] die = {2, 3, 4, 5, 6, 8, 9, 10, 11, 12};
        for (Tile tile : mBoard.getVertexTiles(vIndex)) {
            if (tile.getType() == TileType.UNDISCOVERED) {
                int[] chances = new int[TileType.values().length];
                for (TileType t : TileType.values()) {
                    chances[t.ordinal()] = t.chanceOnUndiscovered;
                }
                int index = Utils.chooseRandomFromSet(chances);
                TileType newType = TileType.values()[index];
                tile.setType(newType);
                Utils.assertTrue (newType.chanceOnUndiscovered > 0);
                int resourceBonus = getRules().getNumResourcesForDiscoveredTerritory();
                onDiscoverTerritory(getCurPlayerNum(), getBoard().getTileIndex(tile));

                // apply bonus of any
                switch (newType) {
                    case GOLD: {
                        // choose random number
                        tile.setDieNum(die[Utils.rand() % die.length]);
                        for (int i = 0; i < resourceBonus; i++)
                            pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
                        break;
                    }
                    case FIELDS:
                    case FOREST:
                    case HILLS:
                    case MOUNTAINS:
                    case PASTURE:
                        tile.setDieNum(die[Utils.rand() % die.length]);
                        getCurPlayer().incrementResource(tile.getResource(), resourceBonus);
                        onDistributeResources(getCurPlayerNum(), tile.getResource(), resourceBonus);
                        break;
                    default:
                        break;

                }

                printinfo(getString("%1$s has discovered a new territory: %2$s", getCurPlayer().getName(), newType));
                getCurPlayer().incrementDiscoveredTerritory(1);
                updateExplorerPlayer();
                for (Route r : mBoard.getTileRoutes(tile)) {
                    if (tile.isWater())
                        r.setAdjacentToWater(true);
                    else if (tile.isLand())
                        r.setAdjacentToLand(true);
                }

                for (Vertex v : mBoard.getTileVertices(tile)) {
                    if (tile.isWater())
                        v.setAdjacentToWater(true);
                    if (tile.isLand())
                        v.setAdjacentToLand(true);
                }
            }
        }
    }

    private void checkForDiscoveredIsland(int vIndex) {
        for (Tile t : mBoard.getVertexTiles(vIndex)) {
            if (t.getIslandNum() > 0) {
                if (!mBoard.isIslandDiscovered(getCurPlayerNum(), t.getIslandNum())) {
                    mBoard.setIslandDiscovered(getCurPlayerNum(), t.getIslandNum(), true);
                    printinfo(getString("%s has discovered an island", getCurPlayer().getName()));
                    onPlayerDiscoveredIsland(getCurPlayerNum(), t.getIslandNum());
                }
            }
        }
    }

    /**
     * Called when a player road becomes blocked, resulting is loss of road length.  Only used when Config.ENABLE_ROAD_BLOCK enabled.
     *
     * @param playerNum
     * @param oldLen
     * @param newLen
     */
    protected void onPlayerRoadLengthChanged(int playerNum, int oldLen, int newLen) {
    }

    // call this whenever a vertex type changes from open to anything or vise versa
    private void updatePlayerRoadsBlocked(int vIndex) {
        if (getRules().isEnableRoadBlock()) {

            int pNum = getBoard().checkForPlayerRouteBlocked(vIndex);
            if (pNum > 0) {
                mBoard.clearRouteLenCache();
                int len = mBoard.computeMaxRouteLengthForPlayer(pNum, getRules().isEnableRoadBlock());
                Player p = getPlayerByPlayerNum(pNum);
                if (len != p.getRoadLength()) {
                    onPlayerRoadLengthChanged(p.getPlayerNum(), p.getRoadLength(), len);
                }
                p.setRoadLength(len);
                updateLongestRoutePlayer();
            }
        }
    }

    /**
     * compute the player num who should have the longest road, or 0 if none exists.
     * soc is not changed.
     *
     * @param soc
     * @return
     */
    public static Player computeLongestRoadPlayer(SOC soc) {
        int maxRoadLen = soc.getRules().getMinLongestLoadLen() - 1;
        Player curLRP = soc.getLongestRoadPlayer();
        if (curLRP != null)
            maxRoadLen = Math.max(maxRoadLen, curLRP.getRoadLength());

        Player maxRoadLenPlayer = curLRP;
        for (Player cur : soc.getPlayers()) {
            int len = cur.getRoadLength();
            if (len > maxRoadLen) {
                maxRoadLen = len;
                maxRoadLenPlayer = cur;
            }
        }

        if (maxRoadLenPlayer == null)
            return null;

        if (maxRoadLenPlayer.getRoadLength() >= soc.getRules().getMinLongestLoadLen())
            return maxRoadLenPlayer;

        return null;
    }

    /**
     * compute the player who should have the largest army.
     * soc is not changed.
     *
     * @param soc
     * @return
     */
    public static Player computeLargestArmyPlayer(SOC soc, Board b) {
        int maxArmySize = soc.getRules().getMinLargestArmySize() - 1;
        Player curLAP = soc.getLargestArmyPlayer();
        if (curLAP != null)
            maxArmySize = Math.max(maxArmySize, curLAP.getArmySize(b));
        Player maxArmyPlayer = curLAP;
        for (Player cur : soc.getPlayers()) {
            if (cur.getArmySize(b) > maxArmySize) {
                maxArmySize = cur.getArmySize(b);
                maxArmyPlayer = cur;
            }
        }

        if (maxArmyPlayer == null)
            return null;

        if (maxArmyPlayer.getArmySize(b) >= soc.getRules().getMinLargestArmySize())
            return maxArmyPlayer;

        return null;
    }

    /**
     * Compute the player who SHOULD have the harbor master points.  This is the single player with most harbor points >= 3.
     *
     * @param soc
     * @return
     */
    public static Player computeHarborMaster(SOC soc) {
        int minHarborPts = 2;
        Player curHM = soc.getHarborMaster();
        if (curHM != null)
            minHarborPts = curHM.getHarborPoints();
        Player maxHM = curHM;
        for (Player cur : soc.getPlayers()) {
            if (cur.getHarborPoints() > minHarborPts) {
                minHarborPts = cur.getHarborPoints();
                maxHM = cur;
            }
        }

        if (maxHM == null)
            return null;

        if (maxHM.getHarborPoints() >= 3)
            return maxHM;

        return null;
    }

    public static Player computeExporer(SOC soc) {
        int minExplorerPts = soc.getRules().getMinMostDiscoveredTerritories() - 1;
        Player curE = soc.getExplorer();
        if (curE != null) {
            minExplorerPts = curE.getNumDiscoveredTerritories();
        }
        Player maxE = curE;
        for (Player cur : soc.getPlayers()) {
            if (cur.getNumDiscoveredTerritories() > minExplorerPts) {
                minExplorerPts = cur.getNumDiscoveredTerritories();
                maxE = cur;
            }
        }
        return maxE;
    }

    /**
     * Called when a player get the longest road or overtakes another player.
     * default method does nothing
     *
     * @param oldPlayerNum -1 if newPlayer is the first to get the longest road
     * @param newPlayerNum player that has the longest road or -1 if this player has lost it
     * @param maxRoadLen
     */
    protected void onLongestRoadPlayerUpdated(int oldPlayerNum, int newPlayerNum, int maxRoadLen) {
    }

    private void updateLongestRoutePlayer() {
        Player maxRoadLenPlayer = computeLongestRoadPlayer(this);
        Player curLRP = getLongestRoadPlayer();
        if (maxRoadLenPlayer == null) {
            if (curLRP != null) {
                printinfo(getString("%s is blocked and has lost the longest road!", curLRP.getName()));
                setLongestRoadPlayer(null);
                onLongestRoadPlayerUpdated(curLRP.getPlayerNum(), -1, 0);
            }
            return;
        }
        if (curLRP != null && maxRoadLenPlayer.getPlayerNum() == curLRP.getPlayerNum())
            return;
        final int maxRoadLen = maxRoadLenPlayer.getRoadLength();

        if (curLRP == null) {
            printinfo(getString("%s has gained the Longest Road!", maxRoadLenPlayer.getName()));
            onLongestRoadPlayerUpdated(-1, maxRoadLenPlayer.getPlayerNum(), maxRoadLen);
        } else if (maxRoadLenPlayer.getRoadLength() > curLRP.getRoadLength()) {
            printinfo(getString("%1$s has overtaken %2$s with the Longest Road!", maxRoadLenPlayer.getName(), curLRP.getName()));
            onLongestRoadPlayerUpdated(curLRP.getPlayerNum(), maxRoadLenPlayer.getPlayerNum(), maxRoadLen);
        }

        setLongestRoadPlayer(maxRoadLenPlayer);
    }

    /**
     * Called when a player get the largest army or overtakes another player.
     * default method does nothing
     *
     * @param oldPlayerNum -1 if newPlayer is the first to get the largest army
     * @param newPlayerNum player that has the largest army
     * @param armySize     current largest army size
     */
    protected void onLargestArmyPlayerUpdated(int oldPlayerNum, int newPlayerNum, int armySize) {
    }

    private void updateLargestArmyPlayer() {
        Player largestArmyPlayer = computeLargestArmyPlayer(this, getBoard());
        Player curLAP = getLargestArmyPlayer();
        if (largestArmyPlayer == null) {
            setLargestArmyPlayer(null);
            return;
        }
        if (curLAP != null && largestArmyPlayer.getPlayerNum() == curLAP.getPlayerNum())
            return;

        final int maxArmySize = largestArmyPlayer.getArmySize(mBoard);
        if (curLAP == null) {
            printinfo(getString("%s Has largest army and gets to take a resource card from another", largestArmyPlayer.getName()));
            onLargestArmyPlayerUpdated(-1, largestArmyPlayer.getPlayerNum(), maxArmySize);
        } else if (largestArmyPlayer.getArmySize(mBoard) > curLAP.getArmySize(mBoard)) {
            printinfo(getString("%1$s overtakes %2$s for the largest Army!", largestArmyPlayer.getName(), curLAP.getName()));
            onLargestArmyPlayerUpdated(curLAP.getPlayerNum(), largestArmyPlayer.getPlayerNum(), maxArmySize);
        }

        setLargestArmyPlayer(largestArmyPlayer);
    }

    /**
     * @param oldPlayerNum player losing the points or -1 if this is first time
     * @param newPlayerNum plyer gaining the points
     * @param harborPts
     */
    protected void onHarborMasterPlayerUpdated(int oldPlayerNum, int newPlayerNum, int harborPts) {
    }

    private void updateHarborMasterPlayer() {
        Player harborMaster = computeHarborMaster(this);
        Player curHM = getHarborMaster();
        if (harborMaster == null) {
            setHarborMasterPlayer(null);
            return;
        }

        if (curHM != null && harborMaster.getPlayerNum() == curHM.getPlayerNum())
            return;

        final int maxHP = harborMaster.getHarborPoints();
        if (curHM == null) {
            printinfo(getString("%s is the Harbor Master!", harborMaster.getName()));
            onHarborMasterPlayerUpdated(-1, harborMaster.getPlayerNum(), maxHP);
        } else {
            printinfo(getString("%1$s overthrows %2$s as the new Harbor Master!", harborMaster.getName(), curHM.getName()));
            onHarborMasterPlayerUpdated(curHM.getPlayerNum(), harborMaster.getPlayerNum(), maxHP);
        }

        setHarborMasterPlayer(harborMaster);
    }

    /**
     * @param oldPlayerName player losing the points or -1 if this is first time giving points
     * @param newPlayerNum  player gaining the points
     * @param harborPts
     */
    protected void onExplorerPlayerUpdated(int oldPlayerName, int newPlayerNum, int harborPts) {
    }

    private void updateExplorerPlayer() {
        Player explorer = computeExporer(this);
        Player curE = getExplorer();
        if (explorer == null) {
            setExplorer(null);
            return;
        }

        if (curE != null && explorer.getPlayerNum() == curE.getPlayerNum())
            return;

        final int maxE = explorer.getNumDiscoveredTerritories();
        if (curE == null) {
            printinfo(getString("%s is an Explorer!", explorer.getName()));
            onExplorerPlayerUpdated(-1, explorer.getPlayerNum(), maxE);
        } else {
            printinfo(getString("%1$s overtakes %2$s as the best explorer!", explorer.getName(), curE.getName()));
            onExplorerPlayerUpdated(curE.getPlayerNum(), explorer.getPlayerNum(), maxE);
        }

        setExplorer(explorer);
    }

    /**
     * Return a list of vertices available for a settlement given a player and board instance.
     *
     * @param soc
     * @param playerNum
     * @param b
     * @return
     */
    static public List<Integer> computeSettlementVertexIndices(SOC soc, int playerNum, Board b) {

        // un-owned settlements must be chosen first (pirate islands)
        List<Integer> vertices = b.getVertIndicesOfType(0, VertexType.OPEN_SETTLEMENT);
        if (vertices.size() > 0)
            return vertices;

        // build an array of vertices legal for the current player
        // to place a settlement.
        for (int i = 0; i < b.getNumAvailableVerts(); i++) {
            Vertex v = b.getVertex(i);
            if (v.getType() != VertexType.OPEN)
                continue;
            if (!v.canPlaceStructure())
                continue;
            boolean isOnIsland = false;
            boolean canAdd = true;
            for (Tile cell : b.getVertexTiles(i)) {
                if (cell.getIslandNum() > 0) {
                    isOnIsland = true;
                } else if (cell.getType() == TileType.UNDISCOVERED) {
                    canAdd = false;
                    break;
                }
            }

            if (!canAdd)
                continue;

            boolean isOnRoute = false;
            for (int ii = 0; ii < v.getNumAdjacentVerts(); ii++) {
                int iv = b.findAdjacentVertex(i, ii);
                if (iv >= 0) {
                    Vertex v2 = b.getVertex(iv);
                    if (v2.isStructure()) {
                        canAdd = false;
                        break;
                    }

                    int ie = b.getRouteIndex(i, iv);
                    if (ie >= 0) {
                        Route e = b.getRoute(ie);
                        if (e.getPlayer() == playerNum) {
                            if (e.getType() == RouteType.DAMAGED_ROAD) {
                                canAdd = false;
                                break;
                            }
                            isOnRoute = true;
                        }
                    }
                }
            }

            if (!canAdd)
                continue;

            if (b.getNumStructuresForPlayer(playerNum) < (soc == null ? 2 : soc.getRules().getNumStartSettlements())) {
                if (soc.getRules().isEnableIslandSettlementsOnSetup() || !isOnIsland)
                    vertices.add(i);
            } else {
                if (isOnRoute)
                    vertices.add(i);
            }
        }
        return vertices;
    }

    /**
     * Return a list of edges available for a road given a player and board instance.
     *
     * @param playerNum
     * @param b
     * @return
     */
    static public List<Integer> computeRoadRouteIndices(int playerNum, Board b) {
        //if (Profiler.ENABLED) Profiler.push("SOC::computeRoadOptions");
        try {

            List<Integer> edges = new ArrayList<Integer>();
            for (int i = 0; i < b.getNumRoutes(); i++) {
                if (b.isRouteAvailableForRoad(i, playerNum))
                    edges.add(i);
            }
            return edges;
        } finally {
            //if (Profiler.ENABLED) Profiler.pop("SOC::computeRoadOptions");
        }
    }

    static public List<Integer> computeShipRouteIndices(SOC soc, int playerNum, Board b) {
        //if (Profiler.ENABLED) Profiler.push("SOC::computeRoadOptions");
        try {

            List<Integer> edges = new ArrayList<>();
            for (int i = 0; i < b.getNumRoutes(); i++) {
                if (b.isRouteAvailableForShip(soc.getRules(), i, playerNum))
                    edges.add(i);
            }
            return edges;
        } finally {
            //if (Profiler.ENABLED) Profiler.pop("SOC::computeRoadOptions");
        }
    }

    /**
     * return a list of vertices a new level 1 knight can be placed (basically any vertex that is on the road of a player that is empty)
     *
     * @param playerNum
     * @param b
     * @return
     */
    static public List<Integer> computeNewKnightVertexIndices(int playerNum, Board b) {
        HashSet<Integer> verts = new HashSet<Integer>();
        // any open vertex on a players road will suffice
        for (Route r : b.getRoutes()) {
            if (r.getPlayer() != playerNum)
                continue;
            if (b.getVertex(r.getFrom()).getType() == VertexType.OPEN)
                verts.add(r.getFrom());
            if (b.getVertex(r.getTo()).getType() == VertexType.OPEN)
                verts.add(r.getTo());
        }
        return new ArrayList<>(verts);
    }

    /**
     * Return list of vertices where a knight can be promoted (this assumes the player has passed the canBuild(Knight) test)
     *
     * @param p
     * @param b
     * @return
     */
    static public List<Integer> computePromoteKnightVertexIndices(Player p, Board b) {
        List<Integer> verts = new ArrayList<>();
        for (int vIndex = 0; vIndex < b.getNumAvailableVerts(); vIndex++) {
            Vertex v = b.getVertex(vIndex);
            if (v.getPlayer() == p.getPlayerNum()) {
                switch (v.getType()) {
                    case BASIC_KNIGHT_ACTIVE:
                    case BASIC_KNIGHT_INACTIVE:
                        verts.add(vIndex);
                        break;
                    case STRONG_KNIGHT_ACTIVE:
                    case STRONG_KNIGHT_INACTIVE:
                        if (p.hasFortress()) {
                            verts.add(vIndex);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        return verts;
    }

    static public List<Integer> computeDiplomatOpenRouteIndices(SOC soc, Board b) {
        List<Integer> allOpenRoutes = new ArrayList<>();
        for (int i = 1; i <= soc.getNumPlayers(); i++) {
            allOpenRoutes.addAll(computeOpenRouteIndices(i, b, true, false));
        }
        return allOpenRoutes;
    }

    static public List<Integer> computeMovableShips(SOC soc, Player p, Board b) {
        // check for movable ships
        List<Integer> movableShips = new ArrayList<>();
        Collection<Integer> ships = computeOpenRouteIndices(p.getPlayerNum(), b, false, true);
        for (int ship : ships) {
            Route shipToMove = b.getRoute(ship);
            RouteType shipType = shipToMove.getType();
            b.setRouteOpen(shipToMove);
            shipToMove.setLocked(true);
            Collection<Integer> openRoutes = computeShipRouteIndices(soc, p.getPlayerNum(), b);
            if (openRoutes.size() > 0) {
                movableShips.add(ship);
            }
            b.setPlayerForRoute(shipToMove, p.getPlayerNum(), shipType);
            b.getRoute(ship).setLocked(false);
        }
        return movableShips;
    }

    static public List<Integer> computeOpenRouteIndices(int playerNum, Board b, boolean checkRoads, boolean checkShips) {
        Set<Integer> options = new HashSet<Integer>();
        for (int eIndex : b.getRouteIndicesForPlayer(playerNum)) {
            Route e = b.getRoute(eIndex);
            // check the obvious
            if (e.isClosed())
                continue;
            if (e.isLocked())
                continue;
            if (e.getType().isVessel) {
                if (!checkShips)
                    continue;
                if (e.isAttacked())
                    continue;
            } else {
                if (!checkRoads)
                    continue;
            }

            // if either vertex is the players settlement, then not movable
            Vertex v0 = b.getVertex(e.getFrom());
            Vertex v1 = b.getVertex(e.getTo());

            if (v0.getType().isStructure && v0.getPlayer() == playerNum)
                continue;
            if (v1.getType().isStructure && v1.getPlayer() == playerNum)
                continue;
            // if there is a route on EACH end, then not open
            int numConnected = 0;
            for (Route ee : b.getVertexRoutes(e.getFrom())) {
                if (ee != e && (!checkShips || ee.isVessel()) && ee.getPlayer() == playerNum) {
                    numConnected++;
                    break;
                }
            }
            for (Route ee : b.getVertexRoutes(e.getTo())) {
                if (ee != e && (!checkShips || ee.isVessel()) && ee.getPlayer() == playerNum) {
                    numConnected++;
                    break;
                }
            }
            if (numConnected < 2) {
                options.add(eIndex);
            }
        }
        return new ArrayList<>(options);
    }

    /**
     * Return list of verts a knight can move to including those where they can displace another player knight.
     * These are all open verts that lie on the same route as the knight.  If expanded knight moves enabled
     * then this includes the vertices that are one unit away, on land, and not on a route.
     *
     * @param knightVertex
     * @param b
     * @return
     */
    static public List<Integer> computeKnightMoveVertexIndices(SOC soc, int knightVertex, Board b) {
        List<Integer> verts = new ArrayList<>();
        Vertex knight = b.getVertex(knightVertex);
        Utils.assertTrue (knight != null);
        Utils.assertTrue (knight.getType().getKnightLevel() > 0);
        boolean[] visitedVerts = new boolean[b.getNumAvailableVerts()];
        visitedVerts[knightVertex] = true;
        findReachableVertsR(b, knight, knightVertex, verts, visitedVerts);
        if (soc.getRules().isEnableKnightExtendedMoves()) {
            Vertex v = b.getVertex(knightVertex);
            for (int i = 0; i < v.getNumAdjacentVerts(); i++) {
                int vIndex = v.getAdjacentVerts()[i];
                Vertex v2 = b.getVertex(vIndex);
                if (v2.getType() == VertexType.OPEN && v2.isAdjacentToLand())
                    verts.add(vIndex);
            }
        }
        return verts;
    }

    static public List<Integer> computeActivateKnightVertexIndices(int playerNum, Board b) {
        return b.getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
    }

    static public List<Integer> computeDisplacedKnightVertexIndices(SOC soc, int displacedKnightVertex, Board b) {
        return computeKnightMoveVertexIndices(soc, displacedKnightVertex, b);
    }

    static public List<Integer> computeMovableKnightVertexIndices(SOC soc, int playerNum, Board b) {
        List<Integer> knights = b.getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE);
        List<Integer> movableKnights = new ArrayList<>();
        for (int kIndex : knights) {
            if (computeKnightMoveVertexIndices(soc, kIndex, b).size() > 0) {
                movableKnights.add(kIndex);
            }
        }
        return movableKnights;
    }

    private static void findReachableVertsR(Board b, Vertex knight, int startVertex, List<Integer> verts, boolean[] visitedVerts) {
        Vertex start = b.getVertex(startVertex);
        for (int i = 0; i < start.getNumAdjacentVerts(); i++) {
            int vIndex = start.getAdjacentVerts()[i];
            if (visitedVerts[vIndex])
                continue;
            visitedVerts[vIndex] = true;
            int rIndex = b.getRouteIndex(startVertex, vIndex);
            Route r = b.getRoute(rIndex);
            if (r.getPlayer() != knight.getPlayer())
                continue;
            Vertex v = b.getVertex(vIndex);
            if (v.getType() == VertexType.OPEN)
                verts.add(vIndex);
            else if (v.isKnight() && v.getPlayer() != knight.getPlayer() && knight.getType().isKnightActive()) {
                int kl = v.getType().getKnightLevel();
                if (kl > 0 && kl < knight.getType().getKnightLevel()) {
                    verts.add(vIndex); // TODO: we can move to this vertex but not pass it?
                }
                continue;
            }
            findReachableVertsR(b, knight, vIndex, verts, visitedVerts);
        }
    }

    /**
     * Return a list of vertices available for a city given a player and board intance.
     *
     * @param playerNum
     * @param b
     * @return
     */
    static public List<Integer> computeCityVertxIndices(int playerNum, Board b) {
        return b.getVertIndicesOfType(playerNum, VertexType.SETTLEMENT);
    }

    /**
     * Return a list of vertices available for a city given a player and board instance.
     *
     * @param playerNum
     * @param b
     * @return
     */
    static public List<Integer> computeCityWallVertexIndices(int playerNum, Board b) {
        return b.getVertIndicesOfType(playerNum, VertexType.CITY);
    }

    /**
     * Return a list of vertices available for a metropolis given a player and board instance
     *
     * @param playerNum
     * @param b
     * @return
     */
    static public List<Integer> computeMetropolisVertexIndices(int playerNum, Board b) {
        return b.getVertIndicesOfType(playerNum, VertexType.CITY, VertexType.WALLED_CITY);
    }

    /**
     * Return a list of MoveTypes available given a player and board instance.
     *
     * @param p
     * @param b
     * @return
     */
    static public List<MoveType> computeMoves(Player p, Board b, SOC soc) {
        LinkedHashSet<MoveType> types = new LinkedHashSet<>();
        types.add(MoveType.CONTINUE);

        if (p.canBuild(BuildableType.City) && b.getNumSettlementsForPlayer(p.getPlayerNum()) > 0)
            types.add(MoveType.BUILD_CITY);

        if (!soc.getRules().isEnableCitiesAndKnightsExpansion()) {
            if (p.canBuild(BuildableType.Development))
                types.add(MoveType.DRAW_DEVELOPMENT);

            for (DevelopmentCardType t : DevelopmentCardType.values()) {
                if (t.moveType != null && p.getUsableCardCount(t) > 0) {
                    types.add(t.moveType);
                }
            }
        }

        if (canPlayerTrade(p, b))
            types.add(MoveType.TRADE);

        if (p.canBuild(BuildableType.Settlement)) {
            for (int i = 0; i < b.getNumAvailableVerts(); i++) {
                if (b.isVertexAvailbleForSettlement(i) && b.isVertexAdjacentToPlayerRoute(i, p.getPlayerNum())) {
                    types.add(MoveType.BUILD_SETTLEMENT);
                    break;
                }
            }
        }

        if (p.canBuild(BuildableType.Road)) {
            if (p.getCardCount(SpecialVictoryType.DamagedRoad) > 0) {
                types.add(MoveType.REPAIR_ROAD);
            } else {
                for (int i = 0; i < b.getNumRoutes(); i++) {
                    if (b.isRouteAvailableForRoad(i, p.getPlayerNum())) {
                        types.add(MoveType.BUILD_ROAD);
                        break;
                    }
                }
            }
        }

        if (soc.getRules().isEnableSeafarersExpansion()) {
            if (p.canBuild(BuildableType.Ship)) {
                for (int i = 0; i < b.getNumRoutes(); i++) {
                    if (b.isRouteAvailableForShip(soc.getRules(), i, p.getPlayerNum())) {
                        types.add(MoveType.BUILD_SHIP);
                        break;
                    }
                }
            }

            if (soc.getRules().isEnableWarShipBuildable()) {
                if (p.canBuild(BuildableType.Warship) && b.getRoutesOfType(p.getPlayerNum(), RouteType.SHIP).size() > 0) {
                    types.add(MoveType.BUILD_WARSHIP);
                }
                if (SOC.computeAttackableShips(soc, p.getPlayerNum(), b).size() > 0) {
                    types.add(MoveType.ATTACK_SHIP);
                }
            }

            // check for movable ships
            if (computeMovableShips(soc, p, b).size() > 0) {
                types.add(MoveType.MOVE_SHIP);
            }

            if (b.getRoutesOfType(p.getPlayerNum(), RouteType.WARSHIP).size() > 1) {
                for (int vIndex : b.getVertIndicesOfType(0, VertexType.PIRATE_FORTRESS)) {
                    for (Route r : b.getVertexRoutes(vIndex)) {
                        if (r.getType().isVessel && r.getPlayer() == p.getPlayerNum()) {
                            types.add(MoveType.ATTACK_PIRATE_FORTRESS);
                            break;
                        }
                    }
                }
            }
        }

        if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {

            for (ProgressCardType t : ProgressCardType.values()) {
                if (t.moveType != null && p.getUsableCardCount(t) > 0) {
                    types.add(t.moveType);
                }
            }

            if (p.canBuild(BuildableType.CityWall)) {
                if (b.getNumVertsOfType(p.getPlayerNum(), VertexType.CITY) > 0) {
                    types.add(MoveType.BUILD_CITY_WALL);
                }
            }

            if (p.canBuild(BuildableType.Knight)) {
                if (b.getOpenKnightVertsForPlayer(p.getPlayerNum()).size() > 0) {
                    types.add(MoveType.HIRE_KNIGHT);
                }
            }

            if (p.canBuild(BuildableType.PromoteKnight)) {
                if (computePromoteKnightVertexIndices(p, b).size() > 0)
                    types.add(MoveType.PROMOTE_KNIGHT);
            }

            if (p.canBuild(BuildableType.ActivateKnight)) {
                if (computeActivateKnightVertexIndices(p.getPlayerNum(), b).size() > 0)
                    types.add(MoveType.ACTIVATE_KNIGHT);
            }

            for (int vIndex : b.getVertIndicesOfType(p.getPlayerNum(), VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE)) {
                if (computeKnightMoveVertexIndices(soc, vIndex, b).size() > 0) {
                    types.add(MoveType.MOVE_KNIGHT);
                    break;
                }
            }

            int numCities = b.getNumVertsOfType(p.getPlayerNum(), VertexType.CITY, VertexType.WALLED_CITY);
            int numMetros = b.getNumVertsOfType(p.getPlayerNum(), VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);

            // metropolis:
            //   Player must have at least 1 city / metropolis to make any improvements
            //   There are only 3 metropolis in play at any one time.
            //   Must have a non-metropolis city to increase beyond level 3 in any area
            //   First player to reach level 4 in an area, gets to convert one of their cities to a metropolis
            //   When a player reaches level 5 they can take the metropolis from another player with level 4
            //   Once a metropolis is at level 5 it cannot be taken away
            //   Can a player attain level 5 if someone else has a level 5 metro in that area? (no for now)

            if (numCities > 0 || numMetros > 0) {
                for (DevelopmentArea area : DevelopmentArea.values()) {
                    int devel = p.getCityDevelopment(area);
                    Utils.assertTrue (devel <= DevelopmentArea.MAX_CITY_IMPROVEMENT);
                    if (devel >= DevelopmentArea.MAX_CITY_IMPROVEMENT || p.getCardCount(area.commodity) <= devel) {
                        continue;
                    }

                    if (devel < DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT - 1) {
                        types.add(area.move);
                        continue;
                    }

                    if (soc.getMetropolisPlayer(area) != p.getPlayerNum() && numCities <= 0) {
                        continue;
                    }

                    if (devel <= DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT) {
                        types.add(area.move);
                        continue;
                    }

                    if (soc.getMetropolisPlayer(area) > 0) {
                        Player o = soc.getPlayerByPlayerNum(soc.getMetropolisPlayer(area));
                        if (o.getCityDevelopment(area) >= DevelopmentArea.MAX_CITY_IMPROVEMENT)
                            continue; // cant advance to max if someone else already has (TODO: confirm this rule or make config)
                    }

                    types.add(area.move);
                }
            }

            if (computeAttackableRoads(soc, p.getPlayerNum(), b).size() > 0) {
                types.add(MoveType.KNIGHT_ATTACK_ROAD);
            }

            if (computeAttackableStructures(soc, p.getPlayerNum(), b).size() > 0) {
                types.add(MoveType.KNIGHT_ATTACK_STRUCTURE);
            }
        }

        ArrayList<MoveType> moves = new ArrayList<MoveType>(types);
        Collections.sort(moves, new Comparator<MoveType>() {

            @Override
            public int compare(MoveType o1, MoveType o2) {
                return o1.priority - o2.priority;
            }

        });

        return moves;
    }

    static public List<Integer> computePirateTileIndices(SOC soc, Board b) {
        if (soc.isPirateAttacksEnabled())
            return Collections.emptyList();
        List<Integer> cellIndices = new ArrayList<>();
        for (int i = 0; i < b.getNumTiles(); i++) {
            Tile cell = b.getTile(i);
            if (cell.isWater()) {
                if (soc.getRules().isEnableWarShipBuildable() && b.getPirateRouteStartTile() < 0) {
                    if (b.getTileRoutesOfType(cell, RouteType.WARSHIP).size() == 0)
                        cellIndices.add(i);
                } else {
                    cellIndices.add(i);
                }
            }
        }
        return cellIndices;
    }

    /**
     * Return a list of cells available for a robber given a board instance.
     *
     * @param b
     * @return
     */
    static public List<Integer> computeRobberTileIndices(SOC soc, Board b) {
        List<Integer> cellIndices = new ArrayList<>();
        if (!soc.getRules().isEnableRobber())
            return cellIndices;
//		boolean desertIncluded = false;
        for (int i = 0; i < b.getNumTiles(); i++) {
            Tile cell = b.getTile(i);
            if (!cell.isLand())
                continue;
            // only test tiles that has opposing players on them
            boolean addTile = true;
            for (int vIndex = 0; vIndex < cell.getNumAdj(); vIndex++) {
                Vertex v = b.getVertex(cell.getAdjVert(vIndex));
                if (v.getPlayer() > 0 && v.getPlayer() != soc.getCurPlayerNum()) {
                    if (v.isKnight()) {
                        addTile = false;
                        break;
                    }
                    if (soc.getPlayerByPlayerNum(v.getPlayer()).getSpecialVictoryPoints() < soc.getRules().getMinVictoryPointsForRobber()) {
                        addTile = false;
                        break;
                    }
                }
            }

            if (addTile) {
                cellIndices.add(i);
            }
        }
        return cellIndices;
    }

    /**
     * Return a list of cells available for a merchant given a board instance.
     *
     * @param b
     * @return
     */
    static public List<Integer> computeMerchantTileIndices(SOC soc, int playerNum, Board b) {
        List<Integer> cellIndices = new ArrayList<>();
        for (int i = 0; i < b.getNumTiles(); i++) {
            if (b.getRobberTileIndex() == i)
                continue;
            Tile cell = b.getTile(i);
            if (cell.isDistributionTile() && cell.getResource() != null) {
                for (Vertex v : b.getTileVertices(cell)) {
                    if (v.isStructure() && v.getPlayer() == playerNum) {
                        cellIndices.add(i);
                        break;
                    }
                }
            }
        }
        return cellIndices;
    }

    /**
     * Return list of players who can be sabotaged by playerNum
     *
     * @param soc
     * @param playerNum
     * @return
     */
    static public List<Integer> computeSaboteurPlayers(SOC soc, int playerNum) {
        List<Integer> players = new ArrayList<>();
        int pts = soc.getPlayerByPlayerNum(playerNum).getPoints();
        for (Player player : soc.getPlayers()) {
            if (player.getPlayerNum() == playerNum)
                continue;
            if (player.getPoints() >= pts && player.getUnusedCardCount()>1)
                players.add(player.getPlayerNum());
        }
        return players;
    }

    /**
     * Compute all the trade options given a player and board instance.
     *
     * @param p
     * @param b
     * @return
     */
    static public List<Trade> computeTrades(Player p, Board b) {
        List<Trade> trades = new ArrayList<Trade>();
        computeTrades(p, b, trades, 100);
        return trades;
    }

    static public int computeCatanStrength(SOC soc, Board b) {

        int str = 0;

        for (int i = 1; i <= soc.getNumPlayers(); i++) {
            str += b.getKnightLevelForPlayer(i, true, false);
        }

        return str;
    }

    static public int computeBarbarianStrength(SOC soc, Board b) {
        int pts = 0;
        if (soc.getRules().getBarbarianPointsPerSettlement() > 0)
            pts += b.getNumVertsOfType(0, VertexType.SETTLEMENT) * soc.getRules().getBarbarianPointsPerSettlement();
        pts += b.getNumVertsOfType(0, VertexType.CITY, VertexType.WALLED_CITY) * soc.getRules().getBarbarianPointsPerCity();
        pts += b.getNumVertsOfType(0, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE) * soc.getRules().getBarbarianPointsPerMetro();
        return pts;
    }

    static private void computeTrades(Player p, Board b, List<Trade> trades, int maxOptions) {

        //if (Profiler.ENABLED) Profiler.push("SOC::computeTradeOptions");

        try {
            int i;

            boolean[] resourcesFound = new boolean[NUM_RESOURCE_TYPES];
            boolean[] commoditiesFound = new boolean[NUM_COMMODITY_TYPES];

            // Check for Merchant Fleet
            if (p.getMerchantFleetTradable() != null) {
                if (p.getCardCount(p.getMerchantFleetTradable()) >= 2) {
                    trades.add(new Trade(p.getMerchantFleetTradable(), 2));
                    switch (p.getMerchantFleetTradable().getCardType()) {
                        case Commodity:
                            commoditiesFound[p.getMerchantFleetTradable().getTypeOrdinal()] = true;
                            break;
                        case Resource:
                            resourcesFound[p.getMerchantFleetTradable().getTypeOrdinal()] = true;
                            break;
                        default:
                            throw new SOCException("Unexpected case");

                    }
                }
            }

            if (trades.size() >= maxOptions)
                return;

            // see if we have a 2:1 trade option

            // check for Trading House ability

            // we can trade 2:1 commodities if we have level 3 or greater trade improvement (Trading House)
            if (p.hasTradingHouse()) {
                for (CommodityType type : CommodityType.values()) {
                    if (!commoditiesFound[type.ordinal()]) {
                        if (p.getCardCount(type) >= 2) {
                            trades.add(new Trade(type, 2));
                            commoditiesFound[type.ordinal()] = true;
                        }
                    }
                }
            }

            // check tiles for ports, merchant
            for (i = 0; i < b.getNumTiles(); i++) {

                if (b.getPirateTileIndex() == i)
                    continue;

                if (trades.size() >= maxOptions)
                    return;

                Tile tile = b.getTile(i);

                if (tile.getResource() == null)
                    continue;

                if (b.getMerchantTileIndex() == i && b.getMerchantPlayer() == p.getPlayerNum()) {
                    if (p.getCardCount(tile.getResource()) < 2)
                        continue;
                } else {

                    if (!tile.isPort() || null == tile.getResource())
                        continue;

                    if (!b.isPlayerAdjacentToTile(p.getPlayerNum(), i))
                        continue;

                    if (p.getCardCount(tile.getResource()) < 2)
                        continue;
                }

                if (resourcesFound[tile.getResource().ordinal()])
                    continue;

                trades.add(new Trade(tile.getResource(), 2));
                resourcesFound[tile.getResource().ordinal()] = true;
            }

            // we have a 3:1 trade option when we are adjacent to a PORT_MULTI
            for (i = 0; i < b.getNumTiles(); i++) {
                if (trades.size() >= maxOptions)
                    return;

                Tile cell = b.getTile(i);
                if (cell.getType() != TileType.PORT_MULTI)
                    continue;

                if (!b.isPlayerAdjacentToTile(p.getPlayerNum(), i))
                    continue;

                // for (int r=0; r<Helper.NUM_RESOURCE_TYPES; r++) {
                for (ResourceType r : ResourceType.values()) {
                    if (!resourcesFound[r.ordinal()] && p.getCardCount(r) >= 3) {
                        trades.add(new Trade(r, 3));
                        resourcesFound[r.ordinal()] = true;
                    }
                }

                for (CommodityType c : CommodityType.values()) {
                    if (!commoditiesFound[c.ordinal()] && p.getCardCount(c) >= 3) {
                        trades.add(new Trade(c, 3));
                        commoditiesFound[c.ordinal()] = true;
                    }
                }
            }

            // look for 4:1 trades
            for (ResourceType r : ResourceType.values()) {
                if (trades.size() >= maxOptions)
                    return;
                if (!resourcesFound[r.ordinal()] && p.getCardCount(r) >= 4) {
                    trades.add(new Trade(r, 4));
                }
            }

            for (CommodityType c : CommodityType.values()) {
                if (trades.size() >= maxOptions)
                    return;
                if (!commoditiesFound[c.ordinal()] && p.getCardCount(c) >= 4) {
                    trades.add(new Trade(c, 4));
                }
            }

        } finally {
            //if (Profiler.ENABLED) Profiler.pop("SOC::computeTradeOptions");
        }
    }

    /**
     * Compute the list of players from which 'p' can take a card
     *
     * @param p
     * @param b
     * @return
     */
    public List<Integer> computeRobberTakeOpponentCardOptions(Player p, Board b, boolean pirate) {
        return computeTakeOpponentCardPlayers(getAllPlayers(), p, b, pirate);
    }

    /**
     * @param players
     * @param p
     * @param b
     * @return
     */
    static public List<Integer> computeTakeOpponentCardPlayers(Player[] players, Player p, Board b, boolean pirate) {

        List<Integer> choices = new ArrayList<>();
        boolean[] playerNums = new boolean[players.length];

        if (pirate) {
            for (int eIndex : b.getTileRouteIndices(b.getTile(b.getPirateTileIndex()))) {
                Route e = b.getRoute(eIndex);
                if (e.getPlayer() == 0)
                    continue;
                if (e.getPlayer() == p.getPlayerNum())
                    continue;
                if (players[e.getPlayer()].getTotalCardsLeftInHand() <= 0)
                    continue;
                playerNums[e.getPlayer()] = true;
            }
        } else {
            Tile cell = b.getTile(b.getRobberTileIndex());
            for (int vIndex : cell.getAdjVerts()) {
                Vertex v = b.getVertex(vIndex);
                if (v.getPlayer() == 0)
                    continue;
                if (v.getPlayer() == p.getPlayerNum())
                    continue;
                if (players[v.getPlayer()].getTotalCardsLeftInHand() <= 0)
                    continue;
                playerNums[v.getPlayer()] = true;
            }
        }

        for (int i = 1; i < playerNums.length; i++)
            if (playerNums[i])
                choices.add(players[i].getPlayerNum());
        return choices;
    }

    /**
     * Return list of vertices for warlord card
     *
     * @param b
     * @param playerNum
     * @return
     */
    static public List<Integer> computeWarlordVertices(Board b, int playerNum) {
        return b.getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
    }

    static public List<Card> computeMerchantFleetCards(Player p) {
        List<Card> tradableCards = new ArrayList<Card>();
        for (ResourceType t : ResourceType.values()) {
            List<Card> cards = p.getUsableCards(t);
            if (cards.size() >= 2) {
                tradableCards.add(new Card(t));
            }
        }

        for (CommodityType t : CommodityType.values()) {
            List<Card> cards = p.getUsableCards(t);
            if (cards.size() >= 2) {
                tradableCards.add(new Card(t));
            }
        }
        return tradableCards;
    }

    /**
     * Return list of players who are not p who have more points than p and who have at least 1 Commodity or Resource card in their hand.
     *
     * @param soc
     * @param p
     * @return
     */
    static public List<Integer> computeWeddingOpponents(SOC soc, Player p) {
        List<Integer> players = new ArrayList<>();
        for (Player player : soc.getPlayers()) {
            if (player.getPlayerNum() == p.getPlayerNum())
                continue;
            if (player.getPoints() <= p.getPoints())
                continue;
            if (player.getUnusedCardCount(CardType.Commodity) > 0 || player.getUnusedCardCount(CardType.Resource) > 0)
                players.add(player.getPlayerNum());
        }
        return players;
    }

    /**
     * Compute the vertices that the player is adjacent to that are pirate fortresses.
     *
     * @param b
     * @param p
     * @return
     */
    static public List<Integer> computeAttackablePirateFortresses(Board b, Player p) {
        HashSet<Integer> verts = new HashSet<Integer>();
        for (int vIndex : b.getVertIndicesOfType(0, VertexType.PIRATE_FORTRESS)) {
            Vertex v = b.getVertex(vIndex);
            for (int v2 : v.getAdjacentVerts()) {
                Route r = b.getRoute(vIndex, v2);
                if (r.getType().isVessel && r.getPlayer() == p.getPlayerNum()) {
                    verts.add(vIndex);
                    break;
                }
            }
        }
        return new ArrayList<>(verts);
    }

    /**
     * Return a list of all the opponent players of playerNum
     *
     * @param soc
     * @param playerNum
     * @return
     */
    public static List<Integer> computeOpponents(SOC soc, int playerNum) {
        List<Integer> p = new ArrayList<>();
        for (int i = 1; i <= soc.getNumPlayers(); i++) {
            if (i != playerNum) {
                p.add(i);
            }
        }
        return p;
    }

    /**
     * Return a list of all opponents with at leat 1 card in their hand
     *
     * @param soc
     * @param playerNum
     * @return
     */
    public static List<Integer> computeOpponentsWithCardsInHand(SOC soc, int playerNum) {
        List<Integer> p = new ArrayList<>();
        for (int i = 1; i <= soc.getNumPlayers(); i++) {
            if (i != playerNum) {
                if (soc.getPlayerByPlayerNum(i).getTotalCardsLeftInHand() > 0)
                    p.add(i);
            }
        }
        return p;
    }

    /**
     * Get the winningest player with most victory points
     *
     * @param soc
     * @return
     */
    static public Player computePlayerWithMostVictoryPoints(SOC soc) {
        Player winning = null;
        int most = 0;
        for (Player p : soc.getPlayers()) {
            int num = p.getSpecialVictoryPoints();
            if (num > most) {
                most = num;
                winning = p;
            }
        }
        return winning;
    }

    /**
     * Return a list of players suitable for the deserter card
     *
     * @param soc
     * @param b
     * @param p
     * @return
     */
    static public List<Integer> computeDeserterPlayers(SOC soc, Board b, Player p) {
        List<Integer> players = new ArrayList<>();
        for (int i = 1; i <= soc.getNumPlayers(); i++) {
            if (i == p.getPlayerNum())
                continue;
            if (0 < b.getNumKnightsForPlayer(i)) {
                players.add(i);
            }
        }
        return players;
    }

    static public List<DevelopmentArea> computeCraneCardImprovements(Player p) {
        ArrayList<DevelopmentArea> options = new ArrayList<DevelopmentArea>();
        for (DevelopmentArea area : DevelopmentArea.values()) {
            int devel = p.getCityDevelopment(area);
            int numCommodity = p.getCardCount(area.commodity);
            if (numCommodity >= devel) {
                options.add(area);
            }
        }
        return options;
    }

    /**
     * @param soc
     * @param p
     * @return
     */
    static public List<Card> computeGiftCards(SOC soc, Player p) {
        List<Card> cards = p.getCards(CardType.Commodity);
        cards.addAll(p.getCards(CardType.Resource));
        return cards;
    }

    /**
     * @param p
     * @param b
     * @return
     */
    static public boolean canPlayerTrade(Player p, Board b) {
        List<Trade> options = new ArrayList<Trade>();
        computeTrades(p, b, options, 1);
        return options.size() > 0;
    }

    /**
     * Get a new array of all players
     *
     * @return
     */
    private Player[] getAllPlayers() {
        Player[] players = new Player[mNumPlayers + 1];
        for (int i = 0; i < mNumPlayers; i++)
            players[i + 1] = mPlayers[i];
        return players;
    }

    /**
     * @return
     */
    public final Iterable<Player> getPlayers() {
        return Arrays.asList(Arrays.copyOf(mPlayers, mNumPlayers));
    }

    private void processCityImprovement(Player p, DevelopmentArea area, int craneAdjust) {
        printinfo(getString("%1$s is improving their %2$s", p.getName(), area.getName()));
        int devel = p.getCityDevelopment(area);
        Utils.assertTrue (devel < DevelopmentArea.MAX_CITY_IMPROVEMENT);
        devel++;
        p.removeCards(area.commodity, devel - craneAdjust);
        p.setCityDevelopment(area, devel);
        onPlayerCityDeveloped(p.getPlayerNum(), area);
        if (checkMetropolis(this, mBoard, devel, getCurPlayerNum(), area)) {
            pushStateFront(State.CHOOSE_METROPOLIS, area);
        }
    }

    /**
     * @param fromTile
     * @param toTile
     */
    protected void onPirateSailing(int fromTile, int toTile) {
    }

    /**
     * @param playerNum
     * @param c
     */
    protected void onCardLost(int playerNum, Card c) {
    }

    /**
     * @param playerNum
     * @param playerStrength
     * @param pirateStrength
     */
    protected void onPirateAttack(int playerNum, int playerStrength, int pirateStrength) {
    }

    private void processPirateAttack() {
        List<Dice> dice = getDice();
        int min = 7;
        for (Dice d : dice) {
            switch (d.getType()) {
                case Event:
                    break;
                case RedYellow:
                case WhiteBlack:
                case YellowRed:
                    min = Math.min(min, d.getNum());
                    break;
            }
        }

        final int pirateStrength = min;
        {
            while (min-- > 0) {
                int fromTile = getBoard().getPirateTileIndex();
                Tile t = getBoard().getPirateTile();
                int toTile = t.getPirateRouteNext();
                if (toTile < 0)
                    toTile = getBoard().getPirateRouteStartTile();
                getBoard().setPirate(-1);
                onPirateSailing(fromTile, toTile);
                getBoard().setPirate(toTile);
            }
        }
        Tile t = getBoard().getPirateTile();
        boolean[] attacked = new boolean[16];
        pushStateFront(State.SET_PLAYER, getCurPlayerNum());
        for (int vIndex : t.getAdjVerts()) {
            Vertex v = getBoard().getVertex(vIndex);
            if (v.getPlayer() < 1)
                continue;
            if (attacked[v.getPlayer()])
                continue;
            if (v.isStructure()) {
                Player p = getPlayerByPlayerNum(v.getPlayer());
                int playerPts = getBoard().getRoutesOfType(v.getPlayer(), RouteType.WARSHIP).size();
                attacked[v.getPlayer()] = true;
                printinfo(getString("Pirate Attack! %1$s strength %2$d pirate strength %3$d", p.getName(), playerPts, pirateStrength));
                onPirateAttack(p.getPlayerNum(), playerPts, pirateStrength);
                if (pirateStrength < playerPts) {
                    // player wins the attack
                    printinfo(getString("%s has defeated the pirates.  Player takes a resource card of their choice", p.getName()));
                    pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
                    pushStateFront(State.SET_PLAYER, v.getPlayer());
                } else if (pirateStrength > playerPts) {
                    printinfo(getString("Pirates have defeated %s. Player loses 2 random resources cards", p.getName()));
                    int numResources = p.getCardCount(CardType.Resource);
                    for (int i = 0; i < 2 && numResources-- > 0; i++) {
                        Card c = p.removeRandomUnusedCard(CardType.Resource);
                        onCardLost(p.getPlayerNum(), c);
                    }
                } else {
                    printinfo(getString("Pirate and %s are of equals strength so attack is nullified", p.getName()));
                }
            }
        }
    }

    private void processDice() {
        // roll the dice

        if (getProductionNum() == 7) {
            printinfo(getString("Uh Oh, %s rolled a 7.", getCurPlayer().getName()));
            pushStateFront(State.SETUP_GIVEUP_CARDS);
            if (getRules().isEnableCitiesAndKnightsExpansion() && mBarbarianAttackCount < getRules().getMinBarbarianAttackstoEnableRobberAndPirate()) {
                pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeOpponentsWithCardsInHand(this, getCurPlayerNum()), null);
            } else if (getRules().isEnableRobber()) {
                if (getRules().isEnableSeafarersExpansion())
                    pushStateFront(State.POSITION_ROBBER_OR_PIRATE_NOCANCEL);
                else
                    pushStateFront(State.POSITION_ROBBER_NOCANCEL);
            } else if (getRules().isEnableSeafarersExpansion()) {
                pushStateFront(State.POSITION_PIRATE_NOCANCEL);
            } else {
                pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeOpponentsWithCardsInHand(this, getCurPlayerNum()), null);
            }
        } else {

            // after the last player takes a turn for this round need to advance 2 players
            // so that the player after the player who rolled the dice gets to roll next
            pushStateFront(State.NEXT_PLAYER);
            pushStateFront(State.NEXT_PLAYER);

            for (int i = 0; i < mNumPlayers - 1; i++) {
                pushStateFront(State.PLAYER_TURN_NOCANCEL);
                pushStateFront(State.NEXT_PLAYER);
            }

            // reset the players ships/development cards usability etc.
            pushStateFront(State.INIT_PLAYER_TURN);

            // do this last so that any states that get pushed are on top
            distributeResources(getProductionNum());

            if (getRules().isEnableCitiesAndKnightsExpansion()) {
                switch (DiceEvent.fromDieNum(getDiceOfType(DiceType.Event, getDice()).getNum())) {
                    case AdvanceBarbarianShip:
                        processBarbarianShip();
                        break;
                    case PoliticsCard:
                        distributeProgressCard(DevelopmentArea.Politics);
                        break;
                    case ScienceCard:
                        distributeProgressCard(DevelopmentArea.Science);
                        break;
                    case TradeCard:
                        distributeProgressCard(DevelopmentArea.Trade);
                        break;
                }
            }
        }
    }

    /**
     * Called when a player loses their metropolis to another player
     *
     * @param loserNum
     * @param stealerNum
     * @param area
     */
    protected void onMetropolisStolen(int loserNum, int stealerNum, DevelopmentArea area) {
    }

    static boolean checkMetropolis(SOC soc, Board b, int devel, int playerNum, DevelopmentArea area) {
        if (devel >= DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT) {
            //List<Integer> metropolis = mBoard.getVertsOfType(0, area.vertexType);
            final int metroPlayer = soc.getMetropolisPlayer(area);
            if (metroPlayer != playerNum && b.getNumVertsOfType(playerNum, VertexType.CITY, VertexType.WALLED_CITY) > 0) { // if we dont already own this metropolis
                if (metroPlayer == 0) { // if it is unowned
                    return true;
                } else {
                    //Utils.assertTrue(metropolis.size() == 1);
                    List<Integer> verts = b.getVertIndicesOfType(metroPlayer, area.vertexType);
                    Utils.assertTrue (verts.size() == 1);
                    Vertex v = b.getVertex(verts.get(0));
                    Utils.assertTrue (v.getPlayer() == metroPlayer);
                    if (v.getPlayer() != soc.getCurPlayerNum()) {
                        Player other = soc.getPlayerByPlayerNum(metroPlayer);
                        final int otherDevel = other.getCityDevelopment(area);
                        Utils.assertTrue (otherDevel >= DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT);
                        if (otherDevel < devel) {
                            soc.printinfo(soc.getString("%1$s loses Metropolis %2$s to %3$s", other.getName(), area.getName(), soc.getCurPlayer().getName()));
                            v.setPlayerAndType(other.getPlayerNum(), VertexType.CITY);
                            soc.onMetropolisStolen(v.getPlayer(), playerNum, area);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param playerNum
     * @param type
     */
    protected void onProgressCardDistributed(int playerNum, ProgressCardType type) {
    }

    /**
     * @param playerNum
     * @param type
     */
    protected void onSpecialVictoryCard(int playerNum, SpecialVictoryType type) {
    }

    private static Dice getDiceOfType(DiceType type, Collection<Dice> dice) {
        for (Dice d : dice) {
            if (d.getType() == type)
                return d;
        }
        throw new SOCException("No dice of type '" + type + "' in: " + dice);
    }

    private void distributeProgressCard(DevelopmentArea area) {
        for (Player p : getPlayers()) {
            List<Dice> dice = getDice();
            if (mProgressCards[area.ordinal()].size() > 0
                    && p.getCardCount(CardType.Progress) < getRules().getMaxProgressCards()
                    && p.getCityDevelopment(area) > 0
                    && p.getCityDevelopment(area) >= getDiceOfType(DiceType.RedYellow, dice).getNum() - 1) {
                Card card = mProgressCards[area.ordinal()].remove(0);
                printinfo(getString("%s draw a progress card", p.getName()));
                if (card.equals(ProgressCardType.Constitution)) {
                    p.addCard(SpecialVictoryType.Constitution);
                    onSpecialVictoryCard(p.getPlayerNum(), SpecialVictoryType.Constitution);
                } else if (card.equals(ProgressCardType.Printer)) {
                    card.setUsed();
                    p.addCard(SpecialVictoryType.Printer);
                    onSpecialVictoryCard(p.getPlayerNum(), SpecialVictoryType.Printer);
                } else {
                    p.addCard(card);
                    onProgressCardDistributed(p.getPlayerNum(), ProgressCardType.values()[card.getTypeOrdinal()]);
                }
            }
        }
    }

    /**
     * Called when the barbarians advance toward catan
     *
     * @param distanceAway
     */
    protected void onBarbariansAdvanced(int distanceAway) {
    }

    /**
     * @param catanStrength
     * @param barbarianStrength
     * @param playerStatus
     */
    protected void onBarbariansAttack(int catanStrength, int barbarianStrength, String[] playerStatus) {
    }

    /**
     * @param playerNum
     * @param tileIndex0
     * @param tileIndex1
     */
    protected void onTilesInvented(int playerNum, int tileIndex0, int tileIndex1) {
    }

    private void processBarbarianShip() {
        mBarbarianDistance -= 1;
        if (mBarbarianDistance == 0) {
            printinfo(getString("The Barbarians are attacking!"));
            int[] playerStrength = new int[mNumPlayers + 1];
            int minStrength = Integer.MAX_VALUE;
            int maxStrength = 0;
            for (int i = 1; i <= getNumPlayers(); i++) {
                playerStrength[i] = mBoard.getNumVertsOfType(i, VertexType.BASIC_KNIGHT_ACTIVE) * VertexType.BASIC_KNIGHT_ACTIVE.getKnightLevel()
                        + mBoard.getNumVertsOfType(i, VertexType.STRONG_KNIGHT_ACTIVE) * VertexType.STRONG_KNIGHT_ACTIVE.getKnightLevel()
                        + mBoard.getNumVertsOfType(i, VertexType.MIGHTY_KNIGHT_ACTIVE) * VertexType.MIGHTY_KNIGHT_ACTIVE.getKnightLevel();
                int numCities = mBoard.getNumVertsOfType(i, VertexType.CITY, VertexType.WALLED_CITY);//, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
                if (numCities == 0) {
                }//minStrength = Integer.MAX_VALUE; // dont count players who have no pillidgable cities
                else
                    minStrength = Math.min(minStrength, playerStrength[i]);
                maxStrength = Math.max(maxStrength, playerStrength[i]);
            }

            int catanStrength = CMath.sum(playerStrength);
            int barbarianStrength = computeBarbarianStrength(this, mBoard);
            String[] playerStatus = new String[mNumPlayers + 1];

            for (int i = 0; i < playerStrength.length; i++) {
                if (playerStrength[i] == Integer.MAX_VALUE)
                    playerStrength[i] = 0;
            }

            for (Player p : getPlayers()) {
                playerStatus[p.getPlayerNum()] = getString("Strength %d", playerStrength[p.getPlayerNum()]);
            }

            if (catanStrength >= barbarianStrength) {
                // find defender
                printinfo(getString("Catan defended itself from the Barbarians!"));
                List<Integer> defenders = new ArrayList<>();
                for (int i = 1; i < playerStrength.length; i++) {
                    if (playerStrength[i] == maxStrength) {
                        defenders.add(i);
                    }
                }
                Utils.assertTrue (defenders.size() > 0);
                if (defenders.size() == 1) {
                    Player defender = getPlayerByPlayerNum(defenders.get(0));
                    defender.addCard(SpecialVictoryType.DefenderOfCatan);
                    printinfo(getString("%s receives the Defender of Catan card!", defender.getName()));
                    for (Player p : getPlayers()) {
                        playerStatus[p.getPlayerNum()] = "[" + playerStrength[p.getPlayerNum()] + "]";
                    }
                    playerStatus[defender.getPlayerNum()] += getString(" Defender of Catan");
                } else {
                    pushStateFront(State.SET_PLAYER, getCurPlayerNum());
                    for (int playerNum : defenders) {
                        if (getPlayerByPlayerNum(playerNum).getCardCount(CardType.Progress) < getRules().getMaxProgressCards()) {
                            playerStatus[playerNum] += getString(" Choose Progress Card");
                            pushStateFront(State.CHOOSE_PROGRESS_CARD_TYPE);
                            pushStateFront(State.SET_PLAYER, playerNum);
                        }
                    }
                }

            } else {

                printinfo(getString("Catan failed to defend against the Barbarians!"));
                List<Integer> pilledged = new ArrayList<>();
                for (int i = 1; i < playerStrength.length; i++) {
                    if (playerStrength[i] == minStrength) {
                        pilledged.add(i);
                    }
                }
//				Utils.assertTrue(pilledged.size() > 0);
                for (int playerNum : pilledged) {
                    List<Integer> cities = mBoard.getVertIndicesOfType(playerNum, VertexType.WALLED_CITY);
                    if (cities.size() > 0) {
                        printinfo(getString("%s has defended their city with a wall", getPlayerByPlayerNum(playerNum).getName()));
                        int cityIndex = Utils.randItem(cities);
                        playerStatus[playerNum] += getString(" Defended by Wall");
                        Vertex v = mBoard.getVertex(cityIndex);
                        v.setPlayerAndType(playerNum, VertexType.CITY);
                    } else {
                        cities = mBoard.getVertIndicesOfType(playerNum, VertexType.CITY);
                        if (cities.size() > 0) {
                            printinfo(getString("%s has their city pillaged", getPlayerByPlayerNum(playerNum).getName()));
                            int cityIndex = Utils.randItem(cities);
                            playerStatus[playerNum] += getString(" City Pillaged");
                            Vertex v = mBoard.getVertex(cityIndex);
                            v.setPlayerAndType(playerNum, VertexType.SETTLEMENT);
                        }
                    }
                }

            }
            onBarbariansAttack(catanStrength, barbarianStrength, playerStatus);
            mBarbarianAttackCount++;
            mBarbarianDistance = getRules().getBarbarianStepsToAttack();

            // all knights revert to inactive
            for (Vertex v : getBoard().getVertsOfType(0, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE)) {
                v.setType(v.getType().deActivatedType());
            }

        } else {
            onBarbariansAdvanced(mBarbarianDistance);
        }
    }

    /**
     * Called when a player takes some resource from another player.  can be called multiple times
     * in a turn.  default method does nothing.
     *
     * @param takerNum
     * @param giverNum
     * @param applied
     * @param amount
     */
    protected void onMonopolyCardApplied(int takerNum, int giverNum, ICardType<?> applied, int amount) {
    }

    private void processMonopoly(ICardType<?> type) {
        // take the specified resource from all other players
        for (int i = 1; i < mNumPlayers; i++) {
            Player cur = mPlayers[(mCurrentPlayer + i) % mNumPlayers];
            int num = cur.getCardCount(type);
            if (num > 0) {
                if (getRules().isEnableCitiesAndKnightsExpansion() && num > 2) {
                    num = 2;
                }
                printinfo(getString("%1$s takes %2$d %3$s card from player %4$s", getCurPlayer().getName(), num, type.getName(), cur.getName()));
                onMonopolyCardApplied(getCurPlayerNum(), cur.getPlayerNum(), type, num);
                cur.incrementResource(type, -num);
                getCurPlayer().incrementResource(type, num);
            }
        }
    }

    /**
     * Called when game over is detected
     */
    protected void onGameOver(int winnerNum) {
    }

    public final boolean save(String fileName) {
        try {
            saveToFile(new File(fileName));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public final boolean load(String fileName) {
        reset();
        File file = new File(fileName);
        if (file.exists()) {
            try {
                loadFromFile(file);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public int getBarbarianDistance() {
        return this.mBarbarianDistance;
    }

    public String getHelpText() {
        if (mStateStack.peek() == null) {
            return getString("Game not running");
        }

        return mStateStack.peek().state.getHelpText();
    }

    /**
     * @param playerNum
     * @param knightIndex
     */
    protected void onPlayerKnightDestroyed(int playerNum, int knightIndex) {
    }

    /**
     * @param playerNum
     * @param knightIndex
     */
    protected void onPlayerKnightDemoted(int playerNum, int knightIndex) {
    }

    /**
     * @param playerNum
     * @param knightIndex
     */
    protected void onPlayerKnightPromoted(int playerNum, int knightIndex) {
    }

    /**
     * @param playerNum
     * @param area
     */
    protected void onPlayerCityDeveloped(int playerNum, DevelopmentArea area) {
    }

    protected void onVertexChosen(int playerNum, Player.VertexChoice mode, Integer vertexIndex, Integer v2) {
    }

    protected void onRouteChosen(int playerNum, Player.RouteChoice mode, Integer routeIndex, Integer shipToMove) {
    }

}
