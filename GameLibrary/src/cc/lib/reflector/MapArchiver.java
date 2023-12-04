package cc.lib.reflector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by Chris Caron on 12/1/23.
 */
class MapArchiver implements Archiver {
    @Override
    public String get(Field field, Reflector<?> a) throws Exception {
        Map<?, ?> m = (Map<?, ?>) field.get(a);
        String s = Reflector.getCanonicalName(m.getClass());
        return s;
    }

    @Override
    public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
        if (value != null && !value.equals("null")) {
            if (o == null || !keepInstances)
                o = Reflector.getClassForName(value).newInstance();
            field.set(a, o);
        } else {
            field.set(a, null);
        }
    }

    @Override
    public void serializeArray(Object arr, RPrintWriter out) throws IOException {
        int len = Array.getLength(arr);
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                Map<?, ?> m = (Map<?, ?>) Array.get(arr, i);
                if (m != null) {
                    out.println(Reflector.getCanonicalName(m.getClass()));
                    Reflector.serializeObject(m, out, true);
                } else
                    out.println("null");
            }
        }
    }

    @Override
    public void deserializeArray(Object arr, RBufferedReader in, boolean keepInstances) throws IOException {
        int len = Array.getLength(arr);
        for (int i = 0; i < len; i++) {
            String clazz = in.readLineOrEOF();
            if (!clazz.equals("null")) {
                try {
                    Map<?, ?> m = (Map<?, ?>) Reflector.getClassForName(clazz).newInstance();
                    Reflector.deserializeMap(m, in, keepInstances);
                    Array.set(arr, i, m);
                } catch (Exception e) {
                    throw new ParseException(in.lineNum, e);
                }
            }
        }
        if (in.readLineOrEOF() != null)
            throw new ParseException(in.lineNum, " expected closing '}'");
    }

}
