package marcos.game.stackmate.core;

import marcos.game.stackmate.core.StackMate.Chip;
import junit.framework.TestCase;

public class StackMateTest extends TestCase {

	protected void setUp() throws Exception {
		StackMate.ENABLE_DEBUG = false;
		StackMatePlayerAI.ENABLE_LOGGING = false;
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void test() {
		StackMate.ENABLE_DEBUG = true;
		StackMatePlayerAI.ENABLE_LOGGING = true;
		StackMate game = new StackMate();
		try {
    		game.newGame();
    		game.initPlayers(new StackMatePlayerAI(), new StackMatePlayerAI());
    		while (!game.isGameOver()) {
    			game.runGame();
    			System.out.println(game);
    			//Thread.sleep(2000);
    		}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void test1000() {
		StackMate game = new StackMate();
		int redWins = 0;
		int blackWins = 0;
		try {
			for (int i=0; i<1000; i++) {
        		game.newGame();
        		game.initPlayers(new StackMatePlayerAI(), new StackMatePlayerAI());
        		while (!game.isGameOver()) {
        			game.runGame();
        			//System.out.println(game);
        			//Thread.sleep(2000);
        		}
        		if (i%50 == 0)
        			System.out.println();
        		if (game.determineWinner() == Chip.RED) {
        			redWins++;
        			System.out.print("R");
        		} else {
        			blackWins++;
        			System.out.print("B");
        		}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		System.out.println("\n\nRed Wins=" + redWins + " BlackWins=" + blackWins);
	}
}
