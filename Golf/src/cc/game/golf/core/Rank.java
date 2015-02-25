package cc.game.golf.core;

import java.util.HashMap;

/**
 * Defines the rank of a card (Ace, King, Queen, Jack, etc.)
 * @author ccaron
 *
 */
public enum Rank
{
    ACE     ("Ace",     "A ", 1),
    TWO     ("Two",     "2 ", 2),
    THREE   ("Three",   "3 ", 3),
    FOUR    ("Four",    "4 ", 4),    
    FIVE    ("Five",    "5 ", 5),
    SIX     ("Six",     "6 ", 6),
    SEVEN   ("Seven",   "7 ", 7),
    EIGHT   ("Eight",   "8 ", 8),
    NINE    ("Nine",    "9 ", 9),
    TEN     ("Ten",     "10", 10),
    JACK    ("Jack",    "J ", 10),
    QUEEN   ("Queen",   "Q ", 10),
    KING    ("King",    "K ", 0),
    JOKER   ("Joker",   "Jo", -1);
    
    private Rank(String pretty ,String s, int value) {
        this.pretty = pretty;
        this.s = s;
        this.value = value;
    }
    
    private final String pretty;
    private final String s;
    public final int value;
    private static final HashMap<String, Rank> stringToRankLookup = new HashMap<String, Rank>();
    
    static {
        for (Rank r: values()) {
            stringToRankLookup.put(r.getRankString().trim(), r);
        }
    }
    
    static Rank getRankFromString(String rankString) {
        return stringToRankLookup.get(rankString.trim());
    }
    
    public String getRankString() {
        return s;
    }

    public String getPrettyString() {
        return pretty;
    }

};
