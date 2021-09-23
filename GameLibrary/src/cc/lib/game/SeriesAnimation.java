package cc.lib.game;

import java.util.LinkedList;

/**
 * Created by Chris Caron on 9/1/21.
 *
 * Executes some number of Animations in series
 */
public class SeriesAnimation<T> extends AAnimation<T> {

    private LinkedList<AAnimation<T>> series = new LinkedList<>();

    public SeriesAnimation() {
        super(1);
    }

    public final SeriesAnimation<T> addAnimation(AAnimation<T> a) {
        series.add(a);
        setDuration(Utils.sumLong(series, s -> s.getDuration()));
        return this;
    }

    @Override
    protected final void onStarted() {
        setDuration(Utils.sumLong(series, s -> s.getDuration()));
    }

    @Override
    protected final void onStartedReversed() {
        setDuration(Utils.sumLong(series, s -> s.getDuration()));
    }

    @Override
    public final synchronized boolean update(T g) {
        while (series.size() > 0 && series.getFirst().update(g)) {
            series.removeFirst();
        }
        return super.update(g);
    }

    @Override
    protected final void draw(T g, float position, float dt) {
    }

    @Override
    public final boolean isDone() {
        return series.size() == 0 ? super.isDone() : false;
    }
}
