package cc.game.othello.core;

import cc.lib.utils.Reflector;

public abstract class OthelloPlayer extends Reflector<OthelloPlayer> {

	static {
		addAllFields(OthelloPlayer.class);
	}
	
	int playerColor; // set by Othello
	
	/**
	 * Put into rowCellCol the place on the board to put their piece.  Return true if piece set, false otherwise.
	 * Game will not advance until true is returned and the values in rowColCell is a valid AVAILABEL cell.
	 * 
	 * @param board
	 * @param rowColCell
	 */
	public abstract boolean chooseCell(OthelloBoard board, int[] rowColCell);

	public final int getPlayerColor() {
		return playerColor;
	}
}
