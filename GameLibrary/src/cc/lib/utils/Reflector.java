package cc.lib.utils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * Derive from this class to handle copying, equals, serializing, deserializing, diffing and more.
 * 
 * Serialization of primitives, arrays and subclasses of Reflector are supported.
 * Also collections are supported if their data types are one of the afore mentioned
 * New types or can be added if they implement an Archiver.
 * 
 * Ways to use this class:
 *
 * 1. Extend Reflector and Don't override serialize and deserialize, simply call addAllField within a static
 * block of your class. Fields you dont want serialized should be annotated with @Omit
 *
 * primitives, primitive arrays, Object Arrays, enums, Collections, Maps, other Reflectors.
 * Fields of external types need to have an archiver attached like so:
 * class A extends Reflector<A> {
 *    java.awt.Color color;
 *    static {
 *       addField(A.class, "color", new Reflector.AArchiver() {
 *   		
 *   		@Override
 *   		public Object parse(String value) throws Exception {
 *   			return Utils.stringToColor(value);
 *   		}
 *   		
 *   		@Override
 *   		public String getStringValue(Object obj) {
 *   			return Utils.colorToString((Color)obj);
 *   		}
 *   	};
 *    }
 * }
 * 
 * 2. Extend this class and override serialize/deserialize.  Use println/readLine to read pre trimmed strings
 * from reader.  When reader returns null, there is no more input.  Example:
 * class A extends Reflector<A> {
 *    String a;
 *    int b;
 *    
 *    public void serialize(PrintWriter out) {
 *    	out.println(a);
 *      out.println(b);
 *    }
 *    
 *    public void deserialize(BufferedReader in) {
 *      a = in.readLine();
 *      b = Integer.parseInt(in.readLine());
 *    }
 * }
 *
 * 3. User static serialze/deserialize methods on known objects, enums, arrays, primitives, maps, collections
 *
 * Known Limitations:
 *
 * - Reflector cannot handle an Object array. Use a Collection if you want serialize unrelated types.
 * - Use caution when hashing reflector objects. If you rely on Object.hashCode, then objects can get
 *   lost after new instances are created for serialization. Consider implementing hashCode in reflector
 *   objects you intend to be hashed.
 *
 *
 * NOTE: Derived classes must support a public zero argument constructor for de-serialization
 * @author ccaron
 *
 */
public class Reflector<T> {

    public static boolean DISABLED = false;

    public static int ARRAY_DIMENSION_VARIATIONS = 2;

    /**
     * Use this annotation to Omit field for usage by Reflector
     * <p>
     * If a class extends Reflector, and that class has called:
     * static {
     * addAllFields(...)
     * }
     * <p>
     * then this annotation is the same as:
     * <p>
     * int myField;
     * <p>
     * static {
     * omitField("myField");
     * }
     *
     * @author chriscaron
     */
    @Target(value = ElementType.FIELD)
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface Omit {
    }

    @Target(value = ElementType.FIELD)
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface Alternate {
        String[] variations();
    }

    private final static Logger log = LoggerFactory.getLogger(Reflector.class);

    /**
     * This flag to support situations where we dont want to create new Reflector instances, just overwrite their values.
     * This can be helpful where we have derived classes that might be anonymous or have class scope that provides some
     * critical functionality that we want to keep and it cannot be instantiated but otherwise shares all the fields with
     * the incoming class type.
     * One thing to be aware of is where deserializing an instance where the input has fields not represented in the
     * instance. Consider enable THROW_ON_UNKNOWN flag when using KEEP_INSTANCES
     */
    public static boolean KEEP_INSTANCES = false;

    /**
     * Turn this on to throw exceptions on any unknown fields.  Default is off.
     */
    public static boolean THROW_ON_UNKNOWN = false;

    private final static Map<Class<?>, Map<Field, Archiver>> classValues = new HashMap<>();
    private final static Map<String, Class<?>> classMap = new HashMap<>();

    public static class MyBufferedReader extends BufferedReader {

        private int markedLineNum = 0;
        int lineNum = 0;
        int depth = 0;

        public MyBufferedReader(Reader arg0) {
            super(arg0);
        }

        @Override
        public String readLine() throws IOException {
            lineNum++;
            try {
                String line = super.readLine();
                if (line == null) {
                    if (depth > 0)
                        throw new EOFException();
                    return null;
                }
                line = line.trim();
                if (line.endsWith("{")) {
                    depth++;
                    return line.substring(0, line.length() - 1).trim();
                }
                if (line.endsWith("}")) {
                    depth--;
                    return null;
                }
                return line;
            } catch (IOException e) {
                throw new IOException("Error on line: " + lineNum + " " + e.getMessage(), e);
            }
        }

        @Override
        public void mark(int readAheadLimit) throws IOException {
            super.mark(readAheadLimit);
            markedLineNum = lineNum;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            lineNum = markedLineNum;
        }


    }

    public static class MyPrintWriter extends PrintWriter {

        static String[] indents;

        static {
            indents = new String[32];
            String indent = "";
            for (int i = 0; i < indents.length; i++) {
                indents[i] = indent;
                indent += "   ";
            }
        }

        public MyPrintWriter(Writer out) {
            super(out, true);
        }

        public MyPrintWriter(OutputStream out) {
            super(out, true);
        }

        private int currentIndent = 0;

        void push() {
            if (currentIndent < indents.length - 1)
                currentIndent++;
        }

        void pop() {
            Utils.assertTrue (currentIndent > 0);
            if (currentIndent > 0)
                currentIndent--;
        }

        @Override
        public void println(Object obj) {
            super.print(indents[currentIndent]);
            super.println(obj);
        }

        @Override
        public void println(String obj) {
            super.print(indents[currentIndent]);
            super.println(obj);
        }

    }

    public interface Archiver {
        String get(Field field, Reflector<?> a) throws Exception;

        void set(Object o, Field field, String value, Reflector<?> a) throws Exception;

        void serializeArray(Object arr, MyPrintWriter out) throws Exception;

        void deserializeArray(Object arr, MyBufferedReader in) throws Exception;
    }

    private static String readLineOrEOF(BufferedReader in) throws IOException {
        while (true) {
            String line = in.readLine();
            if (line == null)
                return null;
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#"))
                continue;
            return line;
        }
    }

    public static abstract class AArchiver implements Archiver {

        public abstract Object parse(String value) throws Exception;

        public String getStringValue(Object obj) {
            return String.valueOf(obj);
        }

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            return getStringValue(field.get(a));
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a) throws Exception {
            field.setAccessible(true);
            if (value == null || value.equals("null"))
                field.set(a, null);
            else
                field.set(a, parse(value));
        }

        @Override
        public void serializeArray(Object arr, MyPrintWriter out) {
            int len = Array.getLength(arr);
            if (len > 0) {
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < len; i++) {
                    buf.append(Array.get(arr, i)).append(" ");
                }
                out.println(buf.toString());
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                String line = readLineOrEOF(in);
                String[] parts = line.split(" ");
                if (parts.length != len)
                    throw new Exception("Expected " + len + " parts but found " + parts.length);
                for (int i = 0; i < len; i++) {
                    Array.set(arr, i, parse(parts[i]));
                }
                in.mark(256);
                if (readLineOrEOF(in) != null)
                    in.reset();
            }
        }
    }

    private static Archiver stringArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Object s = field.get(a);
            if (s == null)
                return "null";
            return "\"" + encodeString((String) s) + "\"";
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a) throws Exception {
            if (value == null || value.equals("null"))
                field.set(a, null);
            else {
                field.set(a, decodeString(value.substring(1, value.length() - 1)));
            }
        }


        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int num = Array.getLength(arr);
            if (num > 0) {
                for (int i = 0; i < num; i++) {
                    Object entry = Array.get(arr, i);
                    if (entry == null)
                        //buf.append("null\n");
                        out.println("null");
                    else
                        out.println("\"" + encodeString((String) entry) + "\"");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                String line = readLineOrEOF(in);
                if (line != null && !line.equals("null")) {
                    String s = decodeString(line.substring(1, line.length() - 1));
                    Array.set(arr, i, s);
                }
            }
            if (readLineOrEOF(in) != null)
                throw new Exception("Line: " + in.lineNum + " expected closing '}'");
        }
    };

    private static Archiver byteArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Byte.parseByte(s);
        }

    };

    private static Archiver integerArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Integer.parseInt(s);
        }

    };

    private static Archiver longArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Long.parseLong(s);
        }

    };

    private static Archiver floatArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Float.parseFloat(s);
        }

    };

    private static Archiver doubleArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Double.parseDouble(s);
        }

    };

    private static Archiver booleanArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Boolean.parseBoolean(s);
        }

    };

    private static Enum<?> findEnumEntry(Class<?> enumClass, String value) throws Exception {
        if (value == null || value.equals("null"))
            return null;
        @SuppressWarnings("unchecked")
        Enum<?>[] constants = ((Class<? extends Enum<?>>) enumClass).getEnumConstants();
        for (Enum<?> e : constants) {
            if (e.name().equals(value)) {
                return e;
            }
            // TODO : Is there a way to use variation annotation on emum?
        }
        throw new Exception("Failed to find enum value: '" + value + "' in available constants: " + Arrays.asList(constants));
    }

    private static Archiver enumArchiver = new Archiver() {
        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            return ((Enum<?>) field.get(a)).name();
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a) throws Exception {
            field.set(a, findEnumEntry(field.getType(), value));
        }

        @Override
        public void serializeArray(Object arr, MyPrintWriter out) {
            int len = Array.getLength(arr);
            if (len > 0) {
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < len; i++) {
                    Object o = Array.get(arr, i);
                    if (o == null)
                        buf.append("null ");
                    else
                        buf.append(((Enum<?>) o).name()).append(" ");
                }
                out.println(buf.toString());
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                String line = readLineOrEOF(in);
                String[] parts = line.split(" ");
                if (parts.length != len)
                    throw new Exception("Expected " + len + " parts but found " + parts.length);
                for (int i = 0; i < len; i++) {
                    Enum<?> enumEntry = findEnumEntry(arr.getClass().getComponentType(), parts[i]);
                    Array.set(arr, i, enumEntry);
                }
                if (readLineOrEOF(in) != null)
                    throw new Exception("Line: " + in.lineNum + " expected closing '}'");
            }
        }
    };

    private final static Map<Class, String> caconicalNameCash = new HashMap<>();

    private static String getCanonicalName(Class clazz) {
        String name;
        if ((name = caconicalNameCash.get(clazz)) != null) {
            return name;
        }
        if (clazz.isAnonymousClass()) {
            clazz = clazz.getSuperclass();
        }
        name = clazz.getCanonicalName();
        caconicalNameCash.put(clazz, name);
        return name;
    }

    private static Archiver archivableArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Object o = field.get(a);
            if (o == null)
                return "null";
            Class<?> clazz = o.getClass();
            String className;
            if (clazz.isAnonymousClass())
                className = getCanonicalName(clazz.getSuperclass());
            else
                className = getCanonicalName(clazz);
            Utils.assertTrue(className != null, "Failed to get className for class %s", clazz);
            return className + " {";
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a) throws Exception {
            if (!value.equals("null") && value != null) {
                value = value.split(" ")[0];
                field.setAccessible(true);
                try {
                    if (!KEEP_INSTANCES || o == null || isImmutable(o))
                        field.set(a, getClassForName(value).newInstance());
                } catch (ClassNotFoundException e) {
                    int dot = value.lastIndexOf('.');
                    if (dot > 0) {
                        String altName = value.substring(0, dot) + "$" + value.substring(dot + 1);
                        field.set(a, getClassForName(altName).newInstance());
                    } else {
                        throw e;
                    }
                }
            } else {
                field.set(a, null);
            }
        }

        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    Reflector<?> o = (Reflector<?>) Array.get(arr, i);
                    if (o != null) {
                        out.println(getCanonicalName(o.getClass()) + " {");
                        out.push();
                        o.serialize(out);
                        out.pop();
                        out.println("}");
                    } else
                        out.println("null");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                int depth = in.depth;
                String line = readLineOrEOF(in);
                if (line.equals("null"))
                    continue;
                Object o = Array.get(arr, i);
                Reflector<?> a;
                if (!KEEP_INSTANCES || o == null || !(o instanceof Reflector) || ((Reflector) o).isImmutable()) {
                    a = (Reflector<?>) getClassForName(line).newInstance();
                } else {
                    a = (Reflector) o;
                }
                a.deserialize(in);
                Array.set(arr, i, a);
                if (in.depth > depth) {
                    if (readLineOrEOF(in) != null)
                        throw new Exception("Line: " + in.lineNum + " expected closing '}'");
                }
            }
            if (readLineOrEOF(in) != null)
                throw new Exception("Line: " + in.lineNum + " expected closing '}'");
        }
    };

    private static Archiver collectionArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Collection<?> c = (Collection<?>) field.get(a);
            String s = getCanonicalName(c.getClass()) + " {";
            return s;
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a) throws Exception {
            if (value != null && !value.equals("null")) {
                if (!KEEP_INSTANCES || o == null)
                    field.set(a, getClassForName(value).newInstance());
            } else {
                field.set(a, null);
            }
        }

        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    Collection<?> c = (Collection<?>) Array.get(arr, i);
                    if (c != null) {
                        out.println(getCanonicalName(c.getClass()) + " {");
                        serializeObject(c, out, true);
                    } else
                        out.println("null");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                String clazz = readLineOrEOF(in);
                Collection c = (Collection) Array.get(arr, i);
                if (!clazz.equals("null")) {
                    Class clars = getClassForName(clazz);
                    if (!KEEP_INSTANCES || c == null || !c.getClass().equals(clars)) {
                        Collection cc = (Collection<?>) clars.newInstance();
                        if (c != null)
                            cc.addAll(c);
                        c = cc;
                    }
                    deserializeCollection(c, in);
                    Array.set(arr, i, c);
                } else {
                    Array.set(arr, i, null);
                }
            }
            if (readLineOrEOF(in) != null)
                throw new Exception("Line: " + in.lineNum + " expected closing '}'");
        }
    };

    private static Archiver mapArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Map<?, ?> m = (Map<?, ?>) field.get(a);
            String s = getCanonicalName(m.getClass()) + " {";
            return s;
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a) throws Exception {
            if (value != null && !value.equals("null")) {
                field.set(a, getClassForName(value).newInstance());
            } else {
                field.set(a, null);
            }
        }

        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    Map<?, ?> m = (Map<?, ?>) Array.get(arr, i);
                    if (m != null) {
                        out.println(getCanonicalName(m.getClass()) + " {");
                        serializeObject(m, out, true);
                    } else
                        out.println("null");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                String clazz = readLineOrEOF(in);
                if (!clazz.equals("null")) {
                    Map<?, ?> m = (Map<?, ?>) getClassForName(clazz).newInstance();
                    deserializeMap(m, in);
                    Array.set(arr, i, m);
                }
            }
            if (readLineOrEOF(in) != null)
                throw new Exception("Line: " + in.lineNum + " expected closing '}'");
        }
    };

    private static Archiver arrayArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Object o = field.get(a);
            String s = getCanonicalName(field.getType().getComponentType()) + " " + Array.getLength(o) + " {";
            return s;
        }

        private Object createArray(Object current, String line) throws Exception {
            String[] parts = line.split(" ");
            if (parts.length < 2)
                throw new Exception("Invalid array description '" + line + "' excepted < 2 parts");
            final int len = Integer.parseInt(parts[1].trim());
            if (!KEEP_INSTANCES || current == null || Array.getLength(current) != len) {
                Class<?> clazz = getClassForName(parts[0].trim());
                return Array.newInstance(clazz, len);
            }
            return current;
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a) throws Exception {
            if (value != null && !value.equals("null")) {
                field.set(a, createArray(o, value));
            } else {
                field.set(a, null);
            }
        }

        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    Archiver compArchiver = getArchiverForType(arr.getClass().getComponentType().getComponentType());
                    Object obj = Array.get(arr, i);
                    if (obj == null) {
                        out.println("null");
                    } else {
                        out.println(getCanonicalName(obj.getClass().getComponentType()) + " " + Array.getLength(obj) + " {");
                        out.push();
                        compArchiver.serializeArray(Array.get(arr, i), out);
                        out.pop();
                        out.println("}");
                    }
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                Class cl = arr.getClass().getComponentType();
                if (cl.getComponentType() != null)
                    cl = cl.getComponentType();
                Archiver compArchiver = getArchiverForType(cl);
                String line = readLineOrEOF(in);
                if (line != null && !line.equals("null")) {
                    Object obj = Array.get(arr, i);
                    obj = createArray(obj, line);
                    Array.set(arr, i, obj);
                    compArchiver.deserializeArray(obj, in);
                }
            }
            if (readLineOrEOF(in) != null)
                throw new Exception("Line: " + in.lineNum + " expected closing '}'");

        }
    };

    // TODO: Support more than 3D arrays?
    // TODO: byte?
    static {

        classMap.put("byte", byte.class);
        classMap.put("byte[]", byte[].class);
        classMap.put("[B", byte[].class);
        classMap.put("byte[][]", byte[][].class);
        classMap.put("[[B", byte[][].class);
        classMap.put("byte[][][]", byte[][][].class);
        classMap.put("[[[B", byte[][][].class);

        classMap.put("int", int.class);
        classMap.put("int[]", int[].class);
        classMap.put("[I", int[].class);
        classMap.put("int[][]", int[][].class);
        classMap.put("[[I", int[][].class);
        classMap.put("int[][][]", int[][].class);
        classMap.put("[[[I", int[][].class);

        classMap.put("float", float.class);
        classMap.put("float[]", float[].class);
        classMap.put("[F", float[].class);
        classMap.put("float[][]", float[][].class);
        classMap.put("[[F", float[][].class);
        classMap.put("float[][][]", float[][][].class);
        classMap.put("[[[F", float[][][].class);

        classMap.put("long", long.class);
        classMap.put("long[]", long[].class);
        classMap.put("[L", long[].class);
        classMap.put("long[][]", long[][].class);
        classMap.put("[[L", long[][].class);
        classMap.put("long[][][]", long[][][].class);
        classMap.put("[[[L", long[][][].class);

        classMap.put("double", double.class);
        classMap.put("double[]", double[].class);
        classMap.put("[D", double[].class);
        classMap.put("double[][]", double[][].class);
        classMap.put("[[D", double[][].class);
        classMap.put("double[][][]", double[][][].class);
        classMap.put("[[[D", double[][][].class);

        classMap.put("boolean", boolean.class);
        classMap.put("boolean[]", boolean[].class);
        classMap.put("[Z", boolean[].class);
        classMap.put("boolean[][]", boolean[][].class);
        classMap.put("[[Z", boolean[][].class);
        classMap.put("boolean[][][]", boolean[][][].class);
        classMap.put("[[[Z", boolean[][][].class);

        classMap.put("java.lang.String[]", String[].class);
        classMap.put("java.lang.String[][]", String[][].class);
        classMap.put("java.lang.String[][][]", String[][][].class);

        classMap.put("java.util.Arrays.ArrayList", ArrayList.class);

        classMap.put("java.lang.Boolean[]", Boolean[].class);
        classMap.put("java.lang.Boolean[][]", Boolean[][].class);
        classMap.put("java.lang.Boolean[][][]", Boolean[][][].class);

        classMap.put("java.lang.Integer[]", Integer[].class);
        classMap.put("java.lang.Integer[][]", Integer[][].class);
        classMap.put("java.lang.Integer[][][]", Integer[][][].class);

        classMap.put("java.lang.Float[]", Float[].class);
        classMap.put("java.lang.Float[][]", Float[][].class);
        classMap.put("java.lang.Float[][][]", Float[][][].class);

        classMap.put("java.lang.Double[]", Double[].class);
        classMap.put("java.lang.Double[][]", Double[][].class);
        classMap.put("java.lang.Double[][][]", Double[][][].class);

        classMap.put("java.lang.Long[]", Long[].class);
        classMap.put("java.lang.Long[][]", Long[][].class);
        classMap.put("java.lang.Long[][][]", Long[][][].class);

        classMap.put("java.lang.Byte[]", Byte[].class);
        classMap.put("java.lang.Byte[][]", Byte[][].class);
        classMap.put("java.lang.Byte[][][]", Byte[][][].class);

    }

    /**
     * This method is usefull for inner classes
     *
     * @param clazz
     */
    public static void registerClass(Class<?> clazz, String ... alternateNames) {
        String sClazz = getCanonicalName(clazz);
        int lastDot = sClazz.lastIndexOf(".");
        for (String alt : alternateNames) {
            String altName = sClazz.substring(0, lastDot+1)+alt;
            classMap.put(altName, clazz);
        }
        addArrayTypes(clazz);
        classMap.put(sClazz, clazz);
    }

    private static Class<?> getClassForName(String forName) throws ClassNotFoundException {
        if (classMap.containsKey(forName))
            return classMap.get(forName);
        try {
            return Reflector.class.getClassLoader().loadClass(forName);
        } catch (ClassNotFoundException e) {
            log.error("Failed to find class '" + forName + "'");
            throw e;
        }
    }

    private static void inheritValues(Class<?> clazz, Map<Field, Archiver> values) {
        if (clazz == null || clazz.equals(Archiver.class))
            return;
        if (classValues.containsKey(clazz)) {
            values.putAll(classValues.get(clazz));
        }
        inheritValues(clazz.getSuperclass(), values);
    }

    private static Map<Field, Archiver> getValues(Class<?> clazz, boolean createIfDNE) {
        try {
            if (getCanonicalName(clazz) == null) {
                if (clazz.getSuperclass() != null)
                    clazz = clazz.getSuperclass();
                else
                    clazz = clazz.getEnclosingClass();
            }
            Map<Field, Archiver> values = null;
            if (classValues.containsKey(clazz)) {
                values = classValues.get(clazz);
            } else if (createIfDNE) {
                // reject unsupported classes
                if (clazz.isAnonymousClass() || clazz.isSynthetic())
                    throw new Exception("Synthetic and anonymous classes not supported");
                // test newInstance works for this clazz
                if (!Modifier.isAbstract(clazz.getModifiers())) {
                    if (clazz.isArray()) {
                        System.out.println("array?");
                    }
                    clazz.newInstance();
                }
                values = new TreeMap<>(fieldComparator);
                // now inherit any values in base classes that were added
                inheritValues(clazz.getSuperclass(), values);
                classValues.put(clazz, values);

                String arrName = getCanonicalName(clazz);
                Class arrClass = clazz;
                for (int i=0; i<ARRAY_DIMENSION_VARIATIONS; i++) {
                    arrName += "[]";
                    arrClass = Array.newInstance(arrClass, 0).getClass();
                    classMap.put(arrName, arrClass);
                }

                //classMap.put(getCanonicalName(clazz) + "[]", Array.newInstance(clazz, 0).getClass());
                //classMap.put(getCanonicalName(clazz) + "[][]", Array.newInstance(Array.newInstance(clazz, 0).getClass(), 0).getClass());
            } else if (clazz.getSuperclass() == null) {
                //log.warn("Cannot find any fields to archive (did you add an addField(...) method in your class?)");
                values = Collections.emptyMap();
            } else {
                //log.warn("No values for " + clazz.getName());
                return getValues(clazz.getSuperclass(), createIfDNE);
            }
            return values;
        } catch (GException e) {
            throw e;
        } catch (Exception e) {
            throw new GException("Cannot instantiate " + getCanonicalName(clazz) + ". Is it public? Does it have a public 0 argument constructor?", e);
        }
    }

    private final static Comparator<Field> fieldComparator = new Comparator<Field>() {
        @Override
        public int compare(Field o1, Field o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    private final static Map<Class, Map<Class, Boolean>> subclassOfCache = new HashMap<>();

    public static boolean isSubclassOf(Class<?> subClass, Class<?> baseClass) {
        Boolean result;
        Map<Class, Boolean> baseCache = subclassOfCache.get(subClass);
        if (baseCache == null) {
            subclassOfCache.put(subClass, baseCache = new HashMap());
        } else {
            if ((result = baseCache.get(baseClass)) != null)
                return result;
        }

        if (subClass == null || subClass.equals(Object.class) || getCanonicalName(subClass).equals(getCanonicalName(Object.class)))
            result = false;
        else if (subClass == baseClass || subClass.equals(baseClass) || getCanonicalName(subClass).equals(getCanonicalName(baseClass)))
            result = true;
        else if (baseClass.isAssignableFrom(subClass))
            result = true;
        else result = isSubclassOf(subClass.getSuperclass(), baseClass);
        baseCache.put(baseClass, result);
        return result;
    }

    private static Archiver getArchiverForType(Class<?> clazz) {
        if (clazz.equals(Byte.class) || clazz.equals(byte.class)) {
            return byteArchiver;
        } else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            return booleanArchiver;
        } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            return longArchiver;
        } else if (clazz.equals(Double.class) || clazz.equals(double.class)) {
            return doubleArchiver;
        } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            return integerArchiver;
        } else if (clazz.equals(Float.class) || clazz.equals(float.class)) {
            return floatArchiver;
        } else if (clazz.equals(String.class)) {
            return stringArchiver;
        } else if (clazz.isEnum() || isSubclassOf(clazz, Enum.class)) {
            addArrayTypes(clazz);
            return enumArchiver;
        } else if (isSubclassOf(clazz, Reflector.class)) {
            return archivableArchiver;
        } else if (isSubclassOf(clazz, Collection.class)) {
            return collectionArchiver;
        } else if (isSubclassOf(clazz, Map.class)) {
            return mapArchiver;
        } else if (clazz.isArray()) {
            // add enums if this is an enum
            addArrayTypes(clazz);
            return arrayArchiver;
        } else {
            throw new GException("No reflector available for class: " + clazz);
        }
    }

    private static void addArrayTypes(Class<?> clazz) {
        String nm = clazz.getName();
        if (classMap.containsKey(nm))
            return;
        if (nm.indexOf("java.lang.Object") > 0)
            throw new GException("Arrays of Objects not supported");
        classMap.put(nm, clazz);
        classMap.put(nm.replace('$', '.'), clazz);
        String nm2 = nm;
        int lBrack = nm.lastIndexOf('[');
        if (lBrack > 0) {
            nm = nm.substring(lBrack + 2, nm.length() - 1);
            nm2 = nm.replace('$', '.');
        }
        try {
            clazz = Class.forName(nm);
            classMap.put(nm2, clazz);
            clazz = Array.newInstance(clazz, 1).getClass();
            classMap.put(nm2 + "[]", clazz);
            clazz = Array.newInstance(clazz, 1).getClass();
            classMap.put(nm2 + "[][]", clazz);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a field of a specific class to be included in the archivable handler.
     * Supported Type:
     * All Primitives
     * Strings
     * Enums
     * Arrays
     * Collections
     * Classes derived from Archivable
     * <p>
     * Also, fields are inherited.
     *
     * @param clazz
     * @param name
     */
    public static void addField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            if (field.getAnnotation(Omit.class) != null) {
                log.debug("Field '" + name + "' has been omitted using Omit annotation.");
                return;
            }
            if (Modifier.isStatic(field.getModifiers()))
                throw new GException("Cannot add static fields");
            field.setAccessible(true);
            Archiver archiver = getArchiverForType(field.getType());
            Map<Field, Archiver> values = getValues(clazz, true);
            if (values.containsKey(field))
                throw new GException("Duplicate field.  Field '" + name + "' has already been included for class: " + getCanonicalName(clazz));
            values.put(field, archiver);
            classMap.put(clazz.getName().replace('$', '.'), clazz);
            //log.debug("Added field '" + name + "' for " + clazz);
        } catch (GException e) {
            throw e;
        } catch (NoSuchFieldException e) {
            if (THROW_ON_UNKNOWN)
                throw new GException("Failed to add field '" + name + "'", e);
            log.warn("Field '" + name + "' not found for class: " + clazz);
        } catch (Exception e) {
            throw new GException("Failed to add field '" + name + "'", e);
        }
    }

    /**
     * Remove a field for archiving for a specific class.  If field not present, then no effect.
     * If field not one of clazz, then runtime exception thrown.
     *
     * @param clazz
     * @param name
     */
    public static void removeField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            Map<Field, Archiver> values = getValues(clazz, false);
            if (values != null) {
                values.remove(field);
            }
        } catch (Exception e) {
            throw new GException(e);
        }
    }

    /**
     * Add a field of a specific class with a custom achiver.
     *
     * @param clazz
     * @param name
     * @param archiver
     * @throws Exception
     */
    public static void addField(Class<?> clazz, String name, Archiver archiver) {
        try {
            Field field = clazz.getDeclaredField(name);
            Map<Field, Archiver> values = getValues(clazz, true);
            if (values.containsKey(field))
                throw new GException("Duplicate field.  Field '" + name + "' has already been included for class: " + getCanonicalName(clazz));
            values.put(field, archiver);
        } catch (GException e) {
            throw e;
        } catch (Exception e) {
            throw new GException("Failed to add field '" + name + "'", e);
        }
    }

    /**
     * @param clazz
     */
    public static void addAllFields(Class<?> clazz) {
        if (DISABLED)
            return;
        addArrayTypes(clazz);
        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers()))
                    continue;
                addField(clazz, f.getName());
            }
            for (Class e : clazz.getClasses()) {
                addArrayTypes(e); // add enclosed classes
            }
        } catch (Exception e) {
            throw new GException("Failed to add all fields", e);
        }
    }

    /**
     * Convenience
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public static String serializeObject(Object obj) throws IOException {
        if (obj == null)
            return null;
        StringWriter out = new StringWriter();
        serializeObject(obj, new MyPrintWriter(out));
        return out.getBuffer().toString();
    }

    /**
     * Allows serializing of non-reflector types
     *
     * @param obj
     * @param out
     * @throws Exception
     */
    public static void serializeObject(Object obj, PrintWriter out) throws IOException {
        MyPrintWriter _out;
        if (out instanceof MyPrintWriter)
            _out = (MyPrintWriter) out;
        else
            _out = new MyPrintWriter(out);
        if (obj.getClass().isArray()) {
            int num = Array.getLength(obj);
            _out.println(getCanonicalName(obj.getClass()) + " " + num + " {");
        } else {
            _out.println(getCanonicalName(obj.getClass()) + " {");
        }
        try {
            serializeObject(obj, _out, true);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * This version will derive the objecft type from the top level element.
     *
     * @param file
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T> T deserializeFromFile(File file) throws IOException {
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                return deserializeObject(new MyBufferedReader(new InputStreamReader(in)));
            }
        } else {
            InputStream in = Reflector.class.getClassLoader().getResourceAsStream(file.getName());
            if (in == null) {
                throw new FileNotFoundException(file.getAbsolutePath());
            }
            return deserializeObject(new MyBufferedReader(new InputStreamReader(in)));
        }
    }

    /**
     * @param o
     * @param file
     * @throws IOException
     */
    public static <T> void serializeToFile(Object o, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            serializeObject(o, new MyPrintWriter(out));
        }
    }

    /**
     * Convenience
     *
     * @param str
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T> T deserializeFromString(String str) throws IOException {
        if (str == null)
            return null;
        StringReader in = new StringReader(str);
        return deserializeObject(new MyBufferedReader(in));
    }

    /**
     * @param in
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T> T deserializeFromInputStream(InputStream in) throws IOException {
        return deserializeObject(new MyBufferedReader(new InputStreamReader(in)));
    }

    /**
     * Get a non reflector object from serialized output (see serializeObject)
     *
     * @param in
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T> T deserializeObject(BufferedReader in) throws IOException {
        MyBufferedReader _in;
        if (in instanceof MyBufferedReader)
            _in = (MyBufferedReader) in;
        else
            _in = new MyBufferedReader(in);
        try {
            Object o = _deserializeObject(_in);
            return (T) o;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error on line " + _in.lineNum + " " + e.getMessage(), e);
        }
    }

    private static Object _deserializeObject(MyBufferedReader in) throws Exception {
        String line = readLineOrEOF(in);
        String[] parts = line.split(" ");
        if (parts.length < 1)
            throw new Exception("Not of form <class> <len>? {");
        Class<?> clazz = getClassForName(parts[0]);
        if (parts.length > 1) {
            Archiver a = getArchiverForType(clazz.getComponentType());
            int len = Integer.parseInt(parts[1]);
            Object o = Array.newInstance(clazz.getComponentType(), len);
            a.deserializeArray(o, in);
            return o;
        }
        return parse(null, clazz, in);
    }

    /**
     * @param obj
     * @param out
     * @param printObjects
     * @throws Exception
     */
    private static void serializeObject(Object obj, MyPrintWriter out, boolean printObjects) throws Exception {
        if (obj == null) {
            out.println("null");
        } else if (obj instanceof Reflector) {
            out.push();
            ((Reflector<?>) obj).serialize(out);
            out.pop();
            out.println("}");
        } else if (obj instanceof Collection) {
            Collection<?> c = (Collection<?>) obj;
            out.push();
            for (Object o : c) {
                if (o != null && o.getClass().isArray()) {
                    int len = Array.getLength(o);
                    out.println(o.getClass().getComponentType().getName() + " " + len + " {");
                } else {
                    if (o == null) {
                        out.println("null");
                        continue;
                    }
                    out.println(getCanonicalName(o.getClass()) + " {");
                }
                serializeObject(o, out, true);
            }
            out.pop();
            out.println("}");
        } else if (obj instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) obj;
            out.push();
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                Object o = entry.getKey();
                out.println(getCanonicalName(o.getClass()) + " {");
                serializeObject(o, out, true);
                o = entry.getValue();
                if (o == null) {
                     out.println("null");
                } else {
                    out.println(getCanonicalName(o.getClass()) + " {");
                    serializeObject(o, out, true);
                }
            }
            out.pop();
            out.println("}");
        } else if (obj.getClass().isArray()) {
            Archiver compArchiver = getArchiverForType(obj.getClass().getComponentType());
            out.push();
            compArchiver.serializeArray(obj, out);
            out.pop();
            out.println("}");
        } else if (printObjects) {
            out.push();
            if (obj instanceof String) {
                out.println("\"" + encodeString((String) obj) + "\"");
            } else {
                out.println(obj);
            }
            out.pop();
            out.println("}");
        }
    }

    static String encodeString(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8").replace("\n", "%0A").replace("\t", "%09");
    }

    static String decodeString(String in) throws Exception {
        return URLDecoder.decode(in, "UTF-8");
    }

    protected synchronized void serialize(PrintWriter out_) throws IOException {
//        Utils.println("Serializing %s", getClass().getName());
        if (Profiler.ENABLED) Profiler.push("Reflector.serialize");
        MyPrintWriter out;
        if (out_ instanceof MyPrintWriter)
            out = (MyPrintWriter) out_;
        else
            out = new MyPrintWriter(out_);
        try {
            Map<Field, Archiver> values = getValues(getClass(), false);
            for (Field field : values.keySet()) {
                Archiver archiver = values.get(field);
                field.setAccessible(true);
                Object obj = field.get(Reflector.this);
                if (obj == null) {
                    out.println(field.getName() + "=null");
                    continue;
                }
                out.println(field.getName() + "=" + archiver.get(field, this));
                serializeObject(obj, out, false);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (Profiler.ENABLED) Profiler.pop("Reflector.serialize");
        }
    }

    /**
     * Override this to do extra handling. Derived should call super.
     *
     * @param out
     * @throws IOException
     */
    public final void serialize(OutputStream out) throws IOException {
        serialize(new MyPrintWriter(out));
    }

    private static String readLineAndClosedParen(MyBufferedReader in) throws IOException {
        String value = readLineOrEOF(in);
        String line = readLineOrEOF(in);
        if (line != null)
            throw new IOException("Expected closing paren } but found: " + line);
        return value;
    }

    private static Object parse(Object current, Class<?> clazz, MyBufferedReader in) throws Exception {
        if (clazz.isEnum())
            return findEnumEntry(clazz, readLineOrEOF(in));
        if (clazz.isArray()) {
            throw new Exception("This method not to be called for array types");
        }
        if (isSubclassOf(clazz, Integer.class)) {
            return Integer.parseInt(readLineAndClosedParen(in));
        }
        if (isSubclassOf(clazz, Float.class)) {
            return Float.parseFloat(readLineAndClosedParen(in));
        }
        if (isSubclassOf(clazz, Long.class)) {
            return Long.parseLong(readLineAndClosedParen(in));
        }
        if (isSubclassOf(clazz, Double.class)) {
            return Double.parseDouble(readLineAndClosedParen(in));
        }
        if (isSubclassOf(clazz, Boolean.class)) {
            return Boolean.parseBoolean(readLineAndClosedParen(in));
        }
        if (isSubclassOf(clazz, Character.class)) {
            return new Character(readLineAndClosedParen(in).trim().charAt(0));
        }
        if (isSubclassOf(clazz, Reflector.class)) {
            Reflector<?> a;
            if (!KEEP_INSTANCES || current == null)
                a = (Reflector<?>) clazz.newInstance();
            else
                a = (Reflector) current;
            a.deserialize(in);
            return a;
        }
        if (isSubclassOf(clazz, Map.class)) {
            Map map = (Map) clazz.newInstance();
            deserializeMap(map, in);
            return map;
        }
        if (isSubclassOf(clazz, Collection.class)) {
            Collection c = (Collection) clazz.newInstance();
            deserializeCollection(c, in);
            return c;
        }
        if (isSubclassOf(clazz, String.class)) {
            String sin = readLineAndClosedParen(in);
            if (sin == null)
                return null;
            return decodeString(sin.substring(1, sin.length() - 1));
        }
        try {
            // try to create from a string constructor
            Constructor<?> cons = clazz.getConstructor(String.class);
            String arg = readLineOrEOF(in);
            return cons.newInstance(arg);
        } catch (NoSuchMethodException e) {
        }
        throw new Exception("Dont know how to parse class " + clazz);
    }

    private static boolean isImmutable(Object o) {
        if (o instanceof String || o instanceof Number)
            return true;
        if (o instanceof Reflector && ((Reflector) o).isImmutable())
            return true;
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void deserializeCollection(Collection c, MyBufferedReader in) throws Exception {
        final int startDepth = in.depth;
        Iterator it = null;

        if (!KEEP_INSTANCES || c.size() == 0 || isImmutable(c.iterator().next()))
            c.clear();
        else {
            it = c.iterator();
        }
        while (true) {
            String line = readLineOrEOF(in);
            if (line == null)
                break;
            Object entry = null;
            boolean doAdd = true;
            if (it != null && it.hasNext()) {
                entry = it.next();
                doAdd = false;
            }
            if (doAdd) {
                it = null;
            }
            if (!line.equals("null")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    int num = Integer.parseInt(parts[1]);
                    Class<?> clazz = getClassForName(parts[0]);
                    if (!KEEP_INSTANCES || entry == null || Array.getLength(entry) != num) {
                        //Class<?> clazz = getClassForName(parts[0]);
                        entry = Array.newInstance(clazz, num);
                    }
                    getArchiverForType(clazz).deserializeArray(entry, in);
                } else {
                    Class<?> clazz;
                    if (!KEEP_INSTANCES || entry == null)
                        clazz = getClassForName(parts[0]);
                    else
                        clazz = entry.getClass();
                    entry = parse(entry, clazz, in);
                }
                if (in.depth > startDepth)
                    if (readLineOrEOF(in) != null)
                        throw new Exception("Line " + in.lineNum + " Expected closing '}'");
            }
            if (doAdd)
                c.add(entry);
        }
        while (it != null && it.hasNext()) {
            it.next();
            it.remove(); // remove any remaining in the collection
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void deserializeMap(Map c, MyBufferedReader in) throws Exception {
        int startDepth = in.depth;
        while (true) {
            String line = readLineOrEOF(in);
            if (line == null || line.equals("null"))
                break;
            Class<?> clazz = getClassForName(line);
            Object key = parse(null, clazz, in);
            if (key == null)
                throw new Exception("null key in map");
            if (in.depth > startDepth) {
                line = readLineOrEOF(in);
                if (line != null)
                    throw new Exception("Expected closing }");
            }
            line = readLineOrEOF(in);
            if (line == null)
                throw new Exception("Missing value from key/value pair in map");
            Object value = null;
            if (line != null && !line.equals("null")) {
                clazz = getClassForName(line);
                value = parse(null, clazz, in);
                if (in.depth > startDepth) {
                    line = readLineOrEOF(in);
                    if (line != null)
                        throw new Exception("Expected closing }");
                }
            }
            c.put(key, value);
        }
    }

    /**
     * @param text
     * @throws Exception
     */
    public synchronized final void deserialize(String text) throws Exception {
        MyBufferedReader reader = new MyBufferedReader(new StringReader(text));
        try {
            deserialize(reader);
        } catch (Exception e) {
            throw new Exception("Error on line " + reader.lineNum + ": " + e.getMessage(), e);
        }
    }

    /**
     * Same as deserialize with KEEP_INSTANCES enabled.
     *
     * @param diff
     * @throws Exception
     */
    public synchronized final void mergeDiff(String diff) throws Exception {
        boolean prev = KEEP_INSTANCES;
        KEEP_INSTANCES = true;
        try {
            deserialize(diff);
        } finally {
            KEEP_INSTANCES = prev;
        }
    }

    /**
     * @param in
     * @throws Exception
     */
    public synchronized final void deserialize(InputStream in) throws IOException {
        MyBufferedReader reader = new MyBufferedReader(new InputStreamReader(in));
        try {
            deserialize(reader);
        } catch (IOException e) {
            throw new IOException("Error on line " + reader.lineNum + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Error on line " + reader.lineNum + ": " + e.getMessage(), e);
        }
    }

    private boolean fieldMatches(Field field, String name) throws Exception {
        if (field.getName().equals(name))
            return true;

        Alternate alt = field.getAnnotation(Alternate.class);
        if (alt != null) {
            for (String variation : alt.variations()) {
                if (name.equals(variation))
                    return true;
            }
        }

        return false;
    }

    /**
     * initialize fields of this object there are explicitly added by addField for this class type.
     *
     * @param _in
     * @throws Exception
     */
    protected synchronized void deserialize(BufferedReader _in) throws Exception {
        if (Profiler.ENABLED) Profiler.push("Reflector.deserialize");
        try {
            MyBufferedReader in = null;
            if (_in instanceof MyBufferedReader)
                in = (MyBufferedReader) _in;
            else
                in = new MyBufferedReader(_in);
            Map<Field, Archiver> values = getValues(getClass(), false);
            final int depth = in.depth;
            while (true) {
                if (in.depth > depth)
                    if (in.readLine() != null)
                        throw new Exception("Line: " + in.lineNum + " Expected closing '}'");
                String line = readLineOrEOF(in);
                if (line == null)
                    break;
                String[] parts = line.split("=");
                if (parts.length < 2)
                    throw new Exception("Line '" + line + "' not of form 'name=value'");
                String name = parts[0].trim();
                for (Field field : values.keySet()) {
                    if (fieldMatches(field, name)) {
                        Archiver archiver = values.get(field);
                        Object instance = field.get(this);
                        archiver.set(instance, field, parts[1], this);
                        if (field.get(Reflector.this) instanceof Reflector) {
                            Reflector<T> ref = (Reflector<T>) field.get(Reflector.this);
                            ref.deserialize(in);
                        } else if (field.getType().isArray()) {
                            Object obj = field.get(this);
                            if (obj != null) {
                                Archiver arrayArchiver = getArchiverForType(obj.getClass().getComponentType());
                                arrayArchiver.deserializeArray(obj, in);
                            }
                        } else if (isSubclassOf(field.getType(), Collection.class)) {
                            Collection<?> collection = (Collection<?>) field.get(this);
                            if (collection != null)
                                deserializeCollection(collection, in);
                        } else if (isSubclassOf(field.getType(), Map.class)) {
                            Map<?, ?> map = (Map<?, ?>) field.get(this);
                            if (map != null)
                                deserializeMap(map, in);
                        }
                        parts = null;
                        break;
                    }
                }
                if (parts != null) {
                    if (THROW_ON_UNKNOWN)
                        throw new Exception("Unknown field: " + name + " not in fields: " + values.keySet());
                    log.error("Unknown field: " + name + " not found in class: " + getClass());// + " not in fields: " + values.keySet());
                    // skip ahead until depth matches current depth
                    while (in.depth > depth) {
                        readLineOrEOF(in);
                    }
                }
            }
        } finally {
            if (Profiler.ENABLED) Profiler.pop("Reflector.deserialize");
        }
    }

    private static boolean isArraysEqual(Object a, Object b) {
        int lenA = Array.getLength(a);
        int lenB = Array.getLength(b);
        if (lenA != lenB)
            return false;
        for (int i = 0; i < lenA; i++) {
            if (!isEqual(Array.get(a, i), Array.get(b, i)))
                return false;
        }
        return true;
    }

    private static boolean isCollectionsEqual(Collection c0, Collection c1) {
        if (c0.size() != c1.size())
            return false;

        Iterator i0 = c0.iterator();
        Iterator i1 = c1.iterator();

        while (i0.hasNext()) {
            if (!isEqual(i0.next(), i1.next()))
                return false;
        }

        return true;
    }

    private static boolean isMapsEqual(Map m0, Map m1) {
        if (m0.size() != m1.size())
            return false;

        for (Object key : m0.keySet()) {
            if (!m1.containsKey(key))
                return false;
            if (!isEqual(m0.get(key), m1.get(key)))
                return false;
        }
        return true;
    }

    private static boolean isEqual(Object a, Object b) {
        if (a == b)
            return true;
        if (a == null || b == null)
            return false;
        if (a instanceof Reflector && b instanceof Reflector) {
            return ((Reflector) a).deepEquals((Reflector) b);
        }
        if (a.getClass().isArray() && b.getClass().isArray())
            return isArraysEqual(a, b);
        if ((a instanceof Collection) && (b instanceof Collection)) {
            return isCollectionsEqual((Collection) a, (Collection) b);
        }
        if ((a instanceof Map) && (b instanceof Map)) {
            return isMapsEqual((Map) a, (Map) b);
        }
        if (!a.getClass().equals(b.getClass()))
            return false;
        return a.equals(b);
    }

    public final boolean deepEquals(Reflector<T> a) {
        if (a == this)
            return true;
        if (a == null)
            return false;
        try {
            Map<Field, Archiver> values = getValues(getClass(), false);
            for (Field f : values.keySet()) {
                try {
                    if (!isEqual(f.get(this), f.get(a)))
                        return false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        } catch (Exception e) {
            return super.equals(a);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return isEqual(this, obj);
    }

    @Override
    public String toString() {
        StringWriter buf = new StringWriter();
        try {
            serialize(new MyPrintWriter(buf));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buf.toString();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> T deepCopy(T o) {
        try {
            if (o == null)
                return null;
            if (o instanceof Reflector)
                return (T) ((Reflector) o).deepCopy();

            if (o.getClass().isArray()) {
                int len = Array.getLength(o);
                Object arr = Array.newInstance(o.getClass().getComponentType(), len);
                for (int i = 0; i < len; i++) {
                    Object oo = deepCopy(Array.get(o, i));
                    Array.set(arr, i, oo);
                }
                return (T) arr;
            }
            if (o instanceof Collection) {
                Collection oldCollection = (Collection) o;
                Collection newCollection = (Collection) o.getClass().newInstance();
                for (Object oo : oldCollection) {
                    newCollection.add(deepCopy(oo));
                }
                return (T) newCollection;
            }
            if (o instanceof Map) {
                Map map = (Map) o;
                Map newMap = (Map) o.getClass().newInstance();
                Iterator it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    newMap.put(deepCopy(entry.getKey()), deepCopy(entry.getValue()));
                }
                return (T) newMap;
            }
            // TODO: Test that this is a primitive, enum otherwise error
            // Hopefully this is a primitive, enum 
            //System.err.println("Dont know how to deepCopy: " + o.getClass() + ": " + o);
            return o;
        } catch (Exception e) {
            throw new GException(e);
        }
    }

    /**
     * Deep copy all fields explicitly set be addField.  Any other fields are NOT deep copied, nor initialized.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public T deepCopy() {
        try {
            Object copy = getClass().newInstance();
            Map<Field, Archiver> values = getValues(getClass(), false);

            for (Field f : values.keySet()) {
                f.setAccessible(true);
                f.set(copy, deepCopy(f.get(this)));
            }

            return (T) copy;
        } catch (Exception e) {
            throw new GException("Failed to deep copy", e);
        }
    }

    /**
     * Collections, Arrays and Maps are shallow copied from this into a new instance
     *
     * @return
     */
    public T shallowCopy() {
        try {
            Object copy = getClass().newInstance();
            Map<Field, Archiver> values = getValues(getClass(), false);

            for (Field f : values.keySet()) {
                f.setAccessible(true);
                f.set(copy, shallowCopy(f.get(this)));
            }

            return (T) copy;

        } catch (Exception e) {
            throw new GException("Failed to shallow copy");
        }
    }

    public static Object shallowCopy(Object o) throws Exception {
        if (o == null) {
            return null;
        }
        if (o instanceof Collection) {
            Collection copy = (Collection) o.getClass().newInstance();
            copy.addAll((Collection) o);
            return copy;
        }
        if (o instanceof Map) {
            Map copy = (Map) o.getClass().newInstance();
            copy.putAll((Map) o);
            return copy;
        }
        if (o.getClass().isArray()) {
            int len = Array.getLength(o);
            Object copy = Array.newInstance(o.getClass(), len);
            for (int i = 0; i < len; i++) {
                Array.set(copy, i, Array.get(o, i));
            }
            return copy;
        }
        if (o instanceof Reflector) {
            return ((Reflector) o).shallowCopy();
        }
        return o;
    }

    /**
     * @param other
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized final T copyFrom(Reflector<T> other) {
        if (other == this) {
            log.error("Copying from self?");
            return (T) this;
        }

        try {
            Map<Field, Archiver> values = getValues(getClass(), false);
            Map<Field, Archiver> otherValues = getValues(other.getClass(), false);

            for (Field f : values.keySet()) {

                if (!otherValues.containsKey(f))
                    continue;

                f.setAccessible(true);
                Object o = f.get(this);
                if (o != null && o instanceof Reflector) {
                    Reflector<T> n = (Reflector<T>) f.get(other);
                    if (n != null)
                        ((Reflector<T>) o).copyFrom(n);
                    else
                        f.set(this, null);
                } else {
                    f.set(this, deepCopy(f.get(other)));
                }
            }

            return (T) this;

        } catch (Exception e) {
            throw new GException("Failed to deep copy", e);
        }
    }

    /**
     * Convenience method
     *
     * @param file
     * @throws IOException
     */
    public final synchronized void saveToFile(File file) throws IOException {
        log.debug("saving to file %s", file.getAbsolutePath());
        try (FileOutputStream out = new FileOutputStream(file)) {
            serialize(out);
        }
    }

    /**
     * Convenience method to attempt a save to file and fail silently on error
     *
     * @param file
     */
    public final void trySaveToFile(File file) {
        try {
            saveToFile(file);
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * Convenience method
     *
     * @param file
     * @throws IOException
     */
    public synchronized void loadFromFile(File file) throws IOException {
        log.debug("Loading from file %s", file.getAbsolutePath());
        try (InputStream in = new FileInputStream(file)) {
            deserialize(in);
        }
    }

    /**
     * Convenience method to load from file and fail silently
     *
     * @param file
     * @returns true on success and false on failure
     */
    public final boolean tryLoadFromFile(File file) {
        try {
            loadFromFile(file);
            return true;
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Return a string that represenrts the differences in other compared to this.
     * The resulting strings can be deserialized back into this with KEEP_INSTANCES to merge in changes.
     * <p>
     * Purpose is to allow for situations where we want to transmit changes in the objects instead of whole object
     *
     * @param other
     * @return
     * @throws Exception
     */
    public synchronized String diff(Reflector<T> other) {
        if (other == null)
            throw new NullPointerException("Reflector.diff - other cannot be null");

        StringWriter out = new StringWriter();
        try (MyPrintWriter writer = new MyPrintWriter(out)) {
            diff(other, writer);
            writer.flush();
        } catch (Exception e) {
            throw new GException(e);
        }
        return out.getBuffer().toString();
    }

    private final void diff(Reflector<T> other, MyPrintWriter writer) throws Exception {

        Map<Field, Archiver> values = null;
        if (isSubclassOf(other.getClass(), getClass()))
            values = getValues(other.getClass(), false);
        else if (isSubclassOf(getClass(), other.getClass()))
            values = getValues(getClass(), false);
        else
            throw new Exception("Classes " + getClass() + " and " + other.getClass() + " are not related");

        for (Field field : values.keySet()) {
            Archiver archiver = values.get(field);
            field.setAccessible(true);

            Object mine = field.get(this);
            Object thrs = null;
            try {
                thrs = field.get(other);
            } catch (IllegalArgumentException e) {
                continue;
            }

            if (isEqual(mine, thrs))
                continue; // no difference

            if (mine == null) {
                // difference is entire contents of them
                writer.println(field.getName() + "=" + archiver.get(field, other));
                serializeObject(thrs, writer, false);
            } else if (thrs == null) {
                writer.println(field.getName() + "=null");
            } else if (isSubclassOf(mine.getClass(), thrs.getClass())) {
                if (mine instanceof Reflector) {
                    writer.println(field.getName() + "=" + getCanonicalName(thrs.getClass()) + " {");
                    writer.push();
                    ((Reflector) mine).diff((Reflector) thrs, writer);
                    writer.pop();
                    writer.println("}");
                } else if (mine instanceof Collection) {
                    diffCollections(field.getName(), (Collection) mine, (Collection) thrs, writer);
                } else if (mine instanceof Map) {
                    diffMaps(field.getName(), (Map) mine, (Map) thrs, writer);
                } else if (mine.getClass().isArray()) {
                    diffArrays(field.getName(), mine, thrs, writer);
                } else {
                    String themStr = archiver.get(field, other);
                    writer.println(field.getName() + "=" + themStr);
                }
            } else {
                throw new Exception("Cannot diff object that are not related");
            }
        }
    }

    private void diffMaps(String name, Map mine, Map thrs, MyPrintWriter writer) throws Exception {
        writer.println(name + "=" + getCanonicalName(thrs.getClass()) + " {");
        writer.push();

        for (Object key : mine.keySet()) {

            serializeObject(key, writer);

            Object mineVal = mine.get(key);
            Object thrsVal = thrs.get(key);

            diffObjects(mineVal, thrsVal, writer);
        }

        for (Object key : thrs.keySet()) {

            if (mine.containsKey(key))
                continue;

            serializeObject(key, writer);
            serializeObject(thrs.get(key), writer);
        }

        writer.pop();
        writer.println("}");
    }

    private void diffObjects(Object o1, Object o2, MyPrintWriter writer) throws Exception {
        if (o2 == null) {
            writer.println("null");
        } else if (o1 == null) {
            serializeObject(o2, writer);
        } else if (o2.getClass().isArray()) {
            diffArrays(null, o1, o2, writer);
        } else if (o2 instanceof Reflector) {
            writer.println(getCanonicalName(o2.getClass()) + " {");
            writer.push();
            if (((Reflector) o2).isImmutable()) {
                ((Reflector) o2).serialize(writer);
            } else {
                ((Reflector) o1).diff((Reflector) o2, writer);
            }
            writer.pop();
            writer.println("}");
        } else {
            serializeObject(o2, writer);
        }
    }

    private void diffCollections(String name, Collection mine, Collection thrs, MyPrintWriter writer) throws Exception {
        writer.println(name + "=" + getCanonicalName(thrs.getClass()) + " {");
        writer.push();

        Iterator i1 = mine.iterator();
        Iterator i2 = thrs.iterator();

        while (i2.hasNext() || i2.hasNext()) {
            Object o1 = i1.hasNext() ? i1.next() : null;
            Object o2 = i2.hasNext() ? i2.next() : null;

            diffObjects(o1, o2, writer);
        }

        writer.pop();
        writer.println("}");
    }

    private static final void diffArrays(String name, Object array1, Object array2, MyPrintWriter writer) throws Exception {
        if (isArraysEqual(array1, array2))
            return;
        int len1 = Array.getLength(array1);
        int len2 = Array.getLength(array2);
        writer.println((name != null ? (name + "=") : "") + array2.getClass().getComponentType().getName().replace('$', '.') + " " + len2 + " {");
        writer.push();
        for (int i = 0; i < len2; i++) {
            Object o1 = i < len1 ? Array.get(array1, i) : null;
            Object o2 = Array.get(array2, i);
            if (o2 == null) {
                writer.println("null");
            } else if (o2 instanceof Reflector) {
                writer.println(getCanonicalName(o2.getClass()) + " {");
                if (o1 == null)
                    serializeObject(o2, writer, true);
                else {
                    writer.push();
                    ((Reflector) o1).diff((Reflector) o2, writer);
                    writer.pop();
                    writer.println("}");
                }
            } else {
                Archiver a = getArchiverForType(o2.getClass());
                a.serializeArray(array2, writer);
                break;
            }
        }
        writer.pop();
        writer.println("}");

    }

    /**
     * Override this for some classes that should be considered immutable
     *
     * @return
     */
    protected boolean isImmutable() {
        return false;
    }

    public static void dump() {
        log.info("classMap=" + classMap.toString().replace(',', '\n'));
        log.info("classValues=" + classValues.toString().replace(',', '\n'));
        log.info("canonicalNameCash=" + caconicalNameCash.toString().replace(',', '\n'));

    }
}
