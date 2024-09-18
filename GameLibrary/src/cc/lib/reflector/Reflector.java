package cc.lib.reflector;

import java.io.BufferedReader;
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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.GException;

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
public class Reflector<T> implements IDirty {

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

    private final static Logger log = LoggerFactory.getLogger(Reflector.class);

    private final static Map<Class<?>, Map<Field, Archiver>> classValues = new HashMap<>();
    private final static Map<String, Class<?>> classMap = new HashMap<>();
    private final static Map<Class, Map<Class, Boolean>> subclassOfCache = new HashMap<>();

    static Enum<?> findEnumEntry(Class<?> enumClass, String value) throws Exception {
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

    private final static Map<Class, String> canonicalNameCache = new HashMap<>();

    static String getCanonicalName(Class clazz) {
        String name = getCanonicalNameOrNull(clazz);
        if (name == null)
            throw new GException("cannot getCannonicalName for : " + clazz);
        return name;
    }

    private static String getCanonicalNameOrNull(Class clazz) {
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
            boolean isAnonymous = clazz.isAnonymousClass();
            boolean isInterface = clazz.isInterface();
            boolean isLocal = clazz.isLocalClass();
            boolean isMember = clazz.isMemberClass();
            boolean isPrimitive = clazz.isPrimitive();
            boolean isSynthetic = clazz.isSynthetic();
            Class<?> enclosingClass = clazz.getEnclosingClass();
            Class<?> declatingClass = clazz.getDeclaringClass();
            Class<?> superClass = clazz.getSuperclass();

            if (isAnonymous || (superClass != null && superClass.isEnum())) {
                clazz = superClass;
            }
            //while (DirtyDelegate.class.isAssignableFrom(clazz)) {
            //    clazz = clazz.getSuperclass();
            //}
            name = clazz.getCanonicalName();
            if (name == null)
                return null;
        }
        canonicalNameCache.put(clazz, name);
        return name;
    }

