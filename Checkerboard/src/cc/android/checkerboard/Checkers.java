package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.math.CMath;

import static cc.android.checkerboard.PieceType.CHECKER;
import static cc.android.checkerboard.PieceType.DAMA_KING;
import static cc.android.checkerboard.PieceType.DAMA_MAN;
import static cc.android.checkerboard.PieceType.EMPTY;
import static cc.android.checkerboard.PieceType.FLYING_KING;

/**
 * Red is positive and black is negative
 * @author chriscaron
 *
 */
public class Checkers extends ACheckboardGame  {

    static {
        addAllFields(Checkers.class);
    }

    protected Checkers(int ranks, int cols) {
        super(ranks, cols, 2);
    }

    public Checkers() {
        super(8,8,2);
    }

	public void initBoard() {
        initRank(0, FAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(1, FAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        initRank(2, FAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(3, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(4, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(5, NEAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        initRank(6, NEAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(7, NEAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        setTurn(Utils.flipCoin() ? FAR : NEAR);
	}

    @Override
	protected void computeMovesForSquare(int rank, int col, Move parent) {
        Piece p = getPiece(rank, col);
        if (p.getPlayerNum() != getTurn())
            throw new AssertionError();

        if (p.getType()== FLYING_KING || p.getType()== DAMA_KING) {
            computeFlyingKingMoves(p, rank, col, parent);
        } else {
            computeMenKingMoves(p, rank, col, parent);
        }
    }

    protected void computeMenKingMoves(Piece p, int rank, int col, Move parent) {

        int [] jdr=null, jdc=null, dr=null, dc=null;
        switch (p.getType()) {
            case KING:
                jdr = dr = new int[] { 1, 1, -1, -1 };
                jdc = dc = new int[] { -1, 1, -1, 1 };
                break;
            case CHECKER:
                if (p.getPlayerNum() == NEAR) {
                    // negative
                    dr = new int [] { -1, -1 };
                    dc = new int [] { -1, 1 };
                } else { // red
                    // positive
                    dr = new int [] { 1, 1 };
                    dc = new int [] { -1, 1 };
                }
                if (canMenJumpBackwards()) {
                    jdr = new int[]{1, 1, -1, -1};
                    jdc = new int[]{-1, 1, -1, 1};
                } else {
                    jdr = dr;
                    jdc = dc;
                }
                break;
            case DAMA_MAN:
                if (p.getPlayerNum() == NEAR) {
                    // negative
                    dr = new int [] { -1, 0, 0 };
                    dc = new int [] {  0, 1, -1 };
                } else { // red
                    // positive
                    dr = new int [] { 1, 0, 0 };
                    dc = new int [] { 0, 1, -1 };
                }
                if (canMenJumpBackwards()) {
                    jdr = new int[] { 1, -1, 0, 0};
                    jdc = new int[] { 0,  0, -1, 1};
                } else {
                    jdr = dr;
                    jdc = dc;
                }
                break;
            case DAMA_KING:
                jdr = dr = new int[] { 1, -1, 0, 0};
                jdc = dc = new int[] { 0,  0, -1, 1};
                break;
            default:
                Utils.unhandledCase(p.getType());
        }

        // check for jumps
        for (int i=0; i<jdr.length; i++) {
            final int rdr  = rank+jdr[i];
            final int cdc  = col +jdc[i];
            final int rdr2 = rank+jdr[i]*2;
            final int cdc2 = col +jdc[i]*2;

            if (!isOnBoard(rdr, cdc))
                continue;

            if (!isOnBoard(rdr2, cdc2))
                continue;

            if (parent != null) {
                if (rdr2 == parent.getStart()[0] && cdc2 == parent.getStart()[1])
                    continue; // cannot make 180 degree turns
            }

            Piece cap = getPiece(rdr, cdc);
            Piece t = getPiece(rdr2, cdc2);
            if (t.getType() != EMPTY)
                continue;

            if (canJumpSelf() && cap.getPlayerNum() == getTurn()) {
                p.addMove(new Move(MoveType.JUMP, getTurn()).setStart(rank, col, p.getType()).setEnd(rdr2, cdc2, p.getType()));
            } else if (!cap.isCaptured() && cap.getPlayerNum() == getOpponent()) {
                p.addMove(new Move(MoveType.JUMP, getTurn()).setStart(rank, col, p.getType()).setEnd(rdr2, cdc2, p.getType()).setCaptured(rdr, cdc, cap.getType()));
            }

        }

        // check for slides
        if (parent == null && !(isJumpsMandatory() && p.getNumMoves()>0)) {
            for (int i = 0; i < dr.length; i++) {
                final int rdr = rank + dr[i];
                final int cdc = col + dc[i];

                if (!isOnBoard(rdr, cdc))
                    continue;
                // t is piece one unit away in this direction
                Piece t = getPiece(rdr, cdc);
                if (t.getType() == EMPTY) {
                    p.addMove(new Move(MoveType.SLIDE, getTurn()).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType()));
                    //new Move(MoveType.SLIDE, rank, col, rdr, cdc, getTurn()));
                }
            }
        }
	}

	/*
	flying kings move any distance along unblocked diagonals, and may capture an opposing man any distance away
	by jumping to any of the unoccupied squares immediately beyond it.

	Since jumped pieces remain on the board until the turn is complete, it is possible to reach a position in a multi-jump move
	where the flying king is blocked from capturing further by a piece already jumped. (TODO)
	 */
    protected void computeFlyingKingMoves(Piece p, int rank, int col, Move parent) {
        final int d = Math.max(RANKS, COLUMNS);

        int [] dr, dc;
        switch (p.getType()) {
            case FLYING_KING:
                dr = new int[] { 1, 1, -1, -1 };
                dc = new int[] { -1, 1, -1, 1 };
                break;
            case DAMA_KING:
                dr = new int[] { 1, -1, 0, 0 };
                dc = new int[] { 0,  0, -1, 1 };
                break;
            default:
                throw new AssertionError("Unhandled case");
        }

        for (int i=0; i<4; i++) {
            MoveType mt = MoveType.SLIDE;
            Piece captured = null;
            int capturedRank=0;
            int capturedCol =0;

            int ii=1;
            if (parent != null) {
                // we are in a multijump, so search forward for the piece to capture
                int ddr = CMath.signOf(parent.getStart()[0] - rank);
                int ddc = CMath.signOf(parent.getStart()[1] - col);

                if (ddr == dr[i] && ddc == dc[i])
                    continue; // cannot go backwards

                for ( ; ii<=d; ii++) {
                    // square we are moving too
                    final int rdr = rank+dr[i]*ii;
                    final int cdc = col+dc[i]*ii;

                    if (!isOnBoard(rdr, cdc))
                        break;

                    Piece t = getPiece(rdr, cdc);
                    if (t.getType() == EMPTY)
                        continue;

                    if (t.isCaptured())
                        break; // cannot jump a piece we already did

                    if (t.getPlayerNum() == getOpponent()) {
                        captured = t;
                        ii++;
                        capturedRank=rdr;
                        capturedCol=cdc;
                    }
                        break;
                    }

                if (captured == null)
                    continue;

                mt = MoveType.FLYING_JUMP;
            }

            for ( ; ii<d; ii++) {

                // square we are moving too
                final int rdr = rank+dr[i]*ii;
                final int cdc = col+dc[i]*ii;

                if (!isOnBoard(rdr, cdc))
                    break;

                // t is piece one unit away in this direction
                Piece t = getPiece(rdr, cdc);

                if (t.getType() == EMPTY) {
                    if (captured == null)
                        p.addMove(new Move(mt, getTurn()).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType()));
                    else
                        p.addMove(new Move(mt, getTurn()).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType()).setCaptured(capturedRank, capturedCol, captured.getType()));

                    continue;
                }

                if (mt != MoveType.SLIDE)
                    break;

                if (t.getPlayerNum() != getOpponent())
                    break;

                mt = MoveType.FLYING_JUMP;
                captured = t;
                capturedRank = rdr;
                capturedCol = cdc;

            }
        }


    }

	protected void removeCapturedPieces() {
        List<int[]> captured = new ArrayList<>();
        for (int i=0; i<RANKS; i++) {
            for (int ii=0; ii<COLUMNS; ii++) {
                Piece p = getPiece(i, ii);
                if (p.isCaptured()) {
                    captured.add(new int[] { i, ii });
                }
            }
        }
        if (captured.size() > 0) {
            onPiecesCaptured(captured);
            for (int[] pos : captured) {
                clearPiece(pos);
            }
        }
    }

    protected void onPiecesCaptured(List<int[]> pieces) {}

    @Override
	public void executeMove(Move move) {
        if (move.getPlayerNum() != getTurn())
            throw new AssertionError();
        lock = null;
		boolean isKinged = false;
		boolean isDamaKing = false;
		Piece p = getPiece(move.getStart());
        // clear everyone all moves
        clearMoves();
		if (move.hasEnd()) {
            int rank = move.getEnd()[0];
            if (move.getMoveType() != MoveType.STACK){
                isKinged = (p.getType() == CHECKER && getStartRank(getOpponent()) == rank);
                isDamaKing = (p.getType() == DAMA_MAN && getStartRank(getOpponent()) == rank);
            }
            p = movePiece(move);
		}

        undoStack.push(move);
        switch (move.getMoveType()) {
            case SLIDE:
                if (isKinged) {
                    p.addMove(new Move(MoveType.STACK, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()).setEnd(move.getEnd()[0], move.getEnd()[1], isFlyingKings() ? PieceType.FLYING_KING : PieceType.KING));
                    lock = p;
                    break;
                }
                if (isDamaKing) {
                    p.addMove(new Move(MoveType.STACK, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()).setEnd(move.getEnd()[0], move.getEnd()[1],  PieceType.DAMA_KING));
                    lock = p;
                    break;
                }
            case END:
                endTurnPrivate();
                return;
            case FLYING_JUMP:
            case JUMP:
                if (move.getCaptured() != null) {
                    if (isCaptureAtEndEnabled()) {
                        getPiece(move.getCaptured()).setCaptured(true);
                    } else {
                        clearPiece(move.getCaptured());
                    }
                }
                if (isKinged) {
                    p.addMove(new Move(MoveType.STACK, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()).setEnd(move.getEnd()[0], move.getEnd()[1], isFlyingKings() ? PieceType.FLYING_KING : PieceType.KING));
                    lock = p;
                }
                if (isDamaKing) {
                    p.addMove(new Move(MoveType.STACK, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()).setEnd(move.getEnd()[0], move.getEnd()[1], PieceType.DAMA_KING));
                    lock = p;
                }
                break;
            case STACK:
                setPiece(move.getStart(), move.getPlayerNum(), move.getEndType());
                break;
        }

        if (!isKinged && !isDamaKing) {
            // recursive compute next move if possible after a jump
            if (move.hasEnd())
                computeMovesForSquare(move.getEnd()[0], move.getEnd()[1], move);
            if (p.getNumMoves() == 0) {
                endTurnPrivate();
            } else if (!isJumpsMandatory()) {
                p.addMove(new Move(MoveType.END, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()));
                lock = p;
            }
        }
	}

    @Override
    protected int recomputeMoves() {
        int num = super.recomputeMoves();
        if (isJumpsMandatory()) {
            boolean hasJumps = false;
            for (Move m : getMoves()) {
                if (m.getMoveType() == MoveType.JUMP || m.getMoveType() == MoveType.FLYING_JUMP) {
                    hasJumps = true;
                    break;
                }
            }
            if (hasJumps) {
                for (int rank = 0; rank < RANKS; rank++) {
                    for (int col = 0; col < COLUMNS; col++) {
                        num -= getPiece(rank, col).removeNonJumps();
                    }
                }
            }
        }
        return num;
    }

    @Override
    protected void endTurnPrivate() {
        removeCapturedPieces();
        super.endTurnPrivate();
    }

    @Override
    public Color getPlayerColor(int side) {
        switch (side) {
            case FAR:
                return Color.RED;
            case NEAR:
                return Color.BLACK;
        }
//        Utils.assertTrue(false);
        return Color.WHITE;
    }

}
