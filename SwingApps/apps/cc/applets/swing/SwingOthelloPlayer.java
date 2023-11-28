package cc.applets.swing;

import cc.game.othello.core.OthelloBoard;
import cc.game.othello.core.OthelloPlayer;

public class SwingOthelloPlayer extends OthelloPlayer {

	@Override
	public boolean chooseCell(OthelloBoard board, int[] rowColCell) {
		return OthelloApplet.instance.pickCell(rowColCell);
	}

}
