package cc.game.kaiser.core;

public enum Suit
{
    HEARTS  ("Hearts"),
    DIAMONDS("Diamonds"),
    CLUBS   ("Clubs"),
    SPADES  ("Spades"),
    NOTRUMP ("NOTRUMP");

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
};