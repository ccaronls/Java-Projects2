package cc.android.checkerboard;

import java.util.Iterator;

import cc.lib.game.Utils;

import static cc.android.checkerboard.PieceType.*;

/**
 * Created by chriscaron on 10/10/17.
 */

public class Chess extends ACheckboardGame {

    static {
        addAllFields(Chess.class);
    }

    private int whiteSide = -1;

    public Chess() {
        super(8,8,2);
    }

    public void newGame() {
        super.newGame();
        whiteSide = Utils.flipCoin() ? FAR : NEAR;
        setTurn(whiteSide);
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
        computeMoves();
    }

    @Override
    public void executeMove(Move move) {
        lock = null;
        clearMoves();
        undoStack.push(move);
        {
            Piece p;
            switch (move.type) {
                case JUMP:
                case SLIDE:
                    if (move.captured != null) {
                        clearPiece(move.getCaptured());
                    }
                    p = getPiece(move.getStart());
                    movePiece(move);
                    // check for pawn advancing
                    if (p.type == PAWN_TOSWAP) {
                        computeMovesForSquare(move.getEnd()[0], move.getEnd()[1], null);
                        lock = p;
                    }
                    // see if this move result
                    break;
                case SWAP:
                    setPieceType(move.getStart(), move.nextType);
                    break;
                case CASTLE: {
                    movePiece(move);
                    p = getPiece(move.getCastleRookStart());
                    Utils.assertTrue(p.type == ROOK_IDLE);
                    p.type = ROOK;
                    setBoard(move.getCastleRookEnd(), p);
                    clearPiece(move.getCastleRookStart());
                    break;
                }
                default:
                    throw new AssertionError();
            }
            updateOpponentKingCheckedState();
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
        switch (king.type) {
            case CHECKED_KING_IDLE:
                if (!checked)
                    king.type = UNCHECKED_KING_IDLE;
                break;
            case CHECKED_KING:
                if (!checked)
                    king.type = UNCHECKED_KING;
                break;
            case UNCHECKED_KING_IDLE:
                if (checked)
                    king.type = CHECKED_KING_IDLE;
                break;
            case UNCHECKED_KING:
                if (checked)
                    king.type = CHECKED_KING;
                break;
        }

    }

    @Override
    protected void reverseMove(Move m, boolean recompute) {
        super.reverseMove(m, recompute);
        updateOpponentKingCheckedState();
    }

    // Return true if p.type is on set of types and p.playerNum equals playerNum
    private boolean testPiece(Piece p, int playerNum, PieceType ... types) {
        for (PieceType t : types) {
            if (p.playerNum == playerNum && p.type == t)
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
        // search in the eight directions and knights whom can
        int [] dr = KNIGHT_DELTAS[0];
        int [] dc = KNIGHT_DELTAS[1];

        for (int i=0; i<8; i++) {
            if (testPiece(getPiece(rank +dr[i], col +dc[i]), playerNum, KNIGHT)) {
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
                if (testPiece(p, playerNum, ROOK, ROOK_IDLE, QUEEN))
                    return true;
                if (p.type != EMPTY)
                    break;
            }
        }

        dr = DIAGONAL_DELTAS[0];
        dc = DIAGONAL_DELTAS[1];
        // search DIAGonals for bishop, queen
        for (int i=0; i<4; i++) {
            for (int ii=1; ii<8; ii++) {
                Piece p = getPiece(rank +dr[i]*ii, col +dc[i]*ii);
                if (testPiece(p, playerNum, BISHOP, QUEEN))
                    return true;
                if (p.type != EMPTY)
                    break;
            }
        }

        return false;
    }

    private void checkForCastle(int rank, int kingCol, int rookCol) {
        Piece king = getPiece(rank, kingCol);
        Piece rook = getPiece(rank, rookCol);
        if (king.type != UNCHECKED_KING_IDLE)
            return;
        if (rook.type != ROOK_IDLE)
            return;
        // check that there are no places in between king and rook and also none of the square is attacked
        int kingEndCol;
        int rookEndCol;
        int opponent = getOpponent(getTurn());
        if (rookCol > kingCol) {
            for (int i=kingCol+1; i<rookCol; i++) {
                if (getPiece(rank, i).type != EMPTY)
                    return;
                if (isSquareAttacked(rank, i, opponent))
                    return;
            }
            kingEndCol = kingCol+2;
            rookEndCol = kingEndCol-1;
        } else {
            // long side castle
            for (int i=rookCol+1; i<kingCol; i++) {
                if (getPiece(rank, i).type != EMPTY)
                    return;
                if (isSquareAttacked(rank, i, opponent))
                    return;
            }
            kingEndCol=kingCol-2;
            rookEndCol=kingEndCol+1;
        }
        king.moves.add(new Move(MoveType.CASTLE, king.playerNum, null, UNCHECKED_KING, rank, kingCol, rank, kingEndCol, rank, rookCol, rank, rookEndCol));
    }

    @Override
    protected void computeMovesForSquare(int rank, int col, Move parent) {
        final Piece p = getPiece(rank, col);
        int tr, tc;
        Piece tp;
        final int opponent = getOpponent(p.playerNum);
        int [] dr=null;
        int [] dc=null;
        int d = 8;
        MoveType mt = MoveType.SLIDE;
        PieceType nextType = null;
        switch (p.type) {
            case PAWN_ENPASSANT:
                p.type = PAWN;
            case PAWN_IDLE:
            case PAWN: {
                // check in front of us 1 space
                tr=rank + getAdvanceDir(p.playerNum);
                tc=col;
                PieceType nextPawn = PAWN;
                if (tr == getStartRank(getOpponent(p.playerNum)))
                    nextPawn = PAWN_TOSWAP;
                if (getPiece(tr, col).type == EMPTY) {
                    p.moves.add(new Move(MoveType.SLIDE, p.playerNum, null, nextPawn, rank, col, tr, tc));
                    if (p.type == PAWN_IDLE) {
                        int tr2 = rank + getAdvanceDir(p.playerNum)*2;
                        // if we have not moved yet then we may be able move 2 squares
                        if (getPiece(tr2, col).type == EMPTY) {
                            p.moves.add(new Move(MoveType.SLIDE, p.playerNum, null, PAWN_ENPASSANT, rank, col, tr2, tc));
                        }

                    }
                }
                // check if we can capture to upper right or upper left
                if ((tp=getPiece(tr, (tc=col+1))).playerNum == opponent) {
                    // if this opponent is the king, then we will be 'checking' him
                    p.moves.add(new Move(MoveType.SLIDE, p.playerNum, tp, nextPawn, rank, col, tr, tc, tr, tc));
                }
                // check if we can capture to upper right or upper left
                if ((tp=getPiece(tr, (tc=col-1))).playerNum == opponent) {
                    p.moves.add(new Move(MoveType.SLIDE, p.playerNum, tp, nextPawn, rank, col, tr, tc, tr, tc));
                }
                // check en passant
                tr = rank;
                if ((tp=getPiece(tr, (tc=col+1))).playerNum == opponent && tp.type == PAWN_ENPASSANT) {
                    p.moves.add(new Move(MoveType.SLIDE, p.playerNum, tp, nextPawn, rank, col, tr+getAdvanceDir(p.playerNum), tc, tr, tc));
                }
                if ((tp=getPiece(tr, (tc=col-1))).playerNum == opponent && tp.type == PAWN_ENPASSANT) {
                    p.moves.add(new Move(MoveType.SLIDE, p.playerNum, tp, nextPawn, rank, col, tr+getAdvanceDir(p.playerNum), tc, tr, tc));
                }
                break;
            }

            case PAWN_TOSWAP:
                for (PieceType np : Utils.toArray(ROOK, KNIGHT, BISHOP, QUEEN)) {
                    p.moves.add(new Move(MoveType.SWAP, p.playerNum, null, np, rank, col));
                }
                break;
            case BISHOP:
                dr = DIAGONAL_DELTAS[0];
                dc = DIAGONAL_DELTAS[1];
                break;
            case KNIGHT:
                dr = KNIGHT_DELTAS[0];
                dc = KNIGHT_DELTAS[1];
                d=1;
                mt = MoveType.JUMP;
                break;
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
            case KING:
            case EMPTY:
            case CHECKER:
            case UNAVAILABLE:
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
                    if (tp.playerNum == opponent) { // look for capture
                        p.moves.add(new Move(mt, p.playerNum, tp, nextType, rank, col, tr, tc, tr, tc));
                        break; // can no longer search along this path
                    } else if (tp.type == EMPTY) { // look for open
                        p.moves.add(new Move(mt, p.playerNum, null, nextType, rank, col, tr, tc));
                    } else {
                        break; // can no longer search along this path
                    }
                }
            }
        }

        // now search moves and remove any that cause our king to be checked
        Iterator<Move> it = p.moves.iterator();
        while (it.hasNext()) {
            Move m = it.next();
            switch (m.type) {
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
    }

    public final static int [][] DIAGONAL_DELTAS = {
            {-1, -1, 1, 1},
            {-1, 1, -1, 1}
    };
    public final static int [][] KNIGHT_DELTAS = {
            {-2, -2, -1, 1, 2,  2,  1, -1},
            {-1,  1,  2, 2, 1, -1, -2, -2}
    };

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