package cc.game.soc.swing;

/**
 * Animation that blocks SOC thread until finished
 * @author chriscaron
 *
 */
public abstract class BlockingAnimation extends Animation {

	BlockingAnimation(long duration) {
		super(duration, 0);
	}

	@Override
	final void onDone() {
        synchronized (this) {
            notify();
        }
	}

	@Override
	final void onStarted() {
        synchronized (this) {
            try {
                wait(getDuration() + 500);
            } catch (Exception e) {}
        }
	}

	
	
}
