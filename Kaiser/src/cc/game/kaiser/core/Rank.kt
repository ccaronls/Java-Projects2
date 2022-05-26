package cc.game.kaiser.core;

/**
 * Defines the rank of a card (Ace, King, Queen, Jack, etc.)
 * @author ccaron
 *
 */
public enum Rank
{
    THREE   ("3 "),
    FIVE    ("5 "),
    SEVEN   ("7 "),
    EIGHT   ("8 "),
    NINE    ("9 "),
    TEN     ("10"),
    JACK    ("J "),
    QUEEN   ("Q "),
    KING    ("K "),
    ACE     ("A ");
    
    private Rank(String s) {
        this.s = s;
    }
    
    private final String s;
    
    public final String getRankString() {
        return s;
    }
};
