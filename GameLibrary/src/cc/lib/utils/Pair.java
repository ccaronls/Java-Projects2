package cc.lib.utils;

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
}
