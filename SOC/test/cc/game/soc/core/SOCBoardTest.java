package cc.game.soc.core;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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
            for (int ii=0; ii<v.getNumAdjacentVerts(); ii++) {
                Vertex vv = b.getVertex(v.getAdjacentVerts()[ii]);
                assertNotNull(vv);
            }
        }
    }    
    
    public void testGetNumVertsOfType() {
    	SOC soc = new SOC();
    	assertTrue(soc.load("/Users/chriscaron/.soc/socsavegame.txt"));
    	Board b = soc.getBoard();
    	
    	VertexType [] values = VertexType.values();
    	int [] counts = new int[values.length];
    	
    	for (int i=0; i<b.getNumAvailableVerts(); i++) {
    	    Vertex v = b.getVertex(i);
    		counts[v.getType().ordinal()] ++;
    	}
    	
    	Utils.shuffle(values);
    	
    	int sum = Utils.sum(counts);
    	
    	assertEquals(sum, b.getNumVertsOfType(0, values));
    	
    	for (VertexType v : VertexType.values()) {
    		assertEquals(counts[v.ordinal()], b.getVertIndicesOfType(0, v).size());
    		assertEquals(counts[v.ordinal()], b.getNumVertsOfType(0, v));
    	}
    	
    	for (int i=1; i<=3; i++) {
    		int numKnights = b.getNumVertsOfType(i, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE);
    		System.out.println("Player " + i + " has " + numKnights + " active knights");
    	}

    	for (int i=0; i<10; i++) {
    		VertexType [] v = new VertexType[Utils.rand() % 3 + 2];
    		for (int ii=0; ii<v.length; ii++) {
    			v[ii] = Utils.randItem(VertexType.values());
    		}
    		
    		sum = 0;
    		for (VertexType vv : v) {
    			sum += counts[vv.ordinal()];
    		}

    		System.out.println("Types=" + Arrays.toString(v) + " num=" + sum);

    		assertEquals(sum, b.getVertIndicesOfType(0, v).size());
    		assertEquals(sum, b.getNumVertsOfType(0, v));
    	}
    	
    	for (int iii=1; iii<4; iii++) {
        	for (int i=0; i<10; i++) {
        		VertexType [] v = new VertexType[Utils.rand() % 3 + 2];
        		for (int ii=0; ii<v.length; ii++) {
        			v[ii] = Utils.randItem(VertexType.values());
        		}
        		
        		int num = b.getVertIndicesOfType(iii, v).size();
        		System.out.println("Player " + iii + " types=" + Arrays.toString(v) + " num=" + num);
        		
        		assertEquals(num, b.getNumVertsOfType(iii, v));
        	}
    		
    	}
    	
    	
    	
    }
}
