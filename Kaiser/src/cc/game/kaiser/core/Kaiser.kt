package cc.game.kaiser.core;

import java.io.*;
import java.util.*;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

/**
 * NO external dependencies in the CORE!
 * @author ccaron
 *
 */
public class Kaiser extends Reflector<Kaiser> {

    static {
        addAllFields(Kaiser.class);
    }
    
    public static boolean DEBUG_ENABLED = false; 

    // Rules based on this page.
    // http://www.pagat.com/pointtrk/kaiser.html

    public static final int NUM_DECK_CARDS = 32;
    public static final int NUM_PLAYERS = 4;
    public static final int NUM_TEAMS = 2;
    
    // tunables
    public static int WINNING_POINTS = 52;
    public static int POINTS_5_HEARTS = 5;
    public static int POINTS_3_CLUBS = -3;
    
    private final Card[] cards = new Card[NUM_DECK_CARDS];
    private int curPlayer; // the current player whose turn it is
    private int startPlayer; // the first player to play in this round
    private int dealer; // the player who is the dealer this round
    private State state; // current game state
    private int numCards; // number of cards in the deck
    private int curBidTeam; // current team that is bidding
    private int numTricks; // number of tricks played this round
    private int numRounds; // number of rounds played

    private final Player[] players = new Player[NUM_PLAYERS];
    private final Team[] teams = new Team[NUM_TEAMS];
    private final Card[] trick = new Card[NUM_PLAYERS];

    /**
     * 
     */
    public Kaiser() {
        initCards();
        teams[0] = new Team("Team0");
        teams[1] = new Team("Team1");
        newGame();
    }

    private void initCards() {
        int num = 0;
        
        Suit [] suits = { Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS, Suit.SPADES };
        for (Suit s : suits ) {
            cards[num++] = new Card(Rank.ACE, s);
            cards[num++] = new Card(Rank.KING, s);
            cards[num++] = new Card(Rank.QUEEN, s);
            cards[num++] = new Card(Rank.JACK, s);
            cards[num++] = new Card(Rank.TEN, s);
            cards[num++] = new Card(Rank.NINE, s);
            cards[num++] = new Card(Rank.EIGHT, s);
            switch (s) {
                case CLUBS:
                    cards[num++] = new Card(Rank.THREE, s); break;
                case HEARTS:
                    cards[num++] = new Card(Rank.FIVE, s); break;
                default:
                    cards[num++] = new Card(Rank.SEVEN, s); break;
            }
            
        }
    }
    
    /**
     * Return set of all cards
     * 
     * @return
     */
    public Card [] getAllCards() {
        return cards;
    }
    
    private Bid [] computeBidOptions(boolean dealer, Hand hand) {
        Bid bid = null;
        if (this.getCurBidTeam() != null)
            bid = getCurBidTeam().bid;
        return computeBidOptions(bid, dealer, hand.getCards());
        
        
        /*
        List<Bid> options = new ArrayList<Bid>();
        if (!dealer)
            options.add(Bid.NO_BID);
        Team curBidTeam = getCurBidTeam();
        int startOffset = 0;
        if (curBidTeam != null && curBidTeam.bid != null) {
            for (int i = 0; i < Bid.NUM_BIDS; i++) {
                if (Bid.ALL_BIDS[i].compareTo(curBidTeam.bid) > 0
                        || (dealer && Bid.ALL_BIDS[i].compareTo(curBidTeam.bid) >= 0))
                    break;
                startOffset++;
            }
        }

        for (int i = startOffset; i < Bid.NUM_BIDS; i++)
            options.add(Bid.ALL_BIDS[i]);

        return options.toArray(new Bid[options.size()]);
        */
    }
    
