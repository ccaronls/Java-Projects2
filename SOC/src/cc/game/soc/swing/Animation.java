package cc.game.soc.swing;

import java.awt.Graphics;

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
        this.lastTime = System.currentTimeMillis();
    }
    
    void start() {
        startTime = System.currentTimeMillis();
        position = 0;
        onStarted();
    }
    
    boolean isDone() {
        return done;
    }
    
    void update(Graphics g) {
        long t = System.currentTimeMillis();
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
    abstract void draw(Graphics g, float position, float dt);
    
    void onDone() {}
    void onStarted() {}
}