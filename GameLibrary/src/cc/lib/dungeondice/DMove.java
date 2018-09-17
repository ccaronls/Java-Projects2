package cc.lib.dungeondice;

import cc.lib.game.IMove;
import cc.lib.utils.Reflector;

public class DMove extends Reflector<DMove> implements IMove {
    int playerNum;

    @Override
    public int getPlayerNum() {
        return playerNum;
    }

}
