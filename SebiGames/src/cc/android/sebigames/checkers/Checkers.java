package cc.android.sebigames.checkers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cc.lib.game.*;
import cc.lib.math.*;

/**
 * Red is positive and black is negative
 * @author chriscaron
 *
 */
public class Checkers implements ICheckerboard {

	private final Piece [][] board = new Piece[8][8]; // rank major
	private int turn = -1;

	private final static int BLACK = 0;
	private final static int RED   = 1;
	
	
/*
	static class Move {
		final int startRank, startCol;
		final int dRank, dCol;
		final int captureRank, captureCol;
		final boolean hasCapture;
		final boolean isJump;
		final Move parent; // provide a way to unwind.  also differentiate if this is an 'extra' move
		
		public Move(int startRank, int startCol, int dRank, int dCol, int captureRank, int captureCol, boolean hasCapture, boolean isJump, Move parent) {
			this.startRank = startRank;
			this.startCol = startCol;
			this.dRank = dRank;
			this.dCol = dCol;
			this.captureRank = captureRank;
			this.captureCol = captureCol;
			this.hasCapture = hasCapture;
			this.isJump = isJump;
			this.parent = parent;
		}
		
		
	}
	
	enum PieceColor {
		NONE, RED, BLACK
	}
	
	enum Piece {
		EMPTY		(PieceColor.NONE, false),
		BLACK_SINGLE(PieceColor.BLACK, false),
		BLACK_KING	(PieceColor.BLACK, true),
		RED_SINGLE	(PieceColor.RED, false),
		RED_KING	(PieceColor.RED, true)
		;
		
		private Piece(PieceColor color, boolean isKing) {
			this.color = color;
			this.isKing = isKing;
		}
		
		final PieceColor color;
		final boolean isKing;
	}*/
	
	@Override
	public void newGame() {
		for (int i=0; i<board.length; i++) {
			Arrays.fill(board[i], new Piece(-1, 0));
			for (int ii=i%2; ii<board[0].length; ii+=2) {
				switch (i) {
					case 0: case 1: case 2:
						board[i][ii] = new Piece(RED, 1); break;
					case 5: case 6: case 7:
						board[i][ii] = new Piece(BLACK, 1); break;
					
				}
			}
		}
		turn = Utils.flipCoin() ? 0 : 1; 		
	}

	@Override
	public Piece getPiece(int rank, int column) {
		return board[rank][column];
	}

	@Override
	public int getRanks() {
		return 8;
	}

	@Override
	public int getColumns() {
		return 8;
	}

	@Override
	public int getNumPlayers() {
		return 2;
	}

	@Override
	public int getCurPlayerNum() {
		return turn;
	}

	@Override
	public List<Move> computeMoves(int rank, int col) {
		return computeMovesForSquare(rank, col, null);
	}

	@Override
	public int getWinner() {
		int [] count = { 0, 0 };
		for (int i=0; i<8; i++) {
			for (int ii=0; ii<8; ii++) {
				if (board[i][ii].stacks > 0)
					count[board[i][ii].playerNum]++;
			}
		}
		if (count[0] != 0 && count[1] != 0)
			return -1;
		
		return count[0] == 0? 1 : 0;
	}
	
	private List<Move> computeMovesForSquare(int rank, int col, Move parent) {
		List<Move> moves = new ArrayList<>();
		
		Piece p = board[rank][col];
		
		if (p.stacks == 0 || p.playerNum != getCurPlayerNum()) {
			return moves;
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
					moves.add(new Move(MoveType.SLIDE, rank, col, rdr, cdc, 0, 0, turn));
			} else {
				// check for jump
				if (isOnBoard(rdr2, cdc2)) {
					Piece j = board[rdr2][cdc2];
					if (j.stacks == 0) {
						// we can jump to here
						if (t.playerNum == getCurPlayerNum()) {
							// we are jumping ourself, no capture
							moves.add(new Move(MoveType.JUMP, rank, col, rdr2, cdc2, 0, 0, turn));
						} else {
							// jump with capture
							moves.add(new Move(MoveType.JUMP_CAPTURE, rank, col, rdr2, cdc2, rdr, cdc, turn));
						}
					}
				}
			}
		}
		
		return moves;
	}

	
	@Override
	public void endTurn() {
		turn = (turn+1) % 2;
	}
	
	@Override
	public List<Move> executeMove(Move move) {
		final Piece t = board[move.startRank][move.startCol];
		board[move.startRank][move.startCol] = new Piece(0,0);
		// check for king
		boolean isKinged = false;
		int rank = move.endRank;// + move.dRank;
		if (rank == 0 && t.stacks == 1 && t.playerNum == BLACK) {
			isKinged = true;
		} else if (rank == board.length-1 && t.stacks == 1 && t.playerNum== RED) {
			isKinged = true;
		}

		if (isKinged) {
			t.stacks=2;
		}
		
		board[move.endRank][move.endCol] = t;
		
		switch (move.type) {
			case JUMP_CAPTURE:
				board[move.captureRank][move.captureCol].stacks = 0;
			case JUMP:
				if (!isKinged)
					break; // recursive compute next move
			case SLIDE:
				endTurn();
				return Collections.EMPTY_LIST;
			case STACK:
			default:
				throw new AssertionError();
		}
		
		
		List<Move> nextMoves = computeMovesForSquare(move.endRank, move.endCol, move);
		if (nextMoves.size() == 0) {
			endTurn();
		}
		return nextMoves;
	}
	
	@Override
	public boolean isOnBoard(int r, int c) {
		return r>=0 && c>=0 && r<board.length && c<board[0].length;
	}
}
