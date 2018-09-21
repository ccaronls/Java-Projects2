package cc.android.checkerboard;

import cc.lib.game.Utils;

import static cc.android.checkerboard.PieceType.DAMA_MAN;
import static cc.android.checkerboard.PieceType.EMPTY;

public class Dama extends Checkers {

    public final void initBoard() {
        initRank(0, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(1, FAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        initRank(2, FAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        initRank(3, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(4, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(5, NEAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        initRank(6, NEAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        initRank(7, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        setTurn(Utils.flipCoin() ? FAR : NEAR);
    }
/*
    @Override
    protected void computeMovesForSquare(int rank, int col, Move parent) {
        Piece p = getPiece(rank, col);
        if (p.playerNum != getTurn())
            throw new AssertionError();

        if (p.type == DAMA_KING) {
            //final int [] dr =  { 1, 0, -1,  0 };
            //final int [] dc =  { 0, 1,  0, -1 };
            computeFlyingKingMoves(p, rank, col, parent);
            //computeFlyingKingMoves(p, rank, col, parent, dr, dc);
            return;
        }

        int [] dr, dc;
        if (p.playerNum == NEAR) {
            // negative
            dr = new int [] { -1, 0, 0 };
            dc = new int [] {  0, 1, -1 };
        } else { // red
            // positive
            dr = new int [] {  1, 0, 0 };
            dc = new int [] {  0, 1, -1 };
        }

        for (int i=0; i<dr.length; i++) {
            final int rdr = rank+dr[i];
            final int cdc = col+dc[i];
            final int rdr2 = rank+dr[i]*2;
            final int cdc2 = col+dc[i]*2;

            if (!isOnBoard(rdr, cdc))
                continue;
            // t is piece one unit away in this direction
            Piece t = getPiece(rdr, cdc);
            if (t.type == EMPTY) {
                if (parent == null)
                    p.moves.add(new Move(MoveType.SLIDE, getTurn(), null, null, rank, col, rdr, cdc));
                //new Move(MoveType.SLIDE, rank, col, rdr, cdc, getTurn()));
            } else {
                // check for jump
                if (isOnBoard(rdr2, cdc2)) {
                    if (parent != null && parent.getStart()[1] == cdc2 && parent.getStart()[0] == rdr2) {
                        continue; // dont allow to jump back to a place we just came from
                    }
                    Piece j = getPiece(rdr2, cdc2);
                    if (j.type == EMPTY) {
                        // we can jump to here
                        if (t.playerNum == getTurn()) {
                            // we are jumping ourself, no capture
                            if (canJumpSelf())
                                p.moves.add(new Move(MoveType.JUMP, getTurn(), null, null, rank, col, rdr2, cdc2));
                        } else {
                            // jump with capture
                            p.moves.add(new Move(MoveType.JUMP, getTurn(), t, null, rank, col, rdr2, cdc2, rdr, cdc));
                        }
                    }
                }
            }
        }
    }*/


    @Override
    protected boolean isJumpsMandatory() {
        return true;
    }

    @Override
    protected boolean canMenJumpBackwards() {
        return true;
    }

    @Override
    protected boolean canJumpSelf() {
        return false;
    }

    @Override
    public BoardType getBoardType() {
        return BoardType.DAMA;
    }
}
