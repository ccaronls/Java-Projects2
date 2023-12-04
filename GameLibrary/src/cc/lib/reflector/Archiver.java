package cc.lib.reflector;

import java.io.IOException;
import java.lang.reflect.Field;

interface Archiver {
    String get(Field field, Reflector<?> a) throws Exception;

    void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception;

    void serializeArray(Object arr, RPrintWriter out) throws IOException;

    void deserializeArray(Object arr, RBufferedReader in, boolean keepInstances) throws IOException;
}
