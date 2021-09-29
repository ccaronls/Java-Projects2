package cc.lib.game;

import junit.framework.TestCase;

/**
 * Created by Chris Caron on 9/28/21.
 */
public class InterpolatorTest extends TestCase {

    class FloatInterp implements IInterpolator<Float> {

        final float start, end;

        public FloatInterp(float start, float end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public Float getAtPosition(float position) {
            return (end-start)*position + start;
        }
    }

    public void testChainInterp() {

        ChainInterpolator<Float> c = new ChainInterpolator<>(
                new FloatInterp(2, 4),
                new FloatInterp(10, 20),
                new FloatInterp(5000, 10000)
        );

        for (int i=0; i<=100; i++) {

            float pos = 0.01f * i;

            System.out.println(String.format("%-1.4f : %3.2f", pos, c.getAtPosition(pos)));


        }


    }

}
