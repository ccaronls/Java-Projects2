package cc.lib.checkerboard;

import java.util.*;

import cc.lib.utils.Reflector;

public class Piece extends Reflector<Piece> {

    static {
        addAllFields(Piece.class);
    }

    private int playerNum;
    private PieceType type;
    @Omit
    int numMoves = 0;
    private boolean captured = false;
    private final int rank, col;
    private int value = 0;
    // pos 0 is closest to the top.
    private LinkedList<Integer> stack = null; // list of player nums (Bashki support)

    void clear() {
        setPlayerNum(-1);
        type = PieceType.EMPTY;
        stack = null;
        captured = false;
        numMoves = 0;
    }

    void copyFrom(Piece from) {
        setPlayerNum(from.getPlayerNum());
        setType(from.getType());
        stack = from.stack;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        Piece p = (Piece)obj;
        return playerNum == p.playerNum
                && rank == p.rank
                && col == p.col
                && type == p.type;
    }

    public Piece() {
        playerNum = -1;
        type = PieceType.EMPTY;
        rank = col = -1;
    }

    public Piece(int rank, int col, int playerNum, PieceType type) {
        if (type == null)
            throw new AssertionError("type cannot be null");
        this.playerNum = playerNum;
        this.type = type;
        this.rank = rank;
        this.col = col;
    }

    public Piece(int [] pos, int playerNum, PieceType type) {
        this(pos[0], pos[1], playerNum, type);
    }

    public int [] getPosition() {
        return new int[] { rank, col };
    }

    public int getPlayerNum() {
        return playerNum;
    }

    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }

    public PieceType getType() {
        return type;
    }

    public void setType(PieceType type) {
        if (type == null || type == PieceType.EMPTY)
            throw new AssertionError("cannot set type to empty");
        this.type = type;
    }

    public int getRank() {
        return rank;
    }

    public int getCol() {
        return col;
    }

    public int [] getRankCol() {
        return new int[] { rank, col };
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
        return stack != null && stack.size() > 0;
    }

    public void addStackTop(int n) {
        if (stack == null)
            stack = new LinkedList<>();
        stack.addFirst(n);
    }

    public void addStackBottom(int n) {
        if (stack == null)
            stack = new LinkedList<>();
        stack.addLast(n);
    }

    public int removeStackTop() {
        int n = stack.removeFirst();
        if (stack.size() == 0)
            stack = null;
        return n;
    }

    public int removeStackBottom() {
        int n = stack.removeLast();
        if (stack.size() == 0)
            stack = null;
        return n;
    }

    public int getStackSize() {
        return stack.size();
    }

    // 0 is the top of the stack (closest to top piece)
    public int getStackAt(int index) {
        return stack.get(index);
    }
}
