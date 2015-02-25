package cc.game.golf.core;

import java.io.*;
import java.util.*;

import cc.lib.utils.Reflector;

/**
 * Golf is a card game where 2 or more players try to collect ranked pairs in order to obtain the least amount of points (like golf)
 * There are some fixed number of rounds (called holes) after which the player with the fewest points wins.
 * 
 * See here for rules:
 * http://en.wikipedia.org/wiki/Golf_(card_game)
 * 
 * DEBUG mode can be enabled by setting static var DEBUD_ENABLED to true prior to instantiation.  This will cause
 * random number generator to be initialized with seed 0 as well as more output.
 *   
 * NOTE: RuntimeException is thrown for improper use.
 *   
 * @author ccaron
 *
 */
public class Golf extends Reflector {

    public static boolean DEBUG_ENABLED = false; 

    static void debugMsg(String msg) {
        if (DEBUG_ENABLED)
            System.err.println("DEBUG: " + msg);
    }
    
    public static final int VERSION = 0; // increment version when data changes
    
    private final Random random;
    
    private List<Player> players = new ArrayList<Player>();
    private Card [] deck;
    private int numDeckCards = 0;
    private final Stack<Card> discardPile = new Stack<Card>();
    private State state = State.INIT;
    private int dealer, curPlayer, knocker, winner;
    private int numRounds;
    private Rules rules = new Rules();
    private final Object lock = new Object(); // thread lock
    
    static {
        addField(Golf.class, "players");
        addField(Golf.class, "deck");
        addField(Golf.class, "numDeckCards");
        addField(Golf.class, "discardPile");
        addField(Golf.class, "state");
        addField(Golf.class, "dealer");
        addField(Golf.class, "curPlayer");
        addField(Golf.class, "knocker");
        addField(Golf.class, "winner");
        addField(Golf.class, "numRounds");
        addField(Golf.class, "rules");
    }
    
    /**
     * Only constructor
     */
    public Golf() {
        this(null);
    }
    
    /**
     * 
     * @param rules
     */
    public Golf(Rules rules) {
        if (DEBUG_ENABLED)
            random = new Random(0);
        else
            random = new Random(System.currentTimeMillis());
        newGame(rules);
    }

    /**
     * Return current state.  States get advanced via the advance() method
     * @return
     */
    public final State getState() {
        return state;
    }
    
    /**
     * Return rand from internal random number generator.
     * @return
     */
    public int rand() {
        return Math.abs(random.nextInt());
    }
    
    /**
     * Get an unmodifiable list representing the current state of the deck 
     * @return
     */
    public final List<Card> getDeck() {
        return Arrays.asList(deck);
    }
    
    /**
     * Get the current rules 
     * @return
     */
    public final Rules getRules() {
        return this.rules;
    }
    
    /**
     * Add a player.  Only valid when in the INIT state (after construction of after newGame)
     * @param p
     * @return the zero based index of the player
     */
    public final int addPlayer(Player p) {
        if (getState() != State.INIT)
            throw new RuntimeException("Cannot add player outside of INIT");
        p.index = players.size();
        players.add(p);
        p.reset(this);
        return p.getPlayerNum();
    }
    
    /**
     * Return interable to iterate over players
     * @return
     */
    public final Iterable<Player> getPlayers() {
        return players;
    }

    /**
     * Get a player by its 0 based index in the players list
     * @param playerNum
     * @return
     */
    public final Player getPlayer(int playerNum) {
        return players.get(playerNum);
    }
    
    /**
     * Get the number of players
     * @return
     */
    public final int getNumPlayers() {
        return players.size();
    }

    /**
     * 
     * @return
     */
    public final int getNumRounds() {
        return this.numRounds;
    }
    
    /**
     * Start a new game with a customized rule set.
     */
    public final void newGame(Rules rules) {
        if (rules != null)
            this.rules = rules;
        newGame();
    }
    
