package cc.lib.monopoly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Player extends Reflector<Player> {

    static {
        addAllFields(Player.class);
    }

    int money;
    int square;
    final List<Card> cards = new ArrayList<>();
    boolean inJail;
    int debt;
    Piece piece;

    public MoveType chooseMove(Monopoly game, List<MoveType> options) {
        if (options.size() == 1)
            return options.get(0);

        int [] weights = new int[options.size()];


        int index = 0;
        Map<GColor, List<Card>> sets = getPropertySets();
        for (MoveType mt : options) {
            switch (mt) {
                case SKIP:
                case ROLL_DICE:
                    weights[index] = 2;
                    break;
                case PURCHASE: {
                    Square sq = getSquare();
                    switch (sq.getType()) {
                        case PROPERTY:
                            if (sets.containsKey(sq.getColor())) {
                                return mt; // always want to purchase from a set we already own
                            }
                            weights[index] = Math.max(1, 4-sets.size()); // reduce likelyhood of buying as our sets go up
                            break;
                        case UTILITY:
                            if (getNumUtilities() > 0)
                                return mt;
                            weights[index] = 1;
                            break;
                        case RAIL_ROAD:
                            weights[index] = 2 + getNumRailroads()*2;
                            break;
                    }
                    break;
                }
                case PAY_BOND:
                    weights[index] = 2;
                    break;
                case MORTGAGE: {
                    /*
                    Square sq = getSquare();
                    if (sq.canPurchase() && getMoney() < sq.getPrice()) {
                        int owner = game.getOwner(sq);
                        if (owner < 0 && owner != game.getCurrentPlayerNum() && getValue() >= getSquare().getPrice()) {
                            weights[index] = Math.max(1, 2 - sets.size());
                        } else if (getMoney() < 200) {
                            weights[index] = 1;
                        }
                    }*/
                    break;
                }
                case UNMORTGAGE:
                    weights[index] = getCardsForMortgage().size();
                    break;
                case PAY_RENT:
                case PAY_KITTY:
                case PAY_PLAYERS:
                case GET_OUT_OF_JAIL_FREE:
                    return mt;

                case BUY_UNIT:
                    weights[index] = 1+getMoney()/100;
                    break;

                case FORFEIT:
                case TRADE:
                    break;
            }
            index++;
        }

        return Utils.chooseRandomWeightedItem(options, weights);

    }

    public enum CardChoiceType {
        CHOOSE_CARD_TO_MORTGAGE,
        CHOOSE_CARD_TO_UNMORTGAGE,
        CHOOSE_CARD_FOR_NEW_UNIT
    }

    public Card chooseCard(Monopoly game, List<Card> cards, CardChoiceType choiceType) {
        Utils.assertTrue(cards.size() > 0);
        if (cards.size() == 1)
            return cards.get(0);
        int bestD = 0;
        Card best = null;
        switch (choiceType) {
            case CHOOSE_CARD_TO_UNMORTGAGE: {
                // choose card that will reduce money the least while increasing rent the most
                for (Card c : cards) {
                    int dMoney = c.getProperty().getMortgageBuybackPrice();
                    int dRent = getRent(c.getProperty(), 7);
                    int delta = dMoney - dRent;
                    if (best == null || delta > bestD) {
                        best = c;
                        bestD = delta;
                    }
                }
                break;
            }
            case CHOOSE_CARD_FOR_NEW_UNIT: {
                // choose card that will reduce money the least while increasing rent the most
                for (Card c : cards) {
                    int dCost = c.getProperty().getHousePrice();
                    int dRent = getRent(c.getProperty(), 7);
                    int delta = dRent-dCost;
                    if (best == null || delta > bestD) {
                        best = c;
                        bestD = delta;
                    }
                }
                break;
            }
            case CHOOSE_CARD_TO_MORTGAGE: {
                // choose card that will increase money the most while reducing loss to rent the most
                for (Card c : cards) {
                    int dMoney = c.getProperty().getMortgageValue();
                    int dRent = getRent(c.getProperty(), 7);
                    int delta = dMoney - dRent;
                    if (best == null || (money + dMoney > delta && delta > bestD)) {
                        best = c;
                        bestD = delta;
                    }
                }
                break;
            }
        }
        return Utils.randItem(cards);
    }

    public Trade chooseTrade(Monopoly game, List<Trade> trades) {
        return trades.get(0);
    }

    public boolean markCardsForSale(Monopoly game, List<Card> sellable) {
        return true;
    }

    public final Piece getPiece() {
        return piece;
    }

    public final void setPiece(Piece piece) {
        this.piece = piece;
    }

    public final Square getSquare() {
        return Square.values()[square];
    }

    public final int getMoney() {
        return money;
    }

    public final List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    protected void addCard(Card card) {
        cards.add(card);
    }

    protected void removeCard(Card card) {
        cards.remove(card);
    }

    public final boolean isInJail() {
        return inJail;
    }

    public final Map<GColor, List<Card>> getPropertySets() {
        Map<GColor, List<Card>> sets = new HashMap<>();
        for (Card c : cards) {
            if (c.property == null || !c.property.isProperty())
                continue;
            List<Card> list = sets.get(c.property.getColor());
            if (list == null) {
                list = new ArrayList<>();
                sets.put(c.property.getColor(), list);
            }
            list.add(c);
        }
        return sets;
    }

    public final int getNumCompletePropertySets() {
        int num = 0;
        Map<GColor, List<Card>> sets = getPropertySets();
        for (List<Card> l : sets.values()) {
            Card c = l.get(0);
            Utils.assertTrue(c.getProperty().getNumForSet() >= l.size());
            if (l.size() == c.getProperty().getNumForSet())
                num++;
        }
        return num;
    }

    public final boolean ownsProperty(Square square) {
        for (Card card : cards) {
            if (card.property == square)
                return true;
        }
        return false;
    }

    public final int getValue() {
        int value = money;
        for (Card c : cards) {
            if (c.property != null && !c.mortgaged) {
                value += c.property.getMortgageValue();
            }
        }
        return value;
    }

    public final Card getCard(Square square) {
        for (Card c : cards) {
            if (c.property == square) {
                return c;
            }
        }
        return null;
    }

    public final int getNumRailroads() {
        int num = 0;
        for (Card c : cards) {
            if (c.property != null && c.property.isRailroad())
                num++;
        }
        return num;
    }

    public final int getNumGetOutOfJailFreeCards() {
        int num = 0;
        for (Card c : this.cards) {
            if (c.isGetOutOfJail())
                num++;
        }
        return num;
    }

    public final int getNumUtilities() {
        int num = 0;
        for (Card c : cards) {
            if (c.property != null && c.property.isUtility())
                num++;
        }
        return num;
    }

    public final int getRent(Square property, int dice) {
        Card card = getCard(property);
        if (card.mortgaged)
            return 0;
        if (property.isRailroad()) {
            switch (getNumRailroads()) {
                case 1:
                    return 25;
                case 2:
                    return 50;
                case 3:
                    return 100;
                case 4:
                    return 200;
                default:
                    Utils.assertTrue(false, "Unvalid value for number of railroads %d", getNumRailroads());
                    return 0;
            }
        } else if (property.isUtility()) {
            switch (getNumUtilities()) {
                case 1:
                    return 4 * dice;
                case 2:
                    return 10 * dice;
                default:
                    Utils.assertTrue(false, "Invalid value for num utilities %d", getNumUtilities());
                    return 0;
            }
        } else if (card.houses == 0) {
            if (hasSet(property))
                return property.getRent(0) * 2;
            return property.getRent(0);
        }
        return property.getRent(card.houses);
    }

    public final int getNumOfSet(Square property) {
        int count = 0;
        for (Card c : cards) {
            if (c.property != null && c.property.getColor().equals(property.getColor())) {
                count++;
            }
        }
        return count;
    }

    public final boolean hasSet(Square property) {
        int count = getNumOfSet(property);
        int min = property.getNumForSet();
        return count == min;
    }

    final void clear() {
        money = 0;
        debt = 0;
        inJail = false;
        cards.clear();
        square = -1;
    }

    final void useGetOutOfJailCard() {
        Iterator<Card> it = cards.iterator();
        while (it.hasNext()) {
            if (it.next().getOutOfJail) {
                it.remove();
                return;
            }
        }
        Utils.assertTrue(false, "Cannot find get out of jail card in cards: %s", cards);
    }

    public final boolean isBankrupt() {
        return square < 0;
    }

    public final List<Card> getCardsForUnMortgage() {
        List<Card> cards = new ArrayList<>();
        for (Card c : this.cards) {
            if (c.property != null && c.mortgaged && c.property.getMortgageBuybackPrice() <= money) {
                cards.add(c);
            }
        }
        return cards;
    }

    public final List<Card> getCardsForMortgage() {
        List<Card> cards = new ArrayList<>();
        for (Card c : this.cards) {
            if (c.property != null && !c.mortgaged) {
                cards.add(c);
            }
        }
        return cards;
    }

    public final List<Card> getCardsOfType(SquareType type) {
        List<Card> cards = new ArrayList<>();
        for (Card c : this.cards) {
            if (c.getProperty() == null)
                continue;
            if (c.getProperty().getType() == type)
                cards.add(c);
        }
        return cards;
    }

    public final List<Card> getCardsForNewHouse() {
        List<Card> cards = new ArrayList<>();
        for (Card c : this.cards) {
            if (c.property != null && c.property.isProperty() && c.property.getHousePrice() <= money && hasSet(c.property) && c.houses < Monopoly.MAX_HOUSES)
                cards.add(c);
        }
        return cards;
    }

    public final int getNumHouses() {
        int num = 0;
        for (Card c : cards) {
            if (c.houses < 5)
                num += c.houses;
        }
        return num;
    }

    public final int getNumHotels() {
        int num = 0;
        for (Card c : cards) {
            if (c.houses == 5)
                num += 1;
        }
        return num;
    }

    public final boolean hasGetOutOfJailFreeCard() {
        for (Card c : cards) {
            if (c.getOutOfJail)
                return true;
        }
        return false;
    }

    public int getTotalUnits() {
        int num = 0;
        for (Card c : cards) {
            num += c.houses;
        }
        return num;
    }

    public int getNumMortgaged() {
        int num = 0;
        for (Card c : cards) {
            if (c.mortgaged)
                num++;
        }
        return num;
    }

    public int getNumUnmortgaged() {
        int num = 0;
        for (Card c : cards) {
            if (c.getProperty() != null && !c.mortgaged)
                num++;
        }
        return num;
    }

    public final List<Card> getCardsForSale() {
        List<Card> sellable = new ArrayList<>();
        for (Card c : cards) {
            if (c.isSellable())
                sellable.add(c);
        }
        return sellable;
    }

    public List<Trade> getTrades() {
        List<Trade> trades = new ArrayList<>();
        for (Card card : cards) {
            if (card.isSellable()) {
                if (hasSet(card.property))
                    continue;
                int num = getNumOfSet(card.property);
                int price = card.property.getPrice() * (1+num) + (2*card.getHouses()*card.property.getHousePrice());
                trades.add(new Trade(card, price, this));
            }
        }
        return trades;
    }

    public String toString() {
        return "\n" + piece.name()
                + "\n  bankrupt=" + isBankrupt()
                + "\n  injail=" + isInJail()
                + "\n  money=" + money
                + "\n  cards=" + cards.size()
                + "\n  mortgaged=" + getNumMortgaged()
                + "\n  unmortgaged=" + getNumUnmortgaged()
                + "\n  value=" + getValue()
                + "\n  sets =" + getPropertySets().size()
                + "\n  complete sets=" + getNumCompletePropertySets()
                + "\n  units=" + getTotalUnits()
                + "\n  rroads=" + getNumRailroads()
                + "\n  utils =" + getNumUtilities()
                ;
    }
}