    static Bid [] computeBidOptions(Bid currentBid, boolean isDealer, Card [] hand) {
        if (Kaiser.DEBUG_ENABLED)
            System.out.println("currentBid= " + currentBid + ", isDealer=" + isDealer + ", hand=" + Arrays.asList(hand));
        List<Bid> options = new ArrayList<Bid>();
        if (!isDealer)
            options.add(Bid.NO_BID);

        Suit [] suits = getBestTrumpOptions(hand);
        int start = Rules.MINIMUM_BID;
        if (currentBid != null) {
            start = currentBid.numTricks;
            if (!isDealer) {
                if (currentBid.trump != Suit.NOTRUMP) {
                    options.add(new Bid(start, Suit.NOTRUMP));
                }
                start++;
            } else if (currentBid.trump == Suit.NOTRUMP) {
                options.add(new Bid(start, Suit.NOTRUMP));
                start++;
            }
                    
        }
        
        for (int i = start; i<=12; i++) {
            for (Suit s: suits) {
                options.add(new Bid(i, s));
            }
        }
        
        return options.toArray(new Bid[options.size()]);
    }
    
    static Suit [] getBestTrumpOptions(Card [] hand) {
        
        // sort the suits based on their relevance in the set of cards.  This is to support bidding
        
        int [] counts = new int[4];
            
        for (Card c: hand) {
            counts[c.suit.ordinal()] ++;//= c.rank.bidValue;
        }

        Suit [] options = Suit.values();//new Suit[] { Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS, Suit.SPADES };
        /*
procedure bubbleSort( A : list of sortable items )
    n = length(A)
    repeat
       swapped = false
       for i = 1 to n-1 inclusive do
          if A[i-1] > A[i] then
             swap(A[i-1], A[i])
             swapped = true
          end if
       end for
       n = n - 1
    until not swapped
end procedure 
         */
        // I know I know.  Oh!  Bubble sort is soooo bad. Actally, because of its ease in implementation, it
        // makes sense when we want to sort a very small array based on the criteria of another array.
        
        boolean swapped = false;
        do {
            swapped = false;
            for (int i=1; i<4; i++) {
                if (counts[i-1] < counts[i]) {
                    swapped = true;
                    {
                        int t = counts[i-1];
                        counts[i-1] = counts[i];
                        counts[i] = t;
                    }
                    {
                        Suit t = options[i-1];
                        options[i-1] = options[i];
                        options[i] = t;
                    }
                }
            }
        } while (swapped);
        
        return options;
    }
    
    private Card [] computePlayerTrickOptions(Player p) {
        boolean hasLead = false;
        if (curPlayer != startPlayer) {
            Suit lead = trick[startPlayer].suit;
            for (int i = 0; i < p.getNumCards(); i++) {
                if (p.getCard(i).suit == lead) {
                    hasLead = true;
                    break;
                }
            }
        }

        ArrayList<Card> list = new ArrayList<Card>();        
        for (int i = 0; i < p.getNumCards(); i++) {
            if (curPlayer == startPlayer || !hasLead) {
                //options[num++] = i;
                list.add(p.getCard(i));
                continue;
            }
            Suit lead = trick[startPlayer].suit;
            if (p.getCard(i).suit == lead) {
                //options[num++] = i;
                list.add(p.getCard(i));
            }
        }
        return list.toArray(new Card[list.size()]);
    }

    private void processTrick() {
        if (trick[startPlayer] == null) {
            state = State.RESET_TRICK;
            return;
        }
        
        int best = getTrickWinnerIndex();
        Player winner = players[best];
        numTricks += 1;
        message("%s wins the trick %d of %d", winner.getName(), numTricks, Player.MAX_PLAYER_TRICKS);

        Hand pTrick = new Hand();
        pTrick.clear();
        pTrick.addCard(trick[0]);
        pTrick.addCard(trick[1]);
        pTrick.addCard(trick[2]);
        pTrick.addCard(trick[3]);
        winner.mTricks[winner.mNumTricks++] = pTrick;
        winner.onWinsTrick(this, pTrick);

        int points = 1;
        for (int i = 0; i < NUM_PLAYERS; i++) {
            if (trick[i].rank == Rank.THREE) {
                message("Team " + getTeam(winner.getTeam()).getName() + " loses " + Math.abs(POINTS_3_CLUBS) + " points for the 3 of clubes");
                points += POINTS_3_CLUBS;
            }
            else if (trick[i].rank == Rank.FIVE) {
                message("Team " + getTeam(winner.getTeam()).getName() + " gains " + Math.abs(POINTS_5_HEARTS) + " points for the 5 of hearts");
                points += POINTS_5_HEARTS;
            }
        }

        startPlayer = best;
        Team team = getTeam(players[best].mTeam);
        team.roundPoints += points;

        // allow all the players to process the trick
        for (int i = 0; i < NUM_PLAYERS; i++)
            players[i].onProcessTrick(this, pTrick, players[best], points);
        // message(best, "Team %s gets %d points", team.name.getStr(), points);

        if (numTricks == Player.MAX_PLAYER_TRICKS) {
            state = State.PROCESS_ROUND;
        } else {
            startPlayer = best;
            state = State.RESET_TRICK;
        }

        clearTricks();
    }

