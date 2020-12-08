package cc.lib.zombicide;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;

public abstract class ZAnimation extends AAnimation<AGraphics> {

    public ZAnimation(long durationMSecs) {
        super(durationMSecs);
    }

    public ZAnimation(long durationMSecs, int repeats) {
        super(durationMSecs, repeats);
    }

    public ZAnimation(long durationMSecs, int repeats, boolean oscilateOnRepeat) {
        super(durationMSecs, repeats, oscilateOnRepeat);
    }

}
