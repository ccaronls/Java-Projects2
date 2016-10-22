package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.Arrays;
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
	private int turn = 0;
	private int numRed=0, numBlack=0;
	
	enum PieceColor {
		RED, BLACK
	}
	
	enum Direction {
		POSITIVE_LEFT,
		POSITIVE_RIGHT,
		NEGATIVE_LEFT,
		NEGATIVE_RIGHT
	}
	
	enum Piece {
		EMPTY		(null, false),
		BLACK_SINGLE(PieceColor.BLACK, false),
		BLACK_KING	(PieceColor.BLACK, true),
		RED_SINGLE	(PieceColor.RED, false),
		RED_KING	(	PieceColor.RED, true)
		;
		
		private Piece(PieceColor color, boolean isKing) {
			this.color = color;
			this.isKing = isKing;
		}
		
		final PieceColor color;
		final boolean isKing;
		
	}
	
	void setup() {
		numRed = numBlack = 0;
		for (int i=0; i<board.length; i++) {
			Arrays.fill(board[i], Piece.EMPTY);
			for (int ii=i%2; ii<board[0].length; ii+=2) {
				switch (i) {
					case 0: case 1: case 2:
						board[i][ii] = Piece.RED_SINGLE; numRed++; break;
					case 5: case 6: case 7:
						board[i][ii] = Piece.BLACK_SINGLE; numBlack++; break;
					
				}
			}
		}
		turn = Utils.flipCoin() ? 1 : -1; 
	}
	
	int getTurnColor() {
		return turn;
	}
	
	private int buildJumpTree(Move move) {
		int [] num = { 0 };
		buildJumpTreeR(move.fromRank, move.fromCol, move.jumpTree, num);
		return num[0];
	}

	final int [] dr = { 1, 1, -1, -1 };
	final int [] dc = { -1, 1, 1, -1 };
	
	boolean canMoveInDirection(int sr, int sc, Direction dir) {
		// we can only advance in rank is either direction for kinds
		return board[sr][sc].direction == dir || board[sr][sc].direction == Direction.ANY;
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
	
	static class Move {
		int fromRank, fromCol; // square to be moved
		int toRank, toCol; // if numJumps == 0, then this is a normal move 
		Jump jumpTree;
		
		public Move(int fromRank, int fromCol, int toRank, int toCol) {
			this.fromRank = fromRank;
			this.fromCol = fromCol;
			this.toRank = toRank;
			this.toCol = toCol;
		}
	}
	
	/**
	 * Return null if game over
	 * @return
	 */
	List<Move> computMoves() {
		List<Move> moves = new ArrayList<>();

		for (int r=0; r<board.length; r++) {
			for (int c=0; c<board[0].length; c++) {
				for (int d=0; d<4; d++) {
					if (!canMoveInDirection(r, c, d))
						continue;
					int testRank = r+dr[d];
					int testCol  = c+dc[d];
					int p = board[r][c];
					if (p == 0) {
						// can move here without capture
						moves.add(new Move(r, c, testRank, testCol));
					} else {
						Move m = new Move(r, c, 0, 0);
						if (buildJumpTree(m) > 0) {
							moves.add(m);
						}
					}
				}
			}
		}
		return moves;
	}
	
	boolean isOnBoard(int r, int c) {
		return r>=0 && c>=0 && r<board.length && c<board[0].length;
	}
}
