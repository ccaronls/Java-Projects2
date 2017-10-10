package cc.android.checkerboard;

import cc.lib.game.IMove;
import cc.lib.game.MiniMaxTree;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 9/14/17.
 */

public class Move extends Reflector<Move> implements IMove {

    static {
        addAllFields(Move.class);
    }

    public final Checkers.MoveType type;
    public final int startRank, startCol;
    public final int endRank, endCol;
    public final int captureRank, captureCol;
    public final int playerNum;
    Piece captured = null;

    public Move() {
        this(null, 0, 0, 0, 0, 0, 0, 0);
    }

    public Move(Checkers.MoveType type, int startRank, int startCol, int endRank, int endCol, int captureRank, int captureCol, int playerNum) {
        this.type = type;
        this.startRank = startRank;
        this.startCol = startCol;
        this.endRank = endRank;
        this.endCol = endCol;
        this.captureCol = captureCol;
        this.captureRank = captureRank;
        this.playerNum = playerNum;
    }

    @Override
    public boolean equals(Object other) {
        Move m = (Move)other;
        return endCol == m.endCol && endRank == m.endRank;
    }

    @Override
    public final String toString() {
        return type.name()   + "[" + startRank + "x" + startCol + "]->[" + endRank + "x" + endCol + "] pn:" + playerNum;
    }

    @Override
    public int getPlayerNum() {
        return playerNum;
    }
}