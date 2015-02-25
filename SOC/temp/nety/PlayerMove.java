package cc.game.soc.nety;

import cc.game.soc.core.SOCTrade;

public class PlayerMove {

    private final PlayerMoveType type;
    private final int index;
    private final SOCTrade trade;

    public String toString() {
        if (trade == null)
            return type + " " + index;
        return type + " " + trade;
    }
    
    public PlayerMove(PlayerMoveType type, int index) {
        this.type = type;
        this.index = index;
        this.trade = null;
    }
    
    public PlayerMove(PlayerMoveType type, SOCTrade trade) {
        this.type = type;
        this.index = -1;
        this.trade = trade;
    }

    public int getIndex() {
        return index;
    }

    public SOCTrade getTrade() {
        return trade;
    }

    public PlayerMoveType getType() {
        return type;
    }
    
    
}