    private void clearTricks() {
        Arrays.fill(trick, null);
    }

    private void processTeam(Team team, boolean winner) {
        int pts = 0;
        if (team.bid == Bid.NO_BID) {
            if (winner || team.totalPoints < 45 || team.roundPoints < 0)
                pts = team.roundPoints;
        } else {
            int multiplier = 1;
            if (team.bid.trump == Suit.NOTRUMP) {
                message("\n\nDOUBLE MULTIPLIER FOR NOTRUMP!!\n\n");
                multiplier = 2;
            }

            if (team.bid.numTricks <= team.roundPoints)
                pts = multiplier * team.roundPoints;
            else
                pts = -multiplier * team.bid.numTricks;//roundPoints;

        }

        team.totalPoints += pts;

        message("Team %s %s %d points for %d total.", team.name,
                pts > 0 ? "gains" : "loses", Math.abs(pts), team.totalPoints);
    }

    private void shuffle() {
        message("shuffling ...");
        for (int i = 0; i < 2000; i++) {
            int a = Utils.getRandom().nextInt(NUM_DECK_CARDS);
            int b = Utils.getRandom().nextInt(NUM_DECK_CARDS);
            Card t = cards[a];
            cards[a] = cards[b];
            cards[b] = t;
        }
    }

    /**
     * Set the name of a team
     * 
     * @param index
     * @param name
     */
    public void setTeam(int index, String name) {
        teams[index].name = name;
    }
    
    /**
     * 
     * @param index
     * @param player
     */
    public void setPlayer(int index, Player player) {
        if (player == null)
            throw new NullPointerException("player cannot be null");
        if (index < 0 || index >= NUM_PLAYERS)
            throw new IndexOutOfBoundsException("Invalid index " + index + " range is [0-" + NUM_PLAYERS + ")");
        players[index] = player;
        player.playerNum = index;
        int i_mod_2 = index % 2;
        int i_div_2 = index / 2;
        players[index].mTeam = i_mod_2;
        teams[i_mod_2].players[i_div_2] = index;
    }

