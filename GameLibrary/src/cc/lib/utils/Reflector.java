package cc.lib.utils;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import cc.lib.game.Utils;

/**
 * Derive from this class to handle copying, equals, serializing, etc.
 * 
 * Serialization of primitives, arrays and subclasses of Reflector are supported.
 * Also collections are supported if their data types are one of the afore mentioned
 * New types can be added if they implement an Archiver.
 * 
 * There are 3 ways to use this class:
 * 1. Extend Reflector and Don't override serialize and deserialize, simple call addField within a static 
 * block of your class for each field you want to be serialized.  There are convenience methods like 
 * addAllFields and omitField to simplify this step.  The following types can be added using addField 
 * with no work:
 * primitives, primitive arrays, Object Arrays, enums, Collections, Maps, Reflectors.
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
 * NOTE: Derived classes must support a public zero argument constructor for de-serialization
 * @author ccaron
 *
 */
public class Reflector<T> {

    public static final class VersionTooOldException extends IOException {
        private VersionTooOldException(int current, int min) {
            super("Version of deserialized object is " + current + " which older than min specified " + min);
        }
    }

    /**
     * Turn this on to throw exceptions on any unknown fields.  Default is off.
     */
    public static boolean ENABLE_THROW_ON_UNKNOWN = false;

    private int version = 0;

    /**
     * Derived classes override this to provide a min loadable version.
     * For instance, if the version loaded is '1' and theis method returns 2, then an exception
     * is thrown
     * @return
     */
    protected int getMinVersion() {
        return 0;
    }
    
