package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

import cc.lib.utils.Reflector;

/**
 * Created by Chris Caron on 3/10/22.
 */
public class ZObjective extends Reflector<ZObjective> {

    static {
        addAllFields(ZObjective.class);
    }

    final List<Integer> objectives = new ArrayList<>();
    final List<Integer> found = new ArrayList<>();
}
