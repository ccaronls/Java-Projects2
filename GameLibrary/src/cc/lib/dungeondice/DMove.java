package cc.lib.dungeondice;

import java.util.List;

import cc.lib.game.IMove;
import cc.lib.utils.Reflector;

public class DMove extends Reflector<DMove> implements IMove {

    final int playerNum;
    final int index;
    final MoveType type;
    final List<Integer> path;

    public DMove() {
        this(null, -1, -1, null);
    }

    public DMove(MoveType type, int playerNum, int index, List<Integer> path) {
        this.playerNum = playerNum;
        this.index = index;
        this.type = type;
        this.path = path;
    }

    @Override
    public int getPlayerNum() {
        return playerNum;
    }

}
