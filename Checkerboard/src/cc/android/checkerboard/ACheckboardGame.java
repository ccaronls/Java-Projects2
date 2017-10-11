package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import cc.lib.game.IGame;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 10/10/17.
 */

public abstract class ACheckboardGame extends Reflector<ACheckboardGame> implements IGame<Move> {

    static {
        addAllFields(ACheckboardGame.class);
    }

    public final static int BLACK = 0;
    public final static int RED   = 1;

    public final int RANKS;
    public final int COLUMNS;
    public final int NUM_PLAYERS;

    private int turn = -1;
    private int computedMoves = -1; // optimization so that multiple calls to compute moves doesnt cause complete rescan unless neccessary
    protected Piece lock = null;
    protected final Stack<Move> undoStack = new Stack<>();

    protected final Piece [][] board; // rank major



    protected ACheckboardGame(int ranks, int columns, int numPlayers) {
        this.RANKS = ranks;
        this.COLUMNS = columns;
        this.NUM_PLAYERS = numPlayers;
        board = new Piece[RANKS][COLUMNS];
    }

    private final static Piece OFF_BOARD = new Piece(-1, PieceType.UNAVAILABLE);

    public final Piece getPiece(int rank, int column) {
        if (isOnBoard(rank, column))
            return board[rank][column];
        return OFF_BOARD;
    }

    public void newGame() {
        turn = Utils.flipCoin() ? 0 : 1;
        lock = null;
        computeMoves(true);
        undoStack.clear();
    }

    public final int computeMoves(boolean refresh) {
        if (lock == null) {
            if (computedMoves >= 0 && !refresh)
                return computedMoves;
            int num = 0;
            for (int rank = 0; rank < RANKS; rank++) {
                for (int col = 0; col < COLUMNS; col++) {
                    Piece p = getPiece(rank, col);
                    p.moves.clear();
                    computeMovesForSquare(rank, col, null);
                    num += p.moves.size();
                }
            }
            return computedMoves = num;
        } else {
            return lock.moves.size();
        }
    }

    @Override
    public final int getCurPlayerNum() {
        return turn;
    }

    /*
	This should be safe to call repeatedly.
	If there is a locked piece then the move set is just the moves associated
	with the locked piece, and it will be unchanged.
	Otherwise the complete move set is recomputed
	 */
    public final int computeMoves() {
        return computeMoves(false);
    }

    protected abstract void computeMovesForSquare(int rank, int col, Move parent);

    public final int getTurn() {
        return turn;
    }

    protected void setTurn(int turn) {
        this.turn = turn;
    }

    public final void clearMoves() {
        for (int i=0; i<RANKS; i++) {
            for (int ii=0; ii<COLUMNS; ii++) {
                getPiece(i, ii).moves.clear();
            }
        }
        computedMoves = -1;
    }

    public final int getAdvancement(int rank, int playerNum) {
        switch (playerNum) {
            case BLACK:
                return board.length-1-rank;
            case RED:
                return rank;
        }
        return -1;
    }

    public final int getRankForKingCurrent() {
        return getRankForKing(getCurPlayerNum());
    }

    public final int getRankForKing(int playerNum) {
        switch (playerNum) {
            case BLACK:
                return 0;
            case RED:
                return board.length-1;
        }
        return -1;
    }

    public final boolean isOnBoard(int r, int c) {
        return r>=0 && c>=0 && r<RANKS && c<COLUMNS;
    }

    public final boolean canUndo() {
        return undoStack.size() > 0;
    }

    /**
     * Need to call super to complete the undo.
     * @return the move that was reversed
     */
    @Override
    public final Move undo() {
        if (undoStack.size() > 0) {
            Move m = undoStack.pop();
            reverseMove(m);
            return m;
        }
        return null;
    }

    protected abstract void reverseMove(Move m);

    public final Piece getLocked() {
        return lock;
    }

    public final Iterable<Piece> getPieces() {

        return new Iterable<Piece>() {
            @Override
            public Iterator<Piece> iterator() {
                return new PieceIterator();
            }
        };
    }

    public class PieceIterator implements Iterator<Piece> {

        int rank=0;
        int col=0;

        @Override
        public boolean hasNext() {
            return rank < RANKS && col < COLUMNS;
        }

        @Override
        public Piece next() {
            Piece p = board[rank][col];
            if (++col >= COLUMNS) {
                col=0;
                rank++;
            }
            return p;
        }
    }

    @Override
    public final Iterable<Move> getMoves() {
        if (lock != null)
            return new ArrayList<>(lock.moves);

        // order the moves such that those the pieces with fewer moves are earliest
        List<Move> moves = new ArrayList<>();
        final List<Piece> pieces = new ArrayList<>();
        for (Piece p : getPieces()) {
            if (p.moves.size() > 0)
                pieces.add(p);
        }
        Collections.sort(pieces, new Comparator<Piece>() {
            @Override
            public int compare(Piece p0, Piece p1) {
                return p0.moves.size() - p1.moves.size();
            }
        });
        for (Piece p : pieces) {
            for (Move m : p.moves)
                moves.add(m);
        }
        return moves;
    }

    public int getOpponent(int player) {
        if (player == BLACK)
            return RED;
        if (player == BLACK)
            return RED;
        throw new AssertionError();
    }
}