    /**
     * 
     */
    public void runGame() {
        int i;
        switch (state) {
            case NEW_GAME:
                message("Begin new game");
                numCards = 0;
                curBidTeam = -1;
                numRounds = 0;
                for (i = 0; i < NUM_PLAYERS; i++) {
                    if (players[i] == null)
                        throw new NullPointerException("Null player at index " + i);
                    players[i].onNewGame(this);
                }
                state = State.NEW_ROUND;
                dealer = Utils.getRandom().nextInt(NUM_PLAYERS);
                curPlayer = (dealer + 1) % NUM_PLAYERS;
                teams[0].totalPoints = 0;
                teams[1].totalPoints = 0;
                message("Teams are: ");
                message("Team A: " + teams[0]);
                message("Team B: " + teams[1]);                
                break;

            case NEW_ROUND:
                message("Starting round %d", numRounds++);
                message("Dealer is %s", getPlayer(dealer).getName());
                for (i = 0; i < NUM_PLAYERS; i++) {
                    players[i].onNewRound(this);
                    players[i].mNumTricks = 0;
                }
                curPlayer = (dealer + 1) % NUM_PLAYERS;
                curBidTeam = -1;
                numCards = 0;
                numTricks = 0;
                shuffle();
                state = State.DEAL;
                clearTricks();
                teams[0].roundPoints = 0;
                teams[1].roundPoints = 0;
                teams[0].bid = Bid.NO_BID;
                teams[1].bid = Bid.NO_BID;
                break;

            case DEAL: {
                Card card = cards[numCards++];
                players[curPlayer].mHand.addCard(card);
                players[curPlayer].onDealtCard(this, card);
                if (numCards == NUM_DECK_CARDS) {
                    state = State.BID;
                    for (int ii = 0; ii < NUM_PLAYERS; ii++) {
                        players[ii].mHand.sort();
                    }
                    curPlayer = (dealer + 1) % NUM_PLAYERS;
                } else {
                    curPlayer = (curPlayer + 1) % NUM_PLAYERS;
                }
                break;
            }

            case BID: {
                Player p = players[curPlayer];
                message("Player %s Place bid", p.getName());
                Bid [] options = computeBidOptions(curPlayer == dealer, p.getHand());
                Bid bid = p.makeBid(this, options);
                if (bid == null) {
                    message("Player %s passes", p.getName());
                    break;
                }
                if (bid.numTricks != 0) {
                    message("%s bids %d %s\n", p.getName(), bid.numTricks,
                            bid.trump.getSuitString());
                    Team bidTeam = getCurBidTeam();
                    if (bidTeam != null)
                        bidTeam.bid = Bid.NO_BID;
                    bidTeam = getTeam(p.mTeam);
                    bidTeam.bid = bid;
                    startPlayer = curPlayer;
                }
                if (curPlayer == dealer) {
                    if (curBidTeam < 0) {
                        startPlayer = (dealer + 1) % NUM_PLAYERS;
                    }
                    curPlayer = startPlayer;
                    state = State.TRICK;
                } else {
                    curPlayer = (curPlayer + 1) % NUM_PLAYERS;
                }
                message("Player %s bids %s", p.getName(), bid.toString());
                break;
            }

            case TRICK: {
                Player p = players[curPlayer];
                Card [] options = computePlayerTrickOptions(p);
                Card card = p.playTrick(this, options);
                if (card == null)
                    break;
                trick[curPlayer] = card;
                message("Player %s played %s", p.getName(), trick[curPlayer].toPrettyString());
                p.mHand.remove(card);
                p.mHand.sort();
                curPlayer = (curPlayer + 1) % NUM_PLAYERS;
                if (curPlayer == startPlayer)
                    state = State.PROCESS_TRICK;
                break;
            }

            case PROCESS_TRICK:
                processTrick();
                break;

            case RESET_TRICK:
                curPlayer = startPlayer;
                state = State.TRICK;
                break;

            case PROCESS_ROUND:
                message("Processing Round ...");
                processTeam(teams[0],
                        teams[0].roundPoints > teams[1].roundPoints);
                processTeam(teams[1],
                        teams[1].roundPoints > teams[0].roundPoints);

                // check for a winner
                if (teams[0].totalPoints >= WINNING_POINTS && teams[0].totalPoints > teams[1].totalPoints) {
                    message("Team %s wins!", teams[0].name);
                    state = State.GAME_OVER;
                }

                if (teams[1].totalPoints >= WINNING_POINTS && teams[1].totalPoints > teams[0].totalPoints) {
                    message("Team %s wins!", teams[1].name);
                    state = State.GAME_OVER;
                }
                
                if (state != State.GAME_OVER) {
                    dealer = (dealer + 1) % NUM_PLAYERS;
                    for (i = 0; i < NUM_PLAYERS; i++)
                        players[i].onProcessRound(this);
                    state = State.NEW_ROUND;
                }
                break;
            case GAME_OVER:
                break;
        }

    }

