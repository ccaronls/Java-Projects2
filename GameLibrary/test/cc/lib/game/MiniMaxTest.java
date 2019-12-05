package cc.lib.game;

import junit.framework.TestCase;

import java.io.File;

import cc.lib.checkerboard.*;

public class MiniMaxTest extends TestCase {

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

    public void testCheckers() {

        Game gm = new Game();
        gm.setRules(new Checkers());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        int i = 0;
        for (i=0; i<500; i++) {
            if (gm.getSelectedPiece() == null)
                System.out.println("********* Frame: " + i + " " + gm);
            gm.runGame();
            if (gm.getWinner() != null)
                break;
        }
        gm.trySaveToFile(new File("minimaxtest_testcheckrs.game"));
        System.out.println(gm);
        assertNotNull("No winner!", gm.getWinner());
        System.out.println("Winner: " + gm.getWinner().getColor() + " in " + i + " runGameCalls");
    }

    public void testChess() throws Exception {
        Game gm = new Game();
        gm.setRules(new Chess());
        gm.setPlayer(Game.FAR, new AIPlayer());
        gm.setPlayer(Game.NEAR, new AIPlayer());
        gm.newGame();
        Game copy = new Game();
        copy.copyFrom(gm);
        System.out.println(gm);
        assertTrue(boardsEqual(gm, copy));
        long num = 0;
        for (Move m : gm.getMoves()) {
            num++;
            gm.executeMove(m);
            for (Move m2 : gm.getMoves()) {
                num++;
                gm.executeMove(m2);
                for (Move m3 : gm.getMoves()) {
                    num ++;
                    gm.executeMove(m3);
                    for (Move m4 : gm.getMoves()) {
                        num++;
                        gm.executeMove(m4);
                        for (Move m5 : gm.getMoves()) {
                            num++;
                            gm.executeMove(m5);
                            gm.undo();
                        }
                        gm.undo();
                    }
                    gm.undo();
                }
                gm.undo();
            }
            gm.undo();
            assertTrue(boardsEqual(gm, copy));
        }
        System.out.println("Evaluated " + num + " moves");

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
