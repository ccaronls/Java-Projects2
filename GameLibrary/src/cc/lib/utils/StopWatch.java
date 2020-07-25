package cc.lib.utils;

public class StopWatch extends Reflector<StopWatch> {

    static {
        addAllFields(StopWatch.class);
    }

    private long startTime = 0;
    private long pauseTime = 0;
    private long curTime = 0;
    private long deltaTime = 0;
    private long lastCaptureTime = 0;
    private boolean started = false;
    
    /**
     * Start the stopwatch.  MUST be the first call
     */
    public synchronized void start() {
        startTime = getClockMiliseconds();
        pauseTime = 0;
        curTime = 0;
        deltaTime = 0;
        lastCaptureTime = 0;
        started = true;
    }
    
    public boolean isPaused() {
        return pauseTime > 0;
    }
    
    /**
     * Pause the stop watch.  getTime/getDeltaTime will not advance until unpause called.
     */
    public synchronized void pause() {
        if (!isPaused())
            pauseTime = getClockMiliseconds();
    }
    
    /**
     * Capture the current time and delta time.  Must be called before calling getTme, getDeltaTiime
     */
    public synchronized void capture() {
        if (started && pauseTime == 0) {
            long t = this.getClockMiliseconds();
            curTime = t - startTime;
            this.deltaTime = curTime - lastCaptureTime;
            this.lastCaptureTime = curTime;
        }
    }
    
    /**
     * Resume the stop watch if paused
     */
    public synchronized void unpause() {
        if (pauseTime > 0) {
            startTime += (getClockMiliseconds() - pauseTime);
            pauseTime = 0;
        }
    }
    
    /**
     * Get the time as of last call to capture()
     * @return
     */
    public synchronized long getTime() {
        if (started)
            return curTime;
        return 0;
    }
    
    /**
     * Get the delta time as of last call to capture()
     * @return
     */
    public synchronized long getDeltaTime() {
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
