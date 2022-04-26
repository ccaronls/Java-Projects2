package cc.lib.utils;

import java.util.HashSet;

/**
 * Created by Chris Caron on 4/26/22.
 */
public class NoDupsSet extends HashSet {

    @Override
    public boolean add(Object o) {
        if (contains(o))
            throw new IllegalArgumentException("Cannot add duplicate object:" + o);
        return super.add(o);
    }
}
