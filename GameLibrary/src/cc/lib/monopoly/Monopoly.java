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
    }

    public final static int NUM_SQUARES = Square.values().length;
    public final static int MAX_PLAYERS = 4;
    public final static int MAX_HOUSES  = 5;

    private final List<Player> players = new ArrayList<>();
    private int currentPlayer;
    private final Stack<State> state = new Stack<>();
    private int die1, die2;
    private int kitty;
    private int birthdayPlayer;
    private final LinkedList<CardActionType> chance = new LinkedList<>();
    private final LinkedList<CardActionType> communityChest = new LinkedList<>();
    private int [] dice;
    private final Rules rules = new Rules();

    public enum State {
        INIT,
        TURN,
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
        CHOOSE_SELLABLE
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

    public void runGame() {
        if (state.isEmpty())
            state.push(State.INIT);
        switch (state.peek()) {
            case INIT: {
                if (players.size() < 2)
                    throw new RuntimeException("Not enough players");
                for (int i = 0; i<getNumPlayers(); i++) {
                    Player p = getPlayer(i);
                    p.square = 0;
                    p.cards.clear();
                    onPlayerGotPaid(i, rules.startMoney);
                    p.money = rules.startMoney; // TODO: Config
                    if (p.piece == null) {
                        p.piece = Utils.randItem(getUnusedPieces());
                    }
                }
                currentPlayer = Utils.rand() % players.size();
                kitty = 0;
                initChance();
                initCommunityChest();
                state.push(State.TURN);
                break;
            }
            case TURN: {
                Player cur = getCurrentPlayer();
                Utils.assertTrue(cur.getValue() > 0);
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
                if (getTradeOptions(cur).size() > 0)
                    moves.add(MoveType.TRADE);
                if (cur instanceof PlayerUser) {
                    for (Card c : cur.getCards()) {
                        if (c.isSellable()) {
                            moves.add(MoveType.MARK_SELLABLE);
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
                if (cur.getSquare().canPurchase() && cur.money >= cur.getSquare().getPrice()) {
                    moves.add(MoveType.PURCHASE);
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                moves.add(MoveType.SKIP);
                MoveType move = cur.chooseMove(this, moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }

            case PAY_RENT: {
                Player cur = getCurrentPlayer();
                List<MoveType> moves = new ArrayList<>();
                Utils.assertTrue(cur.debt > 0);
                if (cur.money >= cur.debt) {
                    moves.add(MoveType.PAY_RENT);
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                MoveType move = cur.chooseMove(this, moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }

            case PAY_KITTY: {
                Player cur = getCurrentPlayer();
                List<MoveType> moves = new ArrayList<>();
                Utils.assertTrue(cur.debt > 0);
                if (cur.money >= cur.debt) {
                    moves.add(MoveType.PAY_KITTY);
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                MoveType move = cur.chooseMove(this, moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }

            case PAY_PLAYERS: {
                Player cur = getCurrentPlayer();
                List<MoveType> moves = new ArrayList<>();
                Utils.assertTrue(cur.debt > 0);
                if (cur.money >= cur.debt) {
                    moves.add(MoveType.PAY_PLAYERS);
                }
                if (cur.getCardsForMortgage().size() > 0) {
                    moves.add(MoveType.MORTGAGE);
                }
                MoveType move = cur.chooseMove(this, moves);
                if (move != null) {
                    processMove(move);
                }
                break;
            }

            case PAY_BIRTHDAY: {
                for (int i=0; i<players.size(); i++) {
                    if (i == birthdayPlayer)
                        continue;
                    Player p = getPlayer(i);
                    if (p.debt > 0) {
                        currentPlayer = i;
                        if (p.money >= p.debt) {
                            state.pop();
                            payMoneyOrMortgage(p.debt, State.PAY_BIRTHDAY);
                            if (p.getValue() == 0) {
                                if (!playerBankrupt())
                                    return;
                            }
                        } else if (p.getValue() > p.debt){
                            state.push(State.CHOOSE_MORTGAGE_PROPERTY);
                        } else {
                            onPlayerReceiveMoneyFromAnother(birthdayPlayer, i, p.getValue());
                            getPlayer(birthdayPlayer).money += p.getValue();
                            playerBankrupt();
                            state.pop();
                        }
                        break;
                    }
                }
                if (state.size() == 1) {
                    currentPlayer = birthdayPlayer;
                    birthdayPlayer = -1;
                    nextPlayer();
                }

                break;
            }

            case CHOOSE_MORTGAGE_PROPERTY: {
                Player cur = getCurrentPlayer();
                List<Card> cards = cur.getCardsForMortgage();
                Card card = cur.chooseCard(this, cards, Player.CardChoiceType.CHOOSE_CARD_TO_MORTGAGE);
                if (card != null) {
                    int mortgageAmt = card.property.getMortgageValue();
                    onPlayerMortgaged(currentPlayer, card.property, mortgageAmt);
                    cur.money += mortgageAmt;
                    card.mortgaged = true;
                    state.pop();
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
                    cur.money -= buyBackAmt;
                    card.mortgaged = false;
                    state.pop();
                }
                break;
            }

            case CHOOSE_PROPERTY_FOR_UNIT: {
                Player cur = getCurrentPlayer();
                List<Card> cards = cur.getCardsForNewHouse();
                Card card = cur.chooseCard(this, cards, Player.CardChoiceType.CHOOSE_CARD_FOR_NEW_UNIT);
                if (card != null) {
                    int houseCost = card.property.getHousePrice();
                    if (card.houses < 4) {
                        onPlayerBoughtHouse(currentPlayer, card.property, houseCost);
                    } else {
                        onPlayerBoughtHotel(currentPlayer, card.property, houseCost);
                    }
                    cur.money -= houseCost;
                    Utils.assertTrue(cur.money >= 0);
                    card.houses++;
                    state.pop();
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
                    cur.money -= trade.getPrice();
                    Utils.assertTrue(cur.money >= 0);
                    trade.getTrader().money += trade.getPrice();
                    trade.getTrader().removeCard(trade.getCard());
                    cur.addCard(trade.getCard());
                    state.pop();
                }
                break;
            }

            case CHOOSE_SELLABLE: {
                Player cur = getCurrentPlayer();
                List<Card> sellable = cur.getSellableCards();
                Utils.assertTrue(sellable.size()>0);
                if (cur.markSellable(this, sellable)) {
                    state.pop();
                }
            }

            case GAME_OVER:
                break;

            default:
                Utils.unhandledCase(state.peek());
        }
    }

    private List<Trade> getTradeOptions(Player p) {
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
        int next = (getCurrentPlayer().square + NUM_SQUARES + squares) % NUM_SQUARES;
        onPlayerMove(currentPlayer, squares, Square.values()[next]);
        Player cur = getCurrentPlayer();
        cur.square += squares;
        if (cur.square >= NUM_SQUARES) {
            cur.square %= NUM_SQUARES;
            getPaid(200);
        }
    }

    private void processMove(MoveType move) {
        Player cur = getCurrentPlayer();
        Utils.assertFalse(cur.isBankrupt());
        switch (move) {
            case SKIP:
                nextPlayer();
                break;
            case ROLL_DICE:
                rollDice();
                if (cur.inJail) {
                    if (die1 == die2) {
                        cur.inJail = false;
                        onPlayerOutOfJail(currentPlayer);
                    }
                    nextPlayer();
                } else {
                    advance(getDice());
                    processSquare();
                }
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

            case TRADE:
                state.push(State.CHOOSE_TRADE);
                break;

            case FORFEIT:
                if (playerBankrupt())
                    nextPlayer();
                break;

            case MARK_SELLABLE:
                state.push(State.CHOOSE_SELLABLE);
                break;

            case PAY_BOND:
                Utils.assertTrue(cur.inJail);
                cur.inJail = false;
                onPlayerOutOfJail(currentPlayer);
                payToKitty(50);
                nextPlayer();
                break;

            case GET_OUT_OF_JAIL_FREE:
                Utils.assertTrue(cur.inJail);
                cur.inJail = false;
                onPlayerOutOfJail(currentPlayer);
                cur.useGetOutOfJailCard();
                nextPlayer();
                break;

            case PAY_RENT: {
                payRent();
                nextPlayer();
                break;
            }

            case PAY_KITTY: {
                payToKitty(cur.debt);
                cur.debt = 0;
                nextPlayer();
                break;
            }

            case PAY_PLAYERS: {
                int amt = cur.debt / getNumActivePlayers();
                for (int i=0; i<players.size(); i++) {
                    if (i == currentPlayer)
                        continue;
                    Player p = getPlayer(i);
                    if (p.isBankrupt())
                        continue;
                    onPlayerReceiveMoneyFromAnother(i, currentPlayer, amt);
                    p.money += amt;
                }
                cur.debt = 0;
                nextPlayer();
                break;
            }

            case PURCHASE: {
                Square sq = Square.values()[cur.square];
                Utils.assertTrue(sq.canPurchase());
                Utils.assertTrue(getOwner(sq) < 0);
                Utils.assertTrue(cur.money >= sq.getPrice());
                onPlayerPurchaseProperty(currentPlayer, sq);
                cur.cards.add(Card.newPropertyCard(sq));
                cur.money -= sq.getPrice();
                nextPlayer();
                break;
            }

        }
    }

    public final void addPlayer(Player player) {
        Utils.assertTrue(players.size() < MAX_PLAYERS);
        this.players.add(player);
    }

    public final List<Piece> getUnusedPieces() {
        List<Piece> pieces = new ArrayList<>(Arrays.asList(Piece.values()));
        for (Player p : players) {
            pieces.remove(p.piece);
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
        die1 = Utils.shiftLeft(dice);
        die2 = Utils.shiftLeft(dice);
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

    private int getRent(int square) {
        Square sq = Square.values()[square];
        int owner = getOwner(sq);
        if (owner >= 0) {
            return getPlayer(owner).getRent(sq, getDice());
        }
        return 0;
    }

    public final Rules getRules() {
        return rules;
    }

    private void advanceToSquare(Square square, int rentScale) {
        Player cur = getCurrentPlayer();

        if (square.canPurchase()) {
            int owner = getOwner(square);
            if (owner < 0 && cur.getValue() >= square.getPrice()) {
                state.push(State.PURCHASE_OR_SKIP);
            } else if (owner >= 0 && owner != currentPlayer) {
                int rent = getPlayer(owner).getRent(square, getDice()) * rentScale;
                if (rent > 0)
                    payMoneyOrMortgage(rent, State.PAY_RENT);
                else
                    nextPlayer();
            } else {
                nextPlayer();
            }
        } else {
            nextPlayer();
        }
    }

    private int getMovesTo(Square target) {
        Player cur = getCurrentPlayer();
        int moves = 1;
        for ( ; moves<NUM_SQUARES; moves++) {
            Square s = Square.values()[(cur.square + moves) % NUM_SQUARES];
            if (s == target) {
                break;
            }
        }
        return moves;
    }

    private void payMoneyOrMortgage(int amount, State payState) {
        Player cur = getCurrentPlayer();
        Utils.assertTrue(amount >= 0);
        if (amount == 0) {
            nextPlayer();
            return;
        }
        if (cur.money >= amount) {
            switch (payState) {
                case PAY_KITTY:
                    payToKitty(amount);
                    break;
                case PAY_RENT:
                    cur.debt = amount;
                    payRent();
                    break;
                case PAY_PLAYERS:
                    int amt = amount / (getNumActivePlayers()-1);
                    for (int i=0; i<players.size(); i++) {
                        if (i == currentPlayer)
                            continue;
                        Player p = getPlayer(i);
                        if (p.isBankrupt())
                            continue;
                        onPlayerReceiveMoneyFromAnother(i, currentPlayer, amt);
                        p.money += amt;
                    }
                    cur.money -= amount;
                    if (cur.getValue()==0) {
                        if (!playerBankrupt())
                            return;
                    }
                    break;
                case PAY_BIRTHDAY:
                    Utils.assertTrue(cur.debt > 0);
                    onPlayerReceiveMoneyFromAnother(birthdayPlayer, currentPlayer, cur.debt);
                    getPlayer(birthdayPlayer).money += cur.debt;
                    cur.money -= cur.debt;
                    cur.debt = 0;
                    return;
                default:
                    Utils.unhandledCase(payState);
            }
            nextPlayer();
        } else if (cur.getValue() > amount) {
            cur.debt = amount;
            state.push(State.CHOOSE_MORTGAGE_PROPERTY);
        } else {
            // player is bankrupt, mortgage everything and pay remaining out
            amount = cur.getValue();
            cur.money = amount;
            cur.debt = amount;
            switch (payState) {
                case PAY_KITTY:
                    payToKitty(amount);
                    break;
                case PAY_RENT:
                    cur.debt = amount;
                    payRent();
                    break;
                case PAY_PLAYERS:
                    int amt = amount / (getNumActivePlayers() - 1);
                    for (int i = 0; i < players.size(); i++) {
                        if (i == currentPlayer)
                            continue;
                        Player p = getPlayer(i);
                        if (p.isBankrupt())
                            continue;
                        onPlayerReceiveMoneyFromAnother(i, currentPlayer, amt);
                        p.money += amt;
                    }
                    break;
                case PAY_BIRTHDAY:
                    Utils.assertTrue(cur.debt > 0);
                    onPlayerReceiveMoneyFromAnother(birthdayPlayer, currentPlayer, cur.debt);
                    getPlayer(birthdayPlayer).money += cur.debt;
                    playerBankrupt();
                    return;
                default:
                    Utils.unhandledCase(payState);
            }
            if (playerBankrupt())
                nextPlayer();
        }
    }

    private void getPaid(int amount) {
        onPlayerGotPaid(currentPlayer, amount);
        getCurrentPlayer().money += amount;
    }

    private void processAction(CardActionType type) {
        Player cur = getCurrentPlayer();
        switch (type) {
            case CH_GO_BACK: {
                int next = (cur.square + NUM_SQUARES - 3) % NUM_SQUARES;
                onPlayerMove(currentPlayer, -3, Square.values()[next]);
                cur.square = next;
                processSquare();
                break;
            }
            case CH_LOAN_MATURES:
                getPaid(150);
                nextPlayer();
                break;
            case CH_MAKE_REPAIRS: {
                int repairs = cur.getNumHouses() * 25 + cur.getNumHotels() * 150;
                if (repairs > 0)
                    payMoneyOrMortgage(repairs, State.PAY_KITTY);
                else
                    nextPlayer();
                break;
            }
            case CH_GET_OUT_OF_JAIL:
                cur.cards.add(Card.newGetOutOfJailFreeCard());
                nextPlayer();
                break;
            case CH_ELECTED_CHAIRMAN:
                payMoneyOrMortgage(50*(getNumActivePlayers()-1), State.PAY_PLAYERS);
                break;
            case CH_ADVANCE_RAILROAD:
            case CH_ADVANCE_RAILROAD2: {
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
            case CH_ADVANCE_READING_RAILROAD: {
                int moves = getMovesTo(Square.READING_RAILROAD);
                advance(moves);
                advanceToSquare(Square.READING_RAILROAD, 1);
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
                payMoneyOrMortgage(15, State.PAY_KITTY);
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
                if (repairs > 0)
                    payMoneyOrMortgage(repairs, State.PAY_KITTY);
                else
                    nextPlayer();
                break;
            }
            case CC_HOSPITAL_FEES:
                payMoneyOrMortgage(100, State.PAY_KITTY);
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
            case CC_BIRTHDAY: {
                int numPlayers = getNumActivePlayers();
                birthdayPlayer = currentPlayer;
                for (int i=0; i<numPlayers; i++) {
                    if (i == birthdayPlayer)
                        continue;
                    Player p = getPlayer(i);
                    if (p.isBankrupt())
                        continue;
                    p.debt = 10;
                    state.push(State.PAY_BIRTHDAY);
                }
                break;
            }
            case CC_SCHOOL_FEES:
                payMoneyOrMortgage(50, State.PAY_KITTY);
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
                payMoneyOrMortgage(50, State.PAY_KITTY);
                break;
            case CC_GET_OUT_OF_JAIL:
                cur.cards.add(Card.newGetOutOfJailFreeCard());
                nextPlayer();
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
                if (p.inJail) {
                    onPlayerOutOfJail(i);
                    p.inJail = false;
                }
            }
        }
        Player cur = getCurrentPlayer();
        cur.square = Square.VISITING_JAIL.ordinal();
        cur.inJail = true;
        nextPlayer();
    }

    private void processSquare() {
        Player cur = getCurrentPlayer();
        Square square = Square.values()[cur.square];
        switch (square) {

            case GO:
            case VISITING_JAIL:
                nextPlayer();
                break;

            case FREE_PARKING:
                if (kitty > 0) {
                    onPlayerGotPaid(currentPlayer, kitty);
                    getCurrentPlayer().money += kitty;
                    kitty = 0;
                }
                nextPlayer();
                break;
            case GOTO_JAIL:
                gotoJail();
                break;

            case INCOME_TAX:
            case LUXURY_TAX:
                payMoneyOrMortgage(Math.round(rules.taxScale * square.getTax()), State.PAY_KITTY);
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
                        state.push(State.PURCHASE_OR_SKIP);
                        break;
                    } else if (owner >= 0) {
                        Card card = players.get(owner).getCard(square);
                        int rent = getPlayer(owner).getRent(card.property, getDice());
                        if (rent > 0)
                            payMoneyOrMortgage(rent, State.PAY_RENT);
                        else
                            nextPlayer();
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
            if (p.isBankrupt())
                continue;
            if (p.getValue() >= rules.valueToWin)
                return i;
            winner = i;
            num++;
        }
        if (num == 1) {
            onPlayerWins(winner);
            return winner;
        }
        return -1;
    }

    // player bankrupt means all their mortgaged property goes back to bank and they have zero money
    private boolean playerBankrupt() {
        onPlayerBankrupt(currentPlayer);
        getCurrentPlayer().clear();
        int winner = getWinner();
        if (winner >= 0) {
            state.clear();
            state.push(State.GAME_OVER);
            return false;
        }
        return true;
    }

    private void nextPlayer() {
        if (getWinner() < 0) {
            do {
                currentPlayer = (currentPlayer + 1) % players.size();
            } while (getCurrentPlayer().isBankrupt());
            state.clear();
            state.push(State.TURN);
        } else {
            state.clear();
            state.push(State.GAME_OVER);
        }
    }

    private void payRent() {
        Player cur = getCurrentPlayer();
        Utils.assertTrue(cur.debt > 0);
        int owner = getOwner(cur.square);
        Utils.assertFalse(owner == currentPlayer);
        onPlayerPaysRent(currentPlayer, owner, cur.debt);
        cur.money -= cur.debt;
        Utils.assertTrue(cur.money >= 0);
        getPlayer(owner).money += cur.debt;
        cur.debt = 0;
        if (cur.getValue() == 0)
            playerBankrupt();
    }

    private void payToKitty(int amt) {
        Utils.assertTrue(amt > 0);
        Player cur = getCurrentPlayer();
        Utils.assertTrue(cur.money >= 0);
        onPlayerPayMoneyToKitty(currentPlayer, amt);
        cur.money -= amt;
        kitty += amt;
        if (cur.getValue() == 0)
            playerBankrupt();
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
            state.pop();
        }
    }

    /**
     *
     * @return
     */
    public final boolean canCancel() {
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
        log.info("%s pays $%d to kitty", getPlayerName(playerNum), amt);
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
