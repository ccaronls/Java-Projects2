package cc.lib.game;

/**
 * Created by Chris Caron on 9/28/21.
 *
 * Interpolates over several interpolators in sequence
 */
public class ChainInterpolator<T> implements IInterpolator<T> {

    final IInterpolator<T> [] chain;

    public ChainInterpolator(IInterpolator<T> ... chain) {
        this.chain = chain;
    }

    @Override
    public T getAtPosition(float position) {
        float steps = 1f / chain.length;
        float step = 0;
        while (step + steps < position) {
            step += steps;
        }

        int idx = Math.round(step * chain.length);
        float pos = (position-step) * chain.length;
        return chain[idx].getAtPosition(pos);
    }
}
