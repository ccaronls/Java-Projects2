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

    public int getHouses() {
        return houses;
    }

    public boolean isMortgaged() {
        return mortgaged;
    }

    public boolean isGetOutOfJail() {
        return getOutOfJail;
    }

    public Square getProperty() {
        return property;
    }

    boolean canMortgage() {
        return property != null && !mortgaged;
    }

    boolean canUnMortgage() {
        return property != null && mortgaged;
    }

    @Override
    public String toString() {
        if (getOutOfJail)
            return "Get Out of Jail Free";
        String s = property.name();
        if (houses > 0 && houses < 5) {
            s += "\nhouses X " + houses;
        } else if (houses == 5) {
            s += "\nHOTEL";
        }
        if (mortgaged) {
            s += "\nMORTGAGED\nBuy Back $" + property.getMortgageBuybackPrice();
        } else {
            s += "\nMorgage\nValue $" + property.getMortgageValue();
        }
        return s;
    }
}
