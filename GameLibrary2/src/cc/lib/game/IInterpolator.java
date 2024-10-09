package cc.lib.game;

/**
 * Created by Chris Caron on 8/18/21.
 */
public interface IInterpolator<T> {

    /**
     * @param position value between [0-1]
     * @return a value between start and end with interpolation using position
     */
    T getAtPosition(float position);
}
