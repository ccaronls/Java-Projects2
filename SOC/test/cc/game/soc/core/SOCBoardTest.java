package cc.game.soc.core;

import java.io.File;
import java.io.IOException;

import cc.lib.game.Utils;
import junit.framework.TestCase;

public class SOCBoardTest extends TestCase {

    public void testGenBoard() throws IOException {
        Board b = new Board();
        b.generateDefaultBoard();
//        b.serialize(System.out);
        File file = new File("testboard.sav");
        b.saveToFile(file);
        Board bb = new Board();
        bb.loadFromFile(file);
        assertEquals(b, bb);
    }
    
    public void testLoadBoard() throws IOException {
        Board b = new Board();
        b.loadFromFile(new File("boards/testboard.txt"));
    }
    
    public void testBoardVertex() throws Exception {
        Board b = new Board();
        b.loadFromFile(new File("boards/testBoard3.txt"));
        for (int i=0; i<b.getNumVerts(); i++) {
            Vertex v = b.getVertex(i);
            for (int ii=0; ii<v.getNumAdjacent(); ii++) {
                Vertex vv = b.getVertex(v.getAdjacent()[ii]);
                assertNotNull(vv);
            }
        }
    }    
}
