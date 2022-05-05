package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.AMultiPhaseAnimation;
import cc.lib.game.Utils;
import cc.lib.utils.GException;

public abstract class ZAnimation extends AMultiPhaseAnimation<AGraphics> {

    public ZAnimation(long ... durations) {
        super(Utils.toLongArray(durations));
    }

    public ZAnimation(long durationMSecs) {
        super(durationMSecs);
    }

    public ZAnimation(long durationMSecs, int repeats) {
        super(durationMSecs, repeats);
    }

    public ZAnimation(long durationMSecs, int repeats, boolean oscilateOnRepeat) {
        super(durationMSecs, repeats, oscilateOnRepeat);
    }

    @Override
    protected void drawPhase(AGraphics g, float position, int phase) {
        throw new GException("Unhandled");
    }
}
