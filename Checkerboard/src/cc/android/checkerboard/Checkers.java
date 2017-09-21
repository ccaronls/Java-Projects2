package cc.android.checkerboard;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

/**
 * Red is positive and black is negative
 * @author chriscaron
 *
 */
public class Checkers extends Reflector<Checkers> { //implements ICheckerboard {

    static {
        addAllFields(Checkers.class);
    }

    enum MoveType {
        END, SLIDE, JUMP, JUMP_CAPTURE, STACK
    }

    public final int RANKS;
    public final int COLUMNS;
    public final int NUM_PLAYERS = 2;

	private final Piece [][] board; // rank major
	private int turn = -1;
    private Piece lock = null;

	private final static int BLACK = 0;
	private final static int RED   = 1;

    private final Stack<Move> undoStack = new Stack<>();

    public Checkers() {
        this(8,8);
    }

    public Checkers(int ranks, int columns) {
        this.RANKS = ranks;
        this.COLUMNS = columns;
        board = new Piece[RANKS][COLUMNS];
    }

	public final void newGame() {
		for (int i=0; i<RANKS; i++) {
            for (int ii=0; ii<COLUMNS; ii++)
                board[i][ii] = new Piece();
            for (int ii=i%2; ii<COLUMNS; ii+=2) {
				switch (i) {
					case 0: case 1: case 2:
						board[i][ii] = new Piece(RED, 1); break;
					case 5: case 6: case 7:
						board[i][ii] = new Piece(BLACK, 1); break;
					
				}
			}
		}
		turn = Utils.flipCoin() ? 0 : 1;
        lock = null;
        computeMoves();
        undoStack.clear();
	}

    @Override
    protected final int getMinVersion() {
        return 1;
    }

	public final Piece getPiece(int rank, int column) {
        return board[rank][column];
    }

	public final int getCurPlayerNum() {
		return turn;
	}

	private final int computeMoves() {
        if (lock == null) {
            int num = 0;
            for (int rank = 0; rank < RANKS; rank++) {
                for (int col = 0; col < COLUMNS; col++) {
                    Piece p = getPiece(rank, col);
                    p.moves.clear();
                    computeMovesForSquare(rank, col, null);
                    num += p.moves.size();
                }
            }
            return num;
        } else {
            return lock.moves.size();
        }
	}

	private void computeMovesForSquare(int rank, int col, Move parent) {
		Piece p = board[rank][col];
		if (p.stacks == 0 || p.playerNum != getCurPlayerNum()) {
			return;
		}

		int [] dr, dc;
		if (p.stacks > 1) {
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
			if (t.stacks == 0) {
				if (parent == null)
					p.moves.add(new Move(MoveType.SLIDE, rank, col, rdr, cdc, 0, 0, turn));
			} else {
				// check for jump
				if (isOnBoard(rdr2, cdc2)) {
                    if (parent != null && parent.startCol == cdc2 && parent.startRank == rdr2) {
                        continue;
                    }
					Piece j = board[rdr2][cdc2];
					if (j.stacks == 0) {
						// we can jump to here
						if (t.playerNum == getCurPlayerNum()) {
							// we are jumping ourself, no capture
							p.moves.add(new Move(MoveType.JUMP, rank, col, rdr2, cdc2, 0, 0, turn));
						} else {
							// jump with capture
							p.moves.add(new Move(MoveType.JUMP_CAPTURE, rank, col, rdr2, cdc2, rdr, cdc, turn));
						}
					}
				}
			}
		}
	}

	public void endTurn() {
		turn = (turn+1) % NUM_PLAYERS;
        lock = null;
        if (computeMoves()==0) {
            onGameOver();
        }
	}

	private void reverseMove(Move m) {
        switch (m.type) {
            case END:
                break;
            case JUMP_CAPTURE:
                board[m.captureRank][m.captureCol] = m.captured;
            case SLIDE:
            case JUMP:
                board[m.startRank][m.startCol] = board[m.endRank][m.endCol];
                board[m.startRank][m.startCol].moves.remove(m);
                board[m.endRank][m.endCol] = new Piece();
                break;
            case STACK:
                getPiece(m.startRank, m.startCol).stacks --;
                break;
        }

        turn = m.playerNum;
        if (lock == null) {
            computeMoves();
        } else {
            //lock.moves.add(m);
            Move parent = null;
            if (undoStack.size() > 0) {
                parent = undoStack.peek();
                if (parent.playerNum != m.playerNum)
                    parent = null;
            }
            getPiece(m.startRank, m.startCol).moves.clear();
            computeMovesForSquare(m.startRank, m.startCol, parent);
        }
    }

    private void clearMoves() {
        for (int i=0; i<RANKS; i++) {
            for (int ii=0; ii<COLUMNS; ii++) {
                getPiece(i, ii).moves.clear();
            }
        }
    }

	public void executeMove(Move move) {
        lock = null;
		boolean isKinged = false;
		final Piece p = board[move.startRank][move.startCol];
        // clear everyone all moves
        clearMoves();
		if (move.startCol != move.endCol && move.startRank != move.endRank) {
    		// check for king
    		int rank = move.endRank;// + move.dRank;
    		if (rank == 0 && p.stacks == 1 && p.playerNum == BLACK) {
    			isKinged = true;
    		} else if (rank == board.length-1 && p.stacks == 1 && p.playerNum== RED) {
    			isKinged = true;
    		}
    		board[move.endRank][move.endCol] = p;
            board[move.startRank][move.startCol] = new Piece();
		}

        undoStack.push(move);

        switch (move.type) {
            case SLIDE:
                if (isKinged) {
                    p.moves.add(new Move(MoveType.STACK, move.endRank, move.endCol, move.endRank, move.endCol, 0, 0, move.playerNum));
                    lock = p;
                    break;
                }
            case END:
                endTurn();
                return; // dont want to make undoable this move?
            case JUMP_CAPTURE:
                move.captured = board[move.captureRank][move.captureCol];
                board[move.captureRank][move.captureCol] = new Piece();
            case JUMP:
                if (isKinged) {
                    p.moves.add(new Move(MoveType.STACK, move.endRank, move.endCol, move.endRank, move.endCol, 0, 0, move.playerNum));
                    lock = p;
                }
                break;
            case STACK:
                p.stacks++;
                break;
        }

        if (!isKinged) {
            // recursive compute next move if possible after a jump
            computeMovesForSquare(move.endRank, move.endCol, move);
            if (p.moves.size() == 0) {
                endTurn();
            } else {
                p.moves.add(new Move(MoveType.END, 0, 0, 0, 0, 0, 0, move.playerNum));
                lock = p;
            }
        }
	}
	
	public final boolean isOnBoard(int r, int c) {
		return r>=0 && c>=0 && r<RANKS && c<COLUMNS;
	}

    /**
     * Called when current player has no turns. Default is to start a new game.
     * Override to intersect this event.
     */
	protected void onGameOver() {
        newGame();
    }

    public final boolean canUndo() {
        return undoStack.size() > 0;
    }

    public final Move undo() {
        if (undoStack.size() > 0) {
            Move m = undoStack.pop();
            reverseMove(m);
            return m;
        }
        return null;
    }

    public final Piece getLocked() {
        return lock;
    }
}
