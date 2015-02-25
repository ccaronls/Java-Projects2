package cc.lib.utils;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Derive from this class to handle copying, equals, serializing, etc.
 * 
 * Serialization of primitives, arrays and subclasses Archivable are supported.
 * Also collections are supported if their data types are one of the afore mentioned
 * New types can be added if they implement an Archiver.
 * 
 * NOTE: Derived classes must support zero argument constructor
 * @author ccaron
 *
 */
public class Archivable {

    private static final String INDENT = "   ";
    
    public interface Archiver {
        String get(Field field, Archivable a) throws Exception;
        void set(Field field, String value, Archivable a) throws Exception;
        void serializeArray(Object arr, PrintWriter out, String indent) throws Exception;
        void deserializeArray(Object arr, BufferedReader in) throws Exception;
    };
    
    private static String readLineOrEOF(BufferedReader in) throws IOException {
        while (true) {
            String line = in.readLine();
            if (line == null)
                throw new EOFException();
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#"))
                continue;
            return line;
        }
    }
    
    private static abstract class AArchiver implements Archiver {
        
        abstract Object parse(String value) throws Exception;
        
        @Override
        public String get(Field field, Archivable a) throws Exception {
            return String.valueOf(field.get(a));
        }

        @Override
        public void set(Field field, String value, Archivable a) throws Exception {
            if (value == null || value.equals("null"))
                field.set(a, null);
            else
                field.set(a, parse(value));
        }
        
