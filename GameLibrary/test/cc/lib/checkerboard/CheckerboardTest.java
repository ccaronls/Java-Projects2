package cc.lib.checkerboard;

import junit.framework.TestCase;

import java.io.File;

import cc.lib.game.Utils;

public class CheckerboardTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        Utils.setRandomSeed(0);
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

    public void testAIFindsWinningMoveCheckers() {
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
        runGame(gm);
    }

    public void testAIFindsWinningMoveChess() {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        gm.clear();
        gm.setPiece(0, 7, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(0, 0, Game.NEAR, PieceType.QUEEN);
        gm.setPiece(0, 1, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.NEAR);
        runGame(gm, true);
    }

    public void testAIFindsWinningMoveChess1() {
        Game gm = new Game();
        gm.setRules(new Chess());
        AIPlayer p;
        gm.setPlayer(Game.FAR, new AIPlayer(4));
        gm.setPlayer(Game.NEAR, p=new AIPlayer(4));
        gm.newGame();
        gm.clear();
        gm.setPiece(0, 7, Game.FAR, PieceType.UNCHECKED_KING);
        gm.setPiece(1, 4, Game.NEAR, PieceType.QUEEN);
        gm.setPiece(2, 5, Game.NEAR, PieceType.UNCHECKED_KING);
        gm.setTurn(Game.NEAR);

        runGame(gm, true);
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
        System.out.println(gm);
        assertTrue(gm.isDraw());
        gm.setPiece(0, 2, Game.NEAR, PieceType.BISHOP);
        System.out.println(gm);
        assertTrue(gm.isDraw());
        gm.clearPiece(0, 2);
        gm.setPiece(0, 2, Game.FAR, PieceType.BISHOP);
        System.out.println(gm);
        assertTrue(gm.isDraw());
    }


    public void testCheckers() {

        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        runGame(gm);
    }

    public void testChess() {
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
        runGame(gm, false);
    }

    private void runGame(Game gm, boolean expectWinner) {

        int i;
        for (i=0; i<500; i++) {
            if (gm.getSelectedPiece() == null)
                System.out.println("********* Frame: " + i + " " + gm);
            gm.runGame();
            gm.trySaveToFile(new File("minimaxtest_testcheckrs.game"));
            if (i == 249) break;
            if (gm.isGameOver())
                break;
        }
        assertTrue("No winner!", !expectWinner || gm.getWinner() != null);
        if (gm.isGameOver()) {
            Player p = gm.getWinner();
            if (p == null) {
                System.out.println("   D R A W   G A M E");
            } else {
                System.out.println("Player " + p.getColor() + " WINS!");
            }
        } else {
            assertTrue("game failed to find winner", false);
        }
    }

    boolean boardsEqual(Game g0, Game g1) {
        assertEquals(g0.getRanks(), g1.getRanks());
        assertEquals(g0.getColumns(), g1.getColumns());
        for (int r = 0; r<g0.getRanks(); r++) {
            for (int c = 0; c<g0.getColumns(); c++) {
                Piece p0, p1;
                if (!(p0=g0.getPiece(r, c)).equals((p1=g1.getPiece(r, c)))) {
                    System.out.println("Piece at position " + r + "," + c + " differ: " + p0 + "\n" + p1);
                    return false;
                }
            }
        }
        return true;
    }

}
