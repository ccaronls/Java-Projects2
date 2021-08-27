package cc.lib.game;

import junit.framework.TestCase;

import org.junit.Assert;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

public class TestUtils extends TestCase {

    public void test_isBoxesOverlapping() {
        Rectangle A = new Rectangle(0,0,1,1);
        Rectangle B = new Rectangle(-1,-1,3,3);
        Rectangle C = new Rectangle(-2, 0, 5, 1);
        
        Rectangle r0 = A;
        Rectangle r1 = B;       
        assertTrue(Utils.isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height));
        assertTrue(Utils.isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height));
        
        r0 = A;
        r1 = A;
        assertTrue(Utils.isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height));
        assertTrue(Utils.isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height));
        
        r0 = A;
        r1 = C;
        assertTrue(Utils.isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height));
        assertTrue(Utils.isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height));

        r0 = B;
        r1 = C;
        assertTrue(Utils.isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height));
        assertTrue(Utils.isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height));

    }
    
    public void test_isBoxesOverlappingNeg() {
        Rectangle A = new Rectangle(0,0,1,1);
        Rectangle B = new Rectangle(1,1,2,2);
        Rectangle C = new Rectangle(100,100,1,1);
        
        Rectangle r0 = A;
        Rectangle r1 = B;       
        assertFalse(Utils.isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height));
        assertFalse(Utils.isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height));
        
        r0 = A;
        r1 = C;
        assertFalse(Utils.isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height));
        assertFalse(Utils.isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height));
        
        r0 = A;
        r1 = C;
        assertFalse(Utils.isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height));
        assertFalse(Utils.isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height));

        r0 = B;
        r1 = C;
        assertFalse(Utils.isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height));
        assertFalse(Utils.isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height));
        
        
    }
    
    public void testBubblesort() {
    	Integer [] sortable = {  4,   2,   1,   6,   2,   8,   0 };
    	String [] letters =   { "D", "C", "B", "E", "C", "G", "A" };
    	
    	Utils.bubbleSort(sortable, letters);
    	
    	System.out.println(Arrays.toString(sortable));
    	System.out.println(Arrays.toString(letters));
    }

    public void testWeightedRandomGet() {
        List<String> items = Arrays.asList("A", "B", "C");
        int [] weights = { 1, 0, 1 };

        for (int i=0; i<10000; i++)
            assertFalse(Utils.chooseRandomWeightedItem(items, weights).equals("B"));
    }

    public void testRotateArray() {

        Integer [] array = { 1,1,1,3,3,3 };
        Integer [] result = new Integer[3];
        Utils.rotate(array, result);
        Assert.assertTrue(Arrays.equals(result, new Integer[] { 1,1,1 }));
        Assert.assertTrue(Arrays.equals(array, new Integer[] { 3,3,3,1,1,1 }));

    }

    public void testColorParsing() throws Exception {

        assertEquals(GColor.fromString("[255,0,0,0]"), GColor.BLACK);
        assertEquals(GColor.fromString("ARGB[255,255,255,255]"), GColor.WHITE);

    }
}
