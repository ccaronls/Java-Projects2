package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cc.lib.utils.Reflector;

public class Piece extends Reflector<Piece> {

    static {
        addAllFields(Piece.class);
    }

    private int playerNum;
    private PieceType type;
    int numMoves = 0;
    private boolean captured = false;
    private final int rank, col;
    private int value = 0;

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
        if (type == null)
            throw new AssertionError("type cannot be null");
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
}
