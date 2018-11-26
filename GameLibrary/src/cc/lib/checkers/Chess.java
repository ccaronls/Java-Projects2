package cc.lib.checkers;

import java.util.Iterator;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

import static cc.lib.checkers.PieceType.*;

/**
 * Created by chriscaron on 10/10/17.
 */

public class Chess extends ACheckboardGame {

    static {
        addAllFields(Chess.class);
    }

    private int whiteSide = -1;
    private long timerLength, timerFar, timerNear;

    public Chess() {
        super(8,8,2);
        setTimer(-1);
    }

    @Omit
    private long startTimeMS = 0;

    public void setTimer(int seconds) {
        this.timerLength = this.timerFar = this.timerNear = seconds*1000;
        startTimeMS = 0;
    }

    @Override
    public void newGame() {
        super.newGame();
        this.timerFar = this.timerNear = this.timerLength;
        startTimeMS = 0;
    }

    public void timerTick(long uptimeMillis) {
        if (startTimeMS <= 0)
            startTimeMS = uptimeMillis;
        long dt = uptimeMillis-startTimeMS;
        switch (getTurn()) {
            case FAR: timerFar -= dt; break;
            case NEAR: timerNear-= dt; break;
        }
        startTimeMS = uptimeMillis;
    }

    public int getTimerLength() {
        return (int)(timerLength/1000);
    }

    public int getTimerFar() {
        return (int)(timerFar/1000);
    }

    public int getTimerNear() {
        return (int)(timerNear/1000);
    }

    public boolean isTimerExpired() {
        if (timerLength <= 0)
            return false;
        if (getTurn() == FAR && timerFar <= 0)
            return true;
        if (getTurn() == NEAR && timerNear <= 0)
            return true;
        return false;
    }

