package cc.games.android.soc;

import cc.lib.game.AGraphics;

abstract class Animation {
    
    long startTime;
    long lastTime;
    final long duration;
    final int maxRepeats;
    float position;
    boolean done;

    Animation(long duration, int maxRepeats) {
        assert(duration > 0);
        this.duration = duration;
        this.maxRepeats = maxRepeats;
    }
    
    long getDuration() {
    	return duration;
    }
    
    void start() {
        lastTime = startTime = System.currentTimeMillis();
        position = 0;
        done = false;
        onStarted();
    }
    
    boolean isDone() {
        return done;
    }
    
    void update(AGraphics g) {
        long t = System.currentTimeMillis();
        long repeats = (t-startTime) / duration;
        if (maxRepeats >= 0 && repeats > maxRepeats) {
            done = true;
//            Utils.println("animation done");
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
    abstract void draw(AGraphics g, float position, float dt);
    
    void onDone() {}
    void onStarted() {}
}