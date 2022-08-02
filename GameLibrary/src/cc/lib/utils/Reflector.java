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
import java.util.zip.CRC32;
import java.util.zip.Checksum;

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
     * Turn this on to throw exceptions on any unknown fields.  Default is off.
     */
    public static boolean THROW_ON_UNKNOWN = false;

    /**
     * Strip package name qualifier from serialize and deserialize
     */
    public static boolean STRIP_PACKAGE_QUALIFIER = false;

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

    private final static Map<Class<?>, Map<Field, Archiver>> classValues = new HashMap<>();
    private final static Map<String, Class<?>> classMap = new HashMap<>();
    private final static Map<Class, Map<Class, Boolean>> subclassOfCache = new HashMap<>();

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

        public String peekLine() throws IOException {
            try {
                mark(1024);
                return super.readLine();
            } finally {
                reset();
            }
        }
    }

    public static class ParseException extends IOException {
        final int lineNum;
        ParseException(int lineNum, String msg) {
            super(msg);
            this.lineNum = lineNum;
        }

        ParseException(int lineNum, Exception e) {
            super(e);
            this.lineNum = lineNum;
        }

        @Override
        public String getMessage() {
            return "Line (" + lineNum + ") " + super.getMessage();
        }
    }

    public static class MyPrintWriter extends PrintWriter {

        final boolean numbered;
        static String[] indents;
        int lineNum = 0;
        boolean indented = false;

        static {
            indents = new String[32];
            String indent = "";
            for (int i = 0; i < indents.length; i++) {
                indents[i] = indent;
                indent += "   ";
            }
        }

        public MyPrintWriter(Writer out, boolean numbered) {
            super(out, true);
            this.numbered = numbered;
        }

        public MyPrintWriter(Writer out) {
            this(out, false);
        }

        public MyPrintWriter(OutputStream out, boolean numbered) {
            super(out, true);
            this.numbered = numbered;
        }

        public MyPrintWriter(OutputStream out) {
            this(out, false);
        }

        private int currentIndent = 0;

        void push() {
            println(" {");
            if (currentIndent < indents.length - 1)
                currentIndent++;
        }

        void pop() {
            Utils.assertTrue (currentIndent > 0);
            if (currentIndent > 0)
                currentIndent--;
            println("}");
        }

        @Override
        public void write(String s) {
            if (!indented) {
                if (numbered)
                    super.write(String.format("%-5d:", lineNum ++ ));
                super.write(indents[currentIndent]);
                indented = true;
            }
            super.write(s);
        }

        @Override
        public void println() {
            super.println();
            indented = false;
            }

        MyPrintWriter p(Object o) {
            write(String.valueOf(o));
            return this;
        }

    }

    public interface Archiver {
        String get(Field field, Reflector<?> a) throws Exception;

        void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception;

        void serializeArray(Object arr, MyPrintWriter out) throws Exception;

        void deserializeArray(Object arr, MyBufferedReader in, boolean keepInstances) throws Exception;
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
        public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
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
                for (int i = 0; i < len; i++) {
                    out.p(Array.get(arr, i)).p(" ");
                }
                out.println();
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in, boolean keepInstances) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                String line = readLineOrEOF(in);
                String[] parts = line.split(" ");
                if (parts.length != len)
                    throw new ParseException(in.lineNum, "Expected " + len + " parts but found " + parts.length);
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
        public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
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
                        out.p("\"").p(encodeString((String) entry)).println("\"");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in, boolean keepInstances) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                String line = readLineOrEOF(in);
                if (line != null && !line.equals("null")) {
                    String s = decodeString(line.substring(1, line.length() - 1));
                    Array.set(arr, i, s);
                }
            }
            if (readLineOrEOF(in) != null)
                throw new ParseException(in.lineNum, " expected closing '}'");
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
        throw new GException("Failed to find enum value: '" + value + "' in available constants: " + Arrays.asList(constants));
    }

    private static Archiver enumArchiver = new Archiver() {
        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            return ((Enum<?>) field.get(a)).name();
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
            field.set(a, findEnumEntry(field.getType(), value));
        }

        @Override
        public void serializeArray(Object arr, MyPrintWriter out) {
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
        public void deserializeArray(Object arr, MyBufferedReader in, boolean keepInstances) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                String line = readLineOrEOF(in);
                String[] parts = line.split(" ");
                if (parts.length != len)
                    throw new ParseException(in.lineNum, "Expected " + len + " parts but found " + parts.length);
                for (int i = 0; i < len; i++) {
                    Enum<?> enumEntry = findEnumEntry(arr.getClass().getComponentType(), parts[i]);
                    Array.set(arr, i, enumEntry);
                }
                if (readLineOrEOF(in) != null)
                    throw new ParseException(in.lineNum, " expected closing '}'");
            }
        }
    };

    private final static Map<Class, String> canonicalNameCache = new HashMap<>();

    private static String getCanonicalName(Class clazz) {
        String name;
        if (STRIP_PACKAGE_QUALIFIER) {
            if (clazz.isAnonymousClass()) {
                clazz = clazz.getSuperclass();
            }
            if ((name = canonicalNameCache.get(clazz)) != null) {
                return name;
            }
            if (!classMap.containsKey(clazz.getSimpleName())) {
                name = clazz.getSimpleName();
                classMap.put(name, clazz);
            } else {
                name = clazz.getCanonicalName();
            }
        } else {
            if ((name = canonicalNameCache.get(clazz)) != null) {
                return name;
            }
            boolean isArray = clazz.isArray();

            boolean isEnum = clazz.isEnum();
            boolean isAnnotation = clazz.isAnnotation();
            boolean isAnnonymous = clazz.isAnonymousClass();
            boolean isInterface = clazz.isInterface();
            boolean isLocal = clazz.isLocalClass();
            boolean isMember = clazz.isMemberClass();
            boolean isPrimitive = clazz.isPrimitive();
            boolean isSynthetic = clazz.isSynthetic();
            Class<?> superClass = clazz.getSuperclass();

            if (clazz.isAnonymousClass() || (superClass != null && superClass.isEnum())) {
                clazz = clazz.getSuperclass();
            }
            //while (DirtyDelegate.class.isAssignableFrom(clazz)) {
            //    clazz = clazz.getSuperclass();
            //}
            name = clazz.getCanonicalName();
            if (name == null)
                throw new GException("cannot getCannonicalName for : " + clazz);
        }
        canonicalNameCache.put(clazz, name);
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
            return className;
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
            if (!value.equals("null") && value != null) {
                value = value.split(" ")[0];
                field.setAccessible(true);
                try {
                    if (!keepInstances || o == null || isImmutable(o))
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
                        out.print(getCanonicalName(o.getClass()));
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
        public void deserializeArray(Object arr, MyBufferedReader in, boolean keepInstances) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                int depth = in.depth;
                String line = readLineOrEOF(in);
                if (line.equals("null")) {
                    Array.set(arr, i, null);
                    continue;
                }
                Object o = Array.get(arr, i);
                Reflector<?> a;
                if (!keepInstances || o == null || !(o instanceof Reflector) || ((Reflector) o).isImmutable()) {
                    a = (Reflector<?>) getClassForName(line).newInstance();
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
                    if (readLineOrEOF(in) != null)
                        throw new ParseException(in.lineNum, " expected closing '}'");
                }
            }
            if (readLineOrEOF(in) != null)
                throw new ParseException(in.lineNum, " expected closing '}'");
        }
    };

    private static Collection newCollectionInstance(String name) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        switch (name) {
            case "java.util.Collections.SynchronizedRandomAccessList":
                return Collections.synchronizedList(new ArrayList());
        }
        return (Collection)getClassForName(name).newInstance();
    }

    private static Archiver collectionArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Collection<?> c = (Collection<?>) field.get(a);
            String s = getCanonicalName(c.getClass());
            return s;
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
            if (value != null && !value.equals("null")) {
                if (!keepInstances || o == null)
                    field.set(a, newCollectionInstance(value));
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
                        out.print(getCanonicalName(c.getClass()));
                        serializeObject(c, out, true);
                    } else
                        out.println("null");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in, boolean keepInstances) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                String clazz = readLineOrEOF(in);
                Collection c = (Collection) Array.get(arr, i);
                if (!clazz.equals("null")) {
                    Class classNm = getClassForName(clazz);
                    if (!keepInstances || c == null || !c.getClass().equals(classNm)) {
                        Collection cc = (Collection<?>) classNm.newInstance();
                        if (c != null)
                            cc.addAll(c);
                        c = cc;
                    }
                    deserializeCollection(c, in, keepInstances);
                    Array.set(arr, i, c);
                } else {
                    Array.set(arr, i, null);
                }
            }
            if (readLineOrEOF(in) != null)
                throw new ParseException(in.lineNum, " expected closing '}'");
        }
    };

    private static Archiver mapArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Map<?, ?> m = (Map<?, ?>) field.get(a);
            String s = getCanonicalName(m.getClass());
            return s;
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
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
                        out.println(getCanonicalName(m.getClass()));
                        serializeObject(m, out, true);
                    } else
                        out.println("null");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in, boolean keepInstances) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                String clazz = readLineOrEOF(in);
                if (!clazz.equals("null")) {
                    Map<?, ?> m = (Map<?, ?>) getClassForName(clazz).newInstance();
                    deserializeMap(m, in, keepInstances);
                    Array.set(arr, i, m);
                }
            }
            if (readLineOrEOF(in) != null)
                throw new ParseException(in.lineNum, " expected closing '}'");
        }
    };

    private static Archiver dirtyArchiver = new Archiver() {
        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Object o = ((DirtyDelegate)field.get(a)).getValue();
            return o == null ? "null" : o.toString();
        }

        @Override
        public void set(Object o, Field field, String value, Reflector<?> a, boolean keepInstances) throws Exception {
            ((DirtyDelegate)field.get(a)).setValueFromString(value == null ? "" : value);
        }

        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            throw new GException("Not implemented");
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in, boolean keepInstances) throws Exception {
            throw new GException("Not implemented");
        }
    };

    private static Archiver arrayArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Object o = field.get(a);
            String s = getCanonicalName(field.getType().getComponentType()) + " " + Array.getLength(o);
            return s;
        }

        private Object createArray(Object current, String line, boolean keepInstances) throws Exception {
            String[] parts = line.split(" ");
            if (parts.length < 2)
                throw new GException("Invalid array description '" + line + "' excepted < 2 parts");
            final int len = Integer.parseInt(parts[1].trim());
            if (!keepInstances || current == null || Array.getLength(current) != len) {
                Class<?> clazz = getClassForName(parts[0].trim());
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
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    Archiver compArchiver = getArchiverForType(arr.getClass().getComponentType().getComponentType());
                    Object obj = Array.get(arr, i);
                    if (obj == null) {
                        out.println("null");
                    } else {
                        out.p(getCanonicalName(obj.getClass().getComponentType())).p(" ").p(Array.getLength(obj));
                        out.push();
                        compArchiver.serializeArray(Array.get(arr, i), out);
                        out.pop();
                    }
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in, boolean keepInstances) throws Exception {
            int len = Array.getLength(arr);
            for (int i = 0; i < len; i++) {
                Class cl = arr.getClass().getComponentType();
                if (cl.getComponentType() != null)
                    cl = cl.getComponentType();
                Archiver compArchiver = getArchiverForType(cl);
                String line = readLineOrEOF(in);
                if (line != null && !line.equals("null")) {
                    Object obj = Array.get(arr, i);
                    obj = createArray(obj, line, keepInstances);
                    Array.set(arr, i, obj);
                    compArchiver.deserializeArray(obj, in, keepInstances);
                }
            }
            if (readLineOrEOF(in) != null)
                throw new ParseException(in.lineNum, " expected closing '}'");

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

        classMap.put("java.util.Arrays.ArrayList", ArrayList.class);

        classMap.put("java.lang.String[]", String[].class);
        classMap.put("java.lang.String[][]", String[][].class);
        classMap.put("java.lang.String[][][]", String[][][].class);

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

        if (STRIP_PACKAGE_QUALIFIER) {
            classMap.put("String[]", String[].class);
            classMap.put("String[][]", String[][].class);
            classMap.put("String[][][]", String[][][].class);

            classMap.put("Boolean[]", Boolean[].class);
            classMap.put("Boolean[][]", Boolean[][].class);
            classMap.put("Boolean[][][]", Boolean[][][].class);

            classMap.put("Integer[]", Integer[].class);
            classMap.put("Integer[][]", Integer[][].class);
            classMap.put("Integer[][][]", Integer[][][].class);

            classMap.put("Float[]", Float[].class);
            classMap.put("Float[][]", Float[][].class);
            classMap.put("Float[][][]", Float[][][].class);

            classMap.put("Double[]", Double[].class);
            classMap.put("Double[][]", Double[][].class);
            classMap.put("Double[][][]", Double[][][].class);

            classMap.put("Long[]", Long[].class);
            classMap.put("Long[][]", Long[][].class);
            classMap.put("Long[][][]", Long[][][].class);

            classMap.put("Byte[]", Byte[].class);
            classMap.put("Byte[][]", Byte[][].class);
            classMap.put("Byte[][][]", Byte[][][].class);

        }

        registerClass(ArrayList.class);
    }

    /**
     * This method is usefull for inner classes
     *
     * @param clazz
     */
    public static void registerClass(Class<?> clazz, String ... alternateNames) {
        String sClazz = getCanonicalName(clazz);
        int lastDot = sClazz.lastIndexOf(".");
        if (STRIP_PACKAGE_QUALIFIER) {
            if (lastDot > 0) {
                String simpleName = sClazz.substring(lastDot + 1);
                if (simpleName.length() > 0 && !classMap.containsKey(simpleName)) {
                    classMap.put(simpleName, clazz);
                }
            }
            for (String alt : alternateNames) {
                if (classMap.containsKey(alt))
                    throw new GException("Alternate name " + alt + " is already mapped to: " + classMap.get(alt));
                classMap.put(alt, clazz);
            }
        } else {
        for (String alt : alternateNames) {
            String altName = sClazz.substring(0, lastDot+1)+alt;
            classMap.put(altName, clazz);
        }
        }
        addArrayTypes(clazz);
        classMap.put(sClazz, clazz);
    }

    private static Class<?> getClassForName(String forName) throws ClassNotFoundException {
        if (classMap.containsKey(forName))
            return classMap.get(forName);
        try {
            return Reflector.class.getClassLoader().loadClass(forName);
            /*
            Class<?> clazz =  Reflector.class.getClassLoader().loadClass(forName);
            classMap.put(forName, clazz);
            return clazz;*/
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

    static Map<Field, Archiver> getValues(Class<?> clazz, boolean createIfDNE) {
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
                    throw new GException("Synthetic and anonymous classes not supported");
                // test newInstance works for this clazz

                if (!Modifier.isAbstract(clazz.getModifiers())) {
                    if (clazz.isArray()) {
                        throw new GException("array?");
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
        registerClass(clazz);
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
        } else if (isSubclassOf(clazz, DirtyDelegate.class)) {
            return dirtyArchiver;
        } else {
            throw new GException("No reflector available for class: " + clazz);
        }
    }

    private static void addArrayTypes(Class<?> clazz) {
        if (clazz.isAnnotation())
            return;
        String nm = clazz.getName();
        if (classMap.containsKey(nm))
            return;
        if (nm.endsWith("$Companion"))
            return;
        if (nm.startsWith("cc.lib.utils.Reflector$"))
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
            String nm = clazz.getName();
            nm = Utils.chopEnd(nm, "$delegate");
            nm = nm.replace('$', '.');
            classMap.put(nm, clazz);
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
        registerClass(clazz);
        addArrayTypes(clazz);
        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (f.getName().endsWith("Companion"))
                    continue;
                if (Modifier.isStatic(f.getModifiers()))
                    continue;
                addField(clazz, f.getName());
            }
            for (Class e : clazz.getClasses()) {
                addArrayTypes(e); // add enclosed classes
            }
        } catch (GException e) {
            throw e;
        } catch (Exception e) {
            throw new GException("Failed to add all fields in " + clazz.getName(), e);
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
        if (obj == null) {
            _out.println("null");
            return;
        }
        if (obj.getClass().isArray()) {
            int num = Array.getLength(obj);
            _out.p(getCanonicalName(obj.getClass())).p(" ").p(num);
        } else {
            _out.print(getCanonicalName(obj.getClass()));
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
     * This version will derive the object type from the top level element.
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
            Object o = _deserializeObject(_in, false);
            return (T) o;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(_in.lineNum, e);
        }
    }

    public static void mergeObject(Object target, String in) throws IOException {
        mergeObject(target, new MyBufferedReader(new StringReader(in)));
    }

    public static <T> T mergeObject(T target, MyBufferedReader _in) throws IOException {
        try {
            if (target == null) {
                return deserializeObject(_in);
            } else if (target.getClass().isArray()) {
                deserializeArray(target, _in, true);
            } else {
                final int startDepth = _in.depth;
                String line = _in.readLine();
                if (line.equals("null")) {
                    return null;
                } else {
                    String[] parts = line.split(" ");
                    if (parts.length > 1) {
                        int num = Integer.parseInt(parts[1]);
                        Class<?> clazz = getClassForName(parts[0]);
                        if (target == null || Array.getLength(target) != num) {
                            //Class<?> clazz = getClassForName(parts[0]);
                            target = (T)Array.newInstance(clazz, num);
                        }
                        getArchiverForType(clazz).deserializeArray(target, _in, true);
                    } else {
                        Class<?> clazz;
                        if (target == null)
                            clazz = getClassForName(parts[0]);
                        else
                            clazz = target.getClass();
                        return (T)parse(target, clazz, _in, true);
                    }
                    if (_in.depth > startDepth)
                        if (readLineOrEOF(_in) != null)
                            throw new ParseException(_in.lineNum, " Expected closing '}'");
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
        return target;
    }


    public static <T> T mergeObject(BufferedReader in) throws IOException {
        MyBufferedReader _in;
        if (in instanceof MyBufferedReader)
            _in = (MyBufferedReader) in;
        else
            _in = new MyBufferedReader(in);
        try {
            Object o = _deserializeObject(_in, true);
            return (T) o;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static Object _deserializeObject(MyBufferedReader in, boolean keepInstances) throws Exception {
        String line = readLineOrEOF(in);
        if (line.equals("null"))
            return null;
        String[] parts = line.split(" ");
        if (parts.length < 1)
            throw new ParseException(in.lineNum, "Not of form <class> <len>? {");
        Class<?> clazz = getClassForName(parts[0]);
        if (parts.length > 1) {
            Archiver a = getArchiverForType(clazz.getComponentType());
            int len = Integer.parseInt(parts[1]);
            Object o = Array.newInstance(clazz.getComponentType(), len);
            a.deserializeArray(o, in, keepInstances);
            return o;
        }
        return parse(null, clazz, in, keepInstances);
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
        } else if (obj instanceof Collection) {
            Collection<?> c = (Collection<?>) obj;
            out.push();
            for (Object o : c) {
                if (o != null && o.getClass().isArray()) {
                    int len = Array.getLength(o);
                    out.p(o.getClass().getComponentType().getName()).p(" ").p(len);
                } else {
                    if (o == null) {
                        out.println("null");
                        continue;
                    }
                    out.p(getCanonicalName(o.getClass()));
                }
                serializeObject(o, out, true);
            }
            out.pop();
        } else if (obj instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) obj;
            out.push();
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                Object o = entry.getKey();
                out.print(getCanonicalName(o.getClass()));
                serializeObject(o, out, true);
                o = entry.getValue();
                if (o == null) {
                     out.println("null");
                } else {
                    out.print(getCanonicalName(o.getClass()));
                    serializeObject(o, out, true);
                }
            }
            out.pop();
        } else if (obj.getClass().isArray()) {
            Archiver compArchiver = getArchiverForType(obj.getClass().getComponentType());
            out.push();
            compArchiver.serializeArray(obj, out);
            out.pop();
        } else if (printObjects) {
            out.push();
            if (obj instanceof String) {
                out.p("\"").p(encodeString((String) obj)).println("\"");
            } else {
                out.println(obj);
            }
            out.pop();
        } else {
            out.println();
        }
    }

    static String encodeString(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8").replace("\n", "%0A").replace("\t", "%09");
    }

    static String decodeString(String in) throws Exception {
        return URLDecoder.decode(in, "UTF-8");
    }

    protected static String getName(Field f) {
        return Utils.chopEnd(f.getName(), "$delegate");
    }

    protected synchronized void serialize(MyPrintWriter out) throws IOException {
        try {
            Map<Field, Archiver> values = getValues(getClass(), false);
            for (Field field : values.keySet()) {
                Archiver archiver = values.get(field);
                field.setAccessible(true);
                Object obj = field.get(Reflector.this);
                if (obj == null) {
                    out.p(getName(field)).println("=null");
                    continue;
                }
                out.p(getName(field)).p("=").p(archiver.get(field, this));
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

    /**
     *
     * @param out
     * @throws IOException
     */
    public final void serialize(PrintWriter out) throws IOException {
        serialize(new MyPrintWriter(out));
    }

    private static String readLineAndClosedParen(MyBufferedReader in) throws IOException {
        String value = readLineOrEOF(in);
        String line = readLineOrEOF(in);
        if (line != null)
            throw new IOException("Expected closing paren } but found: " + line);
        return value;
    }

    private static Class<?> isEnum(Class<?> clazz) {
        if (clazz.isEnum())
            return clazz;
        clazz = clazz.getSuperclass();
        if (clazz != null && clazz.isEnum()) {
            return clazz;
        }
        return null;
    }

    private static Object parse(Object current, Class<?> clazz, MyBufferedReader in, boolean keepInstances) throws Exception {
        Class<?> enumClazz = isEnum(clazz);
        if (enumClazz != null)
            return findEnumEntry(enumClazz, readLineOrEOF(in));
        if (clazz.isArray()) {
            throw new ParseException(in.lineNum, "This method not to be called for array types");
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
            if (!keepInstances || current == null)
                a = (Reflector<?>) clazz.newInstance();
            else
                a = (Reflector) current;
            if (keepInstances)
                a.merge(in);
            else
                a.deserialize(in);
            return a;
        }
        if (isSubclassOf(clazz, Map.class)) {
            Map map = (Map) clazz.newInstance();
            deserializeMap(map, in, keepInstances);
            return map;
        }
        if (isSubclassOf(clazz, Collection.class)) {
            Collection c = (Collection)current;
            if (!keepInstances || current == null)
                c = (Collection) clazz.newInstance();
            deserializeCollection(c, in, keepInstances);
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
        throw new ParseException(in.lineNum, "Dont know how to parse class " + clazz);
    }

    private static boolean isImmutable(Object o) {
        if (o instanceof String || o instanceof Number)
            return true;
        if (o instanceof Reflector && ((Reflector) o).isImmutable())
            return true;
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void deserializeCollection(Collection c, MyBufferedReader in, boolean keepInstances) throws Exception {
        final int startDepth = in.depth;
        Iterator it = null;

        if (!keepInstances || c.size() == 0 || isImmutable(c.iterator().next()))
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
                    if (!keepInstances || entry == null || Array.getLength(entry) != num) {
                        //Class<?> clazz = getClassForName(parts[0]);
                        entry = Array.newInstance(clazz, num);
                    }
                    getArchiverForType(clazz).deserializeArray(entry, in, keepInstances);
                } else {
                    Class<?> clazz;
                    if (!keepInstances || entry == null)
                        clazz = getClassForName(parts[0]);
                    else
                        clazz = entry.getClass();
                    entry = parse(entry, clazz, in, keepInstances);
                }
                if (in.depth > startDepth)
                    if (readLineOrEOF(in) != null)
                        throw new ParseException(in.lineNum, " Expected closing '}'");
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
    private synchronized static void deserializeArray(Object array, MyBufferedReader in, boolean keepInstances) throws Exception {
        final int startDepth = in.depth;
        final int len = Array.getLength(array);
        for (int i=0;i<len;i++) {
            String line = readLineOrEOF(in);
            if (line == null)
                break;
            Object entry = null;
            if (!line.equals("null")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    int num = Integer.parseInt(parts[1]);
                    Class<?> clazz = getClassForName(parts[0]);
                    if (!keepInstances || entry == null || Array.getLength(entry) != num) {
                        //Class<?> clazz = getClassForName(parts[0]);
                        entry = Array.newInstance(clazz, num);
                    }
                    getArchiverForType(clazz).deserializeArray(entry, in, keepInstances);
                } else {
                    Class<?> clazz;
                    if (!keepInstances || entry == null)
                        clazz = getClassForName(parts[0]);
                    else
                        clazz = entry.getClass();
                    entry = parse(entry, clazz, in, keepInstances);
                }
                if (in.depth > startDepth)
                    if (readLineOrEOF(in) != null)
                        throw new ParseException(in.lineNum, " Expected closing '}'");
            }
            Array.set(array, i, entry);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void deserializeMap(Map c, MyBufferedReader in, boolean keepInstances) throws Exception {
        int startDepth = in.depth;
        while (true) {
            String line = readLineOrEOF(in);
            if (line == null || line.equals("null"))
                break;
            Class<?> clazz = getClassForName(line);
            Object key = parse(null, clazz, in, keepInstances);
            if (key == null)
                throw new ParseException(in.lineNum, "null key in map");
            if (in.depth > startDepth) {
                line = readLineOrEOF(in);
                if (line != null)
                    throw new ParseException(in.lineNum, "Expected closing }");
            }
            line = readLineOrEOF(in);
            if (line == null)
                throw new ParseException(in.lineNum, "Missing value from key/value pair in map");
            Object value = null;
            if (line != null && !line.equals("null")) {
                clazz = getClassForName(line);
                value = parse(null, clazz, in, keepInstances);
                if (in.depth > startDepth) {
                    line = readLineOrEOF(in);
                    if (line != null)
                        throw new ParseException(in.lineNum, "Expected closing }");
                }
            }
            c.put(key, value);
        }
    }

    /**
     *
     * @param text
     * @throws IOException
     */
    public synchronized final void deserialize(String text) throws IOException {

        MyBufferedReader reader = new MyBufferedReader(new StringReader(text));
        try {
            deserializeInternal(reader, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Same as deserialize with KEEP_INSTANCES enabled.
     *
     * @param diff
     * @throws Exception
     */
    public synchronized final void merge(String diff) throws Exception {
        merge(new MyBufferedReader(new StringReader(diff)));
    }

    /**
     *
     * @param in
     * @throws IOException
     */
    public synchronized final void deserialize(InputStream in) throws IOException {
        try (MyBufferedReader reader = new MyBufferedReader(new InputStreamReader(in))) {
            deserializeInternal(reader, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private boolean fieldMatches(Field field, String name) {
        if (getName(field).equals(name))
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
     * @param in
     * @throws Exception
     */
    public final void deserialize(Reader in) throws Exception {
        deserializeInternal(((MyBufferedReader)((in instanceof MyBufferedReader) ? in : new MyBufferedReader(in))), false);
    }

    /**
     * initialize fields of this object there are explicitly added by addField for this class type.
     *
     * @param in
     * @throws Exception
     */
    protected void deserialize(MyBufferedReader in) throws Exception {
        deserializeInternal(in, false);
    }

    public final synchronized void merge(InputStream in) throws Exception {
        merge(new MyBufferedReader(new InputStreamReader(in)));
    }

    public final synchronized void merge(BufferedReader in) throws Exception {
        merge((MyBufferedReader)((in instanceof MyBufferedReader) ? in : new MyBufferedReader(in)));
    }

    protected void merge(MyBufferedReader in) throws Exception {
        deserializeInternal(in, true);
    }

    private void deserializeInternal(MyBufferedReader in, boolean keepInstances) throws Exception {

        Map<Field, Archiver> values = getValues(getClass(), false);
        final int depth = in.depth;
            while (true) {
            if (in.depth > depth)
                if (in.readLine() != null)
                    throw new ParseException(in.lineNum, " Expected closing '}'");
            String line = readLineOrEOF(in);
                if (line == null)
                    break;
                String[] parts = line.split("=");
                if (parts.length < 2)
                    throw new ParseException(in.lineNum, " not of form 'name=value'");
                String name = parts[0].trim();
                for (Field field : values.keySet()) {
                    if (fieldMatches(field, name)) {
                        Archiver archiver = values.get(field);
                        Object instance = field.get(this);
                        archiver.set(instance, field, parts[1], this, keepInstances);
                        if (field.get(Reflector.this) instanceof Reflector) {
                            Reflector<T> ref = (Reflector<T>) field.get(Reflector.this);
                        if (keepInstances)
                            ref.merge(in);
                        else
                            ref.deserialize(in);
                        } else if (field.getType().isArray()) {
                            Object obj = field.get(this);
                            if (obj != null) {
                                Archiver arrayArchiver = getArchiverForType(obj.getClass().getComponentType());
                                arrayArchiver.deserializeArray(obj, in, keepInstances);
                            }
                        } else if (isSubclassOf(field.getType(), Collection.class)) {
                            Collection<?> collection = (Collection<?>) field.get(this);
                            if (collection != null)
                            deserializeCollection(collection, in, keepInstances);
                        } else if (isSubclassOf(field.getType(), Map.class)) {
                            Map<?, ?> map = (Map<?, ?>) field.get(this);
                            if (map != null)
                            deserializeMap(map, in, keepInstances);
                        }
                        parts = null;
                        break;
                    }
                }
                if (parts != null) {
                    if (THROW_ON_UNKNOWN)
                        throw new GException("Unknown field: " + name + " not in fields: " + values.keySet());
                    log.error("Unknown field: " + name + " not found in class: " + getClass());// + " not in fields: " + values.keySet());
                    // skip ahead until depth matches current depth
                while (in.depth > depth) {
                    readLineOrEOF(in);
                    }
                }
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

    static boolean isEqual(Object a, Object b) {
        if (a == b)
            return true;
        if (a == null || b == null)
            return false;
        if (!a.getClass().equals(b.getClass()))
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

    public String toStringNumbered() {
        StringWriter buf = new StringWriter();
        try {
            serialize(new MyPrintWriter(buf, true));
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
                Collection newCollection = newCollectionInstance(getCanonicalName(o.getClass()));
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
        loadFromFile(file, false);
    }

    /**
     * Convenience method
     *
     * @param file
     * @throws IOException
     */
    public synchronized void mergeFromFile(File file) throws IOException {
        loadFromFile(file, true);
    }

    /**
     *
     * @param file
     * @param keepInstances
     * @throws IOException
     */
    private void loadFromFile(File file, boolean keepInstances) throws IOException {

        log.debug("Loading from file %s", file.getAbsolutePath());
        try (InputStream in = new FileInputStream(file)) {
            deserializeInternal(new MyBufferedReader(new InputStreamReader(in)), keepInstances);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
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
            loadFromFile(file, false);
            return true;
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Return a string that represents the differences in other compared to this.
     * The resulting strings can be merged into source
     *
     * Example:
     *
     * Given 2 Reflector Objects: a, b;
     * String d = a.diff(b)
     * a.merge(b)
     * assertEquals(a,b)
     *
     *
     * <p>
     * Purpose is to allow for situations where we want to transmit changes in the objects instead of whole object
     *
     * @param other
     * @return
     * @throws Exception
     */
    public final synchronized String diff(T other) {
        if (other == null)
            throw new NullPointerException("Reflector.diff - other cannot be null");
        if (!isSubclassOf(getClass(), other.getClass()))
            throw new IllegalArgumentException("Reflector.diff - other type '" + other.getClass() + " must be same or supertype of '" + getClass() + "'");

        StringWriter out = new StringWriter();
        try (MyPrintWriter writer = new MyPrintWriter(out)) {
            diff(other, writer);
            writer.flush();
        } catch (Exception e) {
            throw new GException(e);
        }
        return out.getBuffer().toString();
    }

    protected void diff(T other, MyPrintWriter out) throws Exception {
        Map<Field, Archiver> values = null;
        if (isSubclassOf(other.getClass(), getClass()))
            values = getValues(other.getClass(), false);
        else if (isSubclassOf(getClass(), other.getClass()))
            values = getValues(getClass(), false);
        else
            throw new ParseException(out.lineNum, "Classes " + getClass() + " and " + other.getClass() + " are not related");

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
                out.print(getName(field) + "=" + archiver.get(field, (Reflector<T>)other));
                serializeObject(thrs, out, false);
            } else if (thrs == null) {
                out.p(getName(field)).println("=null");
            } else if (isSubclassOf(mine.getClass(), thrs.getClass())) {
                out.p(getName(field)).p("=");
                if (mine instanceof Reflector) {
                    out.p(getCanonicalName(thrs.getClass()));
                    out.push();
                    ((Reflector) mine).diff((Reflector) thrs, out);
                    out.pop();
                } else if (mine instanceof Collection) {
                    out.p(getCanonicalName(thrs.getClass()));
                    out.push();
                    diffCollections((Collection) mine, (Collection) thrs, out);
                    out.pop();
                } else if (mine instanceof Map) {
                    out.p(getCanonicalName(thrs.getClass()));
                    out.push();
                    diffMaps(getName(field), (Map) mine, (Map) thrs, out);
                    out.pop();
                } else if (mine.getClass().isArray()) {
                    serializeObject(thrs, out);//diffArrays(mine, thrs, out);
                } else {
                    String themStr = archiver.get(field, (Reflector<T>)other);
                    out.println(themStr);
                }
            } else {
                throw new GException("Cannot diff object that are not related");
            }
        }
    }

    private void diffMaps(String name, Map mine, Map thrs, MyPrintWriter writer) throws Exception {
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
    }

    public static String diffObjects(Object o1, Object o2) throws Exception {
        try (StringWriter out = new StringWriter(128)) {
            diffObjects(o1, o2, new MyPrintWriter(out));
            return out.getBuffer().toString();
        }
    }

    static void diffObjects(Object o1, Object o2, MyPrintWriter writer) throws Exception {
        if (o2 == null) {
            writer.println("null");
        } else if (o1 == null) {
            serializeObject(o2, writer);
        } else if (o2.getClass().isArray()) {
            diffArrays(o1, o2, writer);
        } else if (o2 instanceof Reflector) {
            writer.print(getCanonicalName(o2.getClass()));
            writer.push();
            if (((Reflector) o2).isImmutable()) {
                ((Reflector) o2).serialize(writer);
            } else {
                ((Reflector) o1).diff((Reflector) o2, writer);
            }
            writer.pop();
        } else {
            serializeObject(o2, writer);
        }
    }

    private void diffCollections(Collection mine, Collection thrs, MyPrintWriter writer) throws Exception {
        Iterator i1 = mine.iterator();
        Iterator i2 = thrs.iterator();

        while (i2.hasNext() || i2.hasNext()) {
            Object o1 = i1.hasNext() ? i1.next() : null;
            Object o2 = i2.hasNext() ? i2.next() : null;

            diffObjects(o1, o2, writer);
        }
    }

    private static final void diffArrays(Object array1, Object array2, MyPrintWriter writer) throws Exception {
        if (isArraysEqual(array1, array2))
            return;
        int len1 = Array.getLength(array1);
        int len2 = Array.getLength(array2);
        writer.p(array2.getClass().getComponentType().getName().replace('$', '.')).p(" ").p(len2);
        writer.push();
        for (int i = 0; i < len2; i++) {
            Object o1 = i < len1 ? Array.get(array1, i) : null;
            Object o2 = Array.get(array2, i);
            if (o2 == null) {
                writer.println("null");
            } else if (o2 instanceof Reflector) {
                writer.print(getCanonicalName(o2.getClass()));
                if (o1 == null)
                    serializeObject(o2, writer, true);
                else {
                    writer.push();
                    ((Reflector) o1).diff((Reflector) o2, writer);
                    writer.pop();
                }
            } else {
                Archiver a = getArchiverForType(o2.getClass());
                a.serializeArray(array2, writer);
                break;
            }
        }
        writer.pop();
    }

    /**
     * Override this for some classes that should be considered immutable
     *
     * @return
     */
    protected boolean isImmutable() {
        return false;
    }

    /**
     *
     * @param resetDirtyFlag
     * @return
     */
    public boolean isDirty(boolean resetDirtyFlag) {
        boolean dirty = false;
        Map<Field, Archiver> fields = getValues(getClass(), false);
        for (Map.Entry<Field, Archiver> e : fields.entrySet()) {
            try {
                Object o = e.getKey().get(this);
                if (o instanceof Reflector<?>) {
                    if (((Reflector<?>) o).isDirty(resetDirtyFlag)) {
                        dirty = true;
                        if (!resetDirtyFlag)
                            break;
                    }
                }
            } catch (Exception ex) {
                throw new GException(ex);
            }
        }
        return dirty;
    }

    public void serializeDirty(MyPrintWriter out) throws IOException {
        try {
            Map<Field, Archiver> values = getValues(getClass(), false);
            for (Field field : values.keySet()) {
                field.setAccessible(true);
                Object obj = field.get(Reflector.this);
                if (obj instanceof DirtyReflector) {
                    ((DirtyReflector) obj).serializeDirty(out);
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (Profiler.ENABLED) Profiler.pop("Reflector.serialize");
        }
    }

    public final void serializeDirty(OutputStream out) throws IOException {
        serializeDirty(new MyPrintWriter(out));
    }


    public final String serializeDirtyToString() throws IOException {
        StringWriter buf = new StringWriter();
        try (MyPrintWriter out = new MyPrintWriter(buf)) {
            serializeDirty(out);
        }
        return buf.toString();
    }

    /**
     * CRC32 Checksum
     * @return
     */
    public final long getChecksum() {
        Checksum crc32 = new CRC32();
        try {
            serialize(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    crc32.update(b);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return crc32.getValue();
    }

    public static void dump() {
        log.info("classMap=" + classMap.toString().replace(',', '\n'));
        log.info("classValues=" + classValues.toString().replace(',', '\n'));
        log.info("canonicalNameCache=" + canonicalNameCache.toString().replace(',', '\n'));

    }
}
