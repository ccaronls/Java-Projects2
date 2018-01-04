package cc.android.checkerboard;

import cc.lib.game.IMove;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 9/14/17.
 */

public class Move extends Reflector<Move> implements IMove {

    static {
        addAllFields(Move.class);
    }

    public final MoveType type;
    public final int playerNum;

    private final int [][] squares;

    public final Piece captured;
    public PieceType nextType;

    public Move(MoveType t, int playerNum, Piece captured, PieceType nextType, int ... positions) {
        this.type = t;
        this.captured = captured;
        this.nextType = nextType;
        this.playerNum = playerNum;
        squares = new int[positions.length/2][];
        for (int i=0; i<positions.length; i+=2) {
            int sr = positions[i];
            int er = positions[i+1];
            squares[i/2] = new int[] { sr, er };
        }
    }

    public Move() {
        this(null, -1, null, null);
    }

    public final int [] getStart() {
        return squares[0];
    }

    public final int [] getEnd() {
        return squares[1];
    }

    public final boolean hasEnd() {
        return squares.length >= 2;
    }

    public final int [] getCaptured() {
        return squares[2];
    }

    public final int [] getCastleRookStart() {
        return squares[2];
    }

    public final int [] getCastleRookEnd() {
        return squares[3];
    }

    @Override
    public final String toString() {
        return type.name() + Utils.toString(squares) + " pn:" + playerNum + " nt: " + nextType + " cap:"
                + (captured == null ? "null" : captured.type.name() + "[" + captured.playerNum + "]")
                ;
    }

    @Override
    public int getPlayerNum() {
        return playerNum;
    }
}