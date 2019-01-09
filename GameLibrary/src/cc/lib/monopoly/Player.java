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

    public MoveType chooseMove(List<MoveType> options) {
        return Utils.randItem(options);
    }

    public Card chooseCard(List<Card> cards) {
        return Utils.randItem(cards);
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public Square getSquare() {
        return Square.values()[square];
    }

    public int getMoney() {
        return money;
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public boolean isInJail() {
        return inJail;
    }

    public Map<GColor, List<Card>> getPropertySets() {
        Map<GColor, List<Card>> sets = new HashMap<>();
        for (Card c : cards) {
            if (c.property == null || !c.property.isProperty())
                continue;
            List<Card> list = sets.get(c.property.color);
            if (list == null) {
                list = new ArrayList<>();
                sets.put(c.property.color, list);
            }
            list.add(c);
        }
        return sets;
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

    public final boolean hasSet(Square property) {
        int count = 0;
        int min = property.getNumForSet();
        for (Card c : cards) {
            if (c.property != null && c.property.color.equals(property.color)) {
                count++;
            }
        }
        return count == min;
    }

    final void eliminated() {
        money = 0;
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

    public final boolean isEliminated() {
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
            if (c.getProperty().type == type)
                cards.add(c);
        }
        return cards;
    }

    public final List<Card> getCardsForNewHouse() {
        List<Card> cards = new ArrayList<>();
        for (Card c : this.cards) {
            if (c.property != null && c.property.isProperty() && c.property.getHousePrice() <= money && hasSet(c.property))
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
}
