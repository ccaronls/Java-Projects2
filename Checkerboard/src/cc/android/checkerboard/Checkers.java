package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import cc.lib.game.IGame;
import cc.lib.game.MiniMaxTree;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

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
		for (int i=0; i<RANKS; i++) {
            for (int ii=0; ii<COLUMNS; ii++)
                board[i][ii] = new Piece();
            for (int ii=i%2; ii<COLUMNS; ii+=2) {
				switch (i) {
					case 0: case 1: case 2:
						board[i][ii] = new Piece(RED, PieceType.CHECKER); break;
					case 5: case 6: case 7:
						board[i][ii] = new Piece(BLACK, PieceType.CHECKER); break;
					
				}
			}
		}
		super.newGame();
	}

    @Override
    protected final int getMinVersion() {
        return 1;
    }

    @Override
	protected void computeMovesForSquare(int rank, int col, Move parent) {
		Piece p = board[rank][col];
		if (p.type == PieceType.EMPTY || p.playerNum != getCurPlayerNum()) {
			return;
		}

		int [] dr, dc;
		if (p.type == PieceType.KING) {
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
			Piece t = board[rdr][cdc];
			if (t.type == PieceType.EMPTY) {
				if (parent == null)
					p.moves.add(new Move(MoveType.SLIDE, rank, col, rdr, cdc, getTurn()));
			} else {
				// check for jump
				if (isOnBoard(rdr2, cdc2)) {
                    if (parent != null && parent.startCol == cdc2 && parent.startRank == rdr2) {
                        continue;
                    }
					Piece j = board[rdr2][cdc2];
					if (j.type == PieceType.EMPTY) {
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
        setTurn((getTurn()+1) % NUM_PLAYERS);
        lock = null;
        if (computeMoves(true)==0) {
            onGameOver();
        }
	}

	@Override
	protected void reverseMove(Move m) {
        switch (m.type) {
            case END:
//                turn = m.playerNum;
//                getPiece(m.startRank, m.startCol).moves.add(m);
                break;
            case JUMP_CAPTURE:
                board[m.captureRank][m.captureCol] = m.captured;
            case SLIDE:
            case JUMP:
                board[m.startRank][m.startCol] = board[m.endRank][m.endCol];
                board[m.startRank][m.startCol].moves.clear();
                board[m.endRank][m.endCol] = new Piece();
                break;
            case STACK: {
                Piece p = getPiece(m.startRank, m.startCol);
                if (p.type == PieceType.KING) {
                    board[m.startRank][m.startCol] = new Piece(p.playerNum, PieceType.CHECKER);
                }
                break;
            }
        }

        setTurn(m.playerNum);
        Move parent = null;
        if (undoStack.size() > 0) {
            parent = undoStack.peek();
            if (parent.playerNum != m.playerNum) {
                parent = null;
            }
        }
        if (parent == null) {
            lock = null;
            computeMoves(true);
        } else {
            clearMoves();
            Piece p = getPiece(m.startRank, m.startCol);
            computeMovesForSquare(m.startRank, m.startCol, parent);
            p.moves.add(new Move(MoveType.END, m.startRank, m.startCol, 0, 0, m.playerNum));
            lock = p;
        }
    }

    @Override
	public void executeMove(Move move) {
        lock = null;
		boolean isKinged = false;
		final Piece p = board[move.startRank][move.startCol];
        // clear everyone all moves
        clearMoves();
		if (move.startCol != move.endCol && move.startRank != move.endRank) {
            int rank = move.endRank;
            isKinged = (p.type == PieceType.CHECKER && getRankForKingCurrent() == rank);
    		board[move.endRank][move.endCol] = p;
            board[move.startRank][move.startCol] = new Piece();
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
                board[move.captureRank][move.captureCol] = new Piece();
            case JUMP:
                if (isKinged) {
                    p.moves.add(new Move(MoveType.STACK, move.endRank, move.endCol, move.endRank, move.endCol, move.playerNum));
                    lock = p;
                }
                break;
            case STACK:
                board[move.startRank][move.startCol].type = PieceType.KING;
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

    /**
     * Called when current player has no turns. Default does nothing. Need to call newGame to rest everytihng.
     */
	protected void onGameOver() {

    }


}
