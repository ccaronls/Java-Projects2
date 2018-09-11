package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.math.CMath;

import static cc.android.checkerboard.PieceType.CAPTURED_CHECKER;
import static cc.android.checkerboard.PieceType.CHECKER;
import static cc.android.checkerboard.PieceType.EMPTY;
import static cc.android.checkerboard.PieceType.FLYING_KING;
import static cc.android.checkerboard.PieceType.KING;

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
        if (p.playerNum != getTurn())
            throw new AssertionError();

        if (p.type == FLYING_KING) {
            final int [] dr =  { 1, 1, -1, -1 };
            final int [] dc =  { -1, 1, -1, 1 };
            computeFlyingKingMoves(p, rank, col, parent, dr, dc);
            return;
        }

		int [] dr, dc;
		if (p.type == KING) {
			dr = new int[] { 1, 1, -1, -1 };
			dc = new int[] { -1, 1, -1, 1 };
		} else if (p.playerNum == NEAR) {
			// negative
			dr = new int [] { -1, -1 };
			dc = new int [] { -1, 1 };
		} else { // red
			// positive
			dr = new int [] { 1, 1 };
			dc = new int [] { -1, 1 };
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
	}

	/*
	flying kings move any distance along unblocked diagonals, and may capture an opposing man any distance away
	by jumping to any of the unoccupied squares immediately beyond it.

	Since jumped pieces remain on the board until the turn is complete, it is possible to reach a position in a multi-jump move
	where the flying king is blocked from capturing further by a piece already jumped. (TODO)
	 */
    protected void computeFlyingKingMoves(Piece p, int rank, int col, Move parent, int [] dr, int [] dc) {
        final int d = Math.max(RANKS, COLUMNS);

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
                    if (t.type == EMPTY)
                        continue;

                    if (t.type == CAPTURED_CHECKER)
                        break; // cannot jump a piece we already did

                    if (t.playerNum == getOpponent()) {
                        captured = t;
                        ii++;
                        capturedRank=rdr;
                        capturedCol=cdc;
                        break;
                    }
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

                if (t.type == EMPTY) {
                    if (captured == null)
                        p.moves.add(new Move(mt, getTurn(), null, null, rank, col, rdr, cdc));
                    else
                        p.moves.add(new Move(mt, getTurn(), captured, null, rank, col, rdr, cdc, capturedRank, capturedCol));

                    continue;
                }

                if (mt != MoveType.SLIDE)
                    break;

                if (t.playerNum != getOpponent())
                    break;

                mt = MoveType.FLYING_JUMP;
                captured = t;
                capturedRank = rdr;
                capturedCol = cdc;

            }
        }


    }

    public void endTurn() {
        if (lock != null) {
            for (Move m : lock.moves) {
                if (m.type == MoveType.END) {
                    undoStack.push(m);
                    break;
                }
            }
        }
        endTurnPrivate();
    }

    private void endTurnPrivate() {
        removeCapturedPieces();
        nextTurn();
        lock = null;
        clearMoves();
        if (computeMoves()==0) {
            onGameOver();
        }
	}

	private void removeCapturedPieces() {
        List<int[]> captured = new ArrayList<>();
        for (int i=0; i<RANKS; i++) {
            for (int ii=0; ii<COLUMNS; ii++) {
                Piece p = getPiece(i, ii);
                if (p.type == CAPTURED_CHECKER) {
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
        lock = null;
		boolean isKinged = false;
		final Piece p = getPiece(move.getStart());
        // clear everyone all moves
        clearMoves();
		if (move.hasEnd()) {
            int rank = move.getEnd()[0];
            isKinged = (p.type == CHECKER && getStartRank(getOpponent()) == rank);
            movePiece(move);
		}

        undoStack.push(move);

        switch (move.type) {
            case SLIDE:
                if (isKinged) {
                    p.moves.add(new Move(MoveType.STACK, move.playerNum, null, isFlyingKings() ? PieceType.FLYING_KING : PieceType.KING, move.getEnd()));
                    lock = p;
                    break;
                }
            case END:
                endTurnPrivate();
                return;
            case JUMP:
                if (move.captured != null) {
                    clearPiece(move.getCaptured());
                }
                if (isKinged) {
                    p.moves.add(new Move(MoveType.STACK, move.playerNum, null, isFlyingKings() ? PieceType.FLYING_KING : PieceType.KING, move.getEnd()));
                    lock = p;
                }
                break;
            case FLYING_JUMP:
                setPieceType(move.getCaptured(), CAPTURED_CHECKER);
                break;
            case STACK:
                move.nextType = p.type;
                setPieceType(move.getStart(), isFlyingKings() ? PieceType.FLYING_KING : KING);
                break;
        }

        if (!isKinged) {
            // recursive compute next move if possible after a jump
            if (move.hasEnd())
                computeMovesForSquare(move.getEnd()[0], move.getEnd()[1], move);
            if (p.moves.size() == 0) {
                endTurnPrivate();
            } else if (!isJumpsMandatory()) {
                p.moves.add(new Move(MoveType.END, move.playerNum, null, null, move.getEnd()));
                lock = p;
            }
        }
	}

    @Override
    public Color getPlayerColor(int side) {
        switch (side) {
            case FAR:
                return Color.RED;
            case NEAR:
                return Color.BLACK;
        }
        Utils.assertTrue(false);
        return null;
    }

    protected boolean isFlyingKings() {
        return false;
    }

    protected boolean canJumpSelf() {
        return true;
    }

    protected boolean canMenJumpBackwards() {
        return false; // true for international/russian draughts
    }

}
