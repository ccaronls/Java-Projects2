package cc.lib.ui;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;

public abstract class UIAnimation extends AAnimation<AGraphics> {

    public UIAnimation(long durationMSecs) {
        super(durationMSecs);
    }

    public UIAnimation(long durationMSecs, int repeats) {
        super(durationMSecs, repeats);
    }

    public UIAnimation(long durationMSecs, int repeats, boolean oscilateOnRepeat) {
        super(durationMSecs, repeats, oscilateOnRepeat);
    }
}
