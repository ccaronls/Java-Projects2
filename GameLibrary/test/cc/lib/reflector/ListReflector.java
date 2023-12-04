package cc.lib.reflector;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chris Caron on 12/1/23.
 */
public class ListReflector extends Reflector<ListReflector> {

    static {
        addAllFields(ListReflector.class);
    }

    List<Integer> intList = new ArrayList<>();

}
