package cc.lib.game;

public abstract class AAnimation {
    private long startTime;
    private long lastTime;
    private final long duration;
    private final int maxRepeats;
    private float position;
    private boolean done;
    private boolean started = false;
    
    public AAnimation(long duration, int maxRepeats) {
        assert(duration > 0);
        this.duration = duration;
        this.maxRepeats = maxRepeats;
        this.lastTime = System.currentTimeMillis();
    }

    public final void start(long delay) {
    	if (delay < 0)
    		delay = 0;
        startTime = System.currentTimeMillis() + delay;
        position = 0;
    }

    public final void start() {
    	start(0);
    }
    
    public final boolean isDone() {
        return done;
    }
    
    public final void update(AGraphics g) {
        long t = System.currentTimeMillis();
        if (t < startTime) {
            draw(g, position, 0);
            return;
        } else if (!started) {
        	started = true;
        	onStarted();
        }
        long repeats = (t-startTime) / duration;
        if (maxRepeats >= 0 && repeats > maxRepeats) {
            done = true;
            onDone();
        } else {
            float delta = (t-startTime) % duration;
            position = delta / duration;
            draw(g, position, t-lastTime);
            lastTime = t;
        }
    }
    
    /**
     * Do not call, call update
     * @param g
     * @param position
     */
    public abstract void draw(AGraphics g, float position, float dt);
    
    /**
     * Called from update thread when animation ended.
     */
    protected void onDone() {}
    
    /**
     * Called from update thread when animation is started.
     */
    protected void onStarted() {}
}