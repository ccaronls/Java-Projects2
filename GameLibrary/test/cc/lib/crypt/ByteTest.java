package cc.lib.crypt;

import cc.lib.game.Utils;
import junit.framework.TestCase;

public class ByteTest extends TestCase {

    public void test() {
        
        for (int i=0; i<256; i++) {
            byte b = (byte)i;
            int x = b;
            x = (x+256)%256;
            System.out.println(String.format("%-5d  ->  %-5d   ->%d", i, b, x));
        }
        
        
    }
    
    
    public void testFastShiftBitVector() {
        BitVector v = new BitVector(1);
        v.pushBack(0x08421, 32);
        v.pushBack(0x08421, 32);
        v.pushBack(0x08421, 32);
        System.out.println("    0                               32");
        System.out.println("    v                               v");
        while (v.getLen() > 0) {
            System.out.println("v = " + v);
            v.shiftRight(1);
        }

        int [][] values = new int[100][];
        for (int i=0; i<values.length; i++) {
            values[i] = new int[2];
            values[i][0] = Utils.rand()%1000;
            values[i][1] = Utils.rand()%100;
        }
        
    }
}
