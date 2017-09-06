package cc.lib.game;

/**
 * Convenience class for managing an animation in a rendering pipeline
 * 
 * @author chriscaron
 *
 */
public abstract class AAnimation {
    private long startTime;
    private long lastTime;
    private final long duration;
    private final int maxRepeats;
    private float position = 0;
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
        lastTime = startTime = System.currentTimeMillis() + delay;
        position = 0;
    }

    public final AAnimation start() {
    	start(0);
    	return this;
    }
    
    public final boolean isDone() {
        return done;
    }
    
    /**
     * Rendering loop calls this repeatedly and checks 'isDone' for removal
     * 
     * @param g
     * returns true when isDone
     */
    public final boolean update(AGraphics g) {
        long t = System.currentTimeMillis();
        if (t < startTime) {
            return false;
        } else if (!started) {
        	started = true;
        	lastTime = t;
        	onStarted();
        }

        float delta = (t-startTime) % duration;
        position = delta / duration;
        long repeats = (t-startTime) / duration;
        boolean isDone = false;
        if (maxRepeats >= 0 && repeats > maxRepeats) {
        	position = 1;
        	onDone();
        	isDone = true;
        }
        draw(g, position, t-lastTime);
        lastTime = t;
        return isDone;
    }
    
    public void stop() {
    	if (started) {
    		onDone();
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