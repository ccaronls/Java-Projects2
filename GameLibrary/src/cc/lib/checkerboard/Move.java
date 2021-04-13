package cc.lib.checkerboard;

import java.util.List;

import cc.lib.game.IMove;
import cc.lib.game.Utils;
import cc.lib.utils.GException;
import cc.lib.utils.Reflector;

public class Move extends Reflector<Move> implements IMove, Comparable<Move> {

    static {
        addAllFields(Move.class);
        addAllFields(CapturedPiece.class);
    }

    public static class CapturedPiece extends Reflector<CapturedPiece> {
        final int pos;
        final PieceType type;
        final CapturedPiece next;

        public CapturedPiece() {
            this(-1, null, null);
        }

        public CapturedPiece(int pos, PieceType type, CapturedPiece next) {
            this.pos = pos;
            this.type = type;
            this.next = next;
        }

        @Override
        public String toString() {
            return String.format("[%s %s]", Move.toStr(pos), type);
        }

        public CapturedPiece getNext() {
            return next;
        }

        public int getPosition() {
            return pos;
        }

        public PieceType getType() {
            return type;
        }
    }

    private final MoveType moveType;
    private final int playerNum;

    // consider optimization here for better memory usage (Piece objects instead of arrays)
    private int start = -1;
    private int end = -1;
    private int castleRookStart = -1;
    private int castleRookEnd = -1;
    private int opponentKing = -1;
    private PieceType opponentKingTypeStart = null;
    private PieceType opponentKingTypeEnd = null;
    private PieceType startType, endType;
    private int enpassant = -1;
    private CapturedPiece captured;
    private int jumped = -1;

    @Omit
    Move parent = null;

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
            throw new GException("start type cannot be empty");
        start = startRank<<8 | startCol;
        startType = type;
        compareValue += type.value;
        return this;
    }

    public Move setEnd(int endRank, int endCol, PieceType type) {
        end = endRank << 8 | endCol;
        if (type == null)
            throw new GException("type cannot be null");
        endType = type;
        return this;
    }

    public Move setJumped(int startRank, int startCol) {
        jumped = (startRank << 8) | startCol;
        return this;
    }

    public int getJumped() {
        return jumped;
    }

    public Move addCaptured(int capturedRank, int capturedCol, PieceType type) {
        Utils.assertTrue(0 == (type.flag & PieceType.FLAG_KING));
        CapturedPiece top = captured;
        captured = new CapturedPiece((capturedRank << 8) | capturedCol, type, captured);
        compareValue += 100 + type.value;
        return this;
    }

    public Move setCastle(int castleRookStartRank, int castRookStartCol, int castleRookEndRank, int castleRookEndCol) {
        castleRookStart = castleRookStartRank<<8 | castRookStartCol;
        castleRookEnd = castleRookEndRank<<8 | castleRookEndCol;
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

    public final int getStart() {
        return start;
    }

    public final int getEnd() {
        return end;
    }

    public final boolean hasEnd() {
        return end >= 0 && moveType != MoveType.STACK;
    }

    public final int getNumCaptured() {
        int num = 0;
        for (CapturedPiece p=captured; p!=null; p=p.next)
            num++;
        return num;
    }

    public final CapturedPiece getLastCaptured() {
        return captured;
    }

    public final boolean hasCaptured() {
        return captured != null;
    }

    public final int getCastleRookStart() {
        return castleRookStart;
    }

    public final int getCastleRookEnd() {
        return castleRookEnd;
    }

    static String toStr(int pos) {
        int rnk = pos>>8;
        int col = pos&0xff;

        return String.format("%d,%d", rnk, col);
    }

    @Override
    public final String toString() {
        StringBuffer str = new StringBuffer(64);
        str.append(playerNum).append(":").append(moveType);
        if (start >= 0) {
            str.append(" ").append(startType).append(hasEnd() ? " from:" : " at:").append(toStr(start));
        }
        if (end >= 0) {
            str.append(" to:").append(toStr(end));
            if (endType != startType)
                str.append(" becomes:").append(endType);
        }
        if (captured != null) {
            str.append(" cap:");
            String pcs = "";
            for (CapturedPiece p=captured; p!=null; p=p.next) {
                pcs = p.toString() + (pcs.length() > 0 ? ", " : " ");
            }
            str.append(pcs);
        }
        if (castleRookStart >= 0) {
            str.append(" castle st: ").append(toStr(castleRookStart)).append(" end: ").append(toStr(castleRookEnd));
        }
        if (opponentKing >= 0) { // && opponentKing[2] != opponentKing[3]) {
            str.append(" oppKing: ").append(toStr(opponentKing));
        }
        if (enpassant >= 0) {
            str.append(" enpassant:").append(toStr(enpassant));
        }
        return str.toString();
    }

    @Override
    public int getPlayerNum() {
        return playerNum;
    }

    public PieceType getOpponentKingTypeStart() {
        return opponentKingTypeStart;
    }

    public PieceType getOpponentKingTypeEnd() {
        return opponentKingTypeEnd;
    }

    public int getOpponentKingPos() {
        return opponentKing;
    }

    void setOpponentKingType(int rank, int col, PieceType opponentKingTypeStart, PieceType opponentKingTypeEnd) {
        opponentKing = rank << 8 | col;
        this.opponentKingTypeStart = opponentKingTypeStart;
        this.opponentKingTypeEnd = opponentKingTypeEnd;
        compareValue += opponentKingTypeEnd.value;
    }

    boolean hasOpponentKing() {
        return opponentKing >= 0;
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
                && start == mv.start
                && end == mv.end
                && hasCaptured() == mv.hasCaptured()
                && hasOpponentKing() == mv.hasOpponentKing();
    }

    public final int getCompareValue() {
        return compareValue;
    }


    public String getPathString() {
        StringBuffer buf=new StringBuffer();
        getPathStringR(buf, new String[] { "" });
        return buf.toString();
    }

    private void getPathStringR(StringBuffer buf, String [] indent) {
        if (parent != null)
            parent.getPathStringR(buf, indent);
        buf.append("\n").append(indent[0]).append(toString());
        indent[0] += "  ";
    }

    void setEnpassant(int pos) {
        this.enpassant = pos;
    }

    int getEnpassant() {
        return enpassant;
    }
}