    public void initBoard() {
        whiteSide = Utils.flipCoin() ? FAR : NEAR;
        // this is to enforce the 'quenn on her own color square' rule
        PieceType left = PieceType.QUEEN;
        PieceType right = PieceType.UNCHECKED_KING_IDLE;
        if (whiteSide == FAR) {
            right = PieceType.QUEEN;
            left = PieceType.UNCHECKED_KING_IDLE;
        }

        initRank(0, FAR, ROOK_IDLE, KNIGHT, BISHOP, left, right, BISHOP, KNIGHT, ROOK_IDLE);
        initRank(1, FAR, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE);
        initRank(2, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(3, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(4, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(5, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(6, NEAR, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE);
        initRank(7, NEAR, ROOK_IDLE, KNIGHT, BISHOP, left, right, BISHOP, KNIGHT, ROOK_IDLE);

        setTurn(whiteSide);
    }

    @Override
    public void executeMove(Move move) {
        lock = null;
        Piece p = null;
        clearMoves();
        synchronized (undoStack) {
            undoStack.push(move);
        }
        {
            switch (move.getMoveType()) {
                case END:
                    if (isForfeited()) {
                        onGameOver();
                        return;
                    }
                    break;
                case JUMP:
                case SLIDE:
                    if (move.getCaptured() != null) {
                        clearPiece(move.getCaptured());
                    }
                    p = movePiece(move);
                    // check for pawn advancing
                    if (p.getType() == PAWN_TOSWAP) {
                        computeMovesForSquare(move.getEnd()[0], move.getEnd()[1], null);
                        lock = p;
                    }
                    // see if this move result
                    break;
                case SWAP:
                    setPieceType(move.getStart(), move.getEndType());
                    break;
                case CASTLE: {
                    movePiece(move);
                    p = getPiece(move.getCastleRookStart());
                    Utils.assertTrue(p.getType() == ROOK_IDLE);
                    p.setType(ROOK);
                    setBoard(move.getCastleRookEnd(), p);
                    clearPiece(move.getCastleRookStart());
                    break;
                }
                default:
                    throw new AssertionError();
            }
            updateOpponentKingCheckedState();
        }

        if (p != null && timerLength > 0) {
            lock = p;
            lock.clearMoves();
            lock.addMove(new Move(MoveType.END, p.getPlayerNum()));//, null, null, move.getEnd()));
        }

        if (lock == null) {
            nextTurn();
            if (computeMoves() == 0) {
                onGameOver();
            }
        }

        // check for game over
    }

    private void updateOpponentKingCheckedState() {
        // see if we are checking the opponent
        int [] opponentKing = findPiecePosition(getOpponent(), CHECKED_KING, CHECKED_KING_IDLE, UNCHECKED_KING, UNCHECKED_KING_IDLE);
        Piece king = getPiece(opponentKing);
        boolean checked = isSquareAttacked(opponentKing[0], opponentKing[1], getTurn());
        switch (king.getType()) {
            case CHECKED_KING_IDLE:
                if (!checked)
                    king.setType(UNCHECKED_KING_IDLE);
                break;
            case CHECKED_KING:
                if (!checked)
                    king.setType(UNCHECKED_KING);
                break;
            case UNCHECKED_KING_IDLE:
                if (checked)
                    king.setType(CHECKED_KING_IDLE);
                break;
            case UNCHECKED_KING:
                if (checked)
                    king.setType(CHECKED_KING);
                break;
        }

    }

    @Override
    protected void reverseMove(Move m, boolean recompute) {
        super.reverseMove(m, recompute);
        updateOpponentKingCheckedState();
    }

    // Return true if p.getType() is on set of types and p.getPlayerNum() equals playerNum
    private boolean testPiece(Piece p, int playerNum, PieceType ... types) {
        for (PieceType t : types) {
            if (p.getPlayerNum() == playerNum && p.getType() == t)
                return true;
        }
        return false;
    }

    /**
     * Return true if playerNum is attacking the position
     * @param rank
     * @param col
     * @param playerNum
     * @return
     */
    final boolean isSquareAttacked(int rank, int col, int playerNum) {
        boolean b = isSquareAttackedP(rank, col, playerNum);
        return b;
    }

    final private boolean isSquareAttackedP(int rank, int col, int playerNum) {

        // search in the eight directions and knights whom can
        int [][] kd = getKnightDeltas(rank, col);
        int [] dr = kd[0];
        int [] dc = kd[1];

        for (int i=0; i<dr.length; i++) {
            if (testPiece(board[rank +dr[i]][col +dc[i]], playerNum, KNIGHT)) {
                return true;
            }
        }

        final int adv = getAdvanceDir(getOpponent(playerNum));
        // look for pawns
        if (testPiece(getPiece(rank +adv, col +1), playerNum, PAWN, PAWN_ENPASSANT, PAWN_IDLE))
            return true;
        if (testPiece(getPiece(rank +adv, col -1), playerNum, PAWN, PAWN_ENPASSANT, PAWN_IDLE))
            return true;

        // fan out in all eight directions looking for a opponent king
        dr = NSEW_DIAG_DELTAS[0];
        dc = NSEW_DIAG_DELTAS[1];

        for (int i=0; i<8; i++) {
            if (testPiece(getPiece(rank +dr[i], col +dc[i]), playerNum, CHECKED_KING, UNCHECKED_KING, UNCHECKED_KING_IDLE))
                return true;
        }

        dr = NSEW_DELTAS[0];
        dc = NSEW_DELTAS[1];
        // search NSEW for rook, queen
        for (int i=0; i<4; i++) {
            for (int ii=1; ii<8; ii++) {
                Piece p = getPiece(rank +dr[i]*ii, col +dc[i]*ii);
                if (p.getType() == UNAVAILABLE)
                    break;
                if (testPiece(p, playerNum, ROOK, ROOK_IDLE, QUEEN))
                    return true;
                if (p.getType() != EMPTY)
                    break;
            }
        }

        dr = DIAGONAL_DELTAS[0];
        dc = DIAGONAL_DELTAS[1];
        // search DIAGonals for bishop, queen
        for (int i=0; i<4; i++) {
            for (int ii=1; ii<8; ii++) {
                Piece p = getPiece(rank +dr[i]*ii, col +dc[i]*ii);
                if (p.getType() == UNAVAILABLE)
                    break;
                if (testPiece(p, playerNum, BISHOP, QUEEN))
                    return true;
                if (p.getType() != EMPTY)
                    break;
            }
        }

        return false;
    }

    private void checkForCastle(int rank, int kingCol, int rookCol) {
        Piece king = getPiece(rank, kingCol);
        Piece rook = getPiece(rank, rookCol);
        if (king.getType() != UNCHECKED_KING_IDLE)
            return;
        if (rook.getType() != ROOK_IDLE)
            return;
        // check that there are no places in between king and rook and also none of the square is attacked
        int kingEndCol;
        int rookEndCol;
        int opponent = getOpponent(getTurn());
        if (rookCol > kingCol) {
            for (int i=kingCol+1; i<rookCol; i++) {
                if (getPiece(rank, i).getType() != EMPTY)
                    return;
                if (isSquareAttacked(rank, i, opponent))
                    return;
            }
            kingEndCol = kingCol+2;
            rookEndCol = kingEndCol-1;
        } else {
            // long side castle
            for (int i=rookCol+1; i<kingCol; i++) {
                if (getPiece(rank, i).getType() != EMPTY)
                    return;
                if (isSquareAttacked(rank, i, opponent))
                    return;
            }
            kingEndCol=kingCol-2;
            rookEndCol=kingEndCol+1;
        }
        king.addMove(new Move(MoveType.CASTLE, king.getPlayerNum()).setStart(rank, kingCol, UNCHECKED_KING_IDLE).setEnd(rank, kingEndCol, UNCHECKED_KING).setCastle(rank, rookCol, rank, rookEndCol));
    }

    @Override
    protected void computeMovesForSquare(int rank, int col, Move parent) {
        final Piece p = getPiece(rank, col);
        int tr, tc;
        Piece tp;
        final int opponent = getOpponent(p.getPlayerNum());
        int [] dr=null;
        int [] dc=null;
        int d = Math.max(RANKS, COLUMNS);
        MoveType mt = MoveType.SLIDE;
        PieceType nextType = p.getType();
        switch (p.getType()) {
            case PAWN_ENPASSANT:
                p.setType(PAWN);
            case PAWN_IDLE:
            case PAWN: {
                // check in front of us 1 space
                tr=rank + getAdvanceDir(p.getPlayerNum());
                tc=col;
                PieceType nextPawn = PAWN;
                if (tr == getStartRank(getOpponent(p.getPlayerNum())))
                    nextPawn = PAWN_TOSWAP;
                if (getPiece(tr, col).getType() == EMPTY) {
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextPawn));
                    if (p.getType() == PAWN_IDLE) {
                        int tr2 = rank + getAdvanceDir(p.getPlayerNum())*2;
                        // if we have not moved yet then we may be able move 2 squares
                        if (getPiece(tr2, col).getType() == EMPTY) {
                            p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr2, tc, PAWN_ENPASSANT));
                        }

                    }
                }
                // check if we can capture to upper right or upper left
                if ((tp=getPiece(tr, (tc=col+1))).getPlayerNum() == opponent) {
                    // if this opponent is the king, then we will be 'checking' him
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setCaptured(tr, tc, tp.getType()).setEnd(tr, tc, nextPawn));
                }
                // check if we can capture to upper right or upper left
                if ((tp=getPiece(tr, (tc=col-1))).getPlayerNum() == opponent) {
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setCaptured(tr, tc, tp.getType()).setEnd(tr, tc, nextPawn));
                }
                // check en passant
                tr = rank;
                if ((tp=getPiece(tr, (tc=col+1))).getPlayerNum() == opponent && tp.getType() == PAWN_ENPASSANT) {
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setCaptured(rank, tc, tp.getType()).setEnd(tr+getAdvanceDir(p.getPlayerNum()), tc, nextPawn));
                }
                if ((tp=getPiece(tr, (tc=col-1))).getPlayerNum() == opponent && tp.getType() == PAWN_ENPASSANT) {
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setCaptured(rank, tc, tp.getType()).setEnd(tr+getAdvanceDir(p.getPlayerNum()), tc, nextPawn));
                }
                break;
            }

            case PAWN_TOSWAP:
                for (PieceType np : Utils.toArray(ROOK, KNIGHT, BISHOP, QUEEN)) {
                    p.addMove(new Move(MoveType.SWAP, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(rank, col, np));
                }
                break;
            case BISHOP:
                dr = DIAGONAL_DELTAS[0];
                dc = DIAGONAL_DELTAS[1];
                break;
            case KNIGHT: {
                int [][] kd = getKnightDeltas(rank, col);
                dr = kd[0];//KNIGHT_DELTAS[0];
                dc = kd[1];//KNIGHT_DELTAS[1];
                d = 1;
                mt = MoveType.JUMP;
                break;
            }
            case ROOK_IDLE:
                nextType = ROOK;
            case ROOK:
                dr = NSEW_DELTAS[0];
                dc = NSEW_DELTAS[1];
                break;
            case CHECKED_KING_IDLE:
            case UNCHECKED_KING_IDLE:
                nextType = UNCHECKED_KING;
            case UNCHECKED_KING:
            case CHECKED_KING:
                checkForCastle(rank, col, 0);
                checkForCastle(rank, col, COLUMNS-1);
                d=1;
            case QUEEN:
                dr = NSEW_DIAG_DELTAS[0];
                dc = NSEW_DIAG_DELTAS[1];
                break;
            default:
                throw new AssertionError();
        }

        if (dr != null) {
            Utils.assertTrue(dr.length == dc.length);
            for (int i=0; i<dr.length; i++) {
                // search max d units in a specific direction
                for (int ii=1; ii<=d; ii++) {
                    tr=rank+dr[i]*ii;
                    tc=col +dc[i]*ii;
                    tp = getPiece(tr, tc);
                    if (tp.getPlayerNum() == opponent) { // look for capture
                        p.addMove(new Move(mt, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType).setCaptured(tr, tc, tp.getType()));
                        break; // can no longer search along this path
                    } else if (tp.getType() == EMPTY) { // look for open
                        p.addMove(new Move(mt, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType));
                    } else {
                        break; // can no longer search along this path
                    }
                }
            }
        }

        // now search moves and remove any that cause our king to be checked
        List<Move> moves = p.getMovesList();
        Iterator<Move> it = moves.iterator();
        while (it.hasNext()) {
            Move m = it.next();
            switch (m.getMoveType()) {
                case CASTLE:
                case SWAP:
                    continue;
            }
            if (!m.hasEnd())
                continue;
            movePiece(m);
            int [] kingPos = findPiecePosition(getTurn(), UNCHECKED_KING_IDLE, UNCHECKED_KING, CHECKED_KING, CHECKED_KING_IDLE);
            if (isSquareAttacked(kingPos[0], kingPos[1], opponent))
                it.remove();
            reverseMove(m);
        }
        getPiece(rank, col).setMovesList(moves);
    }

    public final static int [][] DIAGONAL_DELTAS = {
            {-1, -1, 1, 1},
            {-1, 1, -1, 1}
    };
    //public final static int [][] KNIGHT_DELTAS = {
    //        {-2, -2, -1, 1, 2,  2,  1, -1},
    //        {-1,  1,  2, 2, 1, -1, -2, -2}
    //};

    // precompute knight deltas for each square

    @Reflector.Omit
    private static int[][][][] knightDeltas = null;

    public final static int [][] ALL_KNIGHT_DELTAS = {
            {-2, -2, -1, 1, 2,  2,  1, -1},
            {-1,  1,  2, 2, 1, -1, -2, -2}
    };

    int [][] computeKnightDeltaFor(int rank, int col) {
        int [][] d = new int[2][8];
        int n = 0;
        for (int i=0; i<8; i++) {
            if (isOnBoard(rank+ALL_KNIGHT_DELTAS[0][i], col+ALL_KNIGHT_DELTAS[1][i])) {
                d[0][n] = ALL_KNIGHT_DELTAS[0][i];
                d[1][n] = ALL_KNIGHT_DELTAS[1][i];
                n++;
            }
        }
        if (n < 8) {
            int[] t = d[0];
            d[0] = new int[n];
            System.arraycopy(t, 0, d[0], 0, n);

            t = d[1];
            d[1] = new int[n];
            System.arraycopy(t, 0, d[1], 0, n);
        }

        return d;
    }

    private void buildKnightDeltas() {
        knightDeltas = new int[8][8][][];
        for (int i=0; i<8; i++) {
            for (int ii=0; ii<8; ii++) {
                knightDeltas[i][ii] = computeKnightDeltaFor(i, ii);
            }
        }
    }

    public int [][] getKnightDeltas(int rank, int col) {
        if (knightDeltas == null) {
            buildKnightDeltas();
        }
        return knightDeltas[rank][col];
    }

    public final static int [][] NSEW_DELTAS = {
            {1, 0, -1, 0},
            {0, 1, 0, -1}
    };

    public final static int [][] NSEW_DIAG_DELTAS = {
            {1, 0, -1, 0, -1, -1, 1, 1},
            {0, 1, 0, -1, -1, 1, -1, 1}
    };

    @Override
    public Color getPlayerColor(int side) {
        if (whiteSide == side)
            return Color.WHITE;
        return Color.BLACK;
    }
}