    /**
     * 
     * @param fileName
     * @throws IOException
     *
    public void saveGame(String fileName) throws IOException {
        File file = new File(fileName);
        if (DEBUG_ENABLED)
            System.out.println("Writing to " + file.getAbsolutePath());
        saveGame(new FileOutputStream(file));
    }
    
    /**
     * Save a game to the stream.  the stream is always closed.
     * @param output
     * @throws IOException
     *
    public void saveGame(OutputStream output) throws IOException {

        PrintWriter out = new PrintWriter(new OutputStreamWriter(output));

        try {
            int i;
    
            out.println("CUR_PLAYER:" + curPlayer);
            out.println("START_PLAYER:" + startPlayer);
            out.println("DEALER:" + dealer);
            out.println("STATE:" + state.ordinal());
            out.println("NUM_CARDS:" + numCards);
            out.println("CUR_BID_TEAM:" + curBidTeam);
            out.println("NUM_TRICKS:" + numTricks);
            out.println("NUM_ROUNDS:" + numRounds);

            int tricksFound = 0;
            for (i=0; i<trick.length; i++) {
                if (trick[i] != null) {
                    out.println("TRICK:" + i + " " + trick[i]);
                    tricksFound ++;
                }
            }
            //if (tricksFound != numTricks) {
            //    System.err.println("Expected equals");
           // }
//            assert(tricksFound == numTricks);
            if (tricksFound > 0) {
                assert(trick[startPlayer] != null);
            }
    
            for (i = 0; i < NUM_PLAYERS; i++) {
                if (players[i] == null)
                    continue;
                out.println("PLAYER:" + i + " " + describePlayer(players[i])
                        + " \"" + players[i].getName() + "\" {");
                players[i].write(out);
                out.println("}");
            }
    
            for (i = 0; i < NUM_TEAMS; i++) {
                Team tm = teams[i];
                out.println("TEAM:" + i + " \"" + tm.name + "\" {");
                out.println("BID:" + tm.bid);
                out.println("ROUND_PTS:" + tm.roundPoints);
                out.println("TOTAL_PTS:" + tm.totalPoints);
                out.println("}");
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getClass().getSimpleName() + " " + e.getMessage(), e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {}
        }
    }

    /**
     * Convenience
     * 
     * @param fileName
     * @throws IOException
     *
    public void loadGame(String fileName) throws IOException {
        loadGame(new FileInputStream(fileName));
    }
    
    /**
     * Load a game from a stream.  the stream is always closed even on exception.
     * @param in
     * @throws IOException
     *
    public void loadGame(InputStream in) throws IOException {

        final int[] lineNum = new int[1];
        String line;
        BufferedReader input = new BufferedReader(new InputStreamReader(in)) {
            @Override
            public String readLine() throws IOException {
                String line = super.readLine();
                lineNum[0]++;
                return line;
            }
        };

        try {
            while (true) {

                line = input.readLine();
                if (line == null)
                    break;
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#"))
                    continue; // skip past empty lines and comments
                
                int index = 0;
                String entry = parseNextLineElement(line, index++);
                if (entry.equals("CARD")) {
                    int num = Integer.parseInt(parseNextLineElement(line, index++));
                    Card card = Card.parseCard(parseNextLineElement(line, index++) + " " + parseNextLineElement(line, index++));
                    if (num < 0 || num >= NUM_DECK_CARDS) {
                        throw new IOException("Error line " + lineNum
                                + " Invalid value for card num " + num);
                    }
                    cards[num] = card;
                }

                else if (entry.equals("CUR_PLAYER")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    if (n < 0 || n >= NUM_PLAYERS) {
                        throw new Exception("Integer " + n
                                + " out of bounds (0-NUM_PLAYERS]");
                    }
                    this.curPlayer = n;
                }

                else if (entry.equals("START_PLAYER")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    if (n < 0 || n >= NUM_PLAYERS) {
                        throw new Exception("Integer " + n
                                + " out of bounds (0-NUM_PLAYERS]");
                    }
                    this.startPlayer = n;
                }

                else if (entry.equals("DEALER")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    if (n < 0 || n >= NUM_PLAYERS) {
                        throw new Exception("Integer " + n
                                + " out of bounds (0-NUM_PLAYERS]");
                    }
                    this.dealer = n;
                }

                else if (entry.equals("STATE")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    if (n < 0 || n >= State.values().length) {
                        throw new Exception("Integer " + n
                                + " out of bounds (0-" + State.values().length
                                + "]");
                    }
                    this.state = State.values()[n];
                }

                else if (entry.equals("NUM_CARDS")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    if (n < 0 || n > NUM_DECK_CARDS) {
                        throw new Exception("Integer " + n
                                + " out of bounds (0-" + NUM_DECK_CARDS + ")");
                    }
                    this.numCards = n;
                }

                else if (entry.equals("CUR_BID_TEAM")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    if (n >= NUM_TEAMS) {
                        throw new Exception("Integer " + n
                                + " out of bounds (-INF-NUM_TEAMS]");
                    }
                    this.curBidTeam = n;
                }

                else if (entry.equals("NUM_TRICKS")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    if (n < 0) {
                        throw new Exception("Invalid valaue for NUM_TRICKS '"
                                + n + "'");
                    }
                    this.numTricks = n;
                }

                else if (entry.equals("NUM_ROUNDS")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    if (n < 0) {
                        throw new Exception("Invalid valaue for NUM_ROUNDS '"
                                + n + "'");
                    }
                    this.numRounds = n;
                }
                
                else if (entry.equals("TRICK")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    trick[n] = Card.parseCard(parseNextLineElement(line, index++) + " " + parseNextLineElement(line, index++));
                }

                else if (entry.equals("PLAYER")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    Player p = instantiatePlayer(parseNextLineElement(line, index++), parseNextLineElement(line, index++));
                    if (n < 0 || n >= NUM_PLAYERS) {
                        throw new Exception("Integer value '" + n
                                + "' out of bounds [0-" + NUM_PLAYERS + ")");
                    }
                    p.read(this, input);
                    this.setPlayer(n, p);
                }

                else if (entry.equals("TEAM")) {
                    int n = Integer.parseInt(parseNextLineElement(line, index++));
                    if (n < 0 || n >= NUM_TEAMS) {
                        throw new Exception("Integer value '" + n
                                + "' out of bounds [0-" + NUM_TEAMS + ")");
                    }
                    teams[n].name = parseNextLineElement(line, index++);
                    teams[n].parseTeamInfo(input);
                }

                else {
                    throw new Exception("Cant parse line '" + line + "'");
                }
            }

        } catch (Exception e) {
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

    private final static Pattern pattern = Pattern.compile("(\"[A-Za-z0-9_ .]+\")|([1-9][0-9]*)|(0)|([a-zA-Z][^ :]*)");
    
    static String parseNextLineElement(String line, int index) {
        Matcher matcher = pattern.matcher(line);
        int start = 0;
        while (true) {
            if (!matcher.find(start))
                throw new IllegalArgumentException("Failed to find the " + index + "nth entry in line: " + line);
            start = matcher.end();
            if (--index < 0)
                break;
        }
        String s = matcher.group();
        if (s.startsWith("\"")) {
            s = s.substring(1, s.length()-1);
        }
        return s;
    }
    

    
    /**
     * 
     */
    public void newGame() {
        curPlayer = 0;
        startPlayer = 0;
        dealer = 0;
        state = State.NEW_GAME;
        numCards = 0;
        curBidTeam = -1;
        numTricks = 0;
        numRounds = 0;
        teams[0].bid = Bid.NO_BID;
        teams[1].bid = Bid.NO_BID;
        this.clearTricks();
    }

