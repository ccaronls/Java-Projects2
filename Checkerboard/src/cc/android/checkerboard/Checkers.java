package cc.android.checkerboard;

import android.util.Log;

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

    public enum MoveType {
        END, SLIDE, JUMP, JUMP_CAPTURE, STACK
    }

    public enum RobotType {
        RANDOM, STOOPID, SMART, DEBUG_SMART
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
    private RobotType robot = RobotType.RANDOM;
    private int robotPlayer = -1;

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
        robotPlayer = -1;
	}

	public final void newSinglePlayerGame(int difficulty) {
        this.robotPlayer = 1;//Utils.rand() % 2;
        this.robot = RobotType.values()[difficulty];
        checkRobot(computeMoves());
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

	/*
	This should be safe to call repeatedly.
	If there is a locked piece then the move set is just the moves associated
	with the locked piece, and it will be unchanged.
	Otherwise the complete move set is recomputed
	 */
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
		turn = (turn+1) % NUM_PLAYERS;
        lock = null;
        int n;
        if ((n=computeMoves())==0) {
            robotPlayer = -1;
            onGameOver();
        } else {
            checkRobot(n);
        }
	}

	private void reverseMove(Move m) {
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
            case STACK:
                getPiece(m.startRank, m.startCol).stacks --;
                break;
        }

        turn = m.playerNum;
        //lock.moves.add(m);
        Move parent = null;
        if (undoStack.size() > 0) {
            parent = undoStack.peek();
            if (parent.playerNum != m.playerNum) {
                parent = null;
            }
        }
        if (parent == null) {
            lock = null;
            computeMoves();
        } else {
            clearMoves();
            Piece p = getPiece(m.startRank, m.startCol);
            computeMovesForSquare(m.startRank, m.startCol, parent);
            p.moves.add(new Move(MoveType.END, m.startRank, m.startCol, 0, 0, 0, 0, m.playerNum));
            lock = p;
        }
    }

    private void clearMoves() {
        for (int i=0; i<RANKS; i++) {
            for (int ii=0; ii<COLUMNS; ii++) {
                getPiece(i, ii).moves.clear();
            }
        }
    }

    public final int getRankForKingCurrent() {
        return getRankForKing(getCurPlayerNum());
    }

    public final int getRankForKing(int playerNum) {
        switch (playerNum) {
            case BLACK:
                return 0;
            case RED:
                return board.length-1;
        }
    }

	public void executeMove(Move move) {
        lock = null;
		boolean isKinged = false;
		final Piece p = board[move.startRank][move.startCol];
        // clear everyone all moves
        clearMoves();
		if (move.startCol != move.endCol && move.startRank != move.endRank) {
            int rank = move.endRank;// + move.dRank;
            isKinged = (getRankForKingCurrent() == rank);
    		// check for king
            /*
    		if (rank == 0 && p.stacks == 1 && p.playerNum == BLACK) {
    			isKinged = true;
    		} else if (rank == board.length-1 && p.stacks == 1 && p.playerNum== RED) {
    			isKinged = true;
    		}*/
    		board[move.endRank][move.endCol] = p;
            board[move.startRank][move.startCol] = new Piece();
		}

        undoStack.push(move);

        switch (move.type) {
            case SLIDE:
                if (isKinged) {
                    p.moves.add(new Move(MoveType.STACK, move.endRank, move.endCol, move.endRank, move.endCol, -1, -1, move.playerNum));
                    lock = p;
                    break;
                }
            case END:
                endTurnPrivate();
                return;
            case JUMP_CAPTURE:
                move.captured = board[move.captureRank][move.captureCol];
                board[move.captureRank][move.captureCol] = new Piece();
            case JUMP:
                if (isKinged) {
                    p.moves.add(new Move(MoveType.STACK, move.endRank, move.endCol, move.endRank, move.endCol, -1, -1, move.playerNum));
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
                endTurnPrivate();
            } else {
                p.moves.add(new Move(MoveType.END, move.endRank, move.endCol, move.endRank, move.endCol, -1, -1, move.playerNum));
                lock = p;
                checkRobot(p.moves.size());
            }
        } else {
            checkRobot(computeMoves());
        }
	}

	private void checkRobot(int n) {
        if (robotPlayer == getCurPlayerNum()) {
            switch (robot) {
                case RANDOM:
                    doRandomRobot(n);
                    break;
                case STOOPID:
                    doStoopidRobot(n);
                    break;
                case SMART:
                    doSmartRobot(n, 5);
                    break;
                    // TODO: Extend stoopid to perform a min-max descision tree
            }
        }
    }

    private void doRandomRobot(int n) {
        if (n > 0) {
            int mvNum = Utils.rand() % n;
            Piece p;
            for (int i = 0; i < RANKS; i++) {
                for (int ii = 0; ii < COLUMNS; ii++) {
                    if ((p = getPiece(i, ii)).moves.size() > mvNum) {
                        onRobotMoved(p.moves.get(mvNum));
                        return;
                    } else {
                        mvNum -= p.moves.size();
                    }
                }
            }
        }
    }

    private void doStoopidRobot(int n) {
        if (n > 0) {
            Piece p;
            double bestBoardValue = 0;
            Move bestMove = null;
            for (int i = 0; i < RANKS; i++) {
                for (int ii = 0; ii < COLUMNS; ii++) {
                    if ((p = getPiece(i, ii)).moves.size() > 0) {
                        //onRobotMoved(p.moves.get(mvNum));
                        //return;
                        for (Move m : p.moves) {
                            Checkers bd = new Checkers();
                            bd.copyFrom(this);
                            bd.executeMove(m);
                            double d = evaluateBoard(bd.board);
                            if (bestMove == null || d > bestBoardValue) {
                                bestMove = m;
                                bestBoardValue = d;
                            }
                        }
                    }
                }
            }
            onRobotMoved(bestMove);
        }
    }

    public final static class MinMaxTreeNode {
        private MinMaxTreeNode parent, first, last, next;
        private final Checkers game;
        public String meta
        private MinMaxTreeNode(Checkers game) {
            this.game = game;
        }
        private void addChild(MinMaxTreeNode child) {
            if (first == null) {
                first = last = child;
            } else {
                last.next = child;
                last = child;
            }
            child.parent=this;
        }
    }

    private double doSmartRobot(int n, int depth, MinMaxTreeNode root) {
        double bestBoardValue = 0;
        Piece p;
        Move bestMove = null;
        if (n > 0) {
            for (int i = 0; i < RANKS; i++) {
                for (int ii = 0; ii < COLUMNS; ii++) {
                    if ((p = getPiece(i, ii)).moves.size() > 0) {
                        //onRobotMoved(p.moves.get(mvNum));
                        //return;
                        for (Move m : p.moves) {
                            Checkers bd = new Checkers();
                            bd.copyFrom(this);
                            bd.robotPlayer = -1; // make so robot doesnt kick in
                            bd.executeMove(m);
                            MinMaxTreeNode next = null;
                            if (root != null) {
                                root.addChild(next = new MinMaxTreeNode(bd));
                            }
                            double d = evaluateBoard(bd.board);

                            if (depth-- > 0) {
                                d = Math.max(d, bd.doSmartRobot(bd.computeMoves(), depth, next));
                            }
                            if (bestMove == null || d > bestBoardValue) {
                                bestMove = m;
                                bestBoardValue = d;
                            }
                        }
                    }
                }
            }
            onRobotMoved(bestMove);
        }
        return bestBoardValue;
    }


    /**
     * Returns a number between -1 and 1
     *
     * @param board
     * @return
     */
    protected double evaluateBoard(Piece [][] board) {
        int mine = 0;
        int theirs = 0;

        int mineKings=0;
        int theirKings=0;

        int mineAdvance = 0;
        int theirAdvance = 0;

        for (int rank=0; rank<RANKS; rank++) {
            for (Piece p : board[rank]) {
                if (p.playerNum == getCurPlayerNum()) {
                    if (p.stacks == 1) {
                        mine++;
                        mineAdvance += rank;
                    } else if (p.stacks == 2)
                        mineKings++;

                } else if (p.playerNum >= 0) {
                    if (p.stacks == 1) {
                        theirs++;
                        theirAdvance += (RANKS - rank);
                    } else if (p.stacks == 2)
                        theirKings ++;
                }
            }
        }

        double d = 0.01 * (mine - theirs) + 0.1 * (mineKings - theirKings) + 0.02 * (mineAdvance - theirAdvance);
        Log.d("AI", "Board evaluates too: " + d);
        return d;
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

    /**
     * Called when the AI can done something. Will be called at the end of 'executeMove'
     * Not calling super.onRobotMoved means that caller will have to call executeMove(...) to advance the game.
     * @param move
     */
    protected void onRobotMoved(Move move) {

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
