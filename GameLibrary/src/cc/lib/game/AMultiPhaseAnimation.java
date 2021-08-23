package cc.lib.game;

import java.util.List;

/**
 * Created by Chris Caron on 8/18/21.
 */
public abstract class AMultiPhaseAnimation<T> extends AAnimation<T> {

    private long [] durations;

    protected AMultiPhaseAnimation(long [] durations) {
        super(Utils.sum(durations));
        this.durations = durations;
    }

    protected AMultiPhaseAnimation(long duration) {
        super(duration);
        this.durations = Utils.toLongArray(duration);
    }

    public AMultiPhaseAnimation(long durationMSecs, int repeats) {
        super(durationMSecs, repeats);
        this.durations = Utils.toLongArray(durationMSecs);
    }

    public AMultiPhaseAnimation(long durationMSecs, int repeats, boolean oscillateOnRepeat) {
        super(durationMSecs, repeats, oscillateOnRepeat);
        this.durations = Utils.toLongArray(durationMSecs);
    }

    /**
     * Draw a phase of a complete animation. Phase will be a value between [0-durations.length)
     *
     * @param g
     * @param position value between 0-1
     * @param phase
     */
    protected abstract void drawPhase(T g, float position, int phase);

    @Override
    protected void draw(T g, float position, float dt) {
        // phase 1: make skull fade in over the actor
        long dur=0;
        for (int i=0; i<durations.length; i++) {
            long d = durations[i]+dur;
            if (getElapsedTime() < d) {
                float pos = (getElapsedTime()-dur) / (float)durations[i];
                drawPhase(g, pos, i);
                break;
            }
            dur+=durations[i];
        }
    }

    public void setDuration(int phase, long duration) {
        durations[phase] = duration;
        super.setDuration(Utils.sum(durations));
    }

    public void setDurations(long ... durations) {
        this.durations = durations;
        setDuration(Utils.sum(durations));
    }

    public void setDurations(List<Long> durations) {
        long [] ldurs = new long[durations.size()];
        for (int i=0; i<durations.size(); i++)
            ldurs[i] = durations.get(i);
        setDurations(ldurs);
    }
}