        @Override
        public void serializeArray(Object arr, PrintWriter out, String indent) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                StringBuffer buf = new StringBuffer();
                buf.append(indent).append(INDENT);
                for (int i=0; i<len; i++) {
                    buf.append(Array.get(arr, i)).append(" ");
                }
                out.println(buf.toString());
            }
        }

        @Override
        public void deserializeArray(Object arr, BufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                while (true) {
                    String line = readLineOrEOF(in);
                    String [] parts = line.split(" ");
                    if (parts.length != len)
                        throw new Exception("Expected " + len + " parts but found " + parts.length);
                    for (int i=0; i<len; i++) {
                        Array.set(arr, i, parse(parts[i]));
                    }
                    break;
                }
            }
        }        
    }

    
    
    private static Archiver stringArchiver = new Archiver() {
        
        @Override
        public String get(Field field, Archivable a)  throws Exception{
            Object s = field.get(a);
            if (s == null)
                return "null";
            return "\"" + (String)s + "\"";
        }

        @Override
        public void set(Field field, String value, Archivable a)  throws Exception{
            if (value == null || value.equals("null"))
                field.set(a, null);
            else {
                field.set(a, value.substring(1, value.length()-1));
            }
        }

        
        @Override
        public void serializeArray(Object arr, PrintWriter out, String indent) throws Exception {
            int num = Array.getLength(arr);
            if (num > 0) {
                StringBuffer buf = new StringBuffer();
                for (int i=0; i<num; i++) {
                    Object entry = Array.get(arr, i);
                    if (entry == null)
                        buf.append(indent).append(INDENT).append("null\n");
                    else
                        buf.append(indent).append(INDENT).append("\"").append(entry).append("\"\n");
                }
                out.print(buf.toString());
            }
        }

        @Override
        public void deserializeArray(Object arr, BufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i=0; i<len; i++) {
                String line = readLineOrEOF(in);
                if (!line.equals("null"))
                    Array.set(arr, i, line.substring(1, line.length()-1));
            }
                
        }
    };

    private static Archiver integerArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return Integer.parseInt(s);
        }
        
    };

    private static Archiver longArchiver = new AArchiver() {

        @Override
        Object parse(String s) {
            return Long.parseLong(s);
        }
        
    };
    
    private static Archiver floatArchiver = new AArchiver() {

        @Override
        Object parse(String s) {
            return Float.parseFloat(s);
        }
        
    };    
    
    private static Archiver doubleArchiver = new AArchiver() {

        @Override
        Object parse(String s) {
            return Double.parseDouble(s);
        }
    
    };

    private static Archiver booleanArchiver = new AArchiver() {

        @Override
        Object parse(String s) {
            return Boolean.parseBoolean(s);
        }
        
    };
    
    private static Enum<?> findEnumEntry(Class<?> enumClass, String value) throws Exception {
        if (value == null || value.equals("null"))
            return null;
        @SuppressWarnings("unchecked")
        Enum<?> [] constants = ((Class<? extends Enum<?>>)enumClass).getEnumConstants();
        for (Enum<?> e : constants) {
            if (e.name().equals(value)) {
                return e;
            }
        }
        throw new Exception("Failed to find enum value: '" + value + "' in available constants: " + Arrays.asList(constants));
    }
    
    private static Archiver enumArchiver = new Archiver() {
        @Override
        public String get(Field field, Archivable a) throws Exception {
            return ((Enum<?>)field.get(a)).name();
        }

        @Override
        public void set(Field field, String value, Archivable a) throws Exception {
            field.set(a, findEnumEntry(field.getType(), value));
        }
        
        @Override
        public void serializeArray(Object arr, PrintWriter out, String indent) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                StringBuffer buf = new StringBuffer();
                buf.append(indent).append(INDENT);
                for (int i=0; i<len; i++) {
                    Object o = Array.get(arr, i); 
                    buf.append(((Enum<?>)o).name()).append(" ");
                }
                out.println(buf.toString());
            }
        }

        @Override
        public void deserializeArray(Object arr, BufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                while (true) {
                    String line = readLineOrEOF(in);
                    String [] parts = line.split(" ");
                    if (parts.length != len)
                        throw new Exception("Expected " + len + " parts but found " + parts.length);
                    for (int i=0; i<len; i++) {
                        Enum<?> enumEntry = findEnumEntry(arr.getClass().getComponentType(), parts[i]);
                        Array.set(arr, i, enumEntry);
                    }
                    break;
                }
            }
        }
    };
    
    private static Archiver archivableArchiver = new Archiver() {

        @Override
        public String get(Field field, Archivable a) throws Exception {
            Object o = field.get(a);
            if (o == null)
                return "null";
            return o.getClass().getCanonicalName() + " {";
        }

        @Override
        public void set(Field field, String value, Archivable a) throws Exception {
            if (!value.equals("null") && value != null) {
                value = value.split(" ")[0];
                field.set(a, Archivable.class.getClassLoader().loadClass(value).newInstance());
            }
        }
        
        @Override
        public void serializeArray(Object arr, PrintWriter out, String indent) throws Exception {
            int len = Array.getLength(arr);
            //out.println();
            if (len > 0) {
                indent = indent + INDENT;
                for (int i=0; i<len; i++) {
                    Archivable o = (Archivable)Array.get(arr, i);
                    if (o != null) {
                        out.println(indent + o.getClass().getCanonicalName() + " {");
                        o.serializeR(out, indent + INDENT);
                        out.println(indent + "}");
                    }
                    else
                        out.println(indent + "null");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, BufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i=0; i<len; i++) {
                String line = readLineOrEOF(in);
                if (line.equals("null"))
                    continue;
                String [] parts = line.split(" ");
                if (parts.length < 2)
                    throw new Exception("Expected 2 parts in line '" + line + "'");
                Archivable a = (Archivable)Archivable.class.getClassLoader().loadClass(parts[0]).newInstance();
                a.deserialize(in);
                Array.set(arr, i, a);
            }
        }
    };
    
    private static Archiver collectionArchiver = new Archiver() {

        @Override
        public String get(Field field, Archivable a) throws Exception {
            Collection<?> c = (Collection<?>)field.get(a);
            String s = c.getClass().getCanonicalName() + " {";
            return s;
        }

        @Override
        public void set(Field field, String value, Archivable a) throws Exception {
            if (value != null && !value.equals("null")) {
                String [] parts= value.split(" ");
                if (parts.length < 2)
                    throw new Exception("Expected at least 2 parts in " + value);
                field.set(a, Archivable.class.getClassLoader().loadClass(parts[0]).newInstance());
            }
        }
        
        @Override
        public void serializeArray(Object arr, PrintWriter out, String indent) throws Exception {
            int len = Array.getLength(arr);
            //out.println();
            if (len > 0) {
                indent = indent + INDENT;
                for (int i=0; i<len; i++) {
                    Collection<?> c = (Collection<?>)Array.get(arr, i);
                    if (c != null) {
                        //out.println(indent + c.getClass().getCanonicalName() + " {");
                        serializeObject(c, out, indent + INDENT, true);
                        //o.serializeR(out, indent + INDENT);
                        //out.println(indent + "}");
                    }
                    else
                        out.println(indent + "null");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, BufferedReader in) throws Exception {
            // TODO Auto-generated method stub
            throw new Exception("Not implemented");
        }
    };
    
    private static Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();
    
    // TODO: Support more than 3D arrays?
    static {
        classMap.put("int", int.class);
        classMap.put("int[]", int[].class);
        classMap.put("int[][]", int[][].class);
        classMap.put("float", float.class);
        classMap.put("float[]", float[].class);
        classMap.put("float[][]", float[][].class);
        classMap.put("long", long.class);
        classMap.put("long[]", long[].class);
        classMap.put("long[][]", long[][].class);
        classMap.put("double", double.class);
        classMap.put("double[]", double[].class);
        classMap.put("double[][]", double[][].class);
        classMap.put("boolean", boolean.class);
        classMap.put("boolean[]", boolean[].class);
        classMap.put("boolean[][]", boolean[][].class);
        classMap.put("java.lang.String[]", String[].class);
        classMap.put("java.lang.String[][]", String[][].class);
    }
    
    private static Class<?> getClassForName(String forName) throws ClassNotFoundException {
        if (classMap.containsKey(forName))
            return classMap.get(forName);
        try {
            return Class.forName(forName);
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to find class '" + forName + "' in map:\n" + classMap);
            throw e;
        }
    }
    
    private static Archiver arrayArchiver = new Archiver() {

        @Override
        public String get(Field field, Archivable a) throws Exception {
            Object o = field.get(a);
            String s = field.getType().getComponentType().getCanonicalName() + " " + Array.getLength(o);
            return s;
        }

        private Object createArray(String line) throws Exception {
            String [] parts = line.split(" ");
            if (parts.length < 2)
                throw new Exception("Invalid array description '" + line + "' excepted < 2 parts");
            Class<?> clazz =getClassForName(parts[0].trim());
            return Array.newInstance(clazz, Integer.parseInt(parts[1].trim()));
        }
        
        @Override
        public void set(Field field, String value, Archivable a) throws Exception {
            if (value != null && !value.equals("null")) {
                field.set(a, createArray(value));
            }
        }
        
        @Override
        public void serializeArray(Object arr, PrintWriter out, String indent) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                for (int i=0; i<len; i++) {
                    Archiver compArchiver = getArchiverForType(arr.getClass().getComponentType().getComponentType());
                    Object obj = Array.get(arr, i);
                    if (obj == null) {
                        out.println(indent + INDENT + "null");
                    } else {
                        out.println(indent + INDENT + obj.getClass().getComponentType().getCanonicalName() + " " + Array.getLength(obj));
                        compArchiver.serializeArray(Array.get(arr, i), out, indent + INDENT);
                    }
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, BufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i=0; i<len; i++) {
                Archiver compArchiver = getArchiverForType(arr.getClass().getComponentType().getComponentType());
                String line = readLineOrEOF(in);
                if (line != null && !line.equals("null")) {
                    Object obj = createArray(line);
                    Array.set(arr, i, obj);
                    compArchiver.deserializeArray(obj, in);
                }
            }
        }
    };
    
    
    private static Map<Class<?>, Map<Field, Archiver>> classValues = new HashMap<Class<?>, Map<Field, Archiver>>();

    private static Comparator<Field> fieldComparator = new Comparator<Field>() {

        @Override
        public int compare(Field arg0, Field arg1) {
            return arg0.getName().compareTo(arg1.getName());
        }
        
    };
    
    private static void inheritValues(Class<?> clazz, Map<Field, Archiver> values) {
        if (clazz == null || clazz.equals(Archiver.class))
            return;
        if (classValues.containsKey(clazz)) {
            values.putAll(classValues.get(clazz));
        }
        inheritValues(clazz.getSuperclass(), values);
    }
    
    private static Map<Field, Archiver> getValues(Class<?> clazz) {
        try {
            if (clazz.getCanonicalName() == null) {
                if (clazz.getSuperclass() != null)
                    clazz = clazz.getSuperclass();
                else
                    clazz = clazz.getEnclosingClass();
            }
            Map<Field, Archiver> values = null;
            if (classValues.containsKey(clazz)) {
                values = classValues.get(clazz);
            } else {
                // test newInstance works for this clazz
                if (clazz.isAnonymousClass() || clazz.isSynthetic())
                    throw new Exception("Synthetic and anonymous classes not supported");
                if (!Modifier.isAbstract(clazz.getModifiers()))
                    clazz.newInstance();
                values = new TreeMap<Field, Archiver>(fieldComparator);
                // now inherit any values in base classes that were added
                inheritValues(clazz.getSuperclass(), values);
                classValues.put(clazz,  values);
                classMap.put(clazz.getCanonicalName()+"[]", Array.newInstance(clazz, 0).getClass());
                classMap.put(clazz.getCanonicalName()+"[][]", Array.newInstance(Array.newInstance(clazz, 0).getClass(), 0).getClass());
            } 
            return values;
        } catch (Exception e) {
            throw new RuntimeException("class " + clazz.getCanonicalName() + " does not appear to support a zero argument constructor.", e);
        }
    }
    
    private static boolean isSubclassOf(Class<?> subClass, Class<?> baseClass) {
        if (subClass== null || subClass.equals(Object.class) || subClass.getCanonicalName().equals(Object.class.getCanonicalName()))
            return false;
        if (subClass == baseClass || subClass.equals(baseClass) || subClass.getCanonicalName().equals(baseClass.getCanonicalName()))
            return true;
        if (baseClass.isAssignableFrom(subClass))
            return true;
        return isSubclassOf(subClass.getSuperclass(), baseClass);
    }
    
    private static Archiver getArchiverForType(Class<?> clazz) {
        if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            return booleanArchiver;
        } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            return longArchiver;
        } else if (clazz.equals(Double.class) || clazz.equals(double.class)) {
            return doubleArchiver;
        } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            return integerArchiver;
        } else if (clazz.equals(Float.class) || clazz.equals(float.class)){
            return floatArchiver;
        } else if (clazz.equals(String.class)) {
            return stringArchiver;
        } else if (clazz.isEnum()) {
            return enumArchiver;
        } else if (isSubclassOf(clazz, Archivable.class)) {
            return archivableArchiver;
        } else if (isSubclassOf(clazz, Collection.class)) {
            return collectionArchiver;
        } else if (clazz.isArray()) {
            return arrayArchiver;
        } else {
            throw new RuntimeException("No archiver available for class: " + clazz);
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
     * 
     * Also, fields are inherited.
     * @param clazz
     * @param name
     */
    public static void addField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            if (Modifier.isStatic(field.getModifiers()))
                throw new RuntimeException("Cannot add static fields");
            field.setAccessible(true);
            Archiver archiver = getArchiverForType(field.getType());
            Map<Field, Archiver> values = getValues(clazz);
            if (values.containsKey(field))
                throw new RuntimeException("Duplicate field.  Field '" + name + "' has already been included for class: " + clazz.getCanonicalName());
            values.put(field, archiver);
            System.out.println("Added field '" + name + "' for class: " + clazz);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to add field '" + name + "'", e);
        }
    }
    
    /**
     * Add a field of a specific class with a custom achiver.
     * @param clazz
     * @param name
     * @param archiver
     * @throws Exception
     */
    public static void addField(Class<?> clazz, String name, Archiver archiver) throws Exception {
        try {
            Field field = clazz.getDeclaredField(name);
            Map<Field, Archiver> values = getValues(clazz);
            if (values.containsKey(field))
                throw new RuntimeException("Duplicate field.  Field '" + name + "' has already been included for class: " + clazz.getCanonicalName());
            values.put(field, archiver);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to add field '" + name + "'", e);
        }
    }

    /**
     * 
     * @param clazz
     */
    public static void addAllFields(Class<?> clazz) {
        try {
            Field [] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (!Modifier.isStatic(f.getModifiers()))
                    addField(clazz, f.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to add all fields", e);
        }
    }
    
    private static class MyPrintWriter extends PrintWriter {
        public MyPrintWriter(OutputStream out) {
            super(out, true);
            String indent = "";
            for (int i=0; i<32; i++) {
                indents[i] = indent;
                indent += "   ";
            }
        }

        String [] indents;
        int currentIndent = 0;
        
        void push() {
            if (currentIndent < indents.length - 1)
                currentIndent++;
        }
        
        void pop() {
            if (currentIndent > 0)
                currentIndent--;
        }

        @Override
        public void println(String line) {
            super.print(indents[currentIndent]);
            super.println(line);
        }
    }
    
    public final void archive(OutputStream out) throws IOException {
        MyPrintWriter writer = new MyPrintWriter(out);
    }
    
    public static Archivable restore(InputStream in) throws IOException {
        return null;
    }
    
    public final void read(InputStream in) throws IOException {
        
    }
    
    
    
    /**
     * 
     * @param out
     * @throws Exception
     */
    public final void serialize(PrintWriter out) throws IOException {
        try {
            serializeR(out, "");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
    
    /**
     * 
     * @param obj
     * @param out
     * @param indent
     * @param printObjects
     * @throws Exception
     */
    private static void serializeObject(Object obj, PrintWriter out, String indent, boolean printObjects) throws Exception {
        if (obj == null) {
            out.println(indent + "null");
        } else if (obj instanceof Archivable) {
            ((Archivable)obj).serializeR(out, indent + INDENT);
            out.println(indent + "}");
        } else if (obj instanceof Collection) {
            Collection<?> c = (Collection<?>)obj;
            for (Object o: c) {
                out.println(indent + INDENT + o.getClass().getCanonicalName() + " {");
                serializeObject(o, out, indent + INDENT + INDENT, true);
            }
            out.println(indent + "}");
        } else if (obj.getClass().isArray()) {
            Archiver compArchiver = getArchiverForType(obj.getClass().getComponentType());
            compArchiver.serializeArray(obj, out, indent);
        }         
        else if (printObjects) {
            out.println(indent + obj);
        }
    }
    
    private void serializeR(PrintWriter out, String indent) throws Exception {
        Map<Field, Archiver> values = getValues(getClass());
        for (Field field : values.keySet()) {
            Archiver archiver = values.get(field);
            Object obj = field.get(Archivable.this);
            if (obj == null) {
                out.println(indent + field.getName()+"=null");
                continue;
            }
            out.println(indent + field.getName()+"="+archiver.get(field, this));
            serializeObject(obj, out, indent, false);
        }
    }
    
    private static Object parse(Class<?> clazz, BufferedReader in) throws Exception {
        if (clazz.isEnum())
            return findEnumEntry(clazz, readLineOrEOF(in));
        if (clazz.isArray())
            return Array.newInstance(clazz, Integer.parseInt(readLineOrEOF(in)));
        if (isSubclassOf(clazz, Integer.class))
            return Integer.parseInt(readLineOrEOF(in));
        if (isSubclassOf(clazz, Float.class))
            return Float.parseFloat(readLineOrEOF(in));
        if (isSubclassOf(clazz, Long.class))
            return Long.parseLong(readLineOrEOF(in));
        if (isSubclassOf(clazz, Double.class))
            return Double.parseDouble(readLineOrEOF(in));
        if (isSubclassOf(clazz, Boolean.class))
            return Boolean.parseBoolean(readLineOrEOF(in));
        if (isSubclassOf(clazz, Archivable.class)) {
            Archivable a = (Archivable)clazz.newInstance();
            a.deserialize(in);
            return a;
        }
        try {
            // try to create from a string constructor
            Constructor<?> cons = clazz.getConstructor(String.class);
            String arg = readLineOrEOF(in).trim();
            return cons.newInstance(arg);
        } catch (NoSuchMethodException e) {}
        throw new Exception("Dont know how to parse class " + clazz);
    }
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void deserializeCollection(Collection c, BufferedReader in) throws Exception {
        while (true) {
            String line = readLineOrEOF(in).trim();
            if (line.equals("}"))
                break;
            if (line == null || line.equals("null"))
                c.add(null);
            else {
                String [] parts = line.split(" ");
                if (parts.length < 1)
                    throw new Exception("Expected at least 1 parts in line: " + line);
                Class<?> clazz = getClassForName(parts[0]);
                c.add(parse(clazz, in)); 
            }
        }
    }

    public void deserialize(String text) throws Exception {
        BufferedReader in = new BufferedReader(new StringReader(text));
        deserialize(in);
    }
    
    /**
     * initialize fields of this object there are explicitly added by addField for this class type.
     * @param in
     * @throws Exception
     */
    public final void deserialize(BufferedReader in) throws Exception {
        Map<Field, Archiver> values = getValues(getClass());
        while (true) {
            String line = in.readLine();
            //System.out.println("Parsingline: " + line);
            if (line == null)
                break;
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#"))
                continue;
            if (line.startsWith("}"))
                break;
            String [] parts = line.split("=");
            if (parts.length != 2)
                throw new Exception("Mangled line should have 2 parts, instead has " + parts.length);
            String name = parts[0].trim();
            for (Field field : values.keySet()) {
                if (field.getName().equals(name)) {
                    Archiver archiver = values.get(field);
                    archiver.set(field, parts[1], this);
                    if (field.get(Archivable.this) instanceof Archivable) {
                        ((Archivable)field.get(Archivable.this)).deserialize(in);
                    } else if (field.getType().isArray()) {
                        Object obj = field.get(this);
                        if (obj != null) {
                            Archiver arrayArchiver = getArchiverForType(obj.getClass().getComponentType());
                            arrayArchiver.deserializeArray(obj, in);
                        }
                    } else if (isSubclassOf(field.getType(), Collection.class)) {
                        Collection<?> collection = (Collection<?>)field.get(this);
                        if (collection != null)
                            deserializeCollection(collection, in);
                    }
                    parts = null;
                    break;
                }
            }
            if (parts != null)
                //throw new Exception
                System.err.println("Unknown field: " + name + " not in fields: " + values.keySet());
        }
    }
    
    private boolean isArraysEqual(Object a, Object b) {
        int lenA = Array.getLength(a);
        int lenB = Array.getLength(b);
        if (lenA != lenB)
            return false;
        for (int i=0 ;i<lenA; i++) {
            if (!isEqual(Array.get(a,  i), Array.get(b, i)))
                return false;
        }
        return true;
    }
    
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        if (a == b)
            return true;
        if (a.getClass().isArray() && b.getClass().isArray())
            return isArraysEqual(a, b);
        return a.equals(b);
    }
    
    @Override
    public boolean equals(Object arg0) {
        if (arg0 == this)
            return true;
        if (arg0 == null)
            return false;
        Map<Field, Archiver> values = getValues(getClass());
        Archivable a = (Archivable)arg0;
        for (Field f : values.keySet()) {
            try {
                if (!isEqual(f.get(this), f.get(a)))
                    return false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringWriter buf = new StringWriter();
        try {
            serialize(new PrintWriter(buf, true));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buf.toString(); 
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object deepCopy(Object o) {
        try {
            if (o == null)
                return null;
            if (o instanceof Archivable)
                return ((Archivable)o).deepCopy();
            if (o.getClass().isArray()) {
                int len = Array.getLength(o);
                Object arr = Array.newInstance(o.getClass().getComponentType(), len);
                for (int i=0; i<len; i++) {
                    Object oo = deepCopy(Array.get(o, i));
                    Array.set(arr, i, oo);
                }
                return arr;
            }
            if (o instanceof Collection) {
                Collection oldCollection = (Collection)o;
                Collection newCollection = (Collection)o.getClass().newInstance();
                for (Object oo: oldCollection) {
                    newCollection.add(deepCopy(oo));
                }
                return newCollection;
            }
            return o;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Deep copy all fields explicitly set be addField.  Any other fields are NOT deep copied, nor initialized.
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Archivable> T deepCopy() {
        try {
            Archivable a = getClass().newInstance();
            Map<Field, Archiver> values = getValues(getClass());
            
            for (Field f: values.keySet()) {
                f.set(a, deepCopy(f.get(this)));
            }
            
            return (T)a;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deep copy", e);
        }
        
    }
    
    /**
     * Convenience method
     * 
     * @param file
     * @throws IOException
     */
    public void saveToFile(File file) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            PrintWriter writer = new PrintWriter(out, true);
            serialize(writer);
            //writer.flush();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Convenience method
     * 
     * @param file
     * @throws IOException
     */
    public void loadFromFile(File file) throws IOException {
        FileInputStream in = null;
        final int[] lineNum = new int[1];
        try {
            in = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in)) {
                @Override
                public String readLine() throws IOException {
                    String line = super.readLine();
                    lineNum[0]++;
                    return line;
                }
            };       
            deserialize(reader);
        } catch (IOException e) {
            throw new IOException("Error on line " + lineNum[0] + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Error on line " + lineNum[0] + ": " + e.getMessage(), e);
        } finally {
            if (in != null)
                in.close();
        }
    }
}
