package cc.lib.checkerboard;

import junit.framework.TestCase;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class CheckerboardTestFinal extends TestCase {

    final Logger log = LoggerFactory.getLogger(getClass());

    private void runGame(Game gm) {
        runGame(gm, false, 500);
    }

    private void runGame(Game gm, boolean expectWinner, final int maxIterations) {

        long totalNodesEvaluated = 0;
        long t = System.currentTimeMillis();
        int i;
        for (i=0; i<maxIterations; i++) {
            if (gm.getSelectedPiece() == null) {
                log.info("********* Frame: " + i + "\n" + gm);
                //gm.trySaveToFile(new File("outputs/" + gm.getRules().getClass().getSimpleName() + "_" + i + ".txt"));
            }
            gm.runGame();
            totalNodesEvaluated += AIPlayer.evalCount;
            //gm.trySaveToFile(new File("minimaxtest_testcheckrs.game"));
            if (false && AIPlayer.lastSearchResult != null) {
                try (Writer out = new FileWriter("outputs/" + AIPlayer.algorithm + "_tree." + i + ".xml")) {
                    //out.write("<root minimax=\"" + root.bestValue + "\">\n");
                    AIPlayer.dumpTree(out, AIPlayer.lastSearchResult);
                    //out.write("</root>\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                AIPlayer.lastSearchResult = null;
            }
            //if (i == 3) break;
            if (gm.isGameOver())
                break;
        }
        float dtSecs = 0.001f * (System.currentTimeMillis()-t);
        log.info(String.format("runGame time = %3.2f seconds\ntotal nodes evaluated = %d\naverage time per turn = %3.2f seconds",
                dtSecs,
                totalNodesEvaluated,
                dtSecs/i));
        assertTrue("No winner!", !expectWinner || gm.getWinner() != null);
        log.info("GAME OVER\n" + gm);
    }

    public void testCheckers() {
        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testDama() {
        Game gm = new Game();
        gm.setRules(new Dama());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testColumns() {
        Game gm = new Game();
        gm.setRules(new Columns());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testDraughts() {
        Game gm = new Game();
        gm.setRules(new Draughts());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testCanadianDraughts() {
        Game gm = new Game();
        gm.setRules(new CanadianDraughts());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testKingsCourt() {
        Game gm = new Game();
        gm.setRules(new KingsCourt());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testUgolki() {
        Game gm = new Game();
        gm.setRules(new Ugolki());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testSuicide() {
        Game gm = new Game();
        gm.setRules(new Suicide());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testShashki() {
        Game gm = new Game();
        gm.setRules(new Shashki());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testChess() {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }


}
