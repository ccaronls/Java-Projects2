package cc.game.kaiser.core;

import cc.lib.utils.Reflector;

public final class Card extends Reflector<Card> {
    static {
        addAllFields(Card.class);
    }
    
    public final Rank rank;
    public final Suit suit;

    public Card() {
        this(null, null);
    }
    
    Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    @Override
    public String toString() {
        return rank.name() + " " + suit.name();
    }
    
    public String toPrettyString() {
        return rank.getRankString().trim() + " of " + suit.getSuitString();
    }
    
    @Override
    public boolean equals(Object obj) {
        Card card = (Card)obj;
        return card.rank == rank && card.suit == suit;
    }
    
    public static Card parseCard(String str) throws IllegalArgumentException {
        String [] parts = str.split("[ ]+");
        if (parts.length != 2)
            throw new IllegalArgumentException("string not of format <rank> <suit>");
        return new Card(Rank.valueOf(parts[0].trim()), Suit.valueOf(parts[1].trim()));
    }

};
