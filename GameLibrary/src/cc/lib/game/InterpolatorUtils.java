package cc.lib.game;

import cc.lib.game.IInterpolator;

/**
 * Created by Chris Caron on 8/18/21.
 */
public class InterpolatorUtils {

    public static IInterpolator<Float> FLOAT_ZERO = new IInterpolator<Float>() {
        @Override
        public Float getAtPosition(float position) {
            return 0f;
        }
    };

    public static IInterpolator<Float> linear(float start, float end) {
        return new IInterpolator<Float>() {
            @Override
            public Float getAtPosition(float position) {
                return start + (end-start) * position;
            }
        };
    }
}
