package cc.android.checkerboard;

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
public class Checkers {

	private final Piece [][] board = new Piece[8][8]; // rank major
	private PieceColor turn = null;

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
	}
	
	void setup() {
		for (int i=0; i<board.length; i++) {
			Arrays.fill(board[i], Piece.EMPTY);
			for (int ii=i%2; ii<board[0].length; ii+=2) {
				switch (i) {
					case 0: case 1: case 2:
						board[i][ii] = Piece.RED_SINGLE; break;
					case 5: case 6: case 7:
						board[i][ii] = Piece.BLACK_SINGLE; break;
					
				}
			}
		}
		turn = Utils.flipCoin() ? PieceColor.BLACK : PieceColor.RED; 
	}
	
	PieceColor getWinner() {
		int numRed = 0;
		int numBlack = 0;
		for (int i=0; i<8; i++) {
			for (int ii=0; ii<8; ii++) {
				switch (board[i][ii]) {
					case BLACK_KING:
					case BLACK_SINGLE:
						numBlack ++;
						break;
					case EMPTY:
						break;
					case RED_KING:
					case RED_SINGLE:
						numRed ++;
						break;
				}
			}
		}
		if (numRed == 0)
			return PieceColor.BLACK;
		else if (numBlack == 0)
			return PieceColor.RED;
		return PieceColor.NONE;
	}
	
	PieceColor getTurnColor() {
		return turn;
	}
	
	Piece getBoard(int rank, int col) {
		return board[rank][col];
	}
	
	public List<Move> computeMovesForSquare(int rank, int col) {
		return computeMovesForSquare(rank, col, null);
	}
	
	private List<Move> computeMovesForSquare(int rank, int col, Move parent) {
		List<Move> moves = new ArrayList<>();
		
		Piece p = board[rank][col];
		
		if (p == Piece.EMPTY || p.color != getTurnColor()) {
			return moves;
		}

		int [] dr, dc;
		
		if (p.isKing) {
			dr = new int[] { 1, 1, -1, -1 };
			dc = new int[] { -1, 1, -1, 1 };
		} else if (p.color == PieceColor.BLACK) {
			// negative
			dr = new int [] { -1, -1 };
			dc = new int [] { -1, 1 };
		} else { // red
			// positive
			dr = new int [] { 1, 1 };
			dc = new int [] { -1, 1 };
		}
		
		for (int i=0; i<dr.length; i++) {
			if (!isOnBoard(rank+dr[i], col+dc[i]))
				continue;
			Piece t = board[rank+dr[i]][col+dc[i]];
			if (t == Piece.EMPTY) {
				if (parent == null)
					moves.add(new Move(rank, col, dr[i], dc[i], 0, 0, false, false, null));
			} else {
				// check for jump
				if (isOnBoard(rank+dr[i]*2, col+dc[i]*2)) {
					Piece j = board[rank+dr[i]*2][col+dc[i]*2];
					if (j == Piece.EMPTY) {
						// we can jump to here
						if (t.color == getTurnColor()) {
							// we are jumping ourself, no capture
							moves.add(new Move(rank, col, dr[i]*2, dc[i]*2, 0, 0, false, true, null));
						} else {
							// jump with capture
							moves.add(new Move(rank, col, dr[i]*2, dc[i]*2, rank+dr[i], col+dc[i], true, true, null));
						}
					}
				}
			}
		}
		
		return moves;
	}

	void endTurn() {
		turn = turn == PieceColor.BLACK ? PieceColor.RED : PieceColor.BLACK;
	}
	
	List<Move> executeMove(Move move) {
		Piece t = board[move.startRank][move.startCol];
		board[move.startRank][move.startCol] = Piece.EMPTY;
		// check for king
		int rank = move.startRank + move.dRank;
		if (rank == 0 && t.color == PieceColor.BLACK) {
			t = Piece.BLACK_KING;
		} else if (rank == board.length-1 && t.color == PieceColor.RED) {
			t = Piece.RED_KING;
		}
		
		board[move.startRank + move.dRank][move.startCol + move.dCol] = t;
		if (move.hasCapture) {
			board[move.captureRank][move.captureCol] = Piece.EMPTY;
		} else if (move.isJump) {
			// do nothing
		} else {
			endTurn();
			return Collections.EMPTY_LIST;
		}
		List<Move> nextMoves = computeMovesForSquare(move.startRank+move.dRank, move.startCol+move.dCol, move);
		if (nextMoves.size() == 0) {
			endTurn();
		}
		return nextMoves;
	}
	
	
	/*
	private int buildJumpTree(Move move) {
		int [] num = { 0 };
		buildJumpTreeR(move.fromRank, move.fromCol, move.jumpTree, num);
		return num[0];
	}

	private void buildJumpTreeR(int sr, int sc, Jump parent, int [] num) {
		
		int color = board[sr][sc];
		// color is also the direction of movement

		for (int i=0; i<4; i++) {
			if (!canMoveInDirection(sr, sc, i))
				continue;
			int r = sr + dr[i];
			int c = sc + dc[i];
			if (isOnBoard(r, c)) {
				if (board[r][c] < 0 && color > 0 || board[r][c] > 0 && color < 0) {
					// we can jump over this piece
					int captureR = r;
					int captureC = c;
					// this is where we will land
					int jumpR = r + dr[i];
					int jumpC = c + dc[i];
					if (isOnBoard(jumpR, jumpC)) {
						num[0] ++;
						parent.children[i] = new Jump(jumpR, jumpC, captureR, captureC);
						int t = board[captureR][captureC];
						board[captureR][captureC] = 0;
						buildJumpTreeR(jumpR, jumpC, parent.children[i], num);
						board[captureR][captureC] = t;
					}
				}
			}
		}
	}
	

	static class Jump {
		int jumpR, jumpC;
		int captureR, captureC;
		
		public Jump(int jumpR, int jumpC, int captureR, int captureC) {
			super();
			this.jumpR = jumpR;
			this.jumpC = jumpC;
			this.captureR = captureR;
			this.captureC = captureC;
		}



		Jump [] children = new Jump[4];
	}
	
	/**
	 * Return null if game over
	 * @return
	 *
	void computMoves(MoveTree [][] moves) {
		for (int r=0; r<board.length; r++) {
			for (int c=0; c<board[0].length; c++) {
				Piece p = board[r][c];
				MoveTree root = new MoveTree();
				buildMoveTree(r, c, root);
			}
		}
	}
	
	void buildMoveTree(int r, int c, final MoveTree root) {
		final Piece p = board[r][c];
		for (MoveType t : p.moveTypes) {
			int nr = r+t.dRank;
			int nc = c+t.dColumn;
			if (!isOnBoard(nr, nc))
				continue;
			final Piece s = board[nr][nc];
			if (s == Piece.EMPTY) {
				
			}
		}

	}*/
	
	boolean isOnBoard(int r, int c) {
		return r>=0 && c>=0 && r<board.length && c<board[0].length;
	}
}