    private static class MyBufferedReader extends BufferedReader {

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
                	return line.substring(0, line.length()-1).trim();
                }
                if (line.endsWith("}")) {
                	depth--;
                	return null;
                }
                return line;
            } catch (IOException e) {
                throw new IOException("Error on line: " + lineNum, e);
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
    
    private static class MyPrintWriter extends PrintWriter {

        static String [] indents;

        static {
            indents = new String[32];
            String indent = "";
            for (int i=0; i<indents.length; i++) {
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
        	assert(currentIndent > 0);
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
        void set(Field field, String value, Reflector<?> a) throws Exception;
        void serializeArray(Object arr, MyPrintWriter out) throws Exception;
        void deserializeArray(Object arr, MyBufferedReader in) throws Exception;
    };
    
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
        public void set(Field field, String value, Reflector<?> a) throws Exception {
        	field.setAccessible(true);
            if (value == null || value.equals("null"))
                field.set(a, null);
            else
                field.set(a, parse(value));
        }
        
        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                StringBuffer buf = new StringBuffer();
                for (int i=0; i<len; i++) {
                    buf.append(Array.get(arr, i)).append(" ");
                }
                out.println(buf.toString());
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
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
                    in.mark(256);
                    if (readLineOrEOF(in) != null)
                    	in.reset();
                    	//throw new Exception("Line: " + in.lineNum +" expected closing '}'");
                    break;
                }
            }
        }        
    }
    
    private static Archiver stringArchiver = new Archiver() {
        
        @Override
        public String get(Field field, Reflector<?> a)  throws Exception{
            Object s = field.get(a);
            if (s == null)
                return "null";
            return "\"" + (String)s + "\"";
        }

        @Override
        public void set(Field field, String value, Reflector<?> a)  throws Exception{
            if (value == null || value.equals("null"))
                field.set(a, null);
            else {
                field.set(a, value.substring(1, value.length()-1));
            }
        }

        
        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int num = Array.getLength(arr);
            if (num > 0) {
                StringBuffer buf = new StringBuffer();
                for (int i=0; i<num; i++) {
                    Object entry = Array.get(arr, i);
                    if (entry == null)
                        buf.append("null\n");
                    else
                        buf.append("\"").append(entry).append("\"\n");
                }
                out.print(buf.toString());
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i=0; i<len; i++) {
                String line = readLineOrEOF(in);
                if (!line.equals("null"))
                    Array.set(arr, i, line.substring(1, line.length()-1));
            }
            if (readLineOrEOF(in) != null)
            	throw new Exception("Line: " + in.lineNum +" expected closing '}'");
        }
    };

    private static Archiver byteArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return Byte.parseByte(s);
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
		public Object parse(String s) {
            return Long.parseLong(s);
        }
        
    };
    
    private static Archiver floatArchiver = new AArchiver() {

        @Override
		public Object parse(String s) {
            return Float.parseFloat(s);
        }
        
    };    
    
    private static Archiver doubleArchiver = new AArchiver() {

        @Override
		public Object parse(String s) {
            return Double.parseDouble(s);
        }
    
    };

    private static Archiver booleanArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
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
        public String get(Field field, Reflector<?> a) throws Exception {
            return ((Enum<?>)field.get(a)).name();
        }

        @Override
        public void set(Field field, String value, Reflector<?> a) throws Exception {
            field.set(a, findEnumEntry(field.getType(), value));
        }
        
        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                StringBuffer buf = new StringBuffer();
                for (int i=0; i<len; i++) {
                    Object o = Array.get(arr, i); 
                    buf.append(((Enum<?>)o).name()).append(" ");
                }
                out.println(buf.toString());
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            while (true) {
                String line = readLineOrEOF(in);
                String [] parts = line.split(" ");
                if (parts.length != len)
                    throw new Exception("Expected " + len + " parts but found " + parts.length);
                for (int i=0; i<len; i++) {
                    Enum<?> enumEntry = findEnumEntry(arr.getClass().getComponentType(), parts[i]);
                    Array.set(arr, i, enumEntry);
                }
                if (readLineOrEOF(in) != null)
                	throw new Exception("Line: " + in.lineNum +" expected closing '}'");
                break;
            }
        }
    };
    
    private static Archiver archivableArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Object o = field.get(a);
            if (o == null)
                return "null";
            Class<?> clazz = o.getClass();
            String className = null;// = o.getClass().getCanonicalName();
            if (clazz.isAnonymousClass())
                className = clazz.getSuperclass().getCanonicalName();
            else
                className = clazz.getCanonicalName();
            Utils.assertTrue(className != null, "Failed to get className for class %s", clazz);
            return className + " {";
        }

        @Override
        public void set(Field field, String value, Reflector<?> a) throws Exception {
            if (!value.equals("null") && value != null) {
                value = value.split(" ")[0];
                field.setAccessible(true);
                try {
                	field.set(a, getClassForName(value).newInstance());
                } catch (ClassNotFoundException e) {
                	int dot = value.lastIndexOf('.');
                	if (dot > 0) {
                		String altName = value.substring(0, dot) + "$" + value.substring(dot+1);
                    	field.set(a, getClassForName(altName).newInstance());
                	} else {
                		throw e;
                	}
                }
            }
        }
        
        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                for (int i=0; i<len; i++) {
                    Reflector<?> o = (Reflector<?>)Array.get(arr, i);
                    if (o != null) {
                        out.println(o.getClass().getCanonicalName() + " {");
                    	out.push();
                        o.serialize(out);
                        out.pop();
                        out.println("}");
                    }
                    else
                        out.println("null");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
            int len = Array.getLength(arr);
            for (int i=0; i<len; i++) {
                int depth = in.depth;
                String line = readLineOrEOF(in);
                if (line.equals("null"))
                    continue;
                Reflector<?> a = (Reflector<?>)getClassForName(line).newInstance();
                a.deserialize(in);
                Array.set(arr, i, a);
                if (in.depth > depth) {
                	if (readLineOrEOF(in) != null)
                		throw new Exception("Line: " + in.lineNum + " expected closing '}'");
                }
            }
            if (readLineOrEOF(in) != null)
            	throw new Exception("Line: " + in.lineNum +" expected closing '}'");
        }
    };

    private static Archiver collectionArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Collection<?> c = (Collection<?>)field.get(a);
            String s = c.getClass().getCanonicalName() + " {";
            return s;
        }

        @Override
        public void set(Field field, String value, Reflector<?> a) throws Exception {
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
                for (int i=0; i<len; i++) {
                    Collection<?> c = (Collection<?>)Array.get(arr, i);
                    if (c != null) {
                        out.println(c.getClass().getCanonicalName() + " {");
                        serializeObject(c, out, true);
                    }
                    else
                        out.println("null");
                }
            }
        }

        @Override
        public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
        	int len = Array.getLength(arr);
        	for (int i=0; i<len; i++) {
        		String clazz = readLineOrEOF(in);
        		if (!clazz.equals("null")) {
            		Collection<?> c = (Collection<?>)getClassForName(clazz).newInstance();
            		deserializeCollection(c, in);
            		Array.set(arr, i, c);
        		}
        	}
            if (readLineOrEOF(in) != null)
            	throw new Exception("Line: " + in.lineNum +" expected closing '}'");
        }
    };

    private static Archiver mapArchiver = new Archiver() {

		@Override
		public String get(Field field, Reflector<?> a) throws Exception {
			Map<?, ?> m = (Map<?,?>)field.get(a);
			String s = m.getClass().getCanonicalName() + " {";
			return s;
		}

		@Override
		public void set(Field field, String value, Reflector<?> a) throws Exception {
			if (value != null && !value.equals("null")) {
				String [] parts = value.split(" ");
				if (parts.length < 2)
                    throw new Exception("Expected at least 2 parts in " + value);
                field.set(a, getClassForName(parts[0]).newInstance());
			} else {
				field.set(a, null);
			}
		}
		
		@Override
		public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
			int len = Array.getLength(arr);
            if (len > 0) {
                for (int i=0; i<len; i++) {
                    Map<?,?> m = (Map<?,?>)Array.get(arr, i);
                    if (m != null) {
                        out.println(m.getClass().getCanonicalName() + " {");
                        serializeObject(m, out, true);
                    }
                    else
                        out.println("null");
                }
            }		
		}
		
		@Override
		public void deserializeArray(Object arr, MyBufferedReader in) throws Exception {
			int len = Array.getLength(arr);
        	for (int i=0; i<len; i++) {
        		String clazz = readLineOrEOF(in);
        		if (!clazz.equals("null")) {
            		Map<?,?> m = (Map<?,?>)getClassForName(clazz).newInstance();
            		deserializeMap(m, in);
            		Array.set(arr, i, m);
        		}
        	}
            if (readLineOrEOF(in) != null)
            	throw new Exception("Line: " + in.lineNum +" expected closing '}'");		}
	};
	
    private static Archiver arrayArchiver = new Archiver() {

        @Override
        public String get(Field field, Reflector<?> a) throws Exception {
            Object o = field.get(a);
            String s = field.getType().getComponentType().getCanonicalName() + " " + Array.getLength(o) + " {";
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
        public void set(Field field, String value, Reflector<?> a) throws Exception {
            if (value != null && !value.equals("null")) {
                field.set(a, createArray(value));
            } else {
            	field.set(a, null);
            }
        }
        
        @Override
        public void serializeArray(Object arr, MyPrintWriter out) throws Exception {
            int len = Array.getLength(arr);
            if (len > 0) {
                for (int i=0; i<len; i++) {
                    Archiver compArchiver = getArchiverForType(arr.getClass().getComponentType().getComponentType());
                    Object obj = Array.get(arr, i);
                    if (obj == null) {
                        out.println("null");
                    } else {
                        out.println(obj.getClass().getComponentType().getCanonicalName() + " " + Array.getLength(obj) + " {");
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
            for (int i=0; i<len; i++) {
                Archiver compArchiver = getArchiverForType(arr.getClass().getComponentType().getComponentType());
                String line = readLineOrEOF(in);
                if (line != null && !line.equals("null")) {
                    Object obj = createArray(line);
                    Array.set(arr, i, obj);
                    compArchiver.deserializeArray(obj, in);
                }
            }
            if (readLineOrEOF(in) != null)
            	throw new Exception("Line: " + in.lineNum +" expected closing '}'");

        }
    };
    
    private static Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();
    
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
    }
    
    public static void registerEnum(Class<?> enumClazz) {
    	String clazz = enumClazz.getCanonicalName();
    	classMap.put(clazz, enumClazz);
    }
    
    private static Class<?> getClassForName(String forName) throws ClassNotFoundException {
        if (classMap.containsKey(forName))
            return classMap.get(forName);
        try {
            return Reflector.class.getClassLoader().loadClass(forName);
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to find class '" + forName + "'");
            throw e;
        }
    }

    
    private final static Map<Class<?>, Set<String>> omitFields = new HashMap<Class<?>, Set<String>>();
    private final static Map<Class<?>, Map<Field, Archiver>> classValues = new HashMap<Class<?>, Map<Field, Archiver>>();

    private static Comparator<Field> xfieldComparator = new Comparator<Field>() {

        @Override
        public int compare(Field arg0, Field arg1) {
            return arg0.getName().compareTo(arg1.getName());
        }
        
    };
    
    private final static Comparator<Field> fieldComparator = new Comparator<Field>() {

    	Class<?> [] clazzes = {
    		boolean.class,
    		byte.class,
    		int.class,
    		long.class,
    		float.class,
    		double.class,
    		Boolean.class,
    		Byte.class,
    		Integer.class,
    		Long.class,
    		Float.class,
    		Double.class,
    		String.class,
    		Enum.class,
    		Reflector.class,
    		Array.class,
    		Collection.class,
    		Map.class
    	};
    	
    	
    	private int getFieldNumber(Field f) {
    		for (int i=0; i<clazzes.length; i++) {
    			if (clazzes[i] == Array.class && f.getType().isArray())
    				return i;
    			if (f.getType() == clazzes[i] || f.getType().equals(clazzes[i]) || clazzes[i].isAssignableFrom(f.getType()) || isSubclassOf(clazzes[i], f.getType())) {
    				return i;
    			}
    		}
    		//System.err.println("Failed to categorize type '" + f.getType());// + "' into one of " + Arrays.toString(clazzes));
    		return Integer.MAX_VALUE;
    	}
    	
		@Override
		public int compare(Field o1, Field o2) {

			int n0 = getFieldNumber(o1);
			int n1 = getFieldNumber(o2);
			
			if (n0 == n1) {
				return o1.getName().compareTo(o2.getName());
			}
			
			return n0-n1;
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
    
    private static Map<Field, Archiver> getValues(Class<?> clazz, boolean createIfDNE) {
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
                values = new TreeMap<Field, Archiver>(fieldComparator);
                // now inherit any values in base classes that were added
                inheritValues(clazz.getSuperclass(), values);
                classValues.put(clazz,  values);
                classMap.put(clazz.getCanonicalName()+"[]", Array.newInstance(clazz, 0).getClass());
                classMap.put(clazz.getCanonicalName()+"[][]", Array.newInstance(Array.newInstance(clazz, 0).getClass(), 0).getClass());
            } else if (clazz.getSuperclass() == null) {
            	throw new RuntimeException("Cannot find any fields to archive (did you add an addField(...) method in your class?)");
            } else {
                return getValues(clazz.getSuperclass(), createIfDNE);
            }
            return values;
        } catch (RuntimeException e) {
        	throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate " + clazz.getCanonicalName() + ". Is it public? Does it have a public 0 argument constructor?", e);
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
        } else if (clazz.equals(Float.class) || clazz.equals(float.class)){
            return floatArchiver;
        } else if (clazz.equals(String.class)) {
            return stringArchiver;
        } else if (clazz.isEnum() || isSubclassOf(clazz, Enum.class)) {
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
            throw new RuntimeException("No reflector available for class: " + clazz);
        }        
    }
    
    private static void addArrayTypes(Class<?> clazz) {
    	String nm = clazz.getName();
    	if (classMap.containsKey(nm))
    		return;
    	String nm2 = nm;
    	int lBrack = nm.lastIndexOf('[');
    	if (lBrack > 0) {
    		nm = nm.substring(lBrack+2, nm.length()-1);
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

    private HashSet<String> localOmitFields = null;
    
    /**
     * Adds a field local to this object that won't be serialized out.
     * 
     * @param fieldName
     * @throws Exception
     */
    public final void omitField(String fieldName) throws Exception {
    	if (localOmitFields == null) {
    		localOmitFields = new HashSet<String>();
    	}
    	localOmitFields.add(fieldName);
    }
    
    /**
     * Remove any locally omitted fields @see omitField(String)
     */
    public final void removeLocalOmitFields() {
    	localOmitFields = null;
    }
    
    /**
     * Should be called before add(All)Field(s)
     * @param clazz
     * @param fieldName
     */
    public static void omitField(Class<?> clazz, String fieldName) {
    	Set<String> fields = null;
    	if (!omitFields.containsKey(clazz)) {
    		fields = new HashSet<String>();
    		omitFields.put(clazz, fields);
    	} else {
    		fields = omitFields.get(clazz);
    	}
    	fields.add(fieldName);
    }
    
    private static boolean isFieldOmitted(Class<?> clazz, String fieldName) {
    	Set<String> fields = omitFields.get(clazz);
    	if (fields == null)
    		return false;
    	return fields.contains(fieldName);
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
        	if (isFieldOmitted(clazz, name)) {
        		Utils.println("Skipping omitted field '" + name + "'");
        		return;
        	}
            Field field = clazz.getDeclaredField(name);
            if (Modifier.isStatic(field.getModifiers()))
                throw new RuntimeException("Cannot add static fields");
            field.setAccessible(true);
            Archiver archiver = getArchiverForType(field.getType());
            Map<Field, Archiver> values = getValues(clazz, true);
            if (values.containsKey(field))
                throw new RuntimeException("Duplicate field.  Field '" + name + "' has already been included for class: " + clazz.getCanonicalName());
            values.put(field, archiver);
            classMap.put(clazz.getName().replace('$', '.'), clazz);
            Utils.println("Added field '" + name + "' for " + clazz);
        } catch (RuntimeException e) {
            throw e;
        } catch (NoSuchFieldException e) {
        	if (ENABLE_THROW_ON_UNKNOWN)
        		throw new RuntimeException("Failed to add field '" + name + "'", e);
        } catch (Exception e) {
        	throw new RuntimeException("Failed to add field '" + name + "'", e);
        }
    }
    
    /**
     * Remove a field for archiving for a specific class.  If field not present, then no effect.
     * If field not one of clazz, then runtime exception thrown.
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
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Add a field of a specific class with a custom achiver.
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
    	addArrayTypes(clazz);
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
    
    /**
     * 
     * @param obj
     * @param out
     * @throws Exception
     */
    public static void serializeObject(Object obj, PrintWriter out) throws Exception {
    	MyPrintWriter _out;
        if (out instanceof MyPrintWriter)
            _out = (MyPrintWriter)out;
        else
            _out = new MyPrintWriter(out);
        serializeObject(obj, _out, false);
    }
    
    /**
     * 
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
            ((Reflector<?>)obj).serialize(out);
            out.pop();
            out.println("}");
        } else if (obj instanceof Collection) {
            Collection<?> c = (Collection<?>)obj;
            out.push();
            for (Object o: c) {
            	if (o != null && o.getClass().isArray()) {
            		int len = Array.getLength(o);
            		out.println(o.getClass().getComponentType().getName() + " " + len + " {");
            	} else {
            		out.println(o.getClass().getCanonicalName() + " {");
            	}
                serializeObject(o, out, true);
            }
            out.pop();
            out.println("}");
        } else if(obj instanceof Map) {
        	Map<?,?> m = (Map<?,?>)obj;
        	out.push();
        	for (Map.Entry<?, ?> entry : m.entrySet()) {
        		Object o = entry.getKey();
                out.println(o.getClass().getCanonicalName() + " {");
                out.push();
                serializeObject(o, out, true);
                out.pop();
                out.println("}");
                o = entry.getValue();
                out.println(o.getClass().getCanonicalName() + " {");
                out.push();
                serializeObject(o, out, true);
                out.pop();
                out.println("}");
        	}
            out.pop();
            out.println("}");
        } else if (obj.getClass().isArray()) {
            Archiver compArchiver = getArchiverForType(obj.getClass().getComponentType());
            out.push();
            compArchiver.serializeArray(obj, out);
            out.pop();
            out.println("}");
        }         
        else if (printObjects) {
        	out.push();
        	out.println(obj);
        	out.pop();
        	out.println("}");
        }
    }
    
    private final void serialize(PrintWriter out_) throws IOException {
//        Utils.println("Serializing %s", getClass().getName());
        MyPrintWriter out;
        if (out_ instanceof MyPrintWriter)
            out = (MyPrintWriter)out_;
        else
            out = new MyPrintWriter(out_);
        try {
            Map<Field, Archiver> values = getValues(getClass(), false);
            for (Field field : values.keySet()) {
            	if (localOmitFields != null && localOmitFields.contains(field.getName()))
            		continue;
                Archiver archiver = values.get(field);
                field.setAccessible(true);
                Object obj = field.get(Reflector.this);
                if (obj == null) {
                    out.println(field.getName()+"=null");
                    continue;
                }
                out.println(field.getName()+"="+archiver.get(field, this));
                serializeObject(obj, out, false);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Override this to do extra handling. Derived should call super.
     * @param out
     * @throws IOException
     */
    public void serialize(OutputStream out) throws IOException {
        serialize(new MyPrintWriter(out));
    }
    
    private static Object parse(Class<?> clazz, MyBufferedReader in) throws Exception {
        if (clazz.isEnum())
            return findEnumEntry(clazz, readLineOrEOF(in));
        if (clazz.isArray()) {
            throw new Exception("This method not to be called for array types");
        }
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
        if (isSubclassOf(clazz, Reflector.class)) {
            Reflector<?> a = (Reflector<?>)clazz.newInstance();
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
    private static void deserializeCollection(Collection c, MyBufferedReader in) throws Exception {
    	final int startDepth = in.depth;
    	while (true) {
    		String line = readLineOrEOF(in);
    		if (line == null)
    			break;
            Object entry = null;
            if (!line.equals("null")) {
            	String [] parts = line.split(" ");
	            Class<?> clazz = getClassForName(parts[0]);
	            if (parts.length > 1) {
	            	int num = Integer.parseInt(parts[1]);
	            	entry = Array.newInstance(clazz, num);
	                getArchiverForType(clazz).deserializeArray(entry, in);
	            } else
	            	entry = parse(clazz, in); 
	            if (in.depth > startDepth)
    	            if (readLineOrEOF(in) != null)
    	            	throw new Exception("Line " + in.lineNum + " Expected closing '}'");
            }
            c.add(entry);
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void deserializeMap(Map c, MyBufferedReader in) throws Exception {
    	while (true) {
        	String line = readLineOrEOF(in).trim();
            if (line == null || line.equals("null"))
                break;
            Class<?> clazz = getClassForName(line);
	    	Object key = parse(clazz, in);
	    	if (key == null)
	    		throw new Exception("null key in map");
        	line = readLineOrEOF(in).trim();
            if (line == null) // !line.equals("}"))
                throw new Exception("Expected '}' to end the key");
        	line = readLineOrEOF(in).trim();
            if (line == null) //.equals("}"))
                throw new Exception("Missing value from key/value pair in map");
            Object value = null;
            if (line != null && !line.equals("null")) {
	            clazz = getClassForName(line);
	            value = parse(clazz, in);
	            line = readLineOrEOF(in);
	            if (line != null)
	                throw new Exception("Expected '}' to end the value");
            }
	    	c.put(key, value);
    	}
    }

    /**
     *
     * @param text
     * @throws Exception
     */
    public final void deserialize(String text) throws Exception {
        deserialize(new MyBufferedReader(new StringReader(text)));
    }

    /**
     *
     * @param in
     * @throws Exception
     */
    public void deserialize(InputStream in) throws IOException {
        MyBufferedReader reader = new MyBufferedReader(new InputStreamReader(in));
        try {
            deserialize(new MyBufferedReader(new InputStreamReader(in)));
        } catch (VersionTooOldException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException("Error on line " + reader.lineNum + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Error on line " + reader.lineNum + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * initialize fields of this object there are explicitly added by addField for this class type.
     * @param _in
     * @throws Exception
     */
    private void deserialize(BufferedReader _in) throws Exception {
    	MyBufferedReader in = null;
    	if (_in instanceof MyBufferedReader)
    		in = (MyBufferedReader)_in;
    	else
    		in = new MyBufferedReader(_in);
        Map<Field, Archiver> values = getValues(getClass(), false);
    	final int depth = in.depth;
        while (true) {
            if (in.depth > depth)
            	if (in.readLine() != null)
            		throw new Exception("Line: " + in.lineNum + " Expected closing '}");
            String line = readLineOrEOF(in);
            if (line == null)
                break;
            String [] parts = line.split("=");
            if (parts.length < 2)
                throw new Exception("Line '" + line + "' not of form 'name=value'");
            String name = parts[0].trim();
            for (Field field : values.keySet()) {
                if (field.getName().equals(name)) {
                    Archiver archiver = values.get(field);
                    archiver.set(field, parts[1], this);
                    if (field.get(Reflector.this) instanceof Reflector) {
                        Reflector<T> ref = (Reflector<T>)field.get(Reflector.this);
                        ref.deserialize(in);
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
                    } else if (isSubclassOf(field.getType(), Map.class)) {
                    	Map<?,?> map = (Map<?,?>)field.get(this);
                    	if (map != null) 
                    		deserializeMap(map, in);
                    }
                    parts = null;
                    break;
                }
            }
            if (parts != null) {
                if (ENABLE_THROW_ON_UNKNOWN)
                    throw new Exception("Unknown field: " + name + " not in fields: " + values.keySet());
                System.err.println("Unknown field: " + name);// + " not in fields: " + values.keySet());
            }
        }
        if (version < getMinVersion()) {
            throw new VersionTooOldException(version, getMinVersion());
        }
    }
    
    private static boolean isArraysEqual(Object a, Object b) {
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
    
    private static boolean isEqual(Object a, Object b) {
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
    
    @SuppressWarnings("unchecked")
	@Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null)
            return false;
        try {
            Map<Field, Archiver> values = getValues(getClass(), false);
            Reflector<T> a = (Reflector<T>)o;
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
        	return super.equals(o);
        }
    }
    
    @Override
    public String toString() {
        StringWriter buf = new StringWriter();
//        buf.append(super.toString());
        try {
            serialize(new MyPrintWriter(buf));
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
            if (o instanceof Reflector)
                return ((Reflector)o).deepCopy();
            
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
            if (o instanceof Map) {
            	Map map = (Map)o;
            	Map newMap = (Map)o.getClass().newInstance();
            	Iterator it = map.entrySet().iterator();
            	while (it.hasNext()) {
            		Map.Entry entry = (Map.Entry)it.next();
            		newMap.put(deepCopy(entry.getKey()), deepCopy(entry.getValue()));
            	}
            }
            // TODO: Test that this is a primitive, enum otherwise error
            // Hopefully this is a primitive, enum 
            //System.err.println("Dont know how to deepCopy: " + o.getClass() + ": " + o);
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
    public final T deepCopy() {
        try {
            T a = (T)getClass().newInstance();
            Map<Field, Archiver> values = getValues(getClass(), false);
            
            for (Field f: values.keySet()) {
            	f.setAccessible(true);
                f.set(a, deepCopy(f.get(this)));
            }
            
            return a;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deep copy", e);
        }
    }

    /**
     * 
     * @param other
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public final void copyFrom(Reflector<T> other) {
    	try {
            Map<Field, Archiver> values = getValues(getClass(), false);
            
            for (Field f: values.keySet()) {
            	f.setAccessible(true);
            	Object o = f.get(this);
            	if (o != null && o instanceof Reflector) {
            		((Reflector<T>)o).copyFrom((Reflector<T>)f.get(other));
            	} else {
            		f.set(this, deepCopy(f.get(other)));
            	}
            }
    		
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
    public final void saveToFile(File file) throws IOException {
        Utils.println("saving to file %s", file.getAbsolutePath());
        try (FileOutputStream out = new FileOutputStream(file)) {
            serialize(out);
        }
    }

    /**
     * Convenience method
     * 
     * @param file
     * @throws IOException
     */
    public final void loadFromFile(File file) throws IOException {
        Utils.println("Loading from file %s", file.getAbsolutePath());
        try (InputStream in = new FileInputStream(file)) {
            Reflector r = getClass().newInstance();
            r.deserialize(in);
            copyFrom(r);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IOException("Cannot create temp object to deserialize into", e);
        }
    }
}
