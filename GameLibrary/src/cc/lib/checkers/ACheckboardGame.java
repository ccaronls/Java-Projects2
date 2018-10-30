package cc.lib.checkers;

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

    // the player positions are on the far side of board (rank == 0)
    // or NEAR side closest to user.
    public final static int NEAR = 0;
    public final static int FAR  = 1;

    public final int RANKS;
    public final int COLUMNS;
    public final int NUM_PLAYERS;

    private int turn = -1;
    private int computedMoves = -1; // optimization so that multiple calls to compute moves doesnt cause complete rescan unless neccessary
    protected Piece lock = null;
    protected final Stack<Move> undoStack = new Stack<>();
    private boolean forfeited = false;

    // expose so we can allow fast access to array
    public final Piece [][] board; // rank major

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

    public final void forfeit() {
        forfeited = true;
        executeMove(new Move(MoveType.END, getTurn()));
    }

    public Piece movePiece(Move m) {
        Piece p = getPiece(m.getStart());
        setBoard(m.getEnd(), p=new Piece(p.playerNum, m.getEndType()));
        clearPiece(m.getStart());
        return p;
    }

    public final boolean isForfeited() {
        return forfeited;
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
                    if (Utils.linearSearch(types, board[rank][col].type) >= 0) {
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
            return board[pos[0]][pos[1]];//getPiece(pos[0], pos[1]);
        }
        return null;
    }

    public final List<Piece> getCapturedPieces() {
        List<Piece> captured = new ArrayList<>();
        for (Move m : undoStack) {
            if (m.getCaptured() != null)
                captured.add(new Piece(getOpponent(m.getPlayerNum()), m.getCapturedType()));
        }
        return captured;
    }

    /**
     * Should be called after all the pieces have been setup
     */
    public void newGame() {
        if (board[0][0] != null)
            clearMoves();
        lock = null;
        forfeited = false;
        undoStack.clear();
        initBoard();
        computeMoves();
    }

    protected abstract void initBoard();

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
        if (forfeited)
            return 0;
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

    protected void nextTurn() {
        turn = (turn+1) % NUM_PLAYERS;
        clearMoves();
    }

    public final void clearMoves() {
        computedMoves = -1;
        if (board[0][0] == null)
            return;
        for (int i = 0; i < RANKS; i++) {
            for (int ii = 0; ii < COLUMNS; ii++) {
                getPiece(i, ii).moves.clear();
            }
        }
    }

    /**
     * This is the rank closest to the given side
     *
     * @param side
     * @return
     */
    public final int getStartRank(int side) {
        switch (side) {
            case FAR:
                return 0;
            case NEAR:
                return RANKS-1;
        }
        Utils.assertTrue(false, "Unexpected side " + side);
        return -1;
    }

    /**
     * Get how many units of advancement the rank is for a side
     *
     * @param side
     * @param rank
     * @return
     */
    public final int getAdvancementFromStart(int side, int rank) {
        return getStartRank(side) + rank*getAdvanceDir(side);
    }

    /**
     * This is the rank increment based on side
     * @param side
     * @return
     */
    public final int getAdvanceDir(int side) {
        switch (side) {
            case FAR:
                return 1;
            case NEAR:
                return -1;
        }
        Utils.assertTrue(false, "Unexpected side " + side);
        return 0;
    }

    public final boolean isOnBoard(int r, int c) {
        if (r < 0 || c < 0 || r >= RANKS || c >= COLUMNS)
            return false;
        return true;
        //return r>=0 && c>=0 && r<RANKS && c<COLUMNS;
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

    protected void reverseMove(Move m, boolean recompute) {
        Piece p;
        switch (m.getMoveType()) {
            case END:
                break;
            case CASTLE:
                p = getPiece(m.getCastleRookEnd());
                Utils.assertTrue(p.type == PieceType.ROOK, "Expected ROOK was " + p.type);
                setBoard(m.getCastleRookStart(), new Piece(m.getPlayerNum(), PieceType.ROOK_IDLE));
                clearPiece(m.getCastleRookEnd());
                // fallthrough
            case SLIDE:
            case FLYING_JUMP:
            case JUMP:
                setBoard(m.getStart(), new Piece(m.getPlayerNum(), m.getStartType()));
                clearPiece(m.getEnd());
                if (m.getCaptured() != null) {
                    setBoard(m.getCaptured(), new Piece(getOpponent(m.getPlayerNum()), m.getCapturedType()));
                }
                //fallthrough
            case SWAP:
            case STACK:
                break;
        }

        if (!recompute)
            return;

        setTurn(m.getPlayerNum());
        Move parent = null;
        if (undoStack.size() > 0) {
            parent = undoStack.peek();
            if (parent.getPlayerNum() != m.getPlayerNum()) {
                parent = null;
            }
        }
        if (parent == null) {
            lock = null;
            computeMoves(true);
        } else {
            clearMoves();
            p = getPiece(m.getStart());
            computeMovesForSquare(m.getStart()[0], m.getStart()[1], parent);
            if (!isJumpsMandatory())
                p.moves.add(new Move(MoveType.END, m.getPlayerNum()).setStart(m.getStart()[0], m.getStart()[1], m.getStartType()));
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

        // order the moves such that those pieces with fewer moves are earliest
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
        if (player == NEAR)
            return FAR;
        if (player == FAR)
            return NEAR;
        throw new AssertionError();
    }

    protected void initRank(int rank, int player, PieceType...pieces) {
        for (int i=0; i<pieces.length; i++) {
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

    enum Color {
        RED, WHITE, BLACK
    };

    public abstract Color getPlayerColor(int side);

    protected boolean isJumpsMandatory() {
        return false;
    }

    public enum BoardType {
        CHECKERS,
        DAMA
    }

    public BoardType getBoardType() {
        return BoardType.CHECKERS;
    }

    public void endTurn() {
        if (lock != null) {
            for (Move m : lock.moves) {
                if (m.getMoveType() == MoveType.END) {
                    undoStack.push(m);
                    break;
                }
            }
        }
        endTurnPrivate();
    }

    protected void endTurnPrivate() {
        nextTurn();
        lock = null;
        clearMoves();
        if (computeMoves()==0) {
            onGameOver();
        }
    }


}
