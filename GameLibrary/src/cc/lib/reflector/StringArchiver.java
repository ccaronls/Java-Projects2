package cc.lib.reflector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Created by Chris Caron on 12/1/23.
 */
class StringArchiver implements Archiver {

    @Override
    public String get(Field field, Reflector<?> a) throws Exception {
        Object s = field.get(a);
        if (s == null)
            return "null";
        return "\"" + Reflector.encodeString((String) s) + "\"";
    }

    @Override
    public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
        if (value == null || value.equals("null"))
            field.set(a, null);
        else {
            field.set(a, Reflector.decodeString(value.substring(1, value.length() - 1)));
        }
    }


    @Override
    public void serializeArray(Object arr, RPrintWriter out) throws IOException {
        int num = Array.getLength(arr);
        if (num > 0) {
            for (int i = 0; i < num; i++) {
                Object entry = Array.get(arr, i);
                if (entry == null)
                    //buf.append("null\n");
                    out.println("null");
                else
                    out.p("\"").p(Reflector.encodeString((String) entry)).println("\"");
            }
        }
    }

    @Override
    public void deserializeArray(Object arr, RBufferedReader in, boolean keepInstances) throws IOException {
        int len = Array.getLength(arr);
        for (int i = 0; i < len; i++) {
            String line = in.readLineOrEOF();
            if (line != null && !line.equals("null")) {
                String s = Reflector.decodeString(line.substring(1, line.length() - 1));
                Array.set(arr, i, s);
            } else {
                Array.set(arr, i, null);
            }
        }
        if (in.readLineOrEOF() != null)
            throw new ParseException(in.lineNum, " expected closing '}'");
    }
}
