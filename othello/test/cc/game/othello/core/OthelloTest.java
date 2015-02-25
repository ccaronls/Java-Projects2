package cc.game.othello.core;

import junit.framework.TestCase;

public class OthelloTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testBoardCopy() {
		Othello o = new Othello();
		o.newGame();
		OthelloBoard b = o.getBoard();
		OthelloBoard c = b.deepCopy();
		
		for (int i=0; i<b.getNumRows(); i++) {
			for (int j=0; j<b.getNumCols(); j++) {
				assertEquals(c.get(i, j), b.get(i, j));
			}
		}
		
	}
	
	
}
