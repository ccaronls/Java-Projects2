package cc.lib.reflector;

import java.lang.reflect.Field;

import cc.lib.utils.GException;

/**
 * Created by Chris Caron on 12/1/23.
 */
class DirtyArchiver extends AArchiver {
    @Override
    public String get(Field field, Reflector<?> a) throws Exception {
        Object o = ((DirtyDelegate) field.get(a)).getValue();
        return o == null ? "null" : o.toString();
    }

    @Override
    public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
        //((DirtyDelegate)field.get(a)).setValueFromString(value == null ? "" : value);
    }

    @Override
    public Object parse(String value) throws Exception {
        return null;
    }

    @Override
    public void serializeArray(Object arr, RPrintWriter out) {
        throw new GException("Not implemented");
    }

    @Override
    public void deserializeArray(Object arr, RBufferedReader in, boolean keepInstances) {
        throw new GException("Not implemented");
    }
}
