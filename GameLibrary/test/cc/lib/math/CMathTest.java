package cc.lib.math;

import junit.framework.TestCase;

import cc.lib.game.GRectangle;

public class CMathTest extends TestCase {

    public void testNormalDistribution() {
        for (int i = 0; i < 10; i++) {
            System.out.println(CMath.normalDistribution(i, 5));
        }
    }

    public void test_RotateVector() {

        final float[] v0 = {1, 0};
        float[] r0 = {0, 0};

        float[][] results = {
                {0, 1, 0},
                {90, 0, 1},
                {180, -1, 0},
                {270, 0, -1}
        };

        for (int i = 0; i < results.length; i++) {
            float deg = results[i][0];
            float newx = results[i][1];
            float newy = results[i][2];
            CMath.rotateVector(v0, r0, deg);
            System.out.println("testing deg[" + deg + "] v0[" + v0[0] + "," + v0[1] + "] r0 [" + r0[0] + "," + r0[1] + "]");
            assertTrue(CMath.isAlmostEqual(newx, r0[0]) && CMath.isAlmostEqual(newy, r0[1]));
        }

    }

    public void testRotate() {

        GRectangle r = new GRectangle(-2, -4, 4, 8);
        GRectangle r2 = r.rotated(10);

        System.out.println("r=" + r + "\nr2=" + r2);


    }
}