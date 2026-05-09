package cc.lib.reflector;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by Chris Caron on 12/1/23.
 */
class EnumArchiver extends AArchiver {

    private final Enum<?>[] constants;

    EnumArchiver(Class<?> enumClass) {
        constants = ((Class<? extends Enum<?>>) enumClass).getEnumConstants();
    }

    @Override
    public Object parse(String value) throws Exception {
        for (Enum<?> e : constants) {
            if (e.name().equals(value)) {
                return e;
            }
        }
        throw new Exception("Failed to find enum value: '" + value + "' in available constants: " + Arrays.asList(constants));
    }

    @Override
    public String getStringValue(Object obj) {
        return ((Enum<?>) obj).name();
    }

    @Override
    public void serializeArray(Object arr, RPrintWriter out) {
        int len = Array.getLength(arr);
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                Object o = Array.get(arr, i);
                if (o == null)
                    out.p("null ");
                else
                    out.p(((Enum<?>) o).name()).p(" ");
            }
            out.println();
        }
    }
}

