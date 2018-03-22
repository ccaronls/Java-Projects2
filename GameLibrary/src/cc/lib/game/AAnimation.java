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
    private State state = State.IDLE;
    private boolean reverse = false; // 1 for forward, -1 for reversed
    private final boolean oscilateOnRepeat;
    private boolean waiting = false;

    enum State {
        IDLE, STARTED, STOPPED, DONE
    };

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
     * @param durationMSecs duration of one loop, must be > 0
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
    public final AAnimation<T> start(long delayMSecs) {
    	if (delayMSecs < 0)
    		delayMSecs = 0;
        lastTime = startTime = getCurrentTimeMSecs() + delayMSecs;
        position = 0;
        return this;
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
        return state==State.DONE;
    }

    /**
     * Override this is there is some rendering to do while waiting for the animation to start
     *
     * @param g
     */
    protected void drawPrestart(T g) {}

    /**
     * Call this within your rendering loop.  Animation is over when onDone() {} executed
     * 
     * @param g
     * returns true when isDone
     */
    public synchronized final boolean update(T g) {
        float dt = 0;
        long t = getCurrentTimeMSecs();
        if (!isDone()) {
            if (t < startTime) {
                drawPrestart(g);
                return false;
            } else if (state == State.IDLE) {
                state = State.STARTED;
                lastTime = t;
                onStarted();
            }

            float delta = (t - startTime) % duration;
            long repeats = (t - startTime) / duration;

            if (maxRepeats >= 0 && repeats > maxRepeats) {
                position = reverse ? 0 : 1;
                stop();
            } else {
                if (oscilateOnRepeat) {
                    if (repeats % 2 == 1) {
                        reverse = !startDirectionReverse;
                    } else {
                        reverse = startDirectionReverse;
                    }
                }
                if (reverse) {
                    position = 1 - (delta / duration);
                } else {
                    position = delta / duration;
                }
            }
            dt = (float)(t-lastTime)/duration;
        }
        draw(g, position, dt);
        lastTime = t;
        if (state == State.STOPPED) {
            state = State.DONE;
            onDone();
            if (waiting) {
                synchronized (this) {
                    notify();
                }
                waiting = false;
            }
        }
        return isDone();
    }
    
    public synchronized void stop() {
        if (state != State.DONE)
            state = state.STOPPED;
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

    public final boolean isStartDirectionReverse() {
        return startDirectionReverse;
    }

    public final long getStartTime() {
        return startTime;
    }

    public final long getLastTime() {
        return lastTime;
    }

    public final long getDuration() {
        return duration;
    }

    public final int getMaxRepeats() {
        return maxRepeats;
    }

    public final float getPosition() {
        return position;
    }

    public final State getState() {
        return state;
    }

    public final boolean isReverse() {
        return reverse;
    }

    public final boolean isOscilateOnRepeat() {
        return oscilateOnRepeat;
    }

    public final long getElapsedTime() {
        return getCurrentTimeMSecs() - startTime;
    }

    public final long getTimeRemaining() {
        return getDuration() - getElapsedTime();
    }

    public final void waitForDuration() {
        waiting = true;
        if (getDuration() < 0)
            throw new AssertionError("Bad idea=infinite wait");
        Utils.waitNoThrow(this, getDuration()+500);
        waiting = false;
    }

    public final boolean isIdle() {
        return state == State.IDLE;
    }

    public final int getRepeat() { return (int)((System.currentTimeMillis() - startTime) / duration); }
}