    /**
     * Start a new game.  Can be called anytime.  Will reset all players points, hands, and reset the deck.  
     * The default Rules will be applied.
     */
    public final void newGame() {
        synchronized (lock) {
            state = State.INIT;
            dealer = curPlayer = 0;
            numRounds = 0;
            winner = -1;
            for (Player p: players) {
                p.clearPoints();
            }
            initDeck();
        }
    }
    
    /**
     * 
     * @return
     */
    public final int getWinner() {
        return winner;
    }
    
    /**
     * Remove all players and return to init position.  Must add players before calling advance. 
     */
    public final void clear() {
        synchronized (lock) {
            players.clear();
            initDeck();
            newGame(rules);
        }
    }
    
    /**
     * 
     * @param out
     * @throws IOException
     */
    public final void saveGame(OutputStream out) throws IOException {
        synchronized (lock) {
            PrintWriter printer = new PrintWriter(out);
            this.serialize(printer);
            printer.flush();
        }
    }

    /**
     * 
     * @param in
     * @throws IOException
     */
    public final void loadGame(InputStream in) throws IOException {
        final int[] lineNum = new int[1];
        //String line;
        BufferedReader input = new BufferedReader(new InputStreamReader(in)) {
            @Override
            public String readLine() throws IOException {
                String line = super.readLine();
                lineNum[0]++;
                return line;
            }
        };

        try {
            deserialize(input);
        } catch (Exception e) {
            clear();
            e.printStackTrace();
            throw new IOException("Error line:" + lineNum[0] + " "
                    + e.getClass().getSimpleName() + " " + e.getMessage());
        } finally {
            try {
                input.close();
            } catch (Exception e) {
            }
        }
    }
    
