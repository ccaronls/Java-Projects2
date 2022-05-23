package cc.lib.checkerboard;

import cc.lib.utils.GException;
import cc.lib.utils.Reflector;

public class Piece extends Reflector<Piece> {

    static {
        addAllFields(Piece.class);
    }

    private PieceType type;
    @Omit
    int numMoves = 0;
    private boolean captured = false;
    private final int position;
    private int value = 0;
    // pos 0 is closest to the top.
    private int stack, numStacks = 0;

    void clear() {
        setPlayerNum(-1);
        type = PieceType.EMPTY;
        numStacks = 0;
        stack = 0;
        captured = false;
        numMoves = 0;
    }

    void copyFrom(Piece from) {
        setType(from.getType());
        stack = from.stack;
        numStacks = from.numStacks;
        captured = from.captured;
        value = from.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        Piece p = (Piece)obj;
        return stack == p.stack
                && numStacks == p.numStacks
                && position == p.position
                && type == p.type;
    }

    public Piece() {
        stack = 0;
        numStacks = 0;
        type = PieceType.EMPTY;
        position = -1;
    }

    public Piece(int playerNum, PieceType type) {
        this(-1, -1, playerNum, type);
    }

    public Piece(int rank, int col, int playerNum, PieceType type) {
        if (type == null)
            throw new GException("type cannot be null");
        if (playerNum > 1)
            throw new GException("Invaid player num");
        this.type = type;
        this.position = (rank << 8) | col;
        setPlayerNum(playerNum);
    }

    public Piece(int pos, int playerNum, PieceType type) {
        this(pos<<8, pos&0xff, playerNum, type);
    }

    public int getPosition() {
        return position;
    }

    public int getPlayerNum() {
        if (numStacks == 0)
            return -1;
        //return 0 == (stack & (1 << (numStacks-1))) ? 0 : 1;
        return stack & 1;
    }

    public void setPlayerNum(int playerNum) {
        if (playerNum > 1)
            throw new GException("Invalid player num");
        if (playerNum < 0) {
            numStacks=0;
            stack = 0;
        } else if (numStacks <= 1) {
            stack = playerNum;
            numStacks = 1;
        } else {
            stack = stack & ~(1<<(numStacks-1));
            if (playerNum > 0)
                stack |= (1<<(numStacks-1));
        }
    }

    public PieceType getType() {
        return type;
    }

    public void setType(PieceType type) {
        if (type == null || type == PieceType.EMPTY)
            throw new GException("cannot set type to empty");
        this.type = type;
    }

    public int getRank() {
        return position >> 8;
    }

    public int getCol() {
        return position & 0xff;
    }

    public boolean isCaptured() {
        return captured;
    }

    public void setCaptured(boolean captured) {
        this.captured = captured;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public final int getNumMoves() {
        return numMoves;
    }

    public boolean isStacked() {
        return numStacks > 1;
    }

    public void addStackTop(int n) {
        stack = (stack << 1) | n;
        numStacks++;
    }

    public void addStackBottom(int n) {
        if (n < 0 || n > 1)
            throw new GException("Invalid value for n: " + n);
        if (numStacks >= 32)
            throw new GException("Stack overflow");
        if (n > 0)
            stack |= (1<<numStacks);
        numStacks++;
    }

    public int removeStackTop() {
        if (numStacks <= 0)
            throw new GException("Empty stack");
        int n = stack & 0x1;
        stack >>= 1;
        numStacks--;
        return n;
    }

    public int removeStackBottom() {
        if (numStacks <= 0)
            throw new GException("Empty stack");
        int n = stack & (1<<--numStacks);
        return n == 0 ? 0 : 1;
    }

    public int getStackSize() {
        return numStacks;
    }

    // 0 is the top of the stack (closest to top piece)
    public int getStackAt(int index) {
        if (index < 0 || index >= numStacks)
            throw new GException("Index ofut of bounds: Value '" + index + "' out of range of [0-" + numStacks + ")");
        return 0 == (stack & (1 << index)) ? 0 : 1;
    }

    public void setChecked(boolean checked) {
        switch (type) {
            case CHECKED_KING:
                if (!checked)
                    type = PieceType.UNCHECKED_KING;
                break;
            case CHECKED_KING_IDLE:
                if (!checked)
                   type = PieceType.UNCHECKED_KING_IDLE;
                break;
            case UNCHECKED_KING:
                if (checked)
                    type = PieceType.CHECKED_KING;
                break;
            case UNCHECKED_KING_IDLE:
                if (checked)
                    type = PieceType.CHECKED_KING_IDLE;
                break;
            default:
                throw new GException("Unhandled case: " + type);
        }
    }

    @Override
    public String toString() {
        return "Piece{" +
                "PNUM=" + getPlayerNum() +
                ", " + type +
//                ", numMoves=" + numMoves +
                ", pos= [" + getRank() +
                ", " + getCol() +
                "] captured=" + captured +
                ", value=" + value +
                (numStacks > 1 ? ", stacks=" + numStacks : "") +
//                ", stack=" + stack +
                '}';
    }
}
