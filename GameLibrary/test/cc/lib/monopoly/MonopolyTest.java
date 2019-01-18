package cc.lib.monopoly;

import junit.framework.TestCase;

import java.io.File;

import cc.lib.game.Utils;

public class MonopolyTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Utils.DEBUG_ENABLED = true;
        Utils.setRandomSeed(0);
    }

    public void testGame() {
        Monopoly monopoly = new Monopoly();
        monopoly.getRules().startMoney = 500;
        monopoly.addPlayer(new Player());
        monopoly.addPlayer(new Player());
        monopoly.addPlayer(new Player());

        int i=0;
        for (; i<2000; i++) {
            try {
                monopoly.runGame();
            } catch (Throwable e) {
                monopoly.trySaveToFile(new File("monopoly_crash.txt"));
                throw e;
            }
            if (monopoly.isGameOver())
                break;
        }

        System.out.println("Stopped at " + i + " iterations");

        System.out.println("Players=" + monopoly.getPlayersCopy());

        assertTrue(monopoly.isGameOver());
    }

    public void testLotsOfGames() {
        int numSuccsess = 0;
        try {
            for (int i = 0; i < 1000; i++) {
                testGame();
                numSuccsess++;
            }
        } catch (Throwable t) {
            throw t;
        } finally {
            System.out.println("Num successfull games=" + numSuccsess);
        }
    }
}
