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

    private final Piece [][] board; // rank major

    protected ACheckboardGame(int ranks, int columns, int numPlayers) {
        this.RANKS = ranks;
        this.COLUMNS = columns;
        this.NUM_PLAYERS = numPlayers;
        board = new Piece[RANKS][COLUMNS];
    }

    @Override
    public int getMinVersion() {
        return 2;
    }

    private final static Piece OFF_BOARD = new Piece(-1, PieceType.UNAVAILABLE);

    public final Piece getPiece(int [] pos) {
        return getPiece(pos[0], pos[1]);
    }

    public final Piece getPiece(int rank, int column) {
        if (isOnBoard(rank, column)) {
            Piece p = board[rank][column];
            if (p == null)
                throw new AssertionError("null piece at " + rank + ", " + column);
            return p;
        }
        return OFF_BOARD;
    }

    public final void setBoard(int [] pos, Piece p) {
        board[pos[0]][pos[1]] = p;
    }

    public final void movePiece(Move m) {
        Piece p = getPiece(m.getStart());
        setBoard(m.getEnd(), p);
        clearPiece(m.getStart());
        if (m.nextType != null) {
            PieceType t = p.type;
            p.type = m.nextType;
            m.nextType = t;
        }
    }

    public final void clearPiece(int [] pos ) {
        board[pos[0]][pos[1]] = new Piece();
    }

    public final void setPieceType(int [] pos, PieceType t) {
        getPiece(pos).type = t;
    }

    /**
     * return 2 elem array containing the rank and column of the first occurance of t
     * or null if not found
     * @param playerNum
     * @param types
     * @return
     */
    public final int[] findPiecePosition(int playerNum, PieceType ... types) {
        for (int rank=0; rank<RANKS; rank++) {
            for (int col =0; col<COLUMNS; col++) {
                if (board[rank][col].playerNum == playerNum) {
                    for (PieceType t : types) {
                        if (board[rank][col].type == t)
                            return new int[]{rank, col};
                    }
                }
            }
        }
        return null;
    }

    public final Piece findPiece(int playerNum, PieceType ... types) {
        int[] pos = findPiecePosition(playerNum, types);
        if (pos != null) {
            return getPiece(pos[0], pos[1]);
        }
        return null;
    }

    /**
     * Should be called after all the pieces have been setup
     */
    public void newGame() {
        turn = Utils.flipCoin() ? 0 : 1;
        lock = null;
        computeMoves(true);
        undoStack.clear();
    }

    protected int recomputeMoves() {
        int num = 0;
        for (int rank = 0; rank < RANKS; rank++) {
            for (int col = 0; col < COLUMNS; col++) {
                Piece p = getPiece(rank, col);
                p.moves.clear();
                if (p.playerNum == getTurn())
                    computeMovesForSquare(rank, col, null);
                num += p.moves.size();
            }
        }
        return num;
    }

    private final int computeMoves(boolean refresh) {
        if (lock == null) {
            if (computedMoves >= 0 && !refresh)
                return computedMoves;
            int num = recomputeMoves();
            return computedMoves = num;
        } else {
            return lock.moves.size();
        }
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

    @Override
    public final int getTurn() {
        return turn;
    }

    protected final void setTurn(int turn) {
        this.turn = turn;
    }

    protected final void nextTurn() {
        turn = (turn+1) % NUM_PLAYERS;
        clearMoves();
    }

    public final void clearMoves() {
        for (int i=0; i<RANKS; i++) {
            for (int ii=0; ii<COLUMNS; ii++) {
                getPiece(i, ii).moves.clear();
            }
        }
        computedMoves = -1;
    }

    public final int getForward(int playerNum) {
        switch (playerNum) {
            case BLACK:
                return 1;
            case RED:
                return -1;
        }
        throw new AssertionError();
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
        return getRankForKing(getTurn());
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

    public final int getRankForSwapPawn(int playerNum) {
        switch (playerNum) {
            case RED:
                return 0;
            case BLACK:
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
            reverseMove(m, true);
            return m;
        }
        return null;
    }

    protected final void reverseMove(Move m) {
        reverseMove(m, false);
    }

    private final void reverseMove(Move m, boolean recompute) {
        Piece p;
        switch (m.type) {
            case END:
                break;
            case CASTLE:
                p = getPiece(m.getCastleRookEnd());
                Utils.assertTrue(p.type == PieceType.ROOK, "Expected ROOK was " + p.type);
                p.type = PieceType.ROOK_IDLE;
                p.playerNum = m.playerNum;
                setBoard(m.getCastleRookStart(), p);
                clearPiece(m.getCastleRookEnd());
                // fallthrough
            case SLIDE:
            case JUMP:
                setBoard(m.getStart(), getPiece(m.getEnd()));
                clearPiece(m.getEnd());
                if (m.captured != null)
                    setBoard(m.getCaptured(), m.captured);
                //fallthrough
            case SWAP:
            case STACK:
                break;
        }

        if (m.nextType != null) {
            PieceType t = getPiece(m.getStart()).type;
            getPiece(m.getStart()).type = m.nextType;
            m.nextType = t;
        }

        if (!recompute)
            return;

        setTurn(m.playerNum);
        Move parent = null;
        if (undoStack.size() > 0) {
            parent = undoStack.peek();
            if (parent.playerNum != m.playerNum) {
                parent = null;
            }
        }
        if (parent == null) {
            lock = null;
            computeMoves(true);
        } else {
            clearMoves();
            p = getPiece(m.getStart());//.startRank, m.startCol);
            computeMovesForSquare(m.getStart()[0], m.getStart()[1], parent);
            p.moves.add(new Move(MoveType.END, m.playerNum, null, null, m.getStart()));
            lock = p;
        }
    }

    public final Piece getLocked() {
        return lock;
    }

    public final Iterable<Piece> getPieces(int playerNum) {
        List<Piece> all = new ArrayList<>();
        for (Piece p : getPieces()) {
            if (p.playerNum == playerNum)
                all.add(p);
        }
        return all;
    }

    public final Iterable<Piece> getPieces() {

        return new Iterable<Piece>() {
            @Override
            public Iterator<Piece> iterator() {
                return new PieceIterator();
            }
        };
    }

    // TODO: Optimmize not doing full 64 square search. Keeping track of only movable pieces in separate collection.
    final public class PieceIterator implements Iterator<Piece> {

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

    public final int getOpponent() {
        return getOpponent(getTurn());
    }

    public final int getOpponent(int player) {
        if (player == BLACK)
            return RED;
        if (player == RED)
            return BLACK;
        throw new AssertionError();
    }

    protected void initRank(int rank, int player, PieceType...pieces) {
        for (int i=0; i<8; i++) {
            Piece p = board[rank][i] = new Piece(player, pieces[i]);
            switch (p.type) {
                case EMPTY:
                case UNAVAILABLE:
                    p.playerNum = -1;
                    break;
            }
        }
    }

    public void onGameOver() {}
}
