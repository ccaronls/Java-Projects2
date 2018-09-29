package cc.board;

import junit.framework.TestCase;

import java.io.File;

import cc.lib.board.CustomBoard;

public class CustomBoardTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        System.out.println("TEST " + getName());
    }

    public void testSquare() {

        CustomBoard b = new CustomBoard();

        b.addVertex(10, 10);
        b.addVertex(20, 10);
        b.addVertex(20, 20);
        b.addVertex(10,  20);

        b.addEdge(0, 1);
        b.addEdge(1, 2);
        b.addEdge(2, 3);
        b.addEdge(0, 3);

        b.compute();

        assertEquals(1, b.getNumCells());
        assertTrue(b.getCell(0).getNumAdjVerts() == 4);
    }

    public void test2AdjacentSquares() {

        CustomBoard b = new CustomBoard();

        b.addVertex(10, 10);
        b.addVertex(20, 10);
        b.addVertex(20, 20);
        b.addVertex(10,  20);
        b.addVertex(20, 30);
        b.addVertex(10, 30);

        b.addEdge(0, 1);
        b.addEdge(1, 2);
        b.addEdge(2, 3);
        b.addEdge(0, 3);
        b.addEdge(2, 4);
        b.addEdge(4, 5);
        b.addEdge(5, 3);

        b.compute();

        assertEquals(2, b.getNumCells());
        assertEquals(4, b.getCell(0).getNumAdjVerts());
        assertEquals(4, b.getCell(1).getNumAdjVerts());
        assertEquals(2, b.getEdge(2, 3).getNumAdjCells());
    }

    public void test3Cells() {
        CustomBoard b = new CustomBoard();

        b.addVertex(10, 10);
        b.addVertex(20, 0);
        b.addVertex(30, 5);
        b.addVertex(25, 15);
        b.addVertex(5, 25);
        b.addVertex(35, 30);
        b.addVertex(40, 10);

        b.addEdge(0, 1);
        b.addEdge(1, 2);
        b.addEdge(2, 3);
        b.addEdge(0, 4);
        b.addEdge(0, 3);
        b.addEdge(3, 4);
        b.addEdge(3, 5);
        b.addEdge(5, 6);
        b.addEdge(2, 6);

        b.compute();
        assertEquals(3, b.getNumCells());
    }

    public void testSavedBoard() throws Exception {
        CustomBoard b = new CustomBoard();
        b.loadFromFile(new File("bb.backup.board"));
        b.compute();
    }
}
