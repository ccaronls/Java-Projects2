package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.lib.game.IMove;
import cc.lib.utils.Reflector;

public class Move extends Reflector<Move> implements IMove, Comparable<Move> {

    static {
        addAllFields(Move.class);
    }

    private final MoveType moveType;
    private final int playerNum;

    private int [] start;
    private int [] end;
    private int [] castleRookStart;
    private int [] castleRookEnd;
    private int [] opponentKing; // 4 elem array
    private List<int []> captured = null;

    private PieceType startType, endType;

    @Omit
    long bestValue = 0;
    @Omit
    Move path = null;
    @Omit
    int maximize = 0;
    @Omit
    List<Move> children;
    @Omit
    private int compareValue = 0;
    @Omit
    int jumpDepth = 0;

    public final long getBestValue() {
        return bestValue;
    }

    public final Move getPath() {
        return path;
    }

    @Override
    public int compareTo(Move o) {
        return o.compareValue - compareValue;
    }

    String getXmlStartTag(Move parent) {
        if (parent == null) {
            return "<root value=\"" + bestValue + "\" turn=\"" + playerNum + "\">";
        }
        boolean isPath = parent.path == this;
        if (maximize == 0)
            return "<leaf" + (isPath ? " path=\"true\"" : "") + " value=\"" + bestValue + "\">";
        else if (maximize < 0)
            return "<min" + (isPath ? " path=\"true\"" : "") + " value=\"" + bestValue + "\">";
        return "<max" + (isPath ? " path=\"true\"" : "") + " value=\"" + bestValue + "\">";
    }

    String getXmlEndTag(Move parent) {
        if (parent == null) {
            return "</root>";
        }
        if (maximize == 0)
            return "</leaf>";
        else if (maximize < 0)
            return "</min>";
        return "</max>";
    }

    public Move(MoveType t, int playerNum) { //}, Piece captured, PieceType nextType, int ... positions) {
        this.moveType = t;
        this.playerNum = playerNum;
        this.compareValue = t == null ? 0 : t.value;
    }

    public Move setStart(int startRank, int startCol, PieceType type) {
        if (type == PieceType.EMPTY)
            throw new AssertionError("start type cannot be empty");
        start = new int[]{startRank, startCol};
        startType = type;
        compareValue += type.value;
        return this;
    }

    public Move setEnd(int endRank, int endCol, PieceType type) {
        end = new int [] { endRank, endCol };
        if (type == null)
            throw new AssertionError("type cannot be null");
        endType = type;
        return this;
    }

    public Move addCaptured(int capturedRank, int capturedCol, PieceType type) {
        if (captured == null) {
            captured = new ArrayList<>();
        }
        captured.add(new int[] { capturedRank, capturedCol, type.ordinal() });
        compareValue += 100;
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
        return end != null && moveType != MoveType.STACK;
    }

    public final int getNumCaptured() {
        return captured != null ? captured.size() : 0;
    }

    public final int [] getCaptured(int index) {
        return captured.get(index);
    }

    public final PieceType getCapturedType(int index) {
        return PieceType.values()[captured.get(index)[2]];
    }

    public final List<int[]> getCapturedList() {
        return captured;
    }

    public final int [] getLastCaptured() {
        return captured.get(captured.size()-1);
    }

    public final PieceType getLastCapturedType() {
        return PieceType.values()[captured.get(captured.size()-1)[2]];
    }

    public final boolean hasCaptured() {
        return captured != null && captured.size() > 0;
    }

    public final int [] getCastleRookStart() {
        return castleRookStart;
    }

    public final int [] getCastleRookEnd() {
        return castleRookEnd;
    }

    static String toStr(int [] pos) {
        String s = "{" + pos[0] + "," + pos[1];
        if (pos.length > 2)
            s += "," + PieceType.values()[pos[2]];
        if (pos.length > 3)
            s += "," + PieceType.values()[pos[3]];
        s += "}";
        return s;
    }

    @Override
    public final String toString() {
        StringBuffer str = new StringBuffer(64);
        str.append(playerNum).append(":").append(moveType);
        if (start != null) {
            str.append(" ").append(startType).append(hasEnd() ? " from:" : " at:").append(toStr(start));
        }
        if (end != null) {
            str.append(" to:").append(toStr(end));
            if (endType != startType)
                str.append(" becomes:").append(endType);
        }
        if (captured != null) {
            str.append(" cap:");
            for (int i=0; i<captured.size(); i++) {
                if (i > 0)
                    str.append(",");
                str.append(toStr(captured.get(i)));
            }
        }
        if (castleRookStart != null) {
            str.append(" castle st: ").append(toStr(castleRookStart)).append(" end: ").append(toStr(castleRookEnd));
        }
        if (opponentKing != null) { // && opponentKing[2] != opponentKing[3]) {
            str.append(" oppKing: ").append(toStr(opponentKing));
        }
        return str.toString();
    }

    @Override
    public int getPlayerNum() {
        return playerNum;
    }

    public PieceType getOpponentKingTypeStart() {
        return PieceType.values()[opponentKing[2]];
    }

    public PieceType getOpponentKingTypeEnd() {
        return PieceType.values()[opponentKing[3]];
    }

    public int [] getOpponentKingPos() {
        return opponentKing;
    }

    void setOpponentKingType(int rank, int col, PieceType opponentKingTypeStart, PieceType opponentKingTypeEnd) {
        this.opponentKing = new int[] { rank, col, opponentKingTypeStart.ordinal(), opponentKingTypeEnd.ordinal() };
        compareValue += opponentKingTypeEnd.value;
    }

    boolean hasOpponentKing() {
        return opponentKing != null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        Move mv = (Move)obj;
        return playerNum == mv.playerNum
                && moveType == mv.moveType
                && Arrays.equals(start, mv.start)
                && Arrays.equals(end, mv.end)
                && hasCaptured() == mv.hasCaptured()
                && hasOpponentKing() == mv.hasOpponentKing();
    }

    public final int getCompareValue() {
        return compareValue;
    }


}