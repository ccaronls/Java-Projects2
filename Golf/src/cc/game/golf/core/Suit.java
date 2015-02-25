package cc.game.golf.core;

/**
 * Enum of different suits a card can be.
 * @author ccaron
 *
 */
public enum Suit
{
    HEARTS  ("Hearts"),
    DIAMONDS("Diamonds"),
    CLUBS   ("Clubs"),
    SPADES  ("Spades"),
    RED     (" Red"), /** Special suit for Jokers */
    BLACK   (" Black") /** Special suit for Jokers */
    ;

    private Suit(String symbol) {
        this.symbol = symbol;
    }
    
    private final String symbol;
    
    /**
     * Get the human readable string for this suit: Hearts, diamonds, ect.
     * @return
     */
    public final String getSuitString() {
        return symbol;
    }
    
    /**
     * Get the display character for this string (H,S,D or C)
     * @return
     */
    public final char getSuitChar() {
        return this.symbol.charAt(0);
    }
    
    static Suit getSuitFromChar(char c) {
        for (Suit s: values()) {
            if (s.name().charAt(0) == c)
                return s;
        }
        return null;
    }
};