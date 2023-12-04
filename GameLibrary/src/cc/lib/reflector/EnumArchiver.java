package cc.lib.reflector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Created by Chris Caron on 12/1/23.
 */
class EnumArchiver implements Archiver {
    @Override
    public String get(Field field, Reflector<?> a) throws Exception {
        return ((Enum<?>) field.get(a)).name();
    }

    @Override
    public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
        field.set(a, Reflector.findEnumEntry(field.getType(), value));
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
                    Enum<?> enumEntry = Reflector.findEnumEntry(arr.getClass().getComponentType(), parts[i]);
                    Array.set(arr, i, enumEntry);
                } catch (Exception e) {
                    throw new ParseException(in.lineNum, e);
                }
            }
            if (in.readLineOrEOF() != null)
                throw new ParseException(in.lineNum, " expected closing '}'");
        }
    }
}

