package cc.game.kaiser.net;

import cc.lib.net.GameCommandType;

public class Common {

    public static final String VERSION = "CCKAISER-1.0";
    
    public static final int PORT = 23232;
    
    public static final GameCommandType PLAY_TRICK = new GameCommandType("PLAY_TRICK");

    public static final GameCommandType MAKE_BID = new GameCommandType("MAKE_BID");
    
    public static final GameCommandType DEALT_CARD = new GameCommandType("DEALT_CARD");

    public static final GameCommandType SET_PLAYER = new GameCommandType("SET_PLAYER");

    public static final GameCommandType CARD_PLAYED = new GameCommandType("CARD_PLAYED");

}