    /**
     * 
     * @return
     */
    public boolean isGameOver() {
        return state == State.GAME_OVER;
    }

    /**
     * 
     * @return
     */
    public State getState() {
        return state;
    }

    /**
     * 
     * @return
     */
    public Team getCurBidTeam() {
        return curBidTeam < 0 ? null : teams[curBidTeam];
    }

    /**
     * 
     * @param index
     * @return
     */
    public Card getTrick(int index) {
        return trick[index];
    }

    /**
     * 
     * @return
     */
    public Card getTrickLead() {
        return trick[startPlayer];
    }
    
    // package access for unit testing
    static int getTrickWinnerIndex(Suit trump, Card [] cards, int leadIndex) {
        int trickWinner = leadIndex;
        Card lead = cards[leadIndex];
        for (int i=1; i<cards.length; i++) {
            
            int ii = (i+leadIndex) % cards.length;
            
            Card test = cards[ii];
            if (test == null)
                return -1;

            if (trump == Suit.NOTRUMP) {
                
                if (test.suit != lead.suit) {
                    // lead wins
                    continue;
                } else {
                    // normal compare
                }
                
            } else {
                if (lead.suit == trump && test.suit != trump) {
                    // lead wins
                    continue;
                } else if (lead.suit != trump && test.suit == trump) {
                    // test wins
                    lead = test;
                    trickWinner = ii;
                    continue;
                } else {
                    // normal compare
                }
            }
            
            //assert(test.rank != lead.rank);
            //Utils.assertTrue(test.rank != lead.rank, "Expected ranks to be different");
            if (test.rank.ordinal() > lead.rank.ordinal()) {
                lead = test;
                trickWinner = ii;
            }
            
        }
        
        return trickWinner;
        
    }
    
