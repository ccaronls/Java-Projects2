package cc.game.soc.ui;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;

/**
 * Animation that blocks SOC thread until finished
 * @author chriscaron
 *
 */
public abstract class UIBlockingAnimation extends AAnimation<AGraphics> {

	UIBlockingAnimation(long duration) {
		super(duration, 0);
	}

	@Override
	public final void onDone() {
        synchronized (this) {
            notify();
        }
	}

	@Override
	public final void onStarted() {
        synchronized (this) {
            try {
                wait(getDuration() + 500);
            } catch (Exception e) {}
        }
	}

	
	
}
