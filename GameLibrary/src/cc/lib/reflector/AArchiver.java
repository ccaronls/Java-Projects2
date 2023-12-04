package cc.lib.reflector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

public abstract class AArchiver implements Archiver {

    public abstract Object parse(String value) throws Exception;

    public String getStringValue(Object obj) {
        return String.valueOf(obj);
    }

    @Override
    public String get(Field field, Reflector<?> a) throws Exception {
        return getStringValue(field.get(a));
    }

    @Override
    public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
        field.setAccessible(true);
        if (value == null || value.equals("null"))
            field.set(a, null);
        else
            field.set(a, parse(value));
    }

    @Override
    public void serializeArray(Object arr, RPrintWriter out) {
        int len = Array.getLength(arr);
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                out.p(Array.get(arr, i)).p(" ");
            }
            out.println();
        }
    }

    @Override
    public void deserializeArray(Object arr, RBufferedReader in, boolean keepInstances) throws IOException {
        int len = Array.getLength(arr);
        if (len > 0) {
            String line = in.readLineOrEOF();
            String[] parts = line.split(" ");
            if (parts.length != len)
                throw new ParseException(in.lineNum, "Expected " + len + " parts but found " + parts.length);
            for (int i = 0; i < len; i++) {
                try {
                    Array.set(arr, i, parse(parts[i]));
                } catch (Exception e) {
                    throw new ParseException(in.lineNum, e);
                }
            }
            in.mark(256);
            if (in.readLineOrEOF() != null)
                in.reset();
        }
    }
}
