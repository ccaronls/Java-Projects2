package cc.android.checkerboard;

import java.util.Iterator;

import cc.lib.game.Utils;

import static cc.android.checkerboard.PieceType.*;

/**
 * Created by chriscaron on 10/10/17.
 */

public class Chess extends ACheckboardGame {

    public Chess() {
        super(8,8,2);
    }

    @Override
    public void executeMove(Move move) {
        // perform operation
        // visit all enpassant pawns and make them normal pawns
        //for (Piece p : getPieces(getTurn())) {
        //    if (p.type == PAWN_ENPASSANT) {
        //        p.type = PAWN;
        //    }
        //}
        clearMoves();
        undoStack.push(move);
        {
            Piece p;
            switch (move.type) {
                case JUMP:
                case SLIDE:
                    if (move.captured != null) {
                        clearPiece(move.getCaptured());//.captureRank, move.captureCol);
                    }
                    p = getPiece(move.getStart());//startRank, move.startCol);
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
                case CASTLE:
                    setBoard(move.getEnd(), getPiece(move.getStart()));
                    setBoard(move.getCastleRookEnd(), getPiece(move.getCastleRookStart()));
                    clearPiece(move.getStart());
                    clearPiece(move.getCastleRookStart());
                    break;
                default:
                    throw new AssertionError();
            }
        }

        if (lock == null) {
            nextTurn();
            if (computeMoves() == 0) {
                onGameOver();
            }
        }

        // check for game over
    }

    @Omit
    boolean isInCheck = false; //

    protected void onRecomputeMovesBegin() {
    }

    protected void onRecomputeMovesEnd() {
    }

    private boolean testPiece(Piece p, int playerNum, PieceType ... types) {
        for (PieceType t : types) {
            if (p.playerNum == playerNum && p.type == t)
                return true;
        }
        return false;
    }

    final boolean isSquareAttacked(int rank, int col, int playerNum) {
        // search in the eight directions and knights whom can
        int [] dr = KNIGHT_DELTAS[0];
        int [] dc = KNIGHT_DELTAS[1];

        for (int i=0; i<8; i++) {
            if (testPiece(getPiece(rank +dr[i], col +dc[i]), playerNum, KNIGHT)) {
                return true;
            }
        }

        // look for pawns
        if (testPiece(getPiece(rank +getForward(playerNum), col +1), playerNum, PAWN, PAWN_ENPASSANT, PAWN_IDLE))
            return true;
        if (testPiece(getPiece(rank +getForward(playerNum), col -1), playerNum, PAWN, PAWN_ENPASSANT, PAWN_IDLE))
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
/*
    private Move testCheckingOpponent(Move move) {
        movePiece(move);
        computeMovesForSquare(move.endRank, move.endCol, move);
        for (Move m : getPiece(move.endRank, move.endCol).moves) {
            if (m.captured != null && (m.captured.type == UNCHECKED_KING || m.captured.type == UNCHECKED_KING_IDLE)) {
                m.willCheckOpponentKing = true;
                break;
            }
        }
        reverseMove(move);
        return move;
    }
*/
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
            for (int i=kingCol+1; i<rookCol; i++)
                if (getPiece(rank, i).type != EMPTY)
                    return;
            // short side castle
            if (isSquareAttacked(rank, kingCol+1, opponent))
                return;
            if (isSquareAttacked(rank, kingCol+2, opponent))
                return;
            kingEndCol = kingCol+2;
            rookEndCol = kingCol-1;
        } else {
            // long side castle
            for (int i=0; i<kingCol; i++)
                if (getPiece(rank, i).type != EMPTY)
                    return;
            // short side castle
            if (isSquareAttacked(rank, kingCol-1, opponent))
                return;
            if (isSquareAttacked(rank, kingCol-2, opponent))
                return;
            kingEndCol=kingCol-2;
            rookEndCol=kingCol+1;
        }
        king.moves.add(new Move(MoveType.CASTLE, king.playerNum, null, null, rank, kingCol, rank, kingEndCol, rank, rookCol, rank, rookEndCol));
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
                tr=rank + p.getForward();
                tc=col;
                PieceType nextPawn = PAWN;
                if (tr == getRankForKing(getTurn()))
                    nextPawn = PAWN_TOSWAP;
                if (getPiece(tr, col).type == EMPTY) {
                    p.moves.add(new Move(MoveType.SLIDE, p.playerNum, null, nextPawn, rank, col, tr, tc));
                    if (p.type == PAWN_IDLE) {
                        int tr2 = rank + p.getForward()*2;
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
                    p.moves.add(new Move(MoveType.SLIDE, p.playerNum, tp, nextPawn, rank, col, tr+getForward(p.playerNum), tc, tr, tc));
                }
                if ((tp=getPiece(tr, (tc=col-1))).playerNum == opponent && tp.type == PAWN_ENPASSANT) {
                    p.moves.add(new Move(MoveType.SLIDE, p.playerNum, tp, nextPawn, rank, col, tr+getForward(p.playerNum), tc, tr, tc));
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
                checkForCastle(rank, col, 7);
                d=1;
                boolean attacked = isSquareAttacked(rank, col, opponent);
                switch (p.type) {
                    case CHECKED_KING:
                        if (!attacked)
                            p.type = UNCHECKED_KING;
                        break;
                    case CHECKED_KING_IDLE:
                        if (!attacked)
                            p.type = UNCHECKED_KING_IDLE;
                        break;
                    case UNCHECKED_KING:
                        if (attacked)
                            p.type = CHECKED_KING;
                        break;
                    case UNCHECKED_KING_IDLE:
                        if (attacked)
                            p.type = CHECKED_KING_IDLE;
                        break;
                }
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
            if (dr.length != dc.length)
                throw new AssertionError();
            for (int i=0; i<dr.length; i++) {
                // search max d units in a specific direction
                for (int ii=1; ii<=d; ii++) {
                    tr=rank+dr[i]*ii;
                    tc=col +dc[i]*ii;
                    tp = getPiece(tr, tc);
                    if (tp.playerNum == opponent) {
                        p.moves.add(new Move(mt, p.playerNum, tp, nextType, rank, col, tr, tc, tr, tc));
                        break; // can no longer search along this path
                    } else if (tp.type == EMPTY) {
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
            if (m.squares.length < 2)
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

    public void newGame() {
        initRank(0, BLACK, ROOK_IDLE, KNIGHT, BISHOP, QUEEN, UNCHECKED_KING_IDLE, BISHOP, KNIGHT, ROOK_IDLE);
        initRank(1, BLACK, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE);
        initRank(2, -1   , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(3, -1   , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(4, -1   , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(5, -1   , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(6, RED  , PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE);
        initRank(7, RED  , ROOK_IDLE, KNIGHT, BISHOP, QUEEN, UNCHECKED_KING_IDLE, BISHOP, KNIGHT, ROOK_IDLE);
        super.newGame();
    }

}
