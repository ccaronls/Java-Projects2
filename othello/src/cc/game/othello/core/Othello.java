package cc.game.othello.core;

import cc.lib.reflector.Reflector;

public class Othello extends Reflector<Othello> implements OthelloBoard.BoardListener {

	static final int DIM = 12;
	
	static {
		addAllFields(Othello.class);
	}
	
	OthelloBoard board = new OthelloBoard(DIM, DIM);
	
	OthelloPlayer black, white;
	
	boolean blackTurn = false;
	
	public final void newGame() {
		board.clear();
		for (int i=0; i<board.getNumCols(); i++)
			for (int ii=0; ii<board.getNumRows(); ii++) 
				board.set(ii, i, OthelloBoard.CELL_AVAILABLE);
		
		final int d2 = DIM/2;
		
		board.set(d2-1, d2-1, OthelloBoard.CELL_WHITE);
		board.set(d2-1, d2, OthelloBoard.CELL_BLACK);
		board.set(d2, d2, OthelloBoard.CELL_WHITE);
		board.set(d2, d2-1, OthelloBoard.CELL_BLACK);

		blackTurn = false;
	}

	public final boolean isGameOver() {
		return board.getCellCount(OthelloBoard.CELL_AVAILABLE) == 0;
	}

	public final void runGame() {
		int [] cell = new int[2];
		OthelloPlayer cur = blackTurn ? black : white;
		if (cur.chooseCell(board, cell) && board.isCellAvailable(cell[0], cell[1])) {
			board.set(cell[0], cell[1], cur.getPlayerColor());
			board.setSurroundingCellsAvailable(cell[0], cell[1]);
			board.turnOverPieces(cell[0], cell[1], cur.getPlayerColor(), this);
			blackTurn = !blackTurn;
		}
	}


	public final void intiPlayers(OthelloPlayer whitePlayer, OthelloPlayer blackPlayer) {
		black = blackPlayer;
		white = whitePlayer;
		black.playerColor = OthelloBoard.CELL_BLACK;
		white.playerColor = OthelloBoard.CELL_WHITE;
	}
	
	public final OthelloBoard getBoard() {
		return this.board;
	}

	@Override
	public void onCellChanged(int row, int col, int oldColor, int newColor) {}

	
}
