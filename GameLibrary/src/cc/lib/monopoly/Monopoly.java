package cc.lib.monopoly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Reflector;

public class Monopoly extends Reflector<Monopoly> {

    private final static Logger log = LoggerFactory.getLogger(Monopoly.class);

    static {
        addAllFields(Monopoly.class);
        addAllFields(StackItem.class);
    }

    public final static class StackItem extends Reflector<StackItem> {
        final State state;
        final int [] data;

        public StackItem() {
            state = null;
            data = null;
        }

        public StackItem(State state, int ... data) {
            this.state = state;
            this.data = data;
        }

        @Override
        public String toString() {
            return state.name() + Arrays.toString(data);
        }
    }

    public final static int NUM_SQUARES = Square.values().length;
    public final static int MAX_PLAYERS = 4;
    public final static int MAX_HOUSES  = 5;
    public final static int RAILROAD_RENT = 25;

    private final List<Player> players = new ArrayList<>();
    private int currentPlayer;
    private final Stack<StackItem> state = new Stack<>();
    private int die1, die2;
    private int kitty;
    private final LinkedList<CardActionType> chance = new LinkedList<>();
    private final LinkedList<CardActionType> communityChest = new LinkedList<>();
    private int [] dice;
    private final Rules rules = new Rules();

    public enum State {
        INIT,
        TURN,
        SET_PLAYER,
        PURCHASE_OR_SKIP,
        PAY_RENT,
        PAY_KITTY,
        PAY_PLAYERS,
        PAY_BIRTHDAY,
        GAME_OVER,
        CHOOSE_MORTGAGE_PROPERTY,
        CHOOSE_UNMORTGAGE_PROPERTY,
        CHOOSE_PROPERTY_FOR_UNIT,
        CHOOSE_TRADE,
        CHOOSE_CARDS_FOR_SALE,
        CHOOSE_PURCHASE_PROPERTY, // players are offered chance to purchase property if the current player declines
    }

