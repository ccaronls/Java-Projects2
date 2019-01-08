package cc.lib.monopoly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Monopoly extends Reflector<Monopoly> {

    static {
        addAllFields(Monopoly.class);
    }

    public final static int NUM_SQUARES = Square.values().length;
    public final static int MAX_PLAYERS = 4;

    final List<Player> players = new ArrayList<>();
    int currentPlayer;
    final Stack<State> state = new Stack<>();
    int die1, die2;
    int kitty;
    final LinkedList<CardActionType> chance = new LinkedList<>();
    final LinkedList<CardActionType> communityChest = new LinkedList<>();

    public enum State {
        INIT,
        TURN,
        PURCHASE_OR_SKIP,
        PAY_RENT,
        PAY_KITTY,
        PAY_PLAYERS,
        GAME_OVER,
        CHOOSE_MORTGAGE_PROPERTY,
        CHOOSE_UNMORTGAGE_PROPERTY,
        CHOOSE_PROPERTY_FOR_UNIT
    }

    public final void newGame() {
        state.clear();
    }

    public final void clear() {
        players.clear();
        currentPlayer = -1;
        state.clear();
        die1=die2=0;
        kitty=0;
        chance.clear();
        communityChest.clear();
    }

    public final void runGame() {
        Player cur = getCurrentPlayer();
        if (state.isEmpty())
            state.push(State.INIT);
        switch (state.peek()) {
            case INIT: {
                if (players.size() < 2)
                    throw new RuntimeException("Not enough players");
                for (Player p : players) {
                    p.square = 0;
                    p.cards.clear();
                    p.money = 5*1 + 5*5 + 5*10 + 5*20 + 5*50 + 5*100 + 500; // TODO: Config
                }
                currentPlayer = Utils.rand() % players.size();
                kitty = 0;
                initChance();
                initCommunityChest();
                state.push(State.TURN);
                break;
            }
            case TURN: {
                List<MoveType> moves = new ArrayList<>();
                moves.add(MoveType.ROLL_DICE);
                if (cur.inJail) {
                    if (cur.money >= 50) {
                        moves.add(MoveType.PAY_BOND);
                    }
                    if (cur.hasGetOutOfJailFreeCard())
                        moves.add(MoveType.GET_OUT_OF_JAIL_FREE);
                } else {
                    if (cur.getCardsForNewHouse().size() > 0) {
                        moves.add(MoveType.BUY_UNIT);
                    }
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                if (cur.getCardsForUnMortgage().size() > 0) {
                    moves.add(MoveType.UNMORTGAGE);
                }
                MoveType move = cur.chooseMove(moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }


            case PURCHASE_OR_SKIP: {
                List<MoveType> moves = new ArrayList<>();
                if (cur.money > Square.values()[cur.square].getPrice()) {
                    moves.add(MoveType.PURCHASE);
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                moves.add(MoveType.SKIP);
                MoveType move = cur.chooseMove(moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }
            case PAY_RENT: {
                List<MoveType> moves = new ArrayList<>();
                int rent = getRent(cur.square);
                if (cur.money >= rent) {
                    moves.add(MoveType.PAY_RENT);
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                MoveType move = cur.chooseMove(moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }
            case PAY_KITTY: {
                List<MoveType> moves = new ArrayList<>();
                if (cur.money >= cur.debt) {
                    moves.add(MoveType.PAY_KITTY);
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                MoveType move = cur.chooseMove(moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }
            case PAY_PLAYERS: {
                List<MoveType> moves = new ArrayList<>();
                if (cur.money >= cur.debt) {
                    moves.add(MoveType.PAY_KITTY);
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                MoveType move = cur.chooseMove(moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }
            case CHOOSE_MORTGAGE_PROPERTY: {
                List<Card> cards = new ArrayList<>();
                for (Card c : cur.cards) {
                    if (c.canMortgage()) {
                       cards.add(c);
                    }
                }
                Card card = cur.chooseCard(cards);
                if (card != null) {
                    int mortgageAmt = card.property.getMortgageValue();
                    onPlayerMortgaged(currentPlayer, card.property, mortgageAmt);
                    cur.money += mortgageAmt;
                    state.pop();
                }
                break;
            }
            case CHOOSE_UNMORTGAGE_PROPERTY: {
                List<Card> cards = new ArrayList<>();
                for (Card c : cur.cards) {
                    if (c.canUnMortgage()) {
                        cards.add(c);
                    }
                }
                Card card = cur.chooseCard(cards);
                if (card != null) {
                    int buyBackAmt = card.property.getMortgageBuybackPrice();
                    onPlayerUnMortgaged(currentPlayer, card.property, buyBackAmt);
                    cur.money -= buyBackAmt;
                    state.pop();
                }
                break;
            }
            case CHOOSE_PROPERTY_FOR_UNIT: {
                List<Card> cards = new ArrayList<>();
                for (Card c : cur.cards) {
                    if (c.property != null && c.houses < 5 && c.property.getHousePrice() <= cur.money) {
                        cards.add(c);
                    }
                }
                Card card = cur.chooseCard(cards);
                if (card != null) {
                    int houseCost = card.property.getHousePrice();
                    if (card.houses < 4) {
                        onPlayerBoughtHouse(currentPlayer, card.property, houseCost);
                    } else {
                        onPlayerBoughtHotel(currentPlayer, card.property, houseCost);
                    }
                    cur.money -= houseCost;
                    card.houses++;
                    state.pop();
                }
                break;
            }
            case GAME_OVER:
                break;

            default:
                Utils.unhandledCase(state.peek());
        }
    }

    private void advance(int squares) {
        Player cur = getCurrentPlayer();
        cur.square += squares;
        if (cur.square >= NUM_SQUARES) {
            getPaid(200);
            cur.square %= NUM_SQUARES;
        }
        onPlayerMove(currentPlayer, squares);
    }

    private void processMove(MoveType move) {
        Player cur = getCurrentPlayer();
        switch (move) {
            case SKIP:
                nextPlayer();
                break;
            case ROLL_DICE:
                rollDice();
                if (cur.inJail) {
                    if (die1 == die2) {
                        onPlayerOutOfJail(currentPlayer);
                    }
                } else
                    advance(getDice());
                processSquare();
                break;

            case MORTGAGE:
                state.push(State.CHOOSE_MORTGAGE_PROPERTY);
                break;

            case UNMORTGAGE:
                state.push(State.CHOOSE_UNMORTGAGE_PROPERTY);
                break;

            case BUY_UNIT:
                state.push(State.CHOOSE_PROPERTY_FOR_UNIT);
                break;

            case FORFEIT:
                playerEliminated();
                break;

            case PAY_BOND:
                onPlayerOutOfJail(currentPlayer);
                payToKitty(50);
                nextPlayer();
                break;

            case GET_OUT_OF_JAIL_FREE:
                onPlayerOutOfJail(currentPlayer);
                cur.useGetOutOfJailCard();
                nextPlayer();
                break;

            case PAY_RENT: {
                payRent(cur.debt);
                break;
            }

            case PAY_KITTY: {
                payToKitty(cur.debt);
                break;
            }

            case PAY_PLAYERS: {
                int amt = cur.debt / getNumActivePlayers();
                for (int i=0; i<players.size(); i++) {
                    if (i == currentPlayer)
                        continue;
                    Player p = getPlayer(i);
                    if (p.isEliminated())
                        continue;
                    onPlayerReceiveMoney(i, amt);
                    p.money += amt;
                }
                cur.debt = 0;
                break;
            }

            case PURCHASE: {
                Square sq = Square.values()[cur.square];
                Utils.assertTrue(sq.isProperty());
                Utils.assertTrue(getOwner(sq) < 0);
                Utils.assertTrue(cur.money >= sq.getPrice());
                onPlayerPurchaseProperty(currentPlayer, sq);
                cur.cards.add(Card.newPropertyCard(sq));
                cur.money -= sq.getPrice();
                break;
            }
        }
    }

    public final void addPlayer(Player player) {
        Utils.assertTrue(players.size() < MAX_PLAYERS);
        this.players.add(player);
        if (player.piece == null) {
            player.piece = Utils.randItem(getUnusedPiece());
        }
    }

    public final List<Piece> getUnusedPiece() {
        List<Piece> pieces = new ArrayList<>(Arrays.asList(Piece.values()));
        for (Player p : players) {
            pieces.remove(p.piece);
        }
        return pieces;
    }

    public final Player getCurrentPlayer() {
        return players.get(currentPlayer);
    }

    private void rollDice() {
        die1 = 1+Utils.rand()%6;
        die2 = 1+Utils.rand()%6;
        onDiceRolled();
    }

    private int getDice() {
        return die1+die2;
    }

    public final int getDie1() {
        return die1;
    }

    public final int getDie2() {
        return die2;
    }

    void initChance() {
        chance.clear();
        for (CardActionType c : CardActionType.values()) {
            if (c.isChance())
                chance.add(c);
        }
        Utils.shuffle(chance);
    }

    void initCommunityChest() {
        communityChest.clear();
        for (CardActionType c : CardActionType.values()) {
            if (!c.isChance())
                communityChest.add(c);
        }
        Utils.shuffle(communityChest);
    }

    void processCommunityChest() {
        CardActionType c = communityChest.removeLast();
        if (communityChest.size() == 0)
            initCommunityChest();
        onPlayerDrawsCommunityChest(currentPlayer, c);
        processAction(c);
    }

    void processChance() {
        CardActionType c = chance.removeLast();
        if (chance.size() == 0)
            initChance();
        onPlayerDrawsChance(currentPlayer, c);
        processAction(c);
    }

    int getRent(int square) {
        Square sq = Square.values()[square];
        int owner = getOwner(sq);
        if (owner >= 0) {
            return getPlayer(owner).getRent(sq, getDice());
        }
        return 0;
    }

    void advanceToSquare(Square square, int rentScale) {
        Player cur = getCurrentPlayer();

        if (square.isProperty()) {
            int owner = getOwner(square);
            if (owner < 0 && cur.getValue() >= square.getPrice()) {
                state.push(State.PURCHASE_OR_SKIP);
            } else if (owner >= 0 && owner != currentPlayer) {
                int rent = getPlayer(owner).getRent(square, getDice()) * rentScale;
                payMoneyOrElse(rent, owner, State.PAY_RENT);
            } else {
                nextPlayer();
            }
        } else {
            nextPlayer();
        }
    }

    int getMovesTo(Square s) {
        Player cur = getCurrentPlayer();
        int moves = 1;
        for ( ; moves<NUM_SQUARES; moves++) {
            s = Square.values()[(cur.square + moves) % NUM_SQUARES];
            if (s == Square.READING_RR) {
                break;
            }
        }
        return moves;
    }

    void payMoneyOrElse(int amount, int renter, State payState) {
        Player cur = getCurrentPlayer();
        if (amount <= 0) {
            nextPlayer();
        } else if (cur.money >= amount) {
            switch (payState) {
                case PAY_KITTY:
                    payToKitty(amount);
                    break;
                case PAY_RENT:
                    payRent(amount);
                    break;
                case PAY_PLAYERS:
                    int amt = amount / (getNumActivePlayers()-1);
                    for (int i=0; i<players.size(); i++) {
                        if (i == currentPlayer)
                            continue;
                        Player p = getPlayer(i);
                        if (p.isEliminated())
                            continue;
                        onPlayerReceiveMoney(i, amt);
                        p.money += amt;
                    }
                    break;
                default:
                    Utils.unhandledCase(payState);
            }
            cur.money -= amount;
            nextPlayer();
        } else if (cur.getValue() >= amount) {
            cur.debt = amount;
            state.push(payState);
        } else {
            playerEliminated();
        }
    }

    void getPaid(int amount) {
        onPlayerReceiveMoney(currentPlayer, amount);
        getCurrentPlayer().money += amount;
    }

    void processAction(CardActionType type) {
        Player cur = getCurrentPlayer();
        onPlayerDrawCard(currentPlayer, type.desc);
        switch (type) {
            case CH_GO_BACK:
                cur.square = (cur.square+NUM_SQUARES-3) % NUM_SQUARES;
                onPlayerMove(currentPlayer, -3);
                processSquare();
                break;
            case CH_LOAN_MATURES:
                getPaid(150);
                nextPlayer();
                break;
            case CH_MAKE_REPAIRS: {
                int repairs = cur.getNumHouses() * 25 + cur.getNumHotels() * 150;
                payMoneyOrElse(repairs, 0, State.PAY_KITTY);
                break;
            }
            case CH_GET_OUT_OF_JAIL:
                cur.cards.add(Card.newGetOutOfJailFreeCard());
                nextPlayer();
                break;
            case CH_ELECTED_CHAIRMAN:
                payMoneyOrElse(players.size()-1*50, 0, State.PAY_PLAYERS);
                break;
            case CH_ADVANCE_RR:
            case CH_ADVANCE_RR2: {
                int moves = 1;
                Square s = null;
                for ( ; moves<NUM_SQUARES; moves++) {
                    s = Square.values()[(cur.square + moves) % NUM_SQUARES];
                    if (s.isRailroad()) {
                        break;
                    }
                }

                advance(moves);
                advanceToSquare(s, 2);
                break;
            }
            case CH_ADVANCE_ILLINOIS: {
                int moves = getMovesTo(Square.ILLINOIS_AVE);
                advance(moves);
                advanceToSquare(Square.ILLINOIS_AVE, 2);
                break;
            }
            case CH_ADVANCE_TO_NEAREST_UTILITY: {
                int moves = 1;
                Square s = null;
                for ( ; moves<NUM_SQUARES; moves++) {
                    s = Square.values()[(cur.square + moves) % NUM_SQUARES];
                    if (s.isUtility()) {
                        break;
                    }
                }

                advance(moves);
                advanceToSquare(s, 2);
                break;
            }
            case CH_ADVANCE_READING_RR: {
                int moves = getMovesTo(Square.READING_RR);
                advance(moves);
                advanceToSquare(Square.READING_RR, 1);
                break;
            }
            case CH_BANK_DIVIDEND:
                getPaid(50);
                nextPlayer();
                break;
            case CH_GO_TO_JAIL:
                gotoJail();
                break;
            case CH_SPEEDING_TICKET:
                payMoneyOrElse(50, 0, State.PAY_KITTY);
                break;
            case CH_ADVANCE_BOARDWALK: {
                int moves = getMovesTo(Square.BOARDWALK);
                advance(moves);
                advanceToSquare(Square.BOARDWALK, 1);
                break;
            }
            case CH_ADVANCE_ST_CHARLES: {
                int moves = getMovesTo(Square.ST_CHARLES_PLACE);
                advance(moves);
                advanceToSquare(Square.ST_CHARLES_PLACE, 1);
                break;
            }
            case CH_ADVANCE_GO: {
                int moves = getMovesTo(Square.GO);
                advance(moves);
                advanceToSquare(Square.GO, 1);
                break;
            }
            case CC_BANK_ERROR:
                getPaid(200);
                nextPlayer();
                break;
            case CC_SALE_OF_STOCK:
                getPaid(50);
                nextPlayer();
                break;
            case CC_BEAUTY_CONTEST:
                getPaid(10);
                nextPlayer();
                break;
            case CC_ASSESSED_REPAIRS: {
                int repairs = cur.getNumHouses()*40 + cur.getNumHotels()*115;
                payMoneyOrElse(repairs, 0, State.PAY_KITTY);
                break;
            }
            case CC_HOSPITAL_FEES:
                payMoneyOrElse(100, 0, State.PAY_KITTY);
                break;
            case CC_CONSULTANCY_FEE:
                getPaid(25);
                nextPlayer();
                break;
            case CC_HOLIDAY_FUND_MATURES:
                getPaid(100);
                nextPlayer();
                break;
            case CC_LIFE_INSURANCE_MATURES:
                getPaid(100);
                nextPlayer();
                break;
            case CC_BIRTHDAY:
                break;
            case CC_SCHOOL_FEES:
                break;
            case CC_ADVANCE_TO_GO: {
                int moves = getMovesTo(Square.GO);
                advance(moves);
                advanceToSquare(Square.GO, 1);
                getPaid(200);
                break;
            }
            case CC_GO_TO_JAIL:
                gotoJail();
                break;
            case CC_INHERITANCE:
                getPaid(100);
                nextPlayer();
                break;
            case CC_INCOME_TAX_REFUND:
                getPaid(20);
                nextPlayer();
                break;
            case CC_DOCTORS_FEES:
                payMoneyOrElse(50, 0, State.PAY_KITTY);
                break;
            case CC_GET_OUT_OF_JAIL:
                cur.cards.add(Card.newGetOutOfJailFreeCard());
                nextPlayer();
                break;
            default:
                Utils.unhandledCase(type);
        }
    }

    void gotoJail() {
        Player cur = getCurrentPlayer();
        cur.square = Square.VISITING_JAIL.ordinal();
        cur.inJail = true;
        onPlayerGoesToJail(currentPlayer);
        nextPlayer();
    }

    private void processSquare() {
        Player cur = getCurrentPlayer();
        Square square = Square.values()[cur.square];
        switch (square) {

            case GO:
                break;
            case VISITING_JAIL:
                break;
            case FREE_PARKING:
                if (kitty > 0) {
                    onPlayerReceiveMoney(currentPlayer, kitty);
                    getCurrentPlayer().money += kitty;
                    kitty = 0;
                }
                break;
            case GOTO_JAIL:
                gotoJail();
                break;

            case INCOME_TAX:
            case LUXURY_TAX:
                payMoneyOrElse(square.getTax(), 0, State.PAY_KITTY);
                break;

            case COMM_CHEST1:
            case COMM_CHEST2:
            case COMM_CHEST3:
                processCommunityChest();
                break;

            case CHANCE1:
            case CHANCE2:
            case CHANCE3:
                processChance();
                break;

            case MEDITERRANEAN_AVE:
            case BALTIC_AVE:
            case ORIENTAL_AVE:
            case VERMONT_AVE:
            case CONNECTICUT_AVE:
            case ST_CHARLES_PLACE:
            case ELECTRIC_COMPANY:
            case STATES_AVE:
            case VIRGINIA_AVE:
            case READING_RR:
            case B_AND_O_RR:
            case SHORT_LINE_RR:
            case PENNSYLVANIA_RR:
            case ST_JAMES_PLACE:
            case TENNESSEE_AVE:
            case NEW_YORK_AVE:
            case KENTUCKY_AVE:
            case INDIANA_AVE:
            case ILLINOIS_AVE:
            case ATLANTIC_AVE:
            case VENTNOR_AVE:
            case WATER_WORKS:
            case MARVIN_GARDINS:
            case PACIFIC_AVE:
            case NORTH_CAROLINA_AVE:
            case PENNSYLVANIA_AVE:
            case PARK_PLACE:
            case BOARDWALK: {
                int owner = getOwner(square);
                if (owner != currentPlayer) {
                    if (owner < 0 && cur.getValue() >= square.getPrice()) {
                        state.push(State.PURCHASE_OR_SKIP);
                        break;
                    } else if (owner >= 0) {
                        Card card = players.get(owner).getCard(square);
                        int rent = getPlayer(owner).getRent(card.property, getDice());
                        payMoneyOrElse(rent, owner, State.PAY_RENT);
                        break;
                    }
                }
                nextPlayer();
                break;
            }

            default:
                Utils.unhandledCase(square);
        }
    }

    public final int getWinner() {
        int num = 0;
        int winner = -1;
        for (int i=0; i<players.size(); i++) {
            Player p = players.get(i);
            if (p.isEliminated())
                continue;
            winner = i;
            num++;
        }
        if (num == 1)
            return winner;
        return -1;
    }

    // player eliminated means all their mortgaged property goes back to bank and they have zero money
    private void playerEliminated() {
        getCurrentPlayer().eliminated();
        if (getWinner() >= 0) {
            state.clear();
            state.push(State.GAME_OVER);
        } else {
            nextPlayer();
        }
    }

    private void nextPlayer() {
        if (getWinner() < 0) {
            do {
                currentPlayer = (currentPlayer + 1) % players.size();
            } while (getCurrentPlayer().isEliminated());
            state.clear();
            state.push(State.TURN);
        } else {
            state.clear();
            state.push(State.GAME_OVER);
        }
    }

    private void payRent(int amount) {
        Player cur = getCurrentPlayer();
        Utils.assertTrue(cur.debt > 0);
        int owner = getOwner(cur.square);
        Utils.assertFalse(owner == currentPlayer);
        onPlayerPaysRent(currentPlayer, owner, cur.debt);
        cur.money -= cur.debt;
        Utils.assertTrue(cur.money >= 0);
        getPlayer(owner).money += cur.debt;
        cur.debt = 0;

    }

    private void payToKitty(int amt) {
        Utils.assertTrue(amt > 0);
        Player cur = getCurrentPlayer();
        Utils.assertTrue(cur.money >= 0);
        onPlayerPayMoneyToKitty(currentPlayer, amt);
        cur.money -= amt;
        kitty += amt;
    }

    private int getOwner(int sq) {
        return getOwner(Square.values()[sq]);
    }

    private int getOwner(Square square) {
        if (!square.isProperty())
            return -1;
        for (int i=0; i<players.size(); i++) {
            Player p = players.get(i);
            if (p.ownsProperty(square)) {
                return i;
            }
        }
        return -1;
    }

    public final int getNumPlayers() {
        return players.size();
    }

    public final int getNumActivePlayers() {
        int num = 0;
        for (Player p : players) {
            if (p.isEliminated())
                continue;
            num++;
        }
        return num;
    }

    public final Player getPlayer(int index) {
        return players.get(index);
    }

    // CALLBACKS CAN BE OVERRIDDEN TO HANDLE EVENTS

    protected void onDiceRolled() {}

    /**
     * numSquares can be negative
     * @param playerNum
     * @param numSquares
     */
    protected void onPlayerMove(int playerNum, int numSquares) {}

    protected void onPlayerDrawsChance(int playerNum, CardActionType chance) {}

    protected void onPlayerDrawsCommunityChest(int playerNum, CardActionType commChest) {}

    protected void onPlayerReceiveMoney(int playerNum, int amt) {}

    protected void onPlayerPayMoneyToKitty(int playerNum, int amt) {}

    protected void onPlayerDrawCard(int playerNum, String desc) {}

    protected void onPlayerGoesToJail(int playerNum) {}

    protected void onPlayerOutOfJail(int playerNum) {}

    protected void onPlayerPaysRent(int playerNum, int renterPlayer, int amt) {}

    protected void onPlayerMortgaged(int playerNum, Square property, int amt) {}

    protected void onPlayerUnMortgaged(int playerNum, Square property, int amt) {}

    protected void onPlayerPurchaseProperty(int playerNum, Square property) {}

    protected void onPlayerBoughtHouse(int playerNum, Square property, int amt) {}

    protected void onPlayerBoughtHotel(int playerNum, Square property, int amt) {}
}
