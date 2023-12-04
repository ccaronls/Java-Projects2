package cc.lib.reflector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import cc.lib.game.Utils;

/**
 * Created by Chris Caron on 12/1/23.
 */
class ArchivableArchiver implements Archiver {
    @Override
    public String get(Field field, Reflector<?> a) throws Exception {
        Object o = field.get(a);
        if (o == null)
            return "null";
        Class<?> clazz = o.getClass();
        String className;
        if (clazz.isAnonymousClass())
            className = Reflector.getCanonicalName(clazz.getSuperclass());
        else
            className = Reflector.getCanonicalName(clazz);
        Utils.assertTrue(className != null, "Failed to get className for class %s", clazz);
        return className;
    }

    @Override
    public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
        if (!value.equals("null") && value != null) {
            value = value.split(" ")[0];
            field.setAccessible(true);
            try {
                if (!keepInstances || o == null || Reflector.isImmutable(o))
                    field.set(a, Reflector.getClassForName(value).newInstance());
            } catch (ClassNotFoundException e) {
                int dot = value.lastIndexOf('.');
                if (dot > 0) {
                    String altName = value.substring(0, dot) + "$" + value.substring(dot + 1);
                    field.set(a, Reflector.getClassForName(altName).newInstance());
                } else {
                    throw e;
                }
            }
        } else {
            field.set(a, null);
        }
    }

    @Override
    public void serializeArray(Object arr, RPrintWriter out) throws IOException {
        int len = Array.getLength(arr);
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                Reflector<?> o = (Reflector<?>) Array.get(arr, i);
                if (o != null) {
                    out.print(Reflector.getCanonicalName(o.getClass()));
                    out.push();
                    o.serialize(out);
                    out.pop();
                } else {
                    out.println("null");
                }
            }
        }
    }

    @Override
    public void deserializeArray(Object arr, RBufferedReader in, boolean keepInstances) throws IOException {
        int len = Array.getLength(arr);
        for (int i = 0; i < len; i++) {
            int depth = in.depth;
            String line = in.readLineOrEOF();
            if (line.equals("null")) {
                Array.set(arr, i, null);
                continue;
            }
            Object o = Array.get(arr, i);
            Reflector<?> a;
            if (!keepInstances || o == null || !(o instanceof Reflector) || ((Reflector) o).isImmutable()) {
                try {
                    a = (Reflector<?>) Reflector.getClassForName(line).newInstance();
                } catch (Exception e) {
                    throw new ParseException(in.lineNum, e);
                }
            } else {
                a = (Reflector) o;
            }
            if (keepInstances) {
                a.merge(in);
            } else {
                a.deserialize(in);
            }
            Array.set(arr, i, a);
            if (in.depth > depth) {
                if (in.readLineOrEOF() != null)
                    throw new ParseException(in.lineNum, " expected closing '}'");
            }
        }
        if (in.readLineOrEOF() != null)
            throw new ParseException(in.lineNum, " expected closing '}'");
    }

}
