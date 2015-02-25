package cc.game.golf.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.lib.utils.Reflector;

/**
 * Describe a card in the game.  Only classes within this package can create or edit card data.
 * @author ccaron
 *
 */
public class Card extends Reflector
{
    private int deck;
    private Rank rank;
    private Suit suit;
    private boolean showing;
    
    static {
        addAllFields(Card.class);
    }

    /**
     * 
     */
    public Card() {
        deck = -1;
        rank = null;
        suit = null;
        showing = false;
    }

    /**
     * Return a blank card that is not showing.  Can be made unique with deck field.
     * @param deck
     */
    public Card(int deck) {
        this(deck, null, null, false);
    }
    
    /**
     * 
     * @param deck
     * @param rank
     * @param suit
     * @param showing
     */
    Card(int deck, Rank rank, Suit suit, boolean showing) {
        this.deck = deck;
        this.rank = rank;
        this.suit = suit;
        this.showing = showing;
    }

    /**
     * 
     * @param rank
     * @param suit
     * @param showing
     */
    public Card(Rank rank, Suit suit, boolean showing) {
        this(0, rank, suit, showing);
    }
    
    /**
     * 
     * @return
     */
    public final boolean isBlank() {
        return rank == null || suit == null;
    }
    
    @Override
    public String toString() {
        return "(" + deck + ") " + rank.name() + " " + suit.name() + (showing?" up":" down");
    }
    
    /**
     * 
     * @return
     */
    public final String toPrettyString() {
        if (showing) {
            if (rank == Rank.JOKER)
                return suit.getSuitString().trim() + " " + rank.getPrettyString();
            return rank.getPrettyString().trim() + " of " + suit.getSuitString();
        }
        return "Face Down";
    }
    
    @Override
    public boolean equals(Object obj) {
        Card card = (Card)obj;
        return (this == card) || (card.deck == deck && card.rank == rank && card.suit == suit);
    }
    
    /**
     * 
     * @return
     */
    public final boolean isShowing() {
        return this.showing;
    }
    
    /**
     * 
     * @param showing
     */
    final void setShowing(boolean showing) {
        this.showing = showing;
    }
    
    /*
     * Write a collection in shortened format to ease memory usage 
     */
    static String writeCards(Collection<Card> cards) {
        StringBuffer buf = new StringBuffer();
        for (Card c : cards) {
            if (c == null)
                buf.append("null");
            else
                buf.append(String.format("[%d %s %c %d]", c.deck, c.rank.getRankString().trim(), c.suit.name().charAt(0), c.showing ? 1 : 0));
        }
        return buf.toString();
    }
    
    /**
     * Parse a single card string previously written with writeCards
     * @param str
     * @return
     * @throws IllegalArgumentException
     */
    static Card parseCard(String str) throws IllegalArgumentException {
        if (str.equals("null"))
            return null;
        String [] parts = str.split("[ ]+");
        if (parts.length != 4)
            throw new IllegalArgumentException("string not of format '%d %s %c %d'");
        int deck = Integer.parseInt(parts[0]);
        Rank r = Rank.getRankFromString(parts[1]);
        if (r == null)
            throw new NullPointerException();
        Suit s = Suit.getSuitFromChar(parts[2].charAt(0));
        boolean showing = Integer.parseInt(parts[3]) == 0 ? false : true;
        return new Card(deck, r, s, showing);
    }
    
    static List<Card> parseCards(String str) throws IllegalArgumentException {
        String [] parts = str.split("[\\[\\]]");
        ArrayList<Card> cards = new ArrayList<Card>(parts.length);
        for (String part : parts) {
            cards.add(parseCard(part));
        }
        return cards;
    }

    /**
     * 
     * @return
     */
    public final boolean isOneEyedJack() {
        return rank == Rank.JACK && (suit == Suit.SPADES || suit == Suit.HEARTS);
    }

    /**
     * 
     * @return
     */
    public final boolean isSuicideKing() {
        return rank == Rank.KING && suit == Suit.HEARTS;
    }

    /**
     * 
     * @return
     */
    public final int getDeck() {
        return deck;
    }

    /**
     * 
     * @return
     */
    public final Rank getRank() {
        return rank;
    }

    /**
     * 
     * @return
     */
    public final Suit getSuit() {
        return suit;
    }
};
