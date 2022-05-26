package cc.game.kaiser.core;

import cc.lib.utils.Reflector;

public class Bid extends Reflector<Bid> {
    static {
        addAllFields(Bid.class);
    }
    
    
    public final static Bid NO_BID = new Bid(0, Suit.NOTRUMP) {
        public String toString() { return "NO_BID"; }
    };


    public final int numTricks;
    public final Suit trump;

    public Bid() {
        this(0, Suit.NOTRUMP);
    }
    
    Bid(int numTricks, Suit trump) {
        this.numTricks = numTricks;
        this.trump = trump;
    }
    
    @Override
    public final boolean equals(Object obj) {
        Bid bid = (Bid)obj;
        return bid.numTricks == numTricks && bid.trump == trump;
    }
    
    @Override
    public String toString() {
        return "" + numTricks + " " + trump.name();
    }
    
    public static Bid parseBid(String str) throws IllegalArgumentException {
        str = str.trim();
        if (str.equals(NO_BID.toString())) {
            return NO_BID;
        } else {
            String [] parts = str.split("[ ]+");
            if (parts.length != 2)
                throw new IllegalArgumentException("Bid string invalid: \"" + str + "\" not of format: '%d %s'");
            try {
                int numTricks = Integer.parseInt(parts[0].trim());
                Suit trump = Suit.valueOf(parts[1].trim());
                return new Bid(numTricks, trump);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Bid string invalid: \"" + str + "\" failed to parse numTricks from '" + parts[0] + "'");
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Bid string invalid: \"" + str + "\" failed to parse trump suit from '" + parts[1] + "'");
            }
        }
    }    
    
};
