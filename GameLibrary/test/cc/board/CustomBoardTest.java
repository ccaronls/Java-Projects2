package cc.board;

import junit.framework.TestCase;

import cc.lib.board.BCell;
import cc.lib.board.CustomBoard;

public class CustomBoardTest extends TestCase {

    public void test1() {

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

        assertTrue(b.getNumCells() == 2);
        assertTrue(b.getCell(0).getNumAdjVerts() == 4);
        assertTrue(b.getCell(1).getNumAdjVerts() == 4);
        BCell cell = b.getCell(0);

    }

}
