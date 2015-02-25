package cc.game.golf.net;

import cc.game.golf.core.Card;
import cc.lib.utils.Reflector;

public class PlayerInfo extends Reflector {

    static {
        addAllFields(PlayerInfo.class);
    }
    
    public int handPoints;
    public int points;
    public Card [][] cards;
    public String name;
    
}