    public final void newGame() {
        state.clear();
        state.push(new StackItem(State.INIT, -1));
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

    private void pushState(State state, int ... data) {
        this.state.push(new StackItem(state, data));
    }

    private void popState() {
        if (state.size() == 1) {
            System.out.println("Popping last item");
        }
        state.pop();
    }

    public void runGame() {
        if (state.isEmpty()) throw new AssertionError("runGame called with empty stack");
        //    pushState(State.INIT, null);
        log.debug("runGame: states: " + state.toString());
        StackItem item = state.peek();
        switch (item.state) {
            case INIT: {
                Utils.assertTrue(state.size() == 1);
                if (players.size() < 2)
                    throw new RuntimeException("Not enough players");
                for (int i = 0; i<getNumPlayers(); i++) {
                    Player p = getPlayer(i);
                    p.clear();
                    onPlayerGotPaid(i, rules.startMoney);
                    p.addMoney(rules.startMoney);
                    if (p.getPiece() == null) {
                        p.setPiece(Utils.randItem(getUnusedPieces()));
                    }
                }
                currentPlayer = Utils.rand() % players.size();
                kitty = 0;
                initChance();
                initCommunityChest();
                state.clear();
                pushState(State.TURN);
                break;
            }
            case SET_PLAYER: {
                currentPlayer = getData(0);
                popState();
                break;
            }
            case TURN: {
                Player cur = getCurrentPlayer();
                Utils.assertTrue(cur.getValue() > 0);
                List<MoveType> moves = new ArrayList<>();
                moves.add(MoveType.ROLL_DICE);
                if (cur.isInJail()) {
                    if (cur.getMoney() >= cur.getJailBond()) {
                        moves.add(MoveType.PAY_BOND);
                    }
                    if (cur.hasGetOutOfJailFreeCard())
                        moves.add(MoveType.GET_OUT_OF_JAIL_FREE);
                } else {
                    if (cur.getCardsForNewHouse().size() > 0) {
                        moves.add(MoveType.UPGRADE);
                    }
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                if (cur.getCardsForUnMortgage().size() > 0) {
                    moves.add(MoveType.UNMORTGAGE);
                }
                if (getTradeOptions(cur).size() > 0)
                    moves.add(MoveType.TRADE);
                if (cur instanceof PlayerUser) {
                    for (Card c : cur.getCards()) {
                        if (c.isSellable()) {
                            moves.add(MoveType.MARK_CARDS_FOR_SALE);
                            break;
                        }
                    }
                    moves.add(MoveType.FORFEIT);
                }

                MoveType move = cur.chooseMove(this, moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }

            case PURCHASE_OR_SKIP: {
                Player cur = getCurrentPlayer();
                List<MoveType> moves = new ArrayList<>();
                if (cur.getSquare().canPurchase() && cur.getMoney() >= cur.getSquare().getPrice()) {
                    moves.add(MoveType.PURCHASE);
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                moves.add(MoveType.END_TURN);
                MoveType move = cur.chooseMove(this, moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }

            case PAY_RENT: {
                Player cur = getCurrentPlayer();
                int rent = getData(0);
                int toWhom = getData(1);
                if (cur.getMoney() >= rent) {
                    onPlayerPaysRent(currentPlayer, toWhom, rent);
                    cur.addMoney(-rent);
                    getPlayer(toWhom).addMoney(rent);
                    nextPlayer(true);
                } else if (cur.getValue() > rent) {
                    pushState(State.CHOOSE_MORTGAGE_PROPERTY);
                } else {
                    // transfer all property and money the the renter
                    if (cur.getMoney() > 0) {
                        onPlayerGotPaid(toWhom, cur.getMoney() / (getNumActivePlayers()-1));
                    }
                    for (Card c : cur.getCards()) {
                        cur.removeCard(c);
                        getPlayer(toWhom).addCard(c);
                    }
                    if (playerBankrupt(currentPlayer))
                        nextPlayer(true);
                }
                break;
            }

            case PAY_KITTY: {
                Player cur = getCurrentPlayer();
                int amt = getData(0);
                if (cur.getMoney() >= amt) {
                    onPlayerPayMoneyToKitty(currentPlayer, amt);
                    cur.addMoney(-amt);
                    kitty += amt;
                    nextPlayer(true);
                } else if (cur.getValue() > amt) {
                    pushState(State.CHOOSE_MORTGAGE_PROPERTY);
                } else {
                    if (cur.getMoney() > 0) {
                        onPlayerPayMoneyToKitty(currentPlayer, cur.getMoney());
                    }
                    if (playerBankrupt(currentPlayer))
                        nextPlayer(true);
                }
                break;
            }

            case PAY_PLAYERS: {
                Player cur = getCurrentPlayer();
                int amt = getData(0);
                int total = amt * (getNumActivePlayers()-1);
                if (cur.getMoney() >= total) {
                    onPlayerGotPaid(currentPlayer, -total);
                    for (int i=0; i<getNumPlayers(); i++) {
                        if (i == currentPlayer)
                            continue;
                        Player p = getPlayer(i);
                        if (p.isBankrupt())
                            continue;
                        onPlayerGotPaid(i, amt);
                        p.addMoney(amt);
                    }
                    nextPlayer(true);
                } else if (cur.getValue() > total) {
                    pushState(State.CHOOSE_MORTGAGE_PROPERTY);
                } else {
                    // split the money up for the remaining players
                    if (cur.getMoney() > 0 && getNumActivePlayers() > 2) {
                        total = cur.getMoney() / (getNumActivePlayers()-1);
                        for (int i=0; i<getNumPlayers(); i++) {
                            if (i==currentPlayer)
                                continue;
                            Player p = getPlayer(i);
                            if (p.isBankrupt())
                                continue;
                            onPlayerGotPaid(i, total);
                            p.addMoney(total);
                        }
                    }
                    if (playerBankrupt(currentPlayer))
                        nextPlayer(true);
                }
                break;
            }

            case PAY_BIRTHDAY: {
                int amt = getData(0);
                currentPlayer = getData(1);
                int toWhom = getData(2);
                Player cur = getCurrentPlayer();
                if (cur.getMoney() >= amt) {
                    onPlayerGotPaid(currentPlayer, -amt);
                    cur.addMoney(-amt);
                    onPlayerGotPaid(toWhom, amt);
                    getPlayer(toWhom).addMoney(amt);
                    popState();
                } else if (cur.getValue() > amt) {
                    pushState(State.CHOOSE_MORTGAGE_PROPERTY);
                } else {
                    if (playerBankrupt(currentPlayer))
                        popState();
                }
                break;
            }

            case CHOOSE_MORTGAGE_PROPERTY: {
                Player cur = getCurrentPlayer();
                List<Card> cards = cur.getCardsForMortgage();
                Card card = cur.chooseCard(this, cards, Player.CardChoiceType.CHOOSE_CARD_TO_MORTGAGE);
                if (card != null) {
                    int mortgageAmt = card.property.getMortgageValue(card.houses);
                    onPlayerMortgaged(currentPlayer, card.property, mortgageAmt);
                    cur.addMoney(mortgageAmt);
                    card.mortgaged = true;
                    card.houses = 0;
                    popState();
                }
                break;
            }

            case CHOOSE_UNMORTGAGE_PROPERTY: {
                Player cur = getCurrentPlayer();
                List<Card> cards = cur.getCardsForUnMortgage();
                Card card = cur.chooseCard(this, cards, Player.CardChoiceType.CHOOSE_CARD_TO_UNMORTGAGE);
                if (card != null) {
                    int buyBackAmt = card.property.getMortgageBuybackPrice();
                    onPlayerUnMortgaged(currentPlayer, card.property, buyBackAmt);
                    cur.addMoney(-buyBackAmt);
                    card.mortgaged = false;
                    popState();
                }
                break;
            }

            case CHOOSE_PROPERTY_FOR_UNIT: {
                Player cur = getCurrentPlayer();
                List<Card> cards = cur.getCardsForNewHouse();
                Card card = cur.chooseCard(this, cards, Player.CardChoiceType.CHOOSE_CARD_FOR_NEW_UNIT);
                if (card != null) {
                    int houseCost = card.property.getUnitPrice();
                    if (card.houses < 4) {
                        onPlayerBoughtHouse(currentPlayer, card.property, houseCost);
                    } else {
                        onPlayerBoughtHotel(currentPlayer, card.property, houseCost);
                    }
                    cur.addMoney(-houseCost);
                    card.houses++;
                    popState();
                }
                break;
            }

            case CHOOSE_TRADE: {
                Player cur = getCurrentPlayer();
                List<Trade> trades = getTradeOptions(cur);
                Utils.assertTrue(trades.size() > 0);
                Trade trade = cur.chooseTrade(this, trades);
                if (trade != null) {
                    onPlayerTrades(getCurrentPlayerNum(), getPlayerNum(trade.getTrader()), trade.getCard().property, trade.getPrice());
                    cur.addMoney(-trade.getPrice());
                    trade.getTrader().addMoney(trade.getPrice());
                    trade.getTrader().removeCard(trade.getCard());
                    cur.addCard(trade.getCard());
                    popState();
                }
                break;
            }

            case CHOOSE_CARDS_FOR_SALE: {
                Player cur = getCurrentPlayer();
                List<Card> sellable = cur.getCardsForSale();
                Utils.assertTrue(sellable.size()>0);
                if (cur.markCardsForSale(this, sellable)) {
                    popState();
                }
                break;
            }

            case CHOOSE_PURCHASE_PROPERTY: {
                Player cur = getCurrentPlayer();
                int owner = getData(0);
                Player pl = getPlayer(owner);
                Square sq = pl.getSquare();
                if (cur.getMoney() >= sq.getPrice() && owner != currentPlayer) {
                    MoveType move = cur.chooseMove(this, Arrays.asList(MoveType.PURCHASE_UNBOUGHT, MoveType.DONT_PURCHASE, MoveType.MORTGAGE));
                    if (move != null) {
                        processMove(move);
                    }
                } else {
                    nextPlayer(true);
                }
                break;
            }

            case GAME_OVER:
                break;

            default:
                Utils.unhandledCase(state.peek());
        }
    }

    public List<Trade> getTradeOptions(Player p) {
        List<Trade> trades = new ArrayList<>();
        for (Player pp : players) {
            if (pp == p)
                continue;
            for (Trade t : pp.getTrades()) {
                if (p.getMoney() >= t.getPrice()) {
                    trades.add(t);
                }
            }
        }
        return trades;
    }

    private void advance(int squares) {
        final Player cur = getCurrentPlayer();
        int next = (cur.getSquare().ordinal() + NUM_SQUARES + squares) % NUM_SQUARES;
        int nxt = cur.getSquare().ordinal() + squares;
        if (nxt >= NUM_SQUARES) {
            nxt %= NUM_SQUARES;
            getPaid(200);
        }
        onPlayerMove(currentPlayer, squares, Square.values()[next]);
        cur.setSquare(Square.values()[nxt]);
    }
    
    private void processMove(MoveType move) {
        Player cur = getCurrentPlayer();
        Utils.assertFalse(cur.isBankrupt());
        switch (move) {
            case END_TURN: {
                Square sq = cur.getSquare();
                nextPlayer(true);
                pushState(State.SET_PLAYER, currentPlayer);
                if (sq.canPurchase() && getOwner(cur.getSquare()) < 0) {
                    int curNum = getPlayerNum(cur);
                    Utils.assertTrue(curNum >= 0);
                    for (int i=0; i<getNumActivePlayers()-1; i++) {
                        pushState(State.CHOOSE_PURCHASE_PROPERTY, curNum);
                    }
                }
                break;
            }
            case ROLL_DICE:
                rollDice();
                if (cur.isInJail()) {
                    if (die1 == die2) {
                        cur.setInJail(false, rules);
                        onPlayerOutOfJail(currentPlayer);
                    }
                    nextPlayer(true);
                } else {
                    advance(getDice());
                    processSquare();
                }
                break;

            case MORTGAGE:
                pushState(State.CHOOSE_MORTGAGE_PROPERTY);
                break;

            case UNMORTGAGE:
                pushState(State.CHOOSE_UNMORTGAGE_PROPERTY);
                break;

            case UPGRADE:
                pushState(State.CHOOSE_PROPERTY_FOR_UNIT);
                break;

            case TRADE:
                pushState(State.CHOOSE_TRADE);
                break;

            case FORFEIT:
                if (playerBankrupt(currentPlayer))
                    nextPlayer(true);
                break;

            case MARK_CARDS_FOR_SALE:
                pushState(State.CHOOSE_CARDS_FOR_SALE);
                break;

            case PAY_BOND: {
                Utils.assertTrue(cur.isInJail());
                int bond = cur.getJailBond();
                Utils.assertTrue(bond > 0);
                cur.setInJail(false, rules);
                onPlayerOutOfJail(currentPlayer);
                onPlayerGotPaid(currentPlayer, -bond);
                onPlayerPayMoneyToKitty(currentPlayer, bond);
                cur.addMoney(-bond);
                kitty += bond;
                nextPlayer(true);
                break;
            }

            case GET_OUT_OF_JAIL_FREE:
                Utils.assertTrue(cur.isInJail());
                cur.setInJail(false, rules);
                onPlayerOutOfJail(currentPlayer);
                cur.useGetOutOfJailCard();
                nextPlayer(true);
                break;

            case PURCHASE: {
                Square sq = cur.getSquare();
                Utils.assertTrue(sq.canPurchase());
                Utils.assertTrue(getOwner(sq) < 0);
                Utils.assertTrue(cur.getMoney() >= sq.getPrice());
                onPlayerPurchaseProperty(currentPlayer, sq);
                cur.addCard(Card.newPropertyCard(sq));
                cur.addMoney(-sq.getPrice());
                nextPlayer(true); // TODO: Need another state to allow player to make moves after purchase
                break;
            }

            case DONT_PURCHASE: {
                nextPlayer(true);
                break;
            }

            case PURCHASE_UNBOUGHT: {
                int pIndex = getData(0);
                Square sq = getPlayer(pIndex).getSquare();
                Utils.assertTrue(sq.canPurchase());
                Utils.assertTrue(getOwner(sq) < 0);
                Utils.assertTrue(cur.getMoney() >= sq.getPrice());
                onPlayerPurchaseProperty(currentPlayer, sq);
                cur.addCard(Card.newPropertyCard(sq));
                cur.addMoney(-sq.getPrice());
                while (state.size() > 0 && state.peek().state == State.CHOOSE_PURCHASE_PROPERTY) {
                    popState();
                }
                break;
            }

        }
    }

    public Square getPurchasePropertySquare() {
        return getPlayer(getData(0)).getSquare();
    }

    public final void addPlayer(Player player) {
        Utils.assertTrue(players.size() < MAX_PLAYERS);
        this.players.add(player);
    }

    public final List<Piece> getUnusedPieces() {
        List<Piece> pieces = new ArrayList<>(Arrays.asList(Piece.values()));
        for (Player p : players) {
            pieces.remove(p.getPiece());
        }
        return pieces;
    }

    public final Player getCurrentPlayer() {
        return players.get(currentPlayer);
    }

    public final int getCurrentPlayerNum() {
        return currentPlayer;
    }

    private void rollDice() {
        if (dice == null) {
            dice = new int[30];
            for (int i = 0; i < dice.length; i++) {
                dice[i] = 1 + Utils.rand() % 6;
            }
        }
        die1 = Utils.popFirst(dice);
        die2 = Utils.popFirst(dice);
        dice[dice.length-2] = 1+Utils.rand()%6;
        dice[dice.length-1] = 1+Utils.rand()%6;
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

    public final boolean isGameOver() {
        return getWinner() >= 0;
    }

    private void initChance() {
        chance.clear();
        for (CardActionType c : CardActionType.values()) {
            if (c.isChance())
                chance.add(c);
        }
        Utils.shuffle(chance);
    }

    private void initCommunityChest() {
        communityChest.clear();
        for (CardActionType c : CardActionType.values()) {
            if (!c.isChance())
                communityChest.add(c);
        }
        Utils.shuffle(communityChest);
    }

    private void processCommunityChest() {
        CardActionType c = communityChest.removeLast();
        if (communityChest.size() == 0)
            initCommunityChest();
        onPlayerDrawsCommunityChest(currentPlayer, c);
        processAction(c);
    }

    private void processChance() {
        CardActionType c = chance.removeLast();
        if (chance.size() == 0)
            initChance();
        onPlayerDrawsChance(currentPlayer, c);
        processAction(c);
    }

    public int getRent(Square sq) {
        int owner = getOwner(sq);
        if (owner >= 0) {
            return getPlayer(owner).getRent(sq, getDice());
        }
        return 0;
    }

    public final Rules getRules() {
        return rules;
    }

    public State getState() {
        return state.peek().state;
    }

    public int getData(int index) {
        return state.peek().data[index];
    }

    private void advanceToSquare(Square square, int rentScale) {
        Player cur = getCurrentPlayer();

        if (square.canPurchase()) {
            int owner = getOwner(square);
            if (owner < 0 && cur.getValue() >= square.getPrice()) {
                pushState(State.PURCHASE_OR_SKIP);
            } else if (owner >= 0 && owner != currentPlayer) {
                int rent = getPlayer(owner).getRent(square, getDice()) * rentScale;
                if (rent > 0)
                    pushState(State.PAY_RENT, rent, owner);
                else
                    nextPlayer(true);
            } else {
                nextPlayer(true);
            }
        } else {
            nextPlayer(true);
        }
    }

    private int getMovesTo(Square target) {
        Player cur = getCurrentPlayer();
        int moves = 1;
        for ( ; moves<NUM_SQUARES; moves++) {
            Square s = Square.values()[(cur.getSquare().ordinal() + moves) % NUM_SQUARES];
            if (s == target) {
                break;
            }
        }
        return moves;
    }

    private void getPaid(int amount) {
        onPlayerGotPaid(currentPlayer, amount);
        getCurrentPlayer().addMoney(amount);
    }

    private void processAction(CardActionType type) {
        Player cur = getCurrentPlayer();
        switch (type) {
            case CH_GO_BACK: {
                int next = (cur.getSquare().ordinal() + NUM_SQUARES - 3) % NUM_SQUARES;
                onPlayerMove(currentPlayer, -3, Square.values()[next]);
                cur.setSquare(Square.values()[next]);
                processSquare();
                break;
            }
            case CH_LOAN_MATURES:
                getPaid(150);
                nextPlayer(true);
                break;
            case CH_MAKE_REPAIRS: {
                int repairs = cur.getNumHouses() * 25 + cur.getNumHotels() * 150;
                if (repairs > 0)
                    pushState(State.PAY_KITTY, repairs);
                else
                    nextPlayer(true);
                break;
            }
            case CH_GET_OUT_OF_JAIL:
                cur.addCard(Card.newGetOutOfJailFreeCard());
                nextPlayer(true);
                break;
            case CH_ELECTED_CHAIRMAN:
                pushState(State.PAY_PLAYERS, 50);
                break;
            case CH_ADVANCE_RAILROAD:
            case CH_ADVANCE_RAILROAD2: {
                int moves = 1;
                Square s = null;
                for ( ; moves<NUM_SQUARES; moves++) {
                    s = Square.values()[(cur.getSquare().ordinal() + moves) % NUM_SQUARES];
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
                    s = Square.values()[(cur.getSquare().ordinal() + moves) % NUM_SQUARES];
                    if (s.isUtility()) {
                        break;
                    }
                }

                advance(moves);
                advanceToSquare(s, 2);
                break;
            }
            case CH_ADVANCE_READING_RAILROAD: {
                int moves = getMovesTo(Square.READING_RAILROAD);
                advance(moves);
                advanceToSquare(Square.READING_RAILROAD, 1);
                break;
            }
            case CH_BANK_DIVIDEND:
                getPaid(50);
                nextPlayer(true);
                break;
            case CH_GO_TO_JAIL:
                gotoJail();
                break;
            case CH_SPEEDING_TICKET:
                pushState(State.PAY_KITTY, 15);
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
                nextPlayer(true);
                break;
            case CC_SALE_OF_STOCK:
                getPaid(50);
                nextPlayer(true);
                break;
            case CC_BEAUTY_CONTEST:
                getPaid(10);
                nextPlayer(true);
                break;
            case CC_ASSESSED_REPAIRS: {
                int repairs = cur.getNumHouses()*40 + cur.getNumHotels()*115;
                if (repairs > 0)
                    pushState(State.PAY_KITTY, repairs);
                else
                    nextPlayer(true);
                break;
            }
            case CC_HOSPITAL_FEES:
                pushState(State.PAY_KITTY, 100);
                break;
            case CC_CONSULTANCY_FEE:
                getPaid(25);
                nextPlayer(true);
                break;
            case CC_HOLIDAY_FUND_MATURES:
                getPaid(100);
                nextPlayer(true);
                break;
            case CC_LIFE_INSURANCE_MATURES:
                getPaid(100);
                nextPlayer(true);
                break;
            case CC_BIRTHDAY: {
                int numPlayers = getNumActivePlayers();
                for (int i=0; i<numPlayers; i++) {
                    if (i == currentPlayer)
                        continue;
                    Player p = getPlayer(i);
                    if (p.isBankrupt())
                        continue;
                    pushState(State.PAY_BIRTHDAY, 10, i, currentPlayer);
                }
                break;
            }
            case CC_SCHOOL_FEES:
                pushState(State.PAY_KITTY, 50);
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
                nextPlayer(true);
                break;
            case CC_INCOME_TAX_REFUND:
                getPaid(20);
                nextPlayer(true);
                break;
            case CC_DOCTORS_FEES:
                pushState(State.PAY_KITTY, 50);
                break;
            case CC_GET_OUT_OF_JAIL:
                cur.addCard(Card.newGetOutOfJailFreeCard());
                nextPlayer(true);
                break;
            default:
                Utils.unhandledCase(type);
        }
    }

    public final List<Player> getPlayersCopy() {
        return Reflector.deepCopy(players);
    }

    private void gotoJail() {
        onPlayerGoesToJail(currentPlayer);
        if (rules.jailBumpEnabled) {
            for (int i=0; i<getNumPlayers(); i++) {
                Player p = getPlayer(i);
                if (p.isInJail()) {
                    onPlayerOutOfJail(i);
                    p.setInJail(false, rules);
                }
            }
        }
        Player cur = getCurrentPlayer();
        cur.setSquare(Square.VISITING_JAIL);
        cur.setInJail(true, rules);
        nextPlayer(true);
    }

    private void processSquare() {
        Player cur = getCurrentPlayer();
        Square square = cur.getSquare();
        switch (square) {

            case GO:
            case VISITING_JAIL:
                nextPlayer(true);
                break;

            case FREE_PARKING:
                if (kitty > 0) {
                    onPlayerGotPaid(currentPlayer, kitty);
                    getCurrentPlayer().addMoney(kitty);
                    kitty = 0;
                }
                nextPlayer(true);
                break;
            case GOTO_JAIL:
                gotoJail();
                break;

            case INCOME_TAX:
            case LUXURY_TAX:
                pushState(State.PAY_KITTY, Math.round(rules.taxScale * square.getTax()));
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
            case READING_RAILROAD:
            case B_AND_O_RAILROAD:
            case SHORT_LINE_RAILROAD:
            case PENNSYLVANIA_RAILROAD:
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
                        pushState(State.PURCHASE_OR_SKIP);
                        break;
                    } else if (owner >= 0) {
                        Card card = players.get(owner).getCard(square);
                        int rent = getPlayer(owner).getRent(card.property, getDice());
                        if (rent > 0)
                            pushState(State.PAY_RENT, rent, owner);
                        else
                            nextPlayer(true);
                        break;
                    }
                }
                nextPlayer(true);
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
            if (p.isBankrupt())
                continue;
            winner = i;
            num++;
            if (p.getMoney() >= rules.valueToWin) {
                break;
            }
        }
        if (num == 1) {
            onPlayerWins(winner);
            return winner;
        }
        return -1;
    }

    // player bankrupt means all their mortgaged property goes back to bank and they have zero money
    private boolean playerBankrupt(int playerNum) {
        onPlayerBankrupt(playerNum);
        getCurrentPlayer().bankrupt();
        int winner = getWinner();
        if (winner >= 0) {
            state.clear();
            pushState(State.GAME_OVER);
            return false;
        }
        return true;
    }

    private void nextPlayer(boolean pop) {
        if (pop && state.size() > 1)
            popState();

        if (getWinner() < 0) {
            do {
                currentPlayer = (currentPlayer + 1) % players.size();
            } while (getCurrentPlayer().isBankrupt());
            //state.clear();
            //pushState(State.TURN);
        } else {
            state.clear();
            pushState(State.GAME_OVER);
        }
    }

    public final int getOwner(int sq) {
        return getOwner(Square.values()[sq]);
    }

    public final int getOwner(Square square) {
        if (!square.canPurchase())
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
            if (p.isBankrupt())
                continue;
            num++;
        }
        return num;
    }

    public final Player getPlayer(int index) {
        return players.get(index);
    }

    public final int getPlayerNum(Player p) {
        int index = 0;
        for (Player pp : players) {
            if (pp == p)
                return index;
            index++;
        }
        throw new RuntimeException("Player object not apart of players list");
    }

    public final void cancel() {
        if (state.size() > 1) {
            popState();
        }
    }

    /**
     *
     * @return
     */
    public final boolean canCancel() {
        if (state.size() == 0)
            return false;
        switch (state.peek().state) {
            case CHOOSE_CARDS_FOR_SALE:
                return false;
        }
        return state.size() > 1;
    }

    /**
     *
     * @return
     */
    public final int getKitty() {
        return kitty;
    }

    /**
     *
     * @param num
     * @return
     */
    public final String getPlayerName(int num) {
        Player p = getPlayer(num);
        if (p.getPiece() != null)
            return p.getPiece().name();
        return "Player " + num;
    }

    // CALLBACKS CAN BE OVERRIDDEN TO HANDLE EVENTS

    /**
     *
     */
    protected void onDiceRolled() {
        log.info("Dice Rolled: " + getDie1() + "," + getDie2());
    }

    /**
     * numSquares can be negative
     * @param playerNum
     * @param numSquares
     */
    protected void onPlayerMove(int playerNum, int numSquares, Square nextSquare) {
        log.info("%s moved %d squares to %s", getPlayerName(playerNum), numSquares, nextSquare);
    }

    /**
     *
     * @param playerNum
     * @param chance
     */
    protected void onPlayerDrawsChance(int playerNum, CardActionType chance) {
        log.info("%s draws chance card:\n%s", getPlayerName(playerNum), chance.desc);
    }

    /**
     *
     * @param playerNum
     * @param commChest
     */
    protected void onPlayerDrawsCommunityChest(int playerNum, CardActionType commChest) {
        log.info("%s draws community chest card:\n%s", getPlayerName(playerNum), commChest.desc);
    }

    /**
     *
     * @param playerNum
     * @param giverNum
     * @param amt
     */
    protected void onPlayerReceiveMoneyFromAnother(int playerNum, int giverNum, int amt) {
        log.info("%s recievd $%d from %s", getPlayerName(playerNum), amt, getPlayerName(giverNum));
    }

    /**
     *
     * @param playerNum
     * @param amt
     */
    protected void onPlayerGotPaid(int playerNum, int amt) {
        log.info("%s got PAAAID $%d", getPlayerName(playerNum), amt);
    }

    /**
     *
     * @param playerNum
     * @param amt
     */
    protected void onPlayerPayMoneyToKitty(int playerNum, int amt) {
        log.info("%s pays $%d to kitty (%d)", getPlayerName(playerNum), amt, getKitty());
    }

    /**
     *
     * @param playerNum
     */
    protected void onPlayerGoesToJail(int playerNum) {
        log.info("%s goes to JAIL!", getPlayerName(playerNum));
    }

    /**
     *
     * @param playerNum
     */
    protected void onPlayerOutOfJail(int playerNum) {
        log.info("%s got out of JAIL!", getPlayerName(playerNum));
    }

    /**
     *
     * @param playerNum
     * @param renterNum
     * @param amt
     */
    protected void onPlayerPaysRent(int playerNum, int renterNum, int amt) {
        log.info("%s pays $%d rent too %s", getPlayerName(playerNum), amt, getPlayerName(renterNum));
    }

    /**
     *
     * @param playerNum
     * @param property
     * @param amt
     */
    protected void onPlayerMortgaged(int playerNum, Square property, int amt) {
        log.info("%s mortgaged property %s for $%d", getPlayerName(playerNum), property.name(), amt);
    }

    /**
     *
     * @param playerNum
     * @param property
     * @param amt
     */
    protected void onPlayerUnMortgaged(int playerNum, Square property, int amt) {
        log.info("%s unmortaged %s for $%d", getPlayerName(playerNum), property.name(), amt);
    }

    /**
     *
     * @param playerNum
     * @param property
     */
    protected void onPlayerPurchaseProperty(int playerNum, Square property) {
        log.info("%s purchased %s for $%d", getPlayerName(playerNum), property.name(), property.getPrice());
    }

    /**
     *
     * @param playerNum
     * @param property
     * @param amt
     */
    protected void onPlayerBoughtHouse(int playerNum, Square property, int amt) {
        log.info("%s bought a HOUSE for property %s for $%d", getPlayerName(playerNum), property.name(), amt);
    }

    /**
     *
     * @param playerNum
     * @param property
     * @param amt
     */
    protected void onPlayerBoughtHotel(int playerNum, Square property, int amt) {
        log.info("%s bought a HOTEL for property %s for $%d", getPlayerName(playerNum), property.name(), amt);
    }

    /**
     *
     * @param playerNum
     */
    protected void onPlayerBankrupt(int playerNum) {
        log.info("%s IS BANKRUPT!", getPlayerName(playerNum));
    }

    /**
     *
     * @param playerNum
     */
    protected void onPlayerWins(int playerNum) {
        log.info("%s IS THE WINNER!", getPlayerName(playerNum));
    }

    /**
     *
     * @param buyer
     * @param seller
     * @param property
     * @param amount
     */
    protected void onPlayerTrades(int buyer, int seller, Square property, int amount) {
        log.info("%s buys %s from %s for $%d", getPlayerName(buyer), property.name(), getPlayerName(seller), amount);
    }
}
