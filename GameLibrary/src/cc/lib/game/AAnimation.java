package cc.lib.game;

/**
 * General purpose animation runner.
 *
 * Example usage:
 *
 * new MyAnimation extends AAnimation<Graphics> {
 *     protected void draw(Graphics g, float pos, float dt) {
 *         ...
 *     }
 *
 *     protected void onDone() {
 *         ...
 *     }
 * }.start(1000); - starts the animation after 1 second has elapsed
 * 
 * @author chriscaron
 *
 */
public abstract class AAnimation<T> {
    private boolean startDirectionReverse = false;
    private long startTime;
    private long lastTime;
    private final long duration;
    private final int maxRepeats;
    private float position = 0;
    private boolean started = false;
    private boolean done = false;
    private boolean reverse = false; // 1 for forward, -1 for reversed
    private final boolean oscilateOnRepeat;

    /**
     * Create an animation that plays for a fixed time without repeats
     * @param durationMSecs
     */
    public AAnimation(long durationMSecs) {
        this(durationMSecs, 0);
    }

    /**
     * Create a repeating animation. if maxRepeats < 0 then will repeat forever until stopped.
     *
     * @param durationMSecs duration of one loop
     * @param repeats repeats=0 means none. repeats<0 means infinite. repeats>0 means fixed number of repeats
     */
    public AAnimation(long durationMSecs, int repeats) {
        this(durationMSecs, repeats, false);
    }

    /**
     * Create a repeating animation with option to oscilate (play in reverse) on every other repeat
     *
     * @param durationMSecs duration of one loop
     * @param repeats repeats=0 means none. repeats<0 means infinite. repeats>0 means fixed number of repeats
     * @param oscilateOnRepeat when true, a loop will reverse from its current play direction on each repeat
     */
    public AAnimation(long durationMSecs, int repeats, boolean oscilateOnRepeat) {
        assert(durationMSecs > 0);
        this.duration = durationMSecs;
        this.maxRepeats = repeats;
        this.oscilateOnRepeat = oscilateOnRepeat;
        this.lastTime = getCurrentTimeMSecs();
    }

    /**
     * Start the animation after some delay
     * @param delayMSecs
     */
    public final void start(long delayMSecs) {
    	if (delayMSecs < 0)
    		delayMSecs = 0;
        lastTime = startTime = getCurrentTimeMSecs() + delayMSecs;
        position = 0;
    }

    /**
     * Start the animation emmediately
     * @return
     */
    public final AAnimation<T> start() {
    	start(0);
    	return this;
    }

    /**
     * Start the animation in reverse direction after some delay.
     *
     * @param delayMSecs
     * @return
     */
    public final AAnimation<T> startReverse(long delayMSecs) {
        start(delayMSecs);
        position = 1;
        startDirectionReverse = reverse = true;
        return this;
    }

    /**
     * Emmediately start the animation in reverse direction
     * @return
     */
    public final AAnimation<T> startReverse() {
        return startReverse(0);
    }

    /**
     *
     * @return
     */
    public final boolean isDone() {
        return done;
    }
    
    /**
     * Call this within your rendering loop.  Animation is over when onDone() {} executed
     * 
     * @param g
     * returns true when isDone
     */
    public synchronized final boolean update(T g) {
        if (done)
            return true;
        long t = getCurrentTimeMSecs();
        if (t < startTime) {
            return false;
        } else if (!started) {
        	started = true;
        	lastTime = t;
        	onStarted();
        }

        float delta = (t-startTime) % duration;
        long repeats = (t-startTime) / duration;
        if (oscilateOnRepeat) {
            if (repeats % 2 == 1) {
                reverse = !startDirectionReverse;
            } else {
                reverse = startDirectionReverse;
            }
        }
        if (reverse) {
            position = 1 - (delta/duration);
        } else {
            position = delta / duration;
        }
        if (maxRepeats >= 0 && repeats > maxRepeats) {
        	position = reverse ? 0 : 1;
        	stop();
        }
        draw(g, position, t-lastTime);
        lastTime = t;
        return done;
    }
    
    public synchronized void stop() {
        if (!done) {
            done = true;
    		onDone();
    	}
    }

    /**
     * Override this to use a different time method other than System.currentTimeMillis()
     * @return
     */
    protected long getCurrentTimeMSecs() {
        return System.currentTimeMillis();
    }
    
    /**
     * Do not call, call update
     * @param g
     * @param position
     */
    protected abstract void draw(T g, float position, float dt);
    
    /**
     * Called from update thread when animation ended. base method does nothing
     */
    protected void onDone() {}
    
    /**
     * Called from update thread when animation is started. base mthod does nothing
     */
    protected void onStarted() {}
}