    /**
     * Convenience method
     * @param file
     * @throws IOException
     */
    public final void saveGame(File file) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            saveGame(out);
        } finally {
            if (out != null)
                out.close();
        }
    }
    
    /**
     * Convenience method
     * @param file
     * @throws IOException
     */
    public final void loadGame(File file) throws IOException {
        synchronized (lock) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                loadGame(in);
            } finally {
                if (in != null)
                    in.close();
            }
        }
    }
    
    private void nextPlayer() {
        curPlayer = (curPlayer+1) % getNumPlayers();
    }
    
    private Player getCurPlayer() {
        return getPlayer(curPlayer);
    }
    
    /**
     * Return the number of cards dealt to each player
     * @return
     */
    public final int getNumHandCards() {
        return rules.getGameType().getRows() * rules.getGameType().getCols();
    }
    
    /**
     * Advance the state.  Is typically called continuously until state is GAME_OVER.  
     */
    public final void runGame() {
        synchronized (lock) {
            switch (state) {
                case INIT:
                    if (players.size() < 2)
                        throw new RuntimeException("Too few players");
                    knocker = -1;
                    curPlayer = dealer;
                    nextPlayer();
                    for (Player p: players) {
                        p.reset(this);
                    }
                    discardPile.clear();
                    state = State.SHUFFLE;
                    break;
                case SHUFFLE:
                    shuffle();
                    state = State.DEAL;
                    break;
                case DEAL: {
                    int [] rowCol = { -1, -1 };
                    if (getCurPlayer().getPositionOfCard(null, rowCol)) {
                        Card dealt = drawCard(false);
                        onDealCard(curPlayer, dealt, rowCol[0], rowCol[1]);
                        getCurPlayer().setCard(rowCol[0], rowCol[1], dealt);
                    } else {
                        throw new RuntimeException("Failed to add find empty slot to deal card");
                    }
                    if (curPlayer == dealer && getCurPlayer().getNumCards() == getNumHandCards()) {
                        state = State.SETUP_DISCARD_PILE;
                    }
                    nextPlayer();
                    break;
                }
                case SETUP_DISCARD_PILE:
                    discardPile.push(this.drawCard(true));
                    state = State.TURN;
                    break;
                case TURN_OVER_CARDS: {
                    int numRows = getRules().getGameType().getRows();
                    for (int i=0; i<numRows; i++) {
                        if (getCurPlayer().getNumCardsShowing(i) == 0) {
                            int card = getCurPlayer().turnOverCard(this, i);
                            if (card >= 0 && card < getCurPlayer().getNumCols()) {
                                Card c = getCurPlayer().getCard(i, card);
                                onCardTurnedOver(curPlayer, c, i, card);
                                c.setShowing(true);
                                message("Player " + curPlayer + " turned over the " + c.toPrettyString());
                                if (i == numRows-1)
                                    state = State.TURN;
                            }
                            break;
                        }
                    }
                    break;
                }
                case TURN:
                    // pick from the stack or from the discard pile
                    if (getCurPlayer().getNumCardsShowing() == 0) {
                        state = State.TURN_OVER_CARDS;
                    } else if (knocker == curPlayer) { 
                        state = State.PROCESS_ROUND;
                    } else switch (getCurPlayer().chooseDrawPile(this)) {
                        case DTStack:
                            message("Player " + curPlayer + " drawing from the deck");
                            onDrawPileChoosen(curPlayer, DrawType.DTStack);
                            getTopOfDeck().setShowing(true);
                            state = State.DISCARD_OR_PLAY;
                            break;
                        case DTDiscardPile:
                            message("Player " + curPlayer + " drawing from the discard pile");
                            onDrawPileChoosen(curPlayer, DrawType.DTDiscardPile);
                            state = State.PLAY;
                            break;
                        case DTWaiting:
                            break;
                    }
                    break;
                case DISCARD_OR_PLAY: {
                    Card drawn = getTopOfDeck();
                    if (drawn == null)
                        throw new NullPointerException();
                    drawn.setShowing(true);
                    message("Player " + curPlayer + " has drawn the " + drawn.toPrettyString() + " from stack");
                    Card swapped = getCurPlayer().chooseDiscardOrPlay(this, drawn);
                    if (swapped != null) {
                        int [] rowCol = {-1,-1};
                        if (getCurPlayer().getPositionOfCard(swapped, rowCol)) {
                            Card c = getCurPlayer().getCard(rowCol[0], rowCol[1]);
                            onChooseCardToSwap(curPlayer, swapped, rowCol[0], rowCol[1]);
                            swapped.setShowing(true);
                            drawCard(true);
                            onCardSwapped(curPlayer, DrawType.DTStack, drawn, c, rowCol[0], rowCol[1]);
                            getCurPlayer().setCard(rowCol[0], rowCol[1], drawn);
                        } else {
                            drawCard(true);
                            onCardDiscarded(curPlayer, DrawType.DTStack, swapped);
                        }
                        discard(swapped);
                        knockCheck();
                        nextPlayer();
                    }
                    break;
                }
    
                case PLAY: {
                    Card drawn = discardPile.peek();
                    Card swapped = getCurPlayer().chooseCardToSwap(this, drawn);
                    if (swapped == drawn)
                        throw new RuntimeException("Player must return a card from their hand or null");
                    if (swapped != null && swapped != drawn) {
                        int [] rowCol = { -1, -1 };
                        if (!getCurPlayer().getPositionOfCard(swapped, rowCol))
                            throw new RuntimeException("Unknown card: " + swapped);
                        onChooseCardToSwap(curPlayer, swapped, rowCol[0], rowCol[1]);
                        swapped.setShowing(true);
                        discardPile.pop();
                        message("Player " + curPlayer + " swapping the " + swapped.toPrettyString() + " for the " + drawn.toPrettyString() + " from discard pile");
                        onCardSwapped(curPlayer, DrawType.DTDiscardPile, drawn, swapped, rowCol[0], rowCol[1]);
                        getCurPlayer().setCard(rowCol[0], rowCol[1], drawn);
                        discard(swapped);
                        knockCheck();
                        nextPlayer();
                    } 
                    break;
                }
                
                case PROCESS_ROUND: {
                    numRounds ++;
                    message("end of hole " + numRounds + " of " + rules.getNumHoles());
                    int maxRoundPoints = Integer.MIN_VALUE;
                    int leastRoundPoints = Integer.MAX_VALUE;
                    int leastRoundPointsPlayer = -1;
                    for (int i=0; i<getNumPlayers(); i++) {
                        Player p = getPlayer(i);
                        int handPoints = p.getHandPoints(this);
                        if (handPoints < leastRoundPoints) {
                            leastRoundPoints = handPoints;
                            leastRoundPointsPlayer = i;
                        }
                        if (handPoints > maxRoundPoints) {
                            maxRoundPoints = handPoints;
                        }
                    }
    
                    int winnerPoints = Integer.MAX_VALUE;
                    int winnerIndex = -1;
                    for (int i=0; i<getNumPlayers(); i++) {
                        Player p = getPlayer(i);
                        int handPoints = p.getHandPoints(this);
                        if (i == getKnocker()) {
                            if (i == leastRoundPointsPlayer) {
                                // apply bonus
                                switch (rules.getKnockerBonusType()) {
                                    case ZeroOrLess:
                                        message("Knocker gets bonus zero or less");
                                        if (handPoints < 0) {
                                            p.addPoints(handPoints);
                                        }
                                        break;
                                    case MinusNumberOfPlayers:
                                        message("Knocker gets bonus -" + getNumPlayers());
                                        p.addPoints(-getNumPlayers());
                                        break;
                                    case None:
                                        p.addPoints(handPoints);
                                        break;
                                    
                                }
                            } else {
                                switch (rules.getKnockerPenaltyType()) {
                                    case Plus10: 
                                        message("Knocker is penalized +10 points");
                                        p.addPoints(handPoints + 10);
                                        break;
                                    case HandScoreDoubledPlus5:
                                        message("Knocker is penalized double hand score + 5");
                                        p.addPoints(handPoints * 2 + 5);
                                        break;
                                    case EqualToHighestHand:
                                        message("Knocker is penalized points equal to highest hand");
                                        p.addPoints(handPoints + maxRoundPoints);
                                        break;
                                        
                                    case TwiceNumberOfPlayers:
                                        message("Knocker is penalized 2 X number of players");
                                        p.addPoints(handPoints + 2 * getNumPlayers());
                                        break;
                                        
                                    case None:
                                        break;
                                    
                                }
                            }
                        } else {
                            p.addPoints(handPoints);
                        }
                        if (p.getPoints() < winnerPoints) {
                            winnerPoints = p.getPoints();
                            winnerIndex = i;
                        }
                    }
                    
                    if (numRounds == rules.getNumHoles()) {
                        state = State.GAME_OVER;
                        this.winner = winnerIndex;
                        onEndOfGame();
                    } else {
                        initDeck();
                        dealer = (dealer+1) % getNumPlayers();
                        state = State.END_ROUND;
                        onEndOfRound();
                    }
                    break;
                }

                case END_ROUND:
                    state = State.INIT;
                    break;
                
                case GAME_OVER:
                    break;
            }
        }
    }
    
    private void knockCheck() {
        if (knocker < 0 && getCurPlayer().isAllCardsShowing()) {
            knocker = curPlayer;
            message("Player " + curPlayer + " has knocked");
            onKnock(knocker);
        }
        if (knocker >= 0) {
            getCurPlayer().showAllCards();
        }        
        state = State.TURN;
    }
    
    private void discard(Card card) {
        card.setShowing(true);
        this.discardPile.push(card);
        message("Player " + curPlayer + " discarded the " + card.toPrettyString());
    }
    
    /**
     * Get the top card on the discard pile.
     * @return
     */
    public final Card getTopOfDiscardPile() {
        if (discardPile.empty())
            return null;
        return discardPile.peek();
    }
    
    private Card drawCard(boolean showing) {
        if (numDeckCards == 0) {
            if (this.discardPile.size() == 0)
                throw new RuntimeException("Not enough cards");
            for (int i=0; i<discardPile.size(); i++) {
                deck[i] = discardPile.get(i);
                deck[i].setShowing(false);
            }
            numDeckCards = discardPile.size();
            discardPile.clear();
            shuffle();
        }
        deck[--numDeckCards].setShowing(showing);
        return deck[numDeckCards];
    }
    
    /**
     * 
     * @return
     */
    public final Card getTopOfDeck() {
        return deck[numDeckCards-1];
    }
    
    private void initDeck() {
        deck = new Card[(Rank.values().length-1) * 4 * rules.getNumDecks() + rules.getNumJokers()];
        int n = 0;
        for (int k=0; k<rules.getNumDecks(); k++) {
            for (Rank r : Rank.values()) {
                if (r != Rank.JOKER) {
                    for (int j=0; j<4; j++) {
                        deck[n++] = new Card(k, r, Suit.values()[j], true);
                    }
                }
            }
        }
        int deckNum = 0;
        for (int i=0; i<rules.getNumJokers(); i++) {
            deck[n++] = new Card(deckNum, Rank.JOKER, i % 2 == 0 ? Suit.RED : Suit.BLACK, true);
            deckNum += i % 2;
        }
        
        if (n != deck.length)
            throw new RuntimeException("Failed to setup deck correctly");
        numDeckCards = deck.length;
    }

    private void shuffle() {
        // hide all cards
        for (Card c : deck) {
            c.setShowing(false);
        }
        message("Shuffling " + numDeckCards + " cards ...");
        for (int i = 0; i < 2000; i++) {
            int a = rand() % numDeckCards;
            int b = rand() % numDeckCards;
            Card t = deck[a];
            deck[a] = deck[b];
            deck[b] = t;
        }
    }

    /**
     * Get the current player
     * @return
     */
    public final int getCurrentPlayer() {
        return this.curPlayer;
    }
    
    /**
     * Get the dealer
     * @return
     */
    public final int getDealer() {
        return this.dealer;
    }

    /**
     * 
     * @return
     */
    public final int getKnocker() {
        return knocker;
    }
    
    /**
     * Return number of cards left in the deck
     * @return
     */
    public final int getNumDeckCards() {
        return this.numDeckCards;
    }

    /**
     * 
     * @param p
     * @return
     *
    public final int getPlayerIndex(Player p) {
        for (int i=0; i<players.size(); i++)
            if (getPlayer(i) == p)
                return i;
        throw new RuntimeException("Unknown player");
    }

    /**
     * Handle to affect how game messages are handled.  Default prints to stdout.
     * 
     * @param format
     * @param params
     */
    protected void message(String format, Object... params) {
        String msg = String.format(format, params);
        System.out.println(msg);
    }
    
    /**
     * Called when a player has knocked.  Base method does nothing.
     * @param player
     */
    protected void onKnock(int player) {}
    
    /**
     * Called at the end of each round.  Base method does nothing.
     */
    protected void onEndOfRound() {}

    /**
     * Called at the end of the game.  Base method does nothing.
     */
    protected void onEndOfGame() {}
    
    /**
     * Called when a player has swapped a card form the deck or the discard pile.
     * Base method does nothing.
     * @param golf
     * @param dtstack
     * @param c
     */
    protected void onCardSwapped(int player, DrawType dtstack, Card drawn, Card swapped, int row, int col) {}

    /**
     * Called when player discards a card drawn from the deck. Base method does nothing.
     * @param golf
     * @param dtstack
     * @param swapped
     */
    protected void onCardDiscarded(int player, DrawType dtstack, Card swapped) {}
    
    /**
     * Called when a player is dealt a card.  Base method does nothing.
     * @param player
     * @param card
     * @param row
     * @param col
     */
    protected void onDealCard(int player, Card card, int row, int col) {}

    /**
     * Called when a player turns over a card in their hand.  Base method does nothing.
     * @param player
     * @param card
     * @param row
     * @param col
     */
    protected void onCardTurnedOver(int player, Card card, int row, int col) {}
    
    /**
     * Called when a player chooses to draw from stack or discard pile
     * @param player
     * @param type
     */
    protected void onDrawPileChoosen(int player, DrawType type) {}

    /**
     * Called when a player has chosen a card from their hand to swap.  Base method does nothing.
     * @param player
     * @param card
     * @param row
     * @param col
     */
    protected void onChooseCardToSwap(int player, Card card, int row, int col) {}
}
