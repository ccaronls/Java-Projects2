package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import cc.lib.game.IGame;
import cc.lib.game.IMove;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

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
        undoStack.push(move);
        Piece p;
        switch (move.type) {
            case JUMP:
            case SLIDE:
                p = getPiece(move.startRank, move.startCol);
                board[move.endRank][move.endCol] = p;
                board[move.startRank][move.startCol] = new Piece();
                // check for pawn advancing
                if (p.type == PieceType.PAWN_IDLE) {
                    p.type = PieceType.PAWN_ENPASSANT;
                }
                break;
            case SWAP:
                board[move.startRank][move.startCol].type = move.swapped;
                break;
            default:
                throw new AssertionError();
        }

        // visit all enpassant pawns and make then normal pawns

        // check for game over
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
                tr=rank + p.getForward()*2;
                tc=col;
                if (getPiece(tr, col).type == PieceType.EMPTY) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, p.playerNum));
                }
            case PAWN: {
                // check in front of us 1 space
                tr=rank + p.getForward();
                tc=col;
                if (getPiece(tr, col).type == PieceType.EMPTY) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, p.playerNum));
                }
                // check if we can capture to upper right or upper left
                if ((tp=getPiece(tr, (tc=col+1))).playerNum == opponent) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, tr, tc, p.playerNum, tp));
                }
                // check if we can capture to upper right or upper left
                if ((tp=getPiece(tr, (tc=col-1))).playerNum == opponent) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, tr, tc, p.playerNum, tp));
                }
                // see if we can do 2 spaces

                // check en passant
                tr = rank;
                if ((tp=getPiece(tr, (tc=col+1))).playerNum == opponent && tp.type == PieceType.PAWN_ENPASSANT) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, -1, -1, p.playerNum, tp));
                }
                if ((tp=getPiece(tr, (tc=col-1))).playerNum == opponent && tp.type == PieceType.PAWN_ENPASSANT) {
                    p.moves.add(new Move(MoveType.SLIDE, rank, col, tr, tc, -1, -1, p.playerNum, tp));
                }
                break;
            }

            case PAWN_TOSWAP:
                for (PieceType np : Utils.toArray(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN)) {
                    p.moves.add(new Move(MoveType.SWAP, rank, col, rank, col, -1, -1, p.playerNum, np));
                }
                break;
            case BISHOP:
                dr = new int[] { -1, -1,  1,  1 };
                dc = new int[] { -1,  1, -1,  1 };
                break;
            case KNIGHT:
                dr = new int[] { -2, -2, -1,  1, 2,  2,  1, -2 };
                dc = new int[] { -1,  1,  2,  2, 1, -1, -1, -2 };
                d=1;
                mt = MoveType.JUMP;
                break;
            case ROOK:
                dr = new int [] { 1, 0,-1, 0};
                dc = new int [] { 0, 1, 0,-1};
                break;
            case CHECKED_KING:
            case UNCHECKED_KING:
                d=1;
            case QUEEN:
                dr = new int [] { 1, 0,-1, 0, -1, -1,  1,  1};
                dc = new int [] { 0, 1, 0,-1, -1,  1, -1,  1};
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
                    } else if (tp.type == PieceType.EMPTY) {
                        p.moves.add(new Move(mt, rank, col, tr, tc, p.playerNum));
                    } else {
                        break; // can no longer search along this path
                    }
                }
            }
        }
    }

    @Override
    protected void reverseMove(Move m) {

    }

    public void newGame() {
        super.newGame();
        setRank(0, BLACK, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.UNCHECKED_KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK);
        setRank(1, BLACK, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN);
        setRank(2, -1   , PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY);
        setRank(3, -1   , PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY);
        setRank(4, -1   , PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY);
        setRank(5, -1   , PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY);
        setRank(6, BLACK, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN);
        setRank(7, RED,   PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.UNCHECKED_KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK);
    }

    private void setRank(int rank, int player, PieceType...pieces) {
        for (int i=0; i<8; i++) {
            board[rank][i].type = pieces[i];
            board[rank][i].playerNum = player;
        }
    }
}
