package cc.game.soc.ui;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;

/**
 * Animation that blocks SOC thread until finished
 * @author chriscaron
 *
 */
public abstract class UIAnimation extends AAnimation<AGraphics> {
	UIAnimation(long duration) {
		super(duration, 0);
	}

	@Override
	public void onDone() {
        synchronized (this) {
            notify();
        }
	}

}
