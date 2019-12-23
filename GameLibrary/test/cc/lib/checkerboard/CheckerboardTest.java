package cc.lib.checkerboard;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.FileUtils;

public class CheckerboardTest extends TestCase {

    final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void setUp() throws Exception {
        Utils.setRandomSeed(0);
        File dir = new File("outputs");
        if (!dir.exists())
            assertTrue(dir.mkdir());
        else
            FileUtils.deleteDirContents(dir);
    }

    public void test() {

        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        for (int i=0; i<10; i++) {
            System.out.println(gm);
            gm.runGame();
            gm.runGame();
        }
        System.out.println(gm);
    }

    public void testAIFindsWinningMoveCheckers() throws Exception {
        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.init(8, 8);
        gm.clear();
        gm.setPiece(0, 0, Game.NEAR, PieceType.KING);
        gm.setPiece(4, 2, Game.FAR, PieceType.KING);
        gm.setPiece(4, 4, Game.FAR, PieceType.KING);
        gm.setTurn(Game.FAR);

        Game gm2 = gm.deepCopy();

        runGame(gm, true, 10);

        AIPlayer.algorithm = AIPlayer.Algorithm.miniMaxAB;
        runGame(gm2, true, 10);

        assertEquals(gm2.getInfoString(), gm.getInfoString());
    }

    public void testAIFindsWinningMoveChess() {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayer(Game.FAR, new AIPlayer(3));
        gm.setPlayer(Game.NEAR, new AIPlayer(3));
        gm.newGame();
        gm.clear();
        gm.setPiece(0, 7, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(0, 0, Game.NEAR, PieceType.QUEEN);
        gm.setPiece(0, 3, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.NEAR);
        runGame(gm, true, 15);
    }

    public void testAIFindsWinningMoveChess1() {
        Game gm = new Game();
        gm.setRules(new Chess());
        AIPlayer p;
        gm.setPlayer(Game.FAR, new AIPlayer(3));
        gm.setPlayer(Game.NEAR, p=new AIPlayer(3));
        gm.newGame();
        gm.clear();
        gm.setPiece(0, 7, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(1, 4, Game.NEAR, PieceType.QUEEN);
        gm.setPiece(2, 5, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.NEAR);

        runGame(gm, true, 10);
    }

    public void testChessDetermineDraw() {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        gm.clear();
        gm.setPiece(0, 7, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(0, 1, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.FAR);
        gm.refreshGameState();
        System.out.println(gm.getInfoString());
        assertTrue(gm.isDraw());
        gm.setPiece(0, 2, Game.NEAR, PieceType.BISHOP);
        gm.refreshGameState();
        System.out.println(gm.getInfoString());
        assertTrue(gm.isDraw());
        gm.clearPiece(0, 2);
        gm.setPiece(0, 2, Game.FAR, PieceType.BISHOP);
        gm.refreshGameState();
        System.out.println(gm.getInfoString());
        assertTrue(gm.isDraw());
    }


    public void testCheckers() {

        AIPlayer.randomizeDuplicates = false;
        AIPlayer.movePathNodeToFront = false;

        Game gm1 = new Game();
        gm1.setRules(new Checkers());
        gm1.setPlayer(Game.FAR, new AIPlayer(3));
        gm1.setPlayer(Game.NEAR, new AIPlayer(3));
        gm1.newGame();
        runGame(gm1);

        AIPlayer.algorithm = AIPlayer.Algorithm.miniMaxAB;
        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayer(Game.FAR, new AIPlayer(3));
        gm.setPlayer(Game.NEAR, new AIPlayer(3));
        gm.newGame();
        runGame(gm);

        assertEquals(gm.getInfoString(), gm1.getInfoString());

    }

    public void testCheckersMiniMaxABvsNegimax() {

        AIPlayer.randomizeDuplicates = false;
        AIPlayer.movePathNodeToFront = false;

        AIPlayer.algorithm = AIPlayer.Algorithm.minimax;
        Game gm1 = new Game();
        gm1.setRules(new Checkers());
        gm1.setPlayer(Game.FAR, new AIPlayer(3));
        gm1.setPlayer(Game.NEAR, new AIPlayer(3));
        gm1.newGame();
        runGame(gm1);

        AIPlayer.algorithm = AIPlayer.Algorithm.negamax;
        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayer(Game.FAR, new AIPlayer(3));
        gm.setPlayer(Game.NEAR, new AIPlayer(3));
        gm.newGame();
        runGame(gm);

        assertEquals(gm.getInfoString(), gm1.getInfoString());

    }

    public void testUgolki() {
        Game gm = new Game();
        gm.setRules(new Ugolki());
        gm.setPlayer(Game.FAR, new AIPlayer(3));
        gm.setPlayer(Game.NEAR, new AIPlayer(3));
        gm.newGame();
        runGame(gm);
    }

    public void testChess() {
        AIPlayer.algorithm = AIPlayer.Algorithm.miniMaxAB;
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayer(Game.FAR, new AIPlayer(3));
        gm.setPlayer(Game.NEAR, new AIPlayer(3));
        gm.newGame();
        runGame(gm);

    }

    public void testChessSwapPawn() {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayer(Game.FAR, new AIPlayer(5));
        gm.setPlayer(Game.NEAR, new AIPlayer(5));
        gm.newGame();
        gm.clear();
        gm.setPiece(5, 5, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(7, 7, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setPiece(6, 0, Game.FAR, PieceType.PAWN);
        gm.setTurn(Game.FAR);
        runGame(gm);
    }

    private void runGame(Game gm) {
        runGame(gm, false, 500);
    }

    private void runGame(Game gm, boolean expectWinner, final int maxIterations) {

        long totalNodesEvaluated = 0;
        long t = System.currentTimeMillis();
        int i;
        for (i=0; i<maxIterations; i++) {
            if (gm.getSelectedPiece() == null)
                System.out.println("********* Frame: " + i + "\n" + gm.getInfoString());
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
        System.out.println(String.format("runGame time = %3.2f seconds\ntotal nodes evaluated = %d\naverage time per turn = %3.2f seconds",
                dtSecs,
                totalNodesEvaluated,
                dtSecs/i));
        assertTrue("No winner!", !expectWinner || gm.getWinner() != null);
        System.out.println("GAME OVER\n" + gm.getInfoString());
    }

    public void testShashki() throws Exception {

        Game gm = new Game();
        gm.setRules(new Shashki());
        gm.setPlayer(0, new Player());
        gm.setPlayer(1, new Player());
        gm.newGame();
        gm.clear();
        gm.setTurn(Game.NEAR);
        gm.setPiece(0, 0, Game.NEAR, PieceType.KING);
        gm.setPiece(1, 1, Game.FAR, PieceType.CHECKER);
        gm.setPiece(3, 1, Game.FAR, PieceType.CHECKER);
        gm.setPiece(1, 3, Game.FAR, PieceType.CHECKER);
        gm.setPiece(5, 1, Game.FAR, PieceType.CHECKER);
        System.out.println(gm.getInfoString());
        List<Move> moves = gm.getMoves();
        assertTrue(moves.size() == 1);
        gm.executeMove(moves.get(0));
        System.out.println(gm.getInfoString());
        moves = gm.getMoves();
        assertTrue(moves.size() == 1);
    }
}