    static Collection newCollectionInstance(String name) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        switch (name) {
            case "java.util.Collections.SynchronizedRandomAccessList":
                return Collections.synchronizedList(new ArrayList());
            case "java.util.Collections.SingletonList":
                return new ArrayList();
        }
        return (Collection) getClassForName(name).newInstance();
    }

    static Map newMapInstance(String name) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        switch (name) {
            case "java.util.Collections.SynchronizedMap":
                return Collections.synchronizedMap(new HashMap());
        }
        return (Map) getClassForName(name).newInstance();
    }

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
        classMap.put("java.util.Collections.SingletonList", ArrayList.class);

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

    static Class<?> getClassForName(String forName) throws ClassNotFoundException {
        if (classMap.containsKey(forName))
            return classMap.get(forName);
        //return Reflector.class.getClassLoader().loadClass(forName);
        Class<?> clazz = Reflector.class.getClassLoader().loadClass(forName);
        classMap.put(forName, clazz);
        return clazz;
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
            if (getCanonicalNameOrNull(clazz) == null) {
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

    static Archiver getArchiverForType(Class<?> clazz) {
        registerClass(clazz);
        if (clazz.equals(Byte.class) || clazz.equals(byte.class)) {
            return Archivers.byteArchiver;
        } else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            return Archivers.booleanArchiver;
        } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            return Archivers.longArchiver;
        } else if (clazz.equals(Double.class) || clazz.equals(double.class)) {
            return Archivers.doubleArchiver;
        } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            return Archivers.integerArchiver;
        } else if (clazz.equals(Float.class) || clazz.equals(float.class)) {
            return Archivers.floatArchiver;
        } else if (clazz.equals(String.class)) {
            return Archivers.stringArchiver;
        } else if (clazz.isEnum() || isSubclassOf(clazz, Enum.class)) {
            addArrayTypes(clazz);
            return Archivers.enumArchiver;
        } else if (isSubclassOf(clazz, Reflector.class)) {
            return Archivers.archivableArchiver;
        } else if (isSubclassOf(clazz, Collection.class)) {
            return Archivers.collectionArchiver;
        } else if (isSubclassOf(clazz, Map.class)) {
            return Archivers.mapArchiver;
        } else if (clazz.isArray()) {
            // add enums if this is an enum
            addArrayTypes(clazz);
            return Archivers.arrayArchiver;
        } else if (isSubclassOf(clazz, DirtyDelegate.class)) {
            return Archivers.dirtyArchiver;
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
        serializeObject(obj, new RPrintWriter(out));
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
        RPrintWriter _out;
        if (out instanceof RPrintWriter)
            _out = (RPrintWriter) out;
        else
            _out = new RPrintWriter(out);
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
            throw new ParseException(_out.lineNum, e);
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
        RBufferedReader reader = null;
        if (file.exists()) {
            reader = new RBufferedReader(new InputStreamReader(new FileInputStream(file)));
        } else {
            InputStream in = Reflector.class.getClassLoader().getResourceAsStream(file.getName());
            if (in == null) {
                throw new FileNotFoundException(file.getAbsolutePath());
            }
            reader = new RBufferedReader(new InputStreamReader(in));
        }
        try {
            return (T) _deserializeObject(reader, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(reader.lineNum, e);
        } finally {
            reader.close();
        }
    }

    /**
     * @param o
     * @param file
     * @throws IOException
     */
    public static <T> void serializeToFile(Object o, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            serializeObject(o, new RPrintWriter(out));
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
        RBufferedReader in = new RBufferedReader(new StringReader(str));
        try {
            return (T) _deserializeObject(in, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(in.lineNum, e);
        } finally {
            in.close();
        }
    }

    /**
     * @param _in
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T> T deserializeFromInputStream(InputStream _in) throws IOException {
        RBufferedReader in = new RBufferedReader(new InputStreamReader(_in));
        try {
            return (T) _deserializeObject(in, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(in.lineNum, e);
        } finally {
            in.close();
        }
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
        RBufferedReader _in;
        if (in instanceof RBufferedReader)
            _in = (RBufferedReader) in;
        else
            _in = new RBufferedReader(in);
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
        mergeObject(target, new RBufferedReader(new StringReader(in)));
    }

    public static <T> T mergeObject(T target, RBufferedReader _in) throws IOException {
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
                        if (_in.readLineOrEOF() != null)
                            throw new ParseException(_in.lineNum, " Expected closing '}'");
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(_in.lineNum, e);
        }
        return target;
    }


    public static <T> T mergeObject(BufferedReader in) throws IOException {
        RBufferedReader _in;
        if (in instanceof RBufferedReader)
            _in = (RBufferedReader) in;
        else
            _in = new RBufferedReader(in);
        try {
            Object o = _deserializeObject(_in, true);
            return (T) o;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(_in.lineNum, e);
        } finally {
            _in.close();
        }
    }

    private static Object _deserializeObject(RBufferedReader in, boolean keepInstances) throws Exception {
        String line = in.readLineOrEOF();
        if (line == null || line.equals("null"))
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

    static void serializeCollection(Collection<?> c, RPrintWriter out) throws IOException {
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
    }

    static void serializeMap(Map<?, ?> m, RPrintWriter out) throws IOException {
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
    }

    /**
     * @param obj
     * @param out
     * @param printObjects
     * @throws Exception
     */
    static void serializeObject(Object obj, RPrintWriter out, boolean printObjects) throws IOException {
        if (obj == null) {
            out.println("null");
        } else if (obj instanceof DirtyDelegate<?>) {
            ((DirtyDelegate<?>) obj).serialize(out, printObjects);
        } else if (obj instanceof Reflector) {
            out.push();
            ((Reflector<?>) obj).serialize(out);
            out.pop();
        } else if (obj instanceof Collection) {
            Collection<?> c = (Collection<?>) obj;
            serializeCollection(c, out);
        } else if (obj instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) obj;
            serializeMap(m, out);
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

    static String encodeString(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8").replace("\n", "%0A").replace("\t", "%09");
    }

    static String decodeString(String in) throws IOException {
        return URLDecoder.decode(in, "UTF-8");
    }

    protected static String getName(Field f) {
        return Utils.chopEnd(f.getName(), "$delegate");
    }

    protected synchronized void serialize(RPrintWriter out) throws IOException {
        try {
            Map<Field, Archiver> values = getValues(getClass(), false);
            for (Field field : values.keySet()) {
                Archiver archiver = values.get(field);
                field.setAccessible(true);
                Object obj = field.get(Reflector.this);
                String name = getName(field);
                if (obj == null) {
                    out.writeNull(name);
                    continue;
                }
                out.p(name).p("=").p(archiver.get(field, this));
                serializeObject(obj, out, false);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(out.lineNum, e);
        }
    }

    /**
     * Override this to do extra handling. Derived should call super.
     *
     * @param out
     * @throws IOException
     */
    public final void serialize(OutputStream out) throws IOException {
        serialize(new RPrintWriter(out));
    }

    /**
     *
     * @param out
     * @throws IOException
     */
    public final void serialize(PrintWriter out) throws IOException {
        serialize(new RPrintWriter(out, false, false));
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

    protected static <T> T parse(Class<T> clazz, RBufferedReader in) throws IOException {
        return (T) parse(null, clazz, in, false);
    }

    private static Object parse(Object current, Class<?> clazz, RBufferedReader in, boolean keepInstances) throws IOException {
        try {
            Class<?> enumClazz = isEnum(clazz);
            if (enumClazz != null)
                return findEnumEntry(enumClazz, in.readLineOrEOF());
            if (clazz.isArray()) {
                throw new ParseException(in.lineNum, "This method not to be called for array types");
            }
            if (isSubclassOf(clazz, Integer.class)) {
                return Integer.parseInt(in.readLineAndClosedParen());
            }
            if (isSubclassOf(clazz, Float.class)) {
                return Float.parseFloat(in.readLineAndClosedParen());
            }
            if (isSubclassOf(clazz, Long.class)) {
                return Long.parseLong(in.readLineAndClosedParen());
            }
            if (isSubclassOf(clazz, Double.class)) {
                return Double.parseDouble(in.readLineAndClosedParen());
            }
            if (isSubclassOf(clazz, Boolean.class)) {
                return Boolean.parseBoolean(in.readLineAndClosedParen());
            }
            if (isSubclassOf(clazz, Character.class)) {
                return new Character(in.readLineAndClosedParen().trim().charAt(0));
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
                Collection c = (Collection) current;
                if (!keepInstances || current == null)
                    c = (Collection) clazz.newInstance();
                deserializeCollection(c, in, keepInstances);
                return c;
            }
            if (isSubclassOf(clazz, String.class)) {
                String sin = in.readLineAndClosedParen();
                if (sin == null)
                    return null;
                return decodeString(sin.substring(1, sin.length() - 1));
            }
            // try to create from a string constructor
            Constructor<?> cons = clazz.getConstructor(String.class);
            String arg = in.readLineOrEOF();
            return cons.newInstance(arg);
        } catch (Exception e) {
            throw new ParseException(in.lineNum, e);
        }
    }

    static boolean isImmutable(Object o) {
        if (o instanceof String || o instanceof Number)
            return true;
        if (o instanceof Reflector && ((Reflector) o).isImmutable())
            return true;
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void deserializeCollection(Collection c, RBufferedReader in, boolean keepInstances) throws IOException {
        final int startDepth = in.depth;
        Iterator it = null;

        if (!keepInstances || c.size() == 0 || isImmutable(c.iterator().next()))
            c.clear();
        else {
            it = c.iterator();
        }
        while (true) {
            try {
                String line = in.readLineOrEOF();
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
                        if (in.readLineOrEOF() != null)
                            throw new ParseException(in.lineNum, " Expected closing '}'");
                }
                if (doAdd)
                    c.add(entry);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new ParseException(in.lineNum, e);
            }
        }
        while (it != null && it.hasNext()) {
            it.next();
            it.remove(); // remove any remaining in the collection
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private synchronized static void deserializeArray(Object array, RBufferedReader in, boolean keepInstances) throws IOException {
        final int startDepth = in.depth;
        final int len = Array.getLength(array);
        for (int i = 0; i < len; i++) {
            try {
                String line = in.readLineOrEOF();
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
                        if (in.readLineOrEOF() != null)
                            throw new ParseException(in.lineNum, " Expected closing '}'");
                }
                Array.set(array, i, entry);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new ParseException(in.lineNum, e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void deserializeMap(Map c, RBufferedReader in, boolean keepInstances) throws IOException {
        int startDepth = in.depth;
        while (true) {
            String line = in.readLineOrEOF();
            if (line == null || line.equals("null"))
                break;
            try {
                Class<?> clazz = getClassForName(line);
                Object key = parse(null, clazz, in, keepInstances);
                if (key == null)
                    throw new ParseException(in.lineNum, "null key in map");
                if (in.depth > startDepth) {
                    line = in.readLineOrEOF();
                    if (line != null)
                        throw new ParseException(in.lineNum, "Expected closing }");
                }
                line = in.readLineOrEOF();
                if (line == null)
                    throw new ParseException(in.lineNum, "Missing value from key/value pair in map");
                Object value = null;
                if (!line.equals("null")) {
                    clazz = getClassForName(line);
                    value = parse(c.get(key), clazz, in, keepInstances);
                    if (in.depth > startDepth) {
                        line = in.readLineOrEOF();
                        if (line != null)
                            throw new ParseException(in.lineNum, "Expected closing }");
                    }
                }
                c.put(key, value);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new ParseException(in.lineNum, e);
            }
        }
    }

    /**
     * @param text
     * @throws IOException
     */
    public final void deserialize(String text) throws IOException {

        RBufferedReader reader = new RBufferedReader(new StringReader(text));
        try {
            deserializeInternal(reader, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(reader.lineNum, e);
        } finally {
            reader.close();
        }
    }

    /**
     * Same as deserialize with KEEP_INSTANCES enabled.
     *
     * @param diff
     * @throws IOException
     */
    public synchronized final void merge(String diff) throws IOException {
        merge(new RBufferedReader(new StringReader(diff)));
    }

    /**
     *
     * @param in
     * @throws IOException
     */
    public final void deserialize(InputStream in) throws IOException {
        RBufferedReader reader = new RBufferedReader(new InputStreamReader(in));
        try {
            deserializeInternal(reader, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(reader.lineNum, e);
        } finally {
            reader.close();
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
     * @param _in
     * @throws IOException
     */
    public final void deserialize(Reader _in) throws IOException {
        RBufferedReader in = null;
        if (_in instanceof RBufferedReader) {
            in = (RBufferedReader) _in;
        } else {
            in = new RBufferedReader(_in);
        }
        try {
            deserializeInternal(in, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(in.lineNum, e);
        } finally {
            in.close();
        }

    }

    /**
     * initialize fields of this object there are explicitly added by addField for this class type.
     *
     * @param in
     * @throws IOException
     */
    protected void deserialize(RBufferedReader in) throws IOException {
        try {
            deserializeInternal(in, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(in.lineNum, e);
        }
    }

    public final synchronized void merge(InputStream in) throws IOException {
        merge(new RBufferedReader(new InputStreamReader(in)));
    }

    public final synchronized void merge(BufferedReader in) throws IOException {
        merge((RBufferedReader) ((in instanceof RBufferedReader) ? in : new RBufferedReader(in)));
    }

    protected void merge(RBufferedReader in) throws IOException {
        try {
            deserializeInternal(in, true);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(in.lineNum, e);
        }
    }

    private synchronized void deserializeInternal(RBufferedReader in, boolean keepInstances) throws Exception {

        Map<Field, Archiver> values = getValues(getClass(), false);
        final int depth = in.depth;
        while (true) {
            if (in.depth > depth)
                if (in.readLine() != null)
                    throw new ParseException(in.lineNum, " Expected closing '}'");
            String line = in.readLineOrEOF();
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
                    Object obj = field.get(this);
                    if (obj instanceof Reflector) {
                        Reflector<T> ref = (Reflector<T>) obj;
                        if (keepInstances)
                            ref.merge(in);
                        else
                            ref.deserialize(in);
                    } else if (field.get(Reflector.this) instanceof DirtyDelegate<?>) {
                        ((DirtyDelegate<?>) obj).deserialize(in, keepInstances);
                    } else if (field.getType().isArray()) {
                        if (obj != null) {
                            Archiver arrayArchiver = getArchiverForType(obj.getClass().getComponentType());
                            arrayArchiver.deserializeArray(obj, in, keepInstances);
                        }
                    } else if (isSubclassOf(field.getType(), Collection.class)) {
                        Collection<?> collection = (Collection<?>) obj;
                        if (collection != null)
                            deserializeCollection(collection, in, keepInstances);
                    } else if (isSubclassOf(field.getType(), Map.class)) {
                        Map<?, ?> map = (Map<?, ?>) obj;
                        if (map != null)
                            deserializeMap(map, in, keepInstances);
                    }
                    parts = null;
                    break;
                }
            }
            if (parts != null) {
                parseUnknownField(name, parts[1], in);
                // skip ahead until depth matches current depth
                while (in.depth > depth) {
                    in.readLineOrEOF();
                }
            }
        }
    }

    protected void parseUnknownField(String name, String value, RBufferedReader in) throws Exception {
        if (THROW_ON_UNKNOWN)
            throw new ParseException(in.lineNum, "Unknown field: " + name + " not in fields: " + getValues(getClass(), false).keySet());
        log.error("Unknown field: " + name + " not found in class: " + getClass());// + " not in fields: " + values.keySet());
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
        try (RPrintWriter out = new RPrintWriter(buf, false, false)) {
            serialize(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buf.toString();
    }

    public String toStringNumbered() {
        StringWriter buf = new StringWriter();
        try (RPrintWriter in = new RPrintWriter(buf, true, false)) {
            serialize(in);
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
                Map newMap = newMapInstance(getCanonicalName(o.getClass()));
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
        RBufferedReader in = new RBufferedReader(new InputStreamReader(new FileInputStream(file)));
        try {
            if (keepInstances) {
                merge(in);
            } else {
                deserialize(in);
            }
        } finally {
            in.close();
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
     * Override this for some classes that should be considered immutable
     *
     * @return
     */
    protected boolean isImmutable() {
        return false;
    }

    /**
     * @param
     * @return
     */
    public boolean isDirty() {
        return false;
    }

    @Override
    public void markClean() {
    }

    public void serializeDirty(RPrintWriter out) throws IOException {
        serialize(out);
    }

    public final void serializeDirty(OutputStream out) throws IOException {
        serialize(new RPrintWriter(out));
    }


    public final String serializeDirtyToString() throws IOException {
        StringWriter buf = new StringWriter();
        try (RPrintWriter out = new RPrintWriter(buf)) {
            serializeDirty(out);
        }
        return buf.toString();
    }


    public final String serializeToString() throws IOException {
        StringWriter buf = new StringWriter();
        try (RPrintWriter out = new RPrintWriter(buf)) {
            serialize(out);
        }
        return buf.toString();
    }

    /**
     * CRC32 Checksum
     *
     * @return
     */
    public final long getChecksum() {
        Checksum crc32 = new CRC32();
        try {
            serialize(new OutputStream() {
                @Override
                public void write(int b) {
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

    public static Method searchMethods(Object execObj, String method, Class[] types, Object[] params) throws Exception {
        Class clazz = execObj.getClass();
        while (clazz != null && !clazz.equals(Object.class)) {
            for (Method m : clazz.getDeclaredMethods()) {
                m.setAccessible(true);
                if (!m.getName().equals(method))
                    continue;
                log.debug("testMethod:" + m.getName() + " with params:" + Arrays.toString(m.getParameterTypes()));
                Class[] paramTypes = m.getParameterTypes();
                if (paramTypes.length != types.length)
                    continue;
                boolean matchFound = true;
                for (int i = 0; i < paramTypes.length; i++) {
                    if (params[i] == null)
                        continue;
                    if (!isCompatiblePrimitives(paramTypes[i], types[i]) && !Reflector.isSubclassOf(types[i], paramTypes[i])) {
                        matchFound = false;
                        break;
                    }
                }
                if (matchFound)
                    return m;
            }
            clazz = clazz.getSuperclass();
        }
        throw new Exception("Failed to match method '" + method + "' types: " + Arrays.toString(types));
    }

    private final static Map<Class, List> primitiveCompatibilityMap = new HashMap<>();

    static {
        List c;
        primitiveCompatibilityMap.put(boolean.class, c = Arrays.asList(boolean.class, Boolean.class));
        primitiveCompatibilityMap.put(Boolean.class, c);
        primitiveCompatibilityMap.put(byte.class, c = Arrays.asList(byte.class, Byte.class));
        primitiveCompatibilityMap.put(Byte.class, c);
        primitiveCompatibilityMap.put(int.class, c = Arrays.asList(int.class, Integer.class, byte.class, Byte.class));
        primitiveCompatibilityMap.put(Integer.class, c);
        primitiveCompatibilityMap.put(long.class, c = Arrays.asList(int.class, Integer.class, byte.class, Byte.class, long.class, Long.class));
        primitiveCompatibilityMap.put(Long.class, c);
        primitiveCompatibilityMap.put(float.class, c = Arrays.asList(int.class, Integer.class, byte.class, Byte.class, float.class, Float.class));
        primitiveCompatibilityMap.put(Float.class, c);
        primitiveCompatibilityMap.put(double.class, c = Arrays.asList(int.class, Integer.class, byte.class, Byte.class, float.class, Float.class, double.class, Double.class, long.class, Long.class));
        primitiveCompatibilityMap.put(Double.class, c);

    }

    private static boolean isCompatiblePrimitives(Class a, Class b) {
        if (primitiveCompatibilityMap.containsKey(a))
            return b == null || primitiveCompatibilityMap.get(a).contains(b);
        return false;
    }
}
