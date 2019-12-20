package cc.lib.checkers;

import cc.lib.game.IMove;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 9/14/17.
 */

public class Move extends Reflector<Move> implements IMove {

    static {
        addAllFields(Move.class);
    }

    private final MoveType moveType;
    private final int playerNum;

    private int [] start;
    private int [] end;
    private int [] castleRookStart;
    private int [] castleRookEnd;
    private int [] captured;
    private boolean groupCapture = false;

    private PieceType startType, endType, capturedType;

    public Move(MoveType t, int playerNum) { //}, Piece captured, PieceType nextType, int ... positions) {
        this.moveType = t;
        this.playerNum = playerNum;
    }

    public Move setStart(int startRank, int startCol, PieceType type) {
        if (type == PieceType.EMPTY)
            throw new AssertionError("start type cannot be empty");
        start = new int[]{startRank, startCol};
        startType = type;
        return this;
    }

    public Move setEnd(int endRank, int endCol, PieceType type) {
        end = new int [] { endRank, endCol };
        if (type == null)
            throw new AssertionError("type cannot be null");
        endType = type;
        return this;
    }

    public Move setCaptured(int capturedRank, int capturedCol, PieceType type) {
        captured = new int[] { capturedRank, capturedCol };
        capturedType = type;
        return this;
    }

    public Move setCastle(int castleRookStartRank, int castRookStartCol, int castleRookEndRank, int castleRookEndCol) {
        castleRookStart = new int [] { castleRookStartRank, castRookStartCol };
        castleRookEnd = new int   [] { castleRookEndRank, castleRookEndCol };
        return this;
    }

    public Move() {
        this(null, -1);
    }

    public final MoveType getMoveType() {
        return moveType;
    }

    public final PieceType getStartType() {
        return startType;
    }

    public final PieceType getEndType() {
        return endType;
    }

    public final int [] getStart() {
        return start;
    }

    public final int [] getEnd() {
        return end;
    }

    public final boolean hasEnd() {
        return end != null;
    }

    public final int [] getCaptured() {
        return captured;
    }

    public final PieceType getCapturedType() {
        return capturedType;
    }

    public final boolean hasCaptured() {
        return captured != null && capturedType != null;
    }

    public final int [] getCastleRookStart() {
        return castleRookStart;
    }

    public final int [] getCastleRookEnd() {
        return castleRookEnd;
    }

    String toStr(int [] pos) {
        return "{" + pos[0] + "," + pos[1] + "}";
    }

    @Override
    public final String toString() {
        String s = moveType.name() + " pn:" + playerNum;
        if (start != null) {
            s += " spos: " + toStr(start) + " st: " + startType;
        }
        if (end != null) {
            s += " epos: " + toStr(end) + " et: " + endType;
        }
        if (captured != null) {
            s += " cap:" + toStr(captured) + " ct: " + capturedType;
        }
        if (groupCapture) {
            s += " group capture";
        }
        if (castleRookStart != null) {
            s += " castle st: " + toStr(castleRookStart) + " end: " + toStr(castleRookEnd);
        }
        return s;
    }

    @Override
    public int getPlayerNum() {
        return playerNum;
    }

    @Override
    public int getCompareValue() {
        return 0;
    }

    public boolean isGroupCapture() {
        return groupCapture;
    }

    public void setGroupCapture(boolean groupCapture) {
        this.groupCapture = groupCapture;
    }
}