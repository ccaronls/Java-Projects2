package cc.lib.reflector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Created by Chris Caron on 12/1/23.
 */
class CollectionArchiver implements Archiver {

    @Override
    public String get(Field field, Reflector<?> a) throws Exception {
        Collection<?> c = (Collection<?>) field.get(a);
        String s = Reflector.getCanonicalName(c.getClass());
        return s;
    }

    @Override
    public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
        if (value != null && !value.equals("null")) {
            if (!keepInstances || o == null)
                field.set(a, Reflector.newCollectionInstance(value));
        } else {
            field.set(a, null);
        }
    }

    @Override
    public void serializeArray(Object arr, RPrintWriter out) throws IOException {
        int len = Array.getLength(arr);
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                Collection<?> c = (Collection<?>) Array.get(arr, i);
                if (c != null) {
                    out.p(Reflector.getCanonicalName(c.getClass()));
                    Reflector.serializeObject(c, out, true);
                } else
                    out.println("null");
            }
        }
    }

    @Override
    public void deserializeArray(Object arr, RBufferedReader in, boolean keepInstances) throws IOException {
        int len = Array.getLength(arr);
        for (int i = 0; i < len; i++) {
            String[] parts = in.readLineOrEOF().split(" ");
            int expectedSize = -1;
            if (parts.length > 1) {
                expectedSize = Integer.parseInt(parts[1]);
            }
            String clazz = parts[0];
            Collection c = (Collection) Array.get(arr, i);
            if (!clazz.equals("null")) {
                try {
                    Class classNm = Reflector.getClassForName(clazz);
                    if (!keepInstances || c == null || !c.getClass().equals(classNm)) {
                        Collection cc = (Collection<?>) classNm.newInstance();
                        if (c != null)
                            cc.addAll(c);
                        c = cc;
                    }
                    Reflector.deserializeCollection(c, in, keepInstances);
                    Array.set(arr, i, c);
                } catch (Exception e) {
                    throw new ParseException(in.lineNum, e);
                }
            } else {
                Array.set(arr, i, null);
            }
        }
        if (in.readLineOrEOF() != null)
            throw new ParseException(in.lineNum, " expected closing '}'");
    }

}
