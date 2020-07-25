package cc.lib.utils;

public class StopWatch {

    long startTime = 0;
    long pauseTime = 0;
    long curTime = 0;
    long deltaTime = 0;
    long lastCaptureTime = 0;
    boolean started = false;
    
    /**
     * Start the stopwatch.  MUST be the first call
     */
    public void start() {
        startTime = getClockMiliseconds();
        started = true;
        unpause();
    }
    
    public boolean isPaused() {
        return pauseTime > 0;
    }
    
    /**
     * Pause the stop watch.  getTime/getDeltaTime will not advance until unpause called.
     */
    public void pause() {
        if (!isPaused())
            pauseTime = getClockMiliseconds();
    }
    
    /**
     * Capture the current time and delta time.  Must be called before calling getTme, getDeltaTiime
     */
    public long capture() {
        if (started && pauseTime == 0) {
            long t = this.getClockMiliseconds();
            this.curTime = t - startTime;
            this.deltaTime = curTime - lastCaptureTime;
            this.lastCaptureTime = curTime;
        }
        return getTime();
    }
    
    /**
     * Resume the stop watch if paused
     */
    public void unpause() {
        if (pauseTime > 0) {
            startTime += (getClockMiliseconds() - pauseTime);
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
}
