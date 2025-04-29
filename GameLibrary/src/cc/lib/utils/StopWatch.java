package cc.lib.utils;

import java.io.IOException;

import cc.lib.reflector.Omit;
import cc.lib.reflector.RBufferedReader;
import cc.lib.reflector.Reflector;

public class StopWatch extends Reflector<StopWatch> {

    static {
        addAllFields(StopWatch.class);
    }

    @Omit
    protected long startTime = 0;
    @Omit
    protected long pauseTime = 0;
    @Omit
    protected long deltaTime = 0;
    @Omit
    protected long lastCaptureTime = 0;

    protected long curTime = 0;
    protected boolean started = false;
    
    /**
     * Start the stopwatch.  MUST be the first call
     */
    public void start() {
        curTime = 0;
        startTime = getClockMiliseconds();
        started = true;
        unpause();
    }

    public void stop() {
        started = false;
    }
    
    public boolean isPaused() {
        return pauseTime > 0;
    }

    public boolean isStarted() {
        return started;
    }
    
    /**
     * Pause the stop watch.  getTime/getDeltaTime will not advance until unpause called.
     */
    public void pause() {
        if (!isPaused()) {
            pauseTime = getClockMiliseconds();
        }
    }
    
    /**
     * Capture the current time and delta time.  Must be called before calling getTme, getDeltaTiime
     */
    public void capture() {
        if (started && pauseTime == 0) {
            long t = this.getClockMiliseconds();
            curTime += t - startTime;
            startTime = t;
            this.deltaTime = curTime - lastCaptureTime;
            this.lastCaptureTime = curTime;
        }
    }
    
    /**
     * Resume the stop watch if paused
     */
    public void unpause() {
        if (pauseTime > 0) {
            startTime = getClockMiliseconds();
            pauseTime = 0;
        }
    }
    
    /**
     * Get the time as of last call to capture()
     * @return
     */
    public long getTime() {
        if (started)
            return curTime;
        return 0;
    }
    
    /**
     * Get the delta time as of last call to capture()
     * @return
     */
    public long getDeltaTime() {
        if (started)
            return this.deltaTime;
        return 0;
    }
    
    /**
     * Override this to use a different clock mechanism if desired.
     * Default uses System.currentTimeMillis()
     * @return
     */
    protected long getClockMiliseconds() {
        return System.currentTimeMillis();
    }

    @Override
    protected void deserialize(RBufferedReader input) throws IOException {
        super.deserialize(input);
        if (started) {
            pauseTime = startTime = getClockMiliseconds();
        }
    }
}
