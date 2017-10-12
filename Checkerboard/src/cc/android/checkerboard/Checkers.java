package cc.android.checkerboard;

import static cc.android.checkerboard.PieceType.*;

/**
 * Red is positive and black is negative
 * @author chriscaron
 *
 */
public class Checkers extends ACheckboardGame  {

    static {
        addAllFields(Checkers.class);
    }

    public Checkers() {
        super(8,8,2);
    }

	public final void newGame() {
        initRank(0, RED, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(1, RED, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        initRank(2, RED, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(3, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(4, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(5, BLACK, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        initRank(6, BLACK, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(7, BLACK, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);

		super.newGame();
	}

    @Override
    protected final int getMinVersion() {
        return 1;
    }

    @Override
	protected void computeMovesForSquare(int rank, int col, Move parent) {
		Piece p = getPiece(rank, col);
        if (p.playerNum != getCurPlayerNum())
            throw new AssertionError();

		int [] dr, dc;
		if (p.type == KING) {
			dr = new int[] { 1, 1, -1, -1 };
			dc = new int[] { -1, 1, -1, 1 };
		} else if (p.playerNum == BLACK) {
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
					p.moves.add(new Move(MoveType.SLIDE, rank, col, rdr, cdc, getTurn()));
			} else {
				// check for jump
				if (isOnBoard(rdr2, cdc2)) {
                    if (parent != null && parent.startCol == cdc2 && parent.startRank == rdr2) {
                        continue; // dont allow to jump back to a place we just came from
                    }
					Piece j = getPiece(rdr2, cdc2);
					if (j.type == EMPTY) {
						// we can jump to here
						if (t.playerNum == getCurPlayerNum()) {
							// we are jumping ourself, no capture
							p.moves.add(new Move(MoveType.JUMP, rank, col, rdr2, cdc2, getTurn()));
						} else {
							// jump with capture
							p.moves.add(new Move(MoveType.JUMP_CAPTURE, rank, col, rdr2, cdc2, rdr, cdc, getTurn(), t));
						}
					}
				}
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
        nextTurn();
        lock = null;
        clearMoves();
        if (computeMoves()==0) {
            onGameOver();
        }
	}

    @Override
	public void executeMove(Move move) {
        lock = null;
		boolean isKinged = false;
		final Piece p = getPiece(move.startRank, move.startCol);
        // clear everyone all moves
        clearMoves();
		if (move.startCol != move.endCol && move.startRank != move.endRank) {
            int rank = move.endRank;
            isKinged = (p.type == CHECKER && getRankForKingCurrent() == rank);
            movePiece(move);
		}

        undoStack.push(move);

        switch (move.type) {
            case SLIDE:
                if (isKinged) {
                    p.moves.add(new Move(MoveType.STACK, move.endRank, move.endCol, move.endRank, move.endCol, move.playerNum));
                    lock = p;
                    break;
                }
            case END:
                endTurnPrivate();
                return;
            case JUMP_CAPTURE:
                clearPiece(move.captureRank, move.captureCol);
            case JUMP:
                if (isKinged) {
                    p.moves.add(new Move(MoveType.STACK, move.endRank, move.endCol, move.endRank, move.endCol, move.playerNum));
                    lock = p;
                }
                break;
            case STACK:
                setPieceType(move.startRank, move.startCol, KING);
                break;
        }

        if (!isKinged) {
            // recursive compute next move if possible after a jump
            computeMovesForSquare(move.endRank, move.endCol, move);
            if (p.moves.size() == 0) {
                endTurnPrivate();
            } else {
                p.moves.add(new Move(MoveType.END, move.endRank, move.endCol, move.endRank, move.endCol, move.playerNum));
                lock = p;
            }
        }
	}

}
