package cc.android.checkerboard;

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
        clearMoves();
        undoStack.push(move);
        {
            Piece p;
            switch (move.type) {
                case JUMP:
                case SLIDE:
                    p = getPiece(move.startRank, move.startCol);
                    movePiece(move);
                    // check for pawn advancing
                    if (p.type == PAWN_IDLE) {
                        p.type = PAWN_ENPASSANT;
                    } else if (p.type == PAWN && move.endRank == getRankForKing(move.playerNum)) {
                        p.type = PAWN_TOSWAP;
                        computeMovesForSquare(move.endRank, move.endCol, null);
                        lock = p;
                    }
                    // see if this move result
                    break;
                case SWAP:
                    setPieceType(move.startRank, move.startCol, move.swapped);
                    break;
                default:
                    throw new AssertionError();
            }
        }
        // visit all enpassant pawns and make then normal pawns
        for (Piece p : getPieces(getCurPlayerNum())) {
            if (p.type == PAWN_ENPASSANT) {
                p.type = PAWN;
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

    private boolean testPiece(Piece p, int notPlayerNum, PieceType ... types) {
        for (PieceType t : types) {
            if (p.playerNum != notPlayerNum && p.type == t)
                return true;
        }
        return false;
    }

    final boolean isKingInCheck(int playerNum) {
        int [] position = findPiecePosition(playerNum, CHECKED_KING);
        if (position != null)
            return true;
        position = findPiecePosition(playerNum, UNCHECKED_KING);
        final int prank = position[0];
        final int pcol = position[1];
        // search in the eight directions and knights whom can
        int [] dr = KNIGHT_DELTAS[0];//{ -2, -2, -1, 1, 2,  2,  1, -1 };
        int [] dc = KNIGHT_DELTAS[1];//{ -1,  1,  2, 2, 1, -1, -2, -2 };

        for (int i=0; i<8; i++) {
            if (testPiece(getPiece(prank+dr[i], pcol+dc[i]), playerNum, KNIGHT)) {
                return true;
            }
        }

        // look for pawns
        if (testPiece(getPiece(prank+getForward(playerNum), pcol+1), playerNum, PAWN, PAWN_ENPASSANT, PAWN_IDLE))
            return true;
        if (testPiece(getPiece(prank+getForward(playerNum), pcol-1), playerNum, PAWN, PAWN_ENPASSANT, PAWN_IDLE))
            return true;

        // fan out in all eight directions looking for a opponent king
        dr = NSEW_DIAG_DELTAS[0];
        dc = NSEW_DIAG_DELTAS[1];

        for (int i=0; i<8; i++) {
            if (testPiece(getPiece(prank+dr[i], pcol+dc[i]), playerNum, CHECKED_KING, UNCHECKED_KING))
                return true;
        }

        dr = NSEW_DELTAS[0];
        dc = NSEW_DELTAS[1];
        // search NSEW for rook, queen
        for (int i=0; i<4; i++) {
            for (int ii=1; ii<8; ii++) {
                Piece p = getPiece(prank+dr[i]*ii, pcol+dc[i]*ii);
                if (testPiece(p, playerNum, ROOK, QUEEN))
                    return true;
            }
        }

        dr = DIAGONAL_DELTAS[0];
        dc = DIAGONAL_DELTAS[1];
        // search DIAGonals for bishop, queen
        for (int i=1; i<4; i++) {
            for (int ii=1; ii<8; ii++) {
                Piece p = getPiece(prank+dr[i]*ii, pcol+dc[i]*ii);
                if (testPiece(p, playerNum, BISHOP, QUEEN))
                    return true;
            }
        }

        return false;
    }

    private Move testCheckingOpponent(Move move) {
        movePiece(move);
        computeMovesForSquare(move.endRank, move.endCol, move);
        for (Move m : getPiece(move.endRank, move.endCol).moves) {
            if (m.captured != null && m.captured.type == UNCHECKED_KING) {
                m.willCheckOpponentKing = true;
                break;
            }
        }
        reverseMove(move);
        return move;
    }

    @Override
    protected void reverseMove(Move m) {
        super.reverseMove(m);
    }

    @Override
    protected void computeMovesForSquare(int rank, int col, Move parent) {
        Piece p = getPiece(rank, col);
        int tr, tc;
        Piece tp;
        final int opponent = getOpponent(p.playerNum);
        int [] dr=null;
        int [] dc=null;
        int d = 8;
        MoveType mt = MoveType.SLIDE;
        switch (p.type) {
            case PAWN_IDLE:
            case PAWN: {
                boolean skipIdle = true;
                // check in front of us 1 space
                tr=rank + p.getForward();
                tc=col;
                if (getPiece(tr, col).type == EMPTY) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, p.playerNum));
                    if (p.type == PAWN_IDLE) {
                        int tr2 = rank + p.getForward()*2;
                        // if we have not moved yet then we may be able move 2 squares
                        if (getPiece(tr2, col).type == EMPTY) {
                            p.moves.add(new Move(MoveType.SLIDE, rank, col, tr2, tc, p.playerNum));
                        }

                    }
                }
                // check if we can capture to upper right or upper left
                if ((tp=getPiece(tr, (tc=col+1))).playerNum == opponent) {
                    // if this opponent is the king, then we will be 'checking' him
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, tr, tc, p.playerNum, tp));
                }
                // check if we can capture to upper right or upper left
                if ((tp=getPiece(tr, (tc=col-1))).playerNum == opponent) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, tr, tc, p.playerNum, tp));
                }
                // check en passant
                tr = rank;
                if ((tp=getPiece(tr, (tc=col+1))).playerNum == opponent && tp.type == PAWN_ENPASSANT) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, -1, -1, p.playerNum, tp));
                }
                if ((tp=getPiece(tr, (tc=col-1))).playerNum == opponent && tp.type == PAWN_ENPASSANT) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, -1, -1, p.playerNum, tp));
                }
                break;
            }

            case PAWN_TOSWAP:
                for (PieceType np : Utils.toArray(ROOK, KNIGHT, BISHOP, QUEEN)) {
                    p.moves.add(new Move(MoveType.SWAP, rank, col, rank, col, -1, -1, p.playerNum, np));
                }
                break;
            case BISHOP:
                dr = DIAGONAL_DELTAS[0];//new int[] { -1, -1,  1,  1 };
                dc = DIAGONAL_DELTAS[1];//new int[] { -1,  1, -1,  1 };
                break;
            case KNIGHT:
                dr = KNIGHT_DELTAS[0];//new int[] { -2, -2, -1,  1, 2,  2,  1, -2 };
                dc = KNIGHT_DELTAS[1];//new int[] { -1,  1,  2,  2, 1, -1, -1, -2 };
                d=1;
                mt = MoveType.JUMP;
                break;
            case ROOK:
                dr = NSEW_DELTAS[0];//new int [] { 1, 0,-1, 0};
                dc = NSEW_DELTAS[1];//new int [] { 0, 1, 0,-1};
                break;
            case CHECKED_KING:
            case UNCHECKED_KING:
                d=1;
            case QUEEN:
                dr = NSEW_DIAG_DELTAS[0];//new int [] { 1, 0,-1, 0, -1, -1,  1,  1};
                dc = NSEW_DIAG_DELTAS[1];//new int [] { 0, 1, 0,-1, -1,  1, -1,  1};
                break;
            case KING:
            case EMPTY:
            case CHECKER:
            case UNAVAILABLE:
            case PAWN_ENPASSANT:
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
                        p.moves.add(new Move(mt, rank, col, tr, tc, tc, tc, p.playerNum, tp));
                        break;
                    } else if (tp.type == EMPTY) {
                        p.moves.add(new Move(mt, rank, col, tr, tc, p.playerNum));
                    } else {
                        break; // can no longer search along this path
                    }
                }
            }
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
        initRank(0, BLACK, ROOK, KNIGHT, BISHOP, QUEEN, UNCHECKED_KING, BISHOP, KNIGHT, ROOK);
        initRank(1, BLACK, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE);
        initRank(2, -1   , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(3, -1   , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(4, -1   , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(5, -1   , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(6, RED  , PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE);
        initRank(7, RED  , ROOK, KNIGHT, BISHOP, QUEEN, UNCHECKED_KING, BISHOP, KNIGHT, ROOK);
        super.newGame();
    }

}
