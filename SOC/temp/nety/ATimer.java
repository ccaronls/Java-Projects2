package cc.game.soc.nety;

import java.util.Timer;
import java.util.TimerTask;

public abstract class ATimer extends TimerTask {

	Timer timer;
	boolean timerRunning = true;
	boolean timeout = false;
	
	/**
	 * 
	 * @param timeOutMS
	 */
	public void start(long timeOutMS) {
		if (isRunning())
			stop();
		timer = new Timer();
		timer.schedule(this, timeOutMS);
		timerRunning = true;
	}
	
	/**
	 * 
	 *
	 */
	public void stop() {
		cancel();
		if (timer != null)
			timer.cancel();
		timer = null;
		timeout = false;
		timerRunning = false;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return timerRunning;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isTimedOut() {
		return timeout;
	}

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		timeout = true;
		onTimeout();
		timer.purge();
	}
	
	
	public abstract void onTimeout();
}
