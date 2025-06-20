package cc.lib.checkerboard;

import junit.framework.TestCase;

import org.junit.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.FileUtils;

public class CheckerboardTest extends TestCase {

    final Logger log = LoggerFactory.getLogger(getClass());



    private void runGame(Game gm) {
        runGame(gm, false, 10);
    }

    private void runGame(Game gm, boolean expectWinner, final int maxIterations) {

        long totalNodesEvaluated = 0;
        long t = System.currentTimeMillis();
        int i;
        for (i=0; i<maxIterations; i++) {
            if (gm.getSelectedPiece() == null) {
                System.out.println("********* Frame: " + i + "\n" + gm);
                gm.trySaveToFile(new File("outputs/" + gm.getRules().getClass().getSimpleName() + "_" + i + ".txt"));
            }
            gm.runGame();
            totalNodesEvaluated += AIPlayer.stats.getEvalCount();
            //gm.trySaveToFile(new File("minimaxtest_testcheckrs.game"));
            if (AIPlayer.lastSearchResult != null) {
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
        System.out.println("GAME OVER\n" + gm);
    }

    @Override
    protected void setUp() throws Exception {
        Utils.setRandomSeed(0);
        Utils.setDebugEnabled();
        File dir = new File("outputs");
        if (!dir.exists())
            assertTrue(dir.mkdir());
        else
            FileUtils.deleteDirContents(dir);
    }

    public void test() {

        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.newGame();
        for (int i=0; i<10; i++) {
            System.out.println(gm);
            gm.runGame();
            gm.runGame();
        }
        System.out.println(gm);
    }

    public void testInfiniteSelfJumps() {

        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.newGame();
        gm.clear();
        gm.setPiece(0, 0, Game.FAR, PieceType.CHECKER);
        gm.setPiece(5, 4, Game.NEAR, PieceType.KING);
        gm.setPiece(4, 3, Game.NEAR, PieceType.CHECKER);
        gm.setPiece(2, 3, Game.NEAR, PieceType.CHECKER);
        gm.setPiece(2, 5, Game.NEAR, PieceType.CHECKER);
        gm.setPiece(4, 5, Game.NEAR, PieceType.CHECKER);
        gm.setTurn(Game.NEAR);

        System.out.println(gm);
        gm.runGame();
        System.out.println(gm);
    }

    public void testAIFindsWinningMoveCheckers() throws Exception {
        Rules r = new Checkers();
        Game gm = new Game();
        gm.setRules(r);
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.clear();
        gm.setPiece(0, 0, Game.NEAR, PieceType.KING);
        gm.setPiece(4, 2, Game.FAR, PieceType.KING);
        gm.setPiece(4, 4, Game.FAR, PieceType.KING);
        gm.setTurn(Game.FAR);

        Game gm2 = gm.deepCopy();

        runGame(gm, true, 10);

        runGame(gm2, true, 10);

        assertEquals(gm2.toString(), gm.toString());
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
        gm.setPiece(0, 4, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.NEAR);
        runGame(gm, true, 50);
    }

    public void testAIFindsWinningMoveChess1() throws IOException {
        Game gm = new Game();
        gm.setRules(new Chess());
        AIPlayer p;
        gm.setPlayers(new AIPlayer(3), p=new AIPlayer(3));
        gm.clear();
        gm.setPiece(0, 7, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(1, 4, Game.NEAR, PieceType.QUEEN);
        gm.setPiece(2, 5, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.NEAR);
        runGame(gm, true, 1);


    }

    public void testCheckmateDetected() throws IOException {
        Game gm = new Game();
        gm.setRules(new Chess());
        AIPlayer p;
        gm.setPlayers(new AIPlayer(3), p=new AIPlayer(3));
        gm.clear();
        gm.setPiece(0, 7, Game.FAR, PieceType.CHECKED_KING);
        gm.setPiece(1, 6, Game.NEAR, PieceType.QUEEN);
        gm.setPiece(2, 5, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.FAR);
        gm.refreshGameState();
        long score = AIPlayer.Companion.evaluate(gm, 0);
        assertEquals(Long.MIN_VALUE, score);

        assertEquals(Game.NEAR, gm.getRules().getWinner(gm));
    }

    public void testDrawDetected() throws IOException {
        Game gm = new Game();
        gm.setRules(new Chess());
        AIPlayer p;
        gm.setPlayers(new AIPlayer(3), p=new AIPlayer(3));
        gm.clear();
        gm.setPiece(0, 7, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(1, 5, Game.NEAR, PieceType.QUEEN);
        gm.setPiece(2, 5, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.FAR);
        assertEquals(Game.NOP, gm.getRules().getWinner(gm));
    }

    public void testChessDetermineDraw() {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.newGame();
        gm.clear();
        gm.setPiece(0, 7, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(0, 1, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.FAR);
        gm.refreshGameState();
        System.out.println(gm.toString());
        assertTrue(gm.isDraw());
        gm.setPiece(0, 2, Game.NEAR, PieceType.BISHOP);
        gm.refreshGameState();
        System.out.println(gm.toString());
        assertTrue(gm.isDraw());
        gm.clearPiece(0, 2);
        gm.setPiece(0, 2, Game.FAR, PieceType.BISHOP);
        gm.refreshGameState();
        System.out.println(gm.toString());
        assertTrue(gm.isDraw());
    }


    public void x_testCheckers_deterministic() {

        Game gm1 = new Game();
        gm1.setRules(new Checkers());
        gm1.setPlayer(Game.FAR, new AIPlayer(3));
        gm1.setPlayer(Game.NEAR, new AIPlayer(3));
        gm1.newGame();
        runGame(gm1);

        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayer(Game.FAR, new AIPlayer(3));
        gm.setPlayer(Game.NEAR, new AIPlayer(3));
        gm.newGame();
        runGame(gm);

        assertEquals(gm.toString(), gm1.toString());

    }

    public void testCheckers() {
        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayers(new AIPlayer(3), new AIPlayer(3));
        System.out.println(gm.toString());
        gm.newGame();
        runGame(gm);
    }

    public void testCanadianDraughts() {
        Game gm = new Game();
        gm.setRules(new CanadianDraughts());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testSuicide() {
        Game gm = new Game();
        gm.setRules(new Suicide());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void x_testCheckersMiniMaxABvsNegimax() {

        AIPlayer.randomizeDuplicates = false;
        AIPlayer.movePathNodeToFront = false;

        AIPlayer.algorithm = AIPlayer.Algorithm.minimax;
        Game gm1 = new Game();
        gm1.setRules(new Checkers());
        gm1.setPlayers(new AIPlayer(), new AIPlayer());
        gm1.newGame();
        runGame(gm1);

        AIPlayer.algorithm = AIPlayer.Algorithm.negamax;
        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.newGame();
        runGame(gm);

        assertEquals(gm.toString(), gm1.toString());

    }

    public void testUgolki() {
        Game gm = new Game();
        gm.setRules(new Ugolki());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testChess() {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.newGame();
        runGame(gm);

    }

    public void x_testChess_crashfix() throws Exception {
        Game gm = new Game();
        gm.loadFromFile(new File("inputs/Chess_60.txt"));
        runGame(gm);

    }

    public void testChessSwapPawn() {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.clear();
        gm.setPiece(5, 5, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(7, 7, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setPiece(6, 0, Game.FAR, PieceType.PAWN);
        gm.setTurn(Game.FAR);
        runGame(gm);
    }

    public void testShashki() throws Exception {

        Game gm = new Game();
        gm.setRules(new Shashki());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        gm.newGame();
        gm.clear();
        gm.setTurn(Game.NEAR);
        gm.setPiece(0, 0, Game.NEAR, PieceType.KING);
        gm.setPiece(1, 1, Game.FAR, PieceType.CHECKER);
        gm.setPiece(3, 1, Game.FAR, PieceType.CHECKER);
        gm.setPiece(1, 3, Game.FAR, PieceType.CHECKER);
        gm.setPiece(5, 1, Game.FAR, PieceType.CHECKER);
        System.out.println(gm.toString());
        List<Move> moves = gm.getMoves();
        assertTrue(moves.size() == 1);
        gm.executeMove(moves.get(0));
        System.out.println(gm.toString());
        moves = gm.getMoves();
        assertTrue(moves.size() == 1);
    }

    public void x_test_x() throws Exception {
        Game game = new Game();
        game.loadFromFile(new File("inputs/Chess_18.txt"));
        System.out.println(game);
        game.loadFromFile(new File("game_miniMaxAB_error.txt"));

        System.out.println(game);
    }

    public void testGetPieces() {
        Game gm = new Game();
        gm.setRules(new DragonChess());
        gm.setPlayers(new AIPlayer(), new AIPlayer());
        int num = 0;
        for (Piece p : gm.getPieces(-1)) {
            System.out.println(p);
            num++;
        }
        Assert.assertEquals(32, num);
    }

    public void testGetPieces2() {
        Game gm = new Game();
        gm.setRules(new DragonChess());
        gm.setPlayers(new Player(), new Player());
        int num = 0;
        for (Piece p : gm.getPieces(Game.NEAR)) {
            System.out.println(p);
            num++;
        }
        Assert.assertEquals(16, num);
    }

    public void testGetPieces3() {
        Game gm = new Game();
        gm.setRules(new DragonChess());
        gm.setPlayers(new Player(), new Player());
        int num = 0;
        for (Piece p : gm.getPieces(1)) {
            System.out.println(p);
            num++;
        }
        Assert.assertEquals(16, num);
    }

    public void testGetPieces4() {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayers(new Player(), new Player());
        gm.getRules().init(gm);
        int num = 0;
        for (Piece p : gm.getPieces(-1)) {
            System.out.println(p.getType());
            num++;
        }
        Assert.assertEquals(32, num);
    }

    public void testChess1() throws Exception {
        Game game = new Game();
        game.deserialize(FileUtils.openFileOrResource("testResources/chess1.save"));
        //game.setPlayer(0, new Player());
        //game.setPlayer(1, new Player());
        System.out.println(game);

        int num=1;
        HashSet<Piece> set = new HashSet<>();
        for (Piece p : game.getPieces(-1)) {
            Assert.assertFalse(set.contains(p));
            set.add(p);
            System.out.println(num++ + ":" + p);
        }

        List<Move> moves = game.getRules().computeMoves(game);
        for (Move m : moves) {
            System.out.println(m);
        }
    }
}