    /**
     * 
     * @return
     */
    public int getTrickWinnerIndex() {
        return getTrickWinnerIndex(getTrump(), trick, startPlayer);
    }
    
    /**
     * 
     * @return
     */
    public Player getTrickWinner() {
        int index = getTrickWinnerIndex();
        if (index < 0)
            return null;
        return players[index];
    }

    /**
     * Zero based index.
     * @param index
     * @return
     */
    public Team getTeam(int index) {
        return teams[index];
    }

    /**
     * 
     * @return
     */
    public int getDealer() {
        return dealer;
    }

    /**
     * 
     * @return
     */
    public int getStartPlayer() {
        return startPlayer;
    }

    /**
     * Get the current trump suit based on the bid.
     * @return
     */
    public Suit getTrump() {
        return getCurBidTeam() == null || getCurBidTeam().bid == null ? Suit.NOTRUMP
                : getCurBidTeam().bid.trump;
    }

    /**
     * Get the lead suit
     * @return
     */
    public Suit getLead() {
        return trick[startPlayer] != null ? trick[startPlayer].suit : Suit.NOTRUMP;        
    }
    
    /**
     * Zero based index
     * @param index
     * @return
     */
    public Player getPlayer(int index) {
        return players[index];
    }
    
    /**
     * 
     * @return
     */
    public Iterable<Player> getPlayers() {
        return Arrays.asList(players);
    }

    /**
     * 
     * @return
     */
    public int getNumRounds() {
        return numRounds;
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
     * Handle to affect how loadGame instantiates a player from a player description.
     * Default behavior is to treat description as a classname.
     * 
     * Class in question must have constructor with signature:
     * classname(String name)
     * 
     * @param clazz
     * @param name
     * @return
     * @throws Exception
     *
    protected Player instantiatePlayer(String clazz, String name) throws Exception {
        return (Player) getClass().getClassLoader().loadClass(clazz).getConstructor(new Class []{ String.class }).newInstance(name);    
    }
    
    /**
     * Handle to affect how saveGame describes a player.  Default behavior is to use the players classname.
     * @param player
     * @return
     *
    protected String describePlayer(Player player) {
        return player.getClass().getName();
    }

    /**
     * 
     * @return
     */
    public Card[] getDeck() {
        return cards;
    }

    /**
     * Return the winning card from 2 cards.  The current state of the game affects this method, 
     * specifically, the trick lead and trump suit is used.
     * 
     * @param c0
     * @param c1
     * @return
     */
    public Card getWinningCard(Card c0, Card c1) {
        Suit lead = getTrickLead() == null ? Suit.NOTRUMP : getTrickLead().suit;
        Suit trump = getTrump();

        // trump suits win anything that is not a trump
        if (c0.suit == trump && c1.suit != trump)
            return c0;

        if (c0.suit != trump && c1.suit == trump)
            return c1;

        // otherwise lead suits win anythiing that is not a lead
        if (c0.suit == lead && c1.suit != lead)
            return c0;

        if (c0.suit != lead && c1.suit == lead)
            return c1;

        // otherwise use the rank
        if (c0.rank.ordinal() < c1.rank.ordinal())
            return c1;

        return c0;
    }

    /**
     * Get the card instance associated with a rank and suit.
     * return null for invalid cards. 
     * @param rank
     * @param suit
     * @return
     */
    public Card getCard(Rank rank, Suit suit) {
        for (int i = 0; i < NUM_DECK_CARDS; i++) {
            if (cards[i].rank == rank && cards[i].suit == suit)
                return cards[i];
        }
        throw new IllegalArgumentException("No card " + rank + " of " + suit + " in deck");
    }

    public int getNumDeckCards() {
        return NUM_DECK_CARDS;
    }

    public int getNumPlayers() {
        return NUM_PLAYERS;
    }

    public int getNumTeams() {
        return NUM_TEAMS;
    }

    public int getWinningPoints() {
        return WINNING_POINTS;
    }

    public int getTeammate(int player) {
        Team t = teams[getPlayer(player).getTeam()];
        if (t.players[0] == player) {
            return t.players[1];
        } else if (t.players[1] == player) {
            return t.players[0];
        }
        throw new RuntimeException("Cannot find team for player " + player);
    }
    
}
