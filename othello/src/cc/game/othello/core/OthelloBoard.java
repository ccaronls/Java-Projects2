package cc.game.othello.core;

import java.util.Arrays;

import cc.lib.reflector.Reflector;

public class OthelloBoard extends Reflector<OthelloBoard> {

	static interface BoardListener {
		void onCellChanged(int row, int col, int oldColor, int newColor);
	}

	private final static int [] DIR_X = { 1, 1, 0, -1, -1, -1, 0, 1 };
	private final static int [] DIR_Y = { 0, 1, 1, 1, 0, -1, -1, -1 };

	public static final int CELL_UNUSED = 0;
	public static final int CELL_AVAILABLE = 1;
	public static final int CELL_WHITE = 2;
	public static final int CELL_BLACK = 3;
	
	static {
		addAllFields(OthelloBoard.class);
	}

	private final int [][] board;

	public OthelloBoard() {
		this(1,1);
	}
	
	public OthelloBoard(int rows, int columns) {
		board = new int[rows][columns];
	}

	public final void clear() {
		for (int i=0; i<board.length; i++) {
			Arrays.fill(board[i], 0);
		}
	}
	
	public final int get(int row, int col) {
		return board[row][col];
	}
	
	public final void set(int row, int col, int value) {
		board[row][col] = value;
	}
	
	/**
	 * Starting at the given cell, perform a search in each of the eight directions
	 * @param row
	 * @param col
	 * @param player
	 */
	public void turnOverPieces(int row, int col, int player, BoardListener listener) {
		for (int i=0; i<8; i++) {
			turnOverPiecesR(row+DIR_X[i], col+DIR_Y[i], player, DIR_X[i], DIR_Y[i], listener);
		}
	}

	public void turnOverPieces(int row, int col, int player) {
		turnOverPieces(row, col, player, null);
	}
	
	public boolean isValidCell(int row, int col) {
		return row>=0 && col>=0 && row<board.length && col<board[0].length;
	}
	
	private boolean turnOverPiecesR(int row, int col, int player, int dr, int dc, BoardListener listener) {
		if (!isValidCell(row, col))
			return false;
		
		int c = get(row, col);
		switch (c) {
		case CELL_UNUSED:
		case CELL_AVAILABLE:
			return false;
		}
		
		if (c == player)
			return true;

		if (turnOverPiecesR(row+dr, col+dc, player, dr, dc, listener)) {
			if (listener != null)
				listener.onCellChanged(row, col, get(row,col), player);
			set(row, col, player);
			setSurroundingCellsAvailable(row,col);
			return true;
		}
		
		return false;
	}

	final void setSurroundingCellsAvailable(int row, int col) {
		for (int i=0; i<8; i++) {
			setCellAvailableIfOpen(row+DIR_X[i], col+DIR_Y[i]);
		}
		setCellAvailableIfOpen(row+1, col+1);
	}

	private void setCellAvailableIfOpen(int i, int j) {
		if (isValidCell(i,j)) {
			if (get(i, j) == OthelloBoard.CELL_UNUSED) {
				set(i, j, OthelloBoard.CELL_AVAILABLE);
			}
		}
	}

	public final boolean isCellAvailable(int r, int c) {
		return isValidCell(r,c) && get(r,c) == CELL_AVAILABLE;
	}
	
	public final int getNumRows() {
		return board.length;
	}
	
	public final int getNumCols() {
		return board[0].length;
	}

	public final int getCellCount(int value) {
		int count = 0;
		for (int i=0; i<board.length; i++) {
			for (int j=0; j<board[0].length; j++) {
				if (get(i,j) == value)
					count++;
			}
		}
		return count;
	}
}
