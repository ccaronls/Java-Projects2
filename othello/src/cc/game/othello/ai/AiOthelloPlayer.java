package cc.game.othello.ai;

import cc.game.othello.core.OthelloBoard;
import cc.game.othello.core.OthelloPlayer;

public class AiOthelloPlayer extends OthelloPlayer {

	@Override
	public boolean chooseCell(OthelloBoard board, int[] rowColCell) {

		int bestRow = 0;
		int bestCol = 0;
		int bestScore = -1;
		for (int i=0; i<board.getNumRows(); i++) {
			for (int j=0; j<board.getNumCols(); j++) {
				if (board.isCellAvailable(i,j)) {
					OthelloBoard b = board.deepCopy();
					b.set(i, j, getPlayerColor());
					b.turnOverPieces(i, j, getPlayerColor());
					int score = b.getCellCount(getPlayerColor());
					if (score > bestScore) {
						bestScore = score;
						bestRow = i;
						bestCol = j;
					}
				}
			}
		}
	
		rowColCell[0] = bestRow;
		rowColCell[1] = bestCol;
		
		return bestScore >= 0;
	}

}
