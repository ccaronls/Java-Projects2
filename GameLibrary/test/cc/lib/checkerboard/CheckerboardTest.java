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

    public void testSimpleBoardAI() {
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

    private void runGame(Game gm) {
        int i;
        for (i=0; i<500; i++) {
            if (gm.getSelectedPiece() == null)
                System.out.println("********* Frame: " + i + " " + gm);
            gm.runGame();
            gm.trySaveToFile(new File("minimaxtest_testcheckrs.game"));
            //if (i == 0) break;
            if (gm.isGameOver())
                break;
        }
        if (gm.isGameOver()) {
            Player p = gm.getWinner();
            if (p == null) {
                System.out.println("   D R A W   G A M E");
            } else {
                System.out.println("Player " + p.getColor() + " WINS!");
            }
        } else {
            assertTrue("game failed to end", false);
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
