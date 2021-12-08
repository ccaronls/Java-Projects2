package cc.lib.utils;

import java.util.Arrays;

import cc.lib.game.Utils;

/**
 * Created by Chris Caron on 8/17/21.
 */
public class Pair<FIRST,SECOND> {

    public final FIRST first;
    public final SECOND second;

    public Pair(FIRST first, SECOND second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Utils.equals(first, pair.first) &&
                Utils.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(Utils.toArray(first, second));
    }
}
