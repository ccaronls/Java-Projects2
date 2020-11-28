package cc.lib.utils;

import junit.framework.TestCase;

public class GridTest extends TestCase {

    public void testPosIndex() {

        for (int i=0; i<2<<15; i++) {
            for (int ii=0; ii<2<<15; ii++) {
                Grid.Pos p = new Grid.Pos(i, ii);
                int index = p.getIndex();
                Grid.Pos t = Grid.Pos.fromIndex(index);
                assertEquals(p, t);
            }
        }

    }

}
