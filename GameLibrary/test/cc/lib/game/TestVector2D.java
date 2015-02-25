package cc.lib.game;

import junit.framework.TestCase;

public class TestVector2D extends TestCase {
/*
    public void testAddSub() {
        for (float i=0; i<100; i+=1) {
            assertTrue(Vector2D.newTemp(i,i).addEq(Vector2D.newTemp(-i,-i)).equals(Vector2D.newTemp()));
            //System.out.println("i=" + i);
            //if (i == 43) {
            //    System.out.println("x");
            //}
            assertTrue(Vector2D.newTemp(i,i).subEq(Vector2D.newTemp(i,i)).equals(Vector2D.newTemp()));
        }
            
    }
    
    public void testRotate() {
        for (int i=0; i<360; i++) {
            Vector2D v = new Vector2D(Utils.cosine(i) * 10, Utils.sine(i) * 10);
            for (int ii=0; ii<=180; ii++) {
                System.out.println("For i=" + i + ", ii=" + ii);
                if (i == 5 && ii == 180) {
                    System.out.println("x");
                }
                Vector2D v2 = v.rotate(ii, new Vector2D());
                assertEquals(Math.round(v.angleBetween(v2)), ii);
                //assertEquals(Math.round(v2.angleBetween(v)), -ii);
            }
        }

    }
    
    public void testAngleBetween() {
        
        assertEquals((int)Vector2D.newTemp(1,0).angleBetween(Vector2D.newTemp(0,1)), 90);
        assertEquals((int)Vector2D.newTemp(1,0).angleBetween(Vector2D.newTemp(1,1)), 45);
        assertEquals((int)Vector2D.newTemp(1,0).angleBetween(Vector2D.newTemp(1,0)), 0);
        assertEquals((int)Vector2D.newTemp(1,0).angleBetween(Vector2D.newTemp(-1,0)), 180);
        assertEquals((int)Vector2D.newTemp(1,0).angleBetween(Vector2D.newTemp(-1,1)), 135);
        assertEquals((int)Vector2D.newTemp(1,0).angleBetween(Vector2D.newTemp(2,0).rotateEq(30)), 30);

        for (int i=0; i<180; i+=2) {
            assertEquals(Math.round(Vector2D.newTemp(1,0).angleBetween(Vector2D.newTemp(1,0).rotateEq(i))), i);
        }
        
        
    }
    
    public void testMinMax() {
        Vector2D a = new Vector2D(10,10);
        Vector2D b = new Vector2D(-10,10);
        Vector2D c = new Vector2D(10,-10);
        Vector2D d = new Vector2D(-10,-10);
        
        assertEquals(a.min(b, Vector2D.newTemp()), (new Vector2D(-10,10)));
        assertEquals(b.min(c, Vector2D.newTemp()), (new Vector2D(-10,-10)));
        assertEquals(a.min(c, Vector2D.newTemp()), (new Vector2D(10,-10)));
        assertEquals(a.min(d, Vector2D.newTemp()), (new Vector2D(-10,-10)));
        
        
    }
  */  
}
