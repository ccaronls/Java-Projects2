package cc.lib.reflector;

import java.io.IOException;
import java.lang.reflect.Field;

import cc.lib.utils.GException;

/**
 * Created by Chris Caron on 12/1/23.
 */
class DirtyArrayArchiver implements Archiver {

    @Override
    public String get(Field field, Reflector<?> a) throws Exception {
        DirtyArray o = (DirtyArray) field.get(a);
        String s = Reflector.getCanonicalName(field.getType().getComponentType()) + " " + o.getSize();
        return s;
    }

    private Object createArray(Object current, String line, boolean keepInstances) throws Exception {
        DirtyArray<?> array = (DirtyArray<?>) current;
        String[] parts = line.split(" ");
        if (parts.length < 2)
            throw new GException("Invalid array description '" + line + "' excepted < 2 parts");
        final int len = Integer.parseInt(parts[1].trim());
        if (!keepInstances || current == null || array.getSize() != len) {
            return new DirtyArray(len);
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
    public void serializeArray(Object _arr, RPrintWriter out) throws IOException {
        DirtyArray<?> arr = (DirtyArray<?>) _arr;
        int len = arr.getSize();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                Archiver compArchiver = Reflector.getArchiverForType(arr.getClass().getComponentType().getComponentType());
                Object obj = arr.get(i);
                if (obj == null) {
                    out.println("null");
                } else {
                    out.p(Reflector.getCanonicalName(obj.getClass().getComponentType())).p(" ").p(arr.getSize());
                    out.push();
                    compArchiver.serializeArray(arr.get(i), out);
                    out.pop();
                }
            }
        }
    }

    @Override
    public void deserializeArray(Object _arr, RBufferedReader in, boolean keepInstances) throws IOException {
        DirtyArray arr = (DirtyArray) _arr;
        int len = arr.getSize();
        for (int i = 0; i < len; i++) {
            in.markDepth();
            try {
                Class cl = arr.getClass().getComponentType();
                if (cl.getComponentType() != null)
                    cl = cl.getComponentType();
                Archiver compArchiver = Reflector.getArchiverForType(cl);
                String line = in.readLineOrEOF();
                if (line != null && !line.equals("null")) {
                    Object obj = arr.get(i);
                    try {
                        obj = createArray(obj, line, keepInstances);
                    } catch (Exception e) {
                        throw new ParseException(in.getLineNum(), e);
                    }
                    arr.set(i, obj);
                    compArchiver.deserializeArray(obj, in, keepInstances);
                }
            } finally {
                in.restoreDepth();
            }
        }
    }

}
