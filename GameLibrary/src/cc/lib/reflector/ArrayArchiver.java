package cc.lib.reflector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import cc.lib.utils.GException;

/**
 * Created by Chris Caron on 12/1/23.
 */
class ArrayArchiver implements Archiver {

    @Override
    public String get(Field field, Reflector<?> a) throws Exception {
        Object o = field.get(a);
        String s = Reflector.getCanonicalName(field.getType().getComponentType()) + " " + Array.getLength(o);
        return s;
    }

    private Object createArray(Object current, String line, boolean keepInstances) throws Exception {
        String[] parts = line.split(" ");
        if (parts.length < 2)
            throw new GException("Invalid array description '" + line + "' excepted < 2 parts");
        final int len = Integer.parseInt(parts[1].trim());
        if (!keepInstances || current == null || Array.getLength(current) != len) {
            Class<?> clazz = Reflector.getClassForName(parts[0].trim());
            return Array.newInstance(clazz, len);
        }
        return current;
    }

    @Override
    public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
        if (value != null && !value.equals("null")) {
            field.set(a, createArray(o, value, keepInstances));
        } else {
            field.set(a, null);
        }
    }

    @Override
    public void serializeArray(Object arr, RPrintWriter out) throws IOException {
        int len = Array.getLength(arr);
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                Archiver compArchiver = Reflector.getArchiverForType(arr.getClass().getComponentType().getComponentType());
                Object obj = Array.get(arr, i);
                if (obj == null) {
                    out.println("null");
                } else {
                    out.p(Reflector.getCanonicalName(obj.getClass().getComponentType())).p(" ").p(Array.getLength(obj));
                    out.push();
                    compArchiver.serializeArray(Array.get(arr, i), out);
                    out.pop();
                }
            }
        }
    }

    @Override
    public void deserializeArray(Object arr, RBufferedReader in, boolean keepInstances) throws IOException {
        int len = Array.getLength(arr);
        for (int i = 0; i < len; i++) {
            Class cl = arr.getClass().getComponentType();
            if (cl.getComponentType() != null)
                cl = cl.getComponentType();
            Archiver compArchiver = Reflector.getArchiverForType(cl);
            String line = in.readLineOrEOF();
            if (line != null && !line.equals("null")) {
                Object obj = Array.get(arr, i);
                try {
                    obj = createArray(obj, line, keepInstances);
                } catch (Exception e) {
                    throw new ParseException(in.lineNum, e);
                }
                Array.set(arr, i, obj);
                compArchiver.deserializeArray(obj, in, keepInstances);
            }
        }
        if (in.readLineOrEOF() != null)
            throw new ParseException(in.lineNum, " expected closing '}'");

    }

}
