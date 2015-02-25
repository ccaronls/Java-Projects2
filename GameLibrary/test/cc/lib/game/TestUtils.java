package cc.lib.game;

import java.awt.Rectangle;

import junit.framework.TestCase;

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
    
    public void test_RotateVector() {
        
        final float [] v0 = { 1,0 };
        float [] r0 = {0,0};
        
        float [][] results = {
                { 0, 1, 0 },
                { 90, 0, 1 },
                { 180, -1, 0},
                { 270, 0, -1}
        };
        
        for (int i=0; i<results.length; i++) {
            float deg = results[i][0];
            float newx = results[i][1];
            float newy = results[i][2];
            Utils.rotateVector(v0, r0, deg);
            System.out.println("testing deg[" + deg + "] v0[" + v0[0] + "," + v0[1] + "] r0 [" + r0[0] + "," + r0[1] + "]");
            assertTrue(Utils.isAlmostEqual(newx, r0[0]) && Utils.isAlmostEqual(newy, r0[1]));
        }
        
    }
    
}
