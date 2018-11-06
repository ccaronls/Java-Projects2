package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.AAnimation;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 9/14/17.
 */

public class Piece extends Reflector<Piece> {

    static {
        addAllFields(Piece.class);
    }

    private int playerNum;
    private PieceType type;
    private final List<Move> moves = new ArrayList<>();
    private boolean captured = false;

    @Omit
    public AAnimation a;

    public Piece() {
        playerNum = -1;
        type = PieceType.EMPTY;
    }

    Piece(int playerNum, PieceType type) {
        if (type == null)
            throw new AssertionError("type cannot be null");
        this.playerNum = playerNum;
        this.type = type;
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

    public Iterable<Move> getMoves() {
        return new ArrayList<>(moves);
    }

    public void clearMoves() {
        moves.clear();
    }

    public int getNumMoves() {
        return moves.size();
    }

    public void addMove(Move m) {
        moves.add(m);
    }

    public int removeNonJumps() {
        int numRemoved = 0;
        Iterator<Move> it = moves.iterator();
        while (it.hasNext()) {
            Move m = it.next();
            switch (m.getMoveType()) {
                case JUMP:
                case FLYING_JUMP:
                    continue;
            }
            it.remove();
            numRemoved++;
        }
        return numRemoved;
    }

    public Iterator<Move> getMovesIterator() {
        return moves.iterator();
    }

    public List<Move> getMovesList() {
        return new ArrayList<>(moves);
    }

    public void setMovesList(List<Move> m) {
        moves.clear();
        moves.addAll(m);
    }

    public Move getMove(int index) {
        return moves.get(index);
    }

    public boolean isCaptured() {
        return captured;
    }

    public void setCaptured(boolean captured) {
        this.captured = captured;
    }
}