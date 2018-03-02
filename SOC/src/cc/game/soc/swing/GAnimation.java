package cc.game.soc.swing;

import cc.lib.game.AAnimation;
import java.awt.Graphics;

/**
 * Animation that blocks SOC thread until finished
 * @author chriscaron
 *
 */
public abstract class GAnimation extends AAnimation<Graphics> {
	GAnimation(long duration) {
		super(duration, 0);
	}

	@Override
	public void onDone() {
        synchronized (this) {
            notify();
        }
	}

}
