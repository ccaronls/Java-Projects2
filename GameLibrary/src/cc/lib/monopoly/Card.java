package cc.lib.monopoly;

import cc.lib.utils.Reflector;

public class Card extends Reflector<Card> {

    static {
        addAllFields(Card.class);
    }

    Square property;
    boolean getOutOfJail;
    int houses;
    boolean mortgaged;

    static Card newGetOutOfJailFreeCard() {
        Card c = new Card();
        c.getOutOfJail = true;
        return c;
    }

    static Card newPropertyCard(Square property) {
        Card c = new Card();
        c.property = property;
        return c;
    }

    boolean canMortgage() {
        return property != null && !mortgaged;
    }

    boolean canUnMortgage() {
        return property != null && mortgaged;
    }
}
