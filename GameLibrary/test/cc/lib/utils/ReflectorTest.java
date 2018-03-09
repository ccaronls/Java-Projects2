package cc.lib.utils;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class ReflectorTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Reflector.KEEP_INSTANCES = false;
        System.out.println("Start Test: " + getName());
        System.out.println("--------------------------------------------------------");        
    }
    
    public void testAllNull() throws Exception {
        MyArchivable t = new MyArchivable();
        String data = t.toString();
        System.out.println(data);
        MyArchivable s = new MyArchivable();
        BufferedReader reader = new BufferedReader(new StringReader(data));
        s.deserialize(reader);
        assertEquals(t, s);
        MyArchivable u = t.deepCopy();
        assertEquals(u, t);
        assertEquals(u, s);
    }        
    
    public void testArrays() throws Exception {
        MyArchivable t = new MyArchivable();
        t.my2DIntArray = new int[][] {
                new int[] { 10, 20 },
                new int[] { 30, 40 },
                new int[] { 50, 60, 70 },
        };
        t.my3DDoubleArray = new double [][][] {
                new double [][] {
                        new double [] { 1,2,3,4,5 },
                        new double [] { 6,7,8,9,10 }
                },
                new double [][] {
                        new double [] { 10,20,30 },
                        new double [] { 40,50,60 }
                }
        };
        t.myIntArray = new int[] { 1,1,1,1,2,2,2,2,3,3,3,3 };
        t.myStringArray = new String[] { "Hello", "goodbye", "so long" };
        String data = t.toString();
        System.out.println(data);
        MyArchivable s = new MyArchivable();
        BufferedReader reader = new BufferedReader(new StringReader(data));
        s.deserialize(reader);
        assertEquals(t, s);
        MyArchivable u = t.deepCopy();
        assertEquals(u, t);
        assertEquals(u, s);
    }
    
    public void testCollections() throws Exception {
        MyArchivable t = new MyArchivable();
        t.myList = new ArrayList();
        t.myList.addAll(Arrays.asList(new String [] { "a", "B", "c", "dddd" }));
        t.myIntList = new LinkedList();
        t.myIntList.addAll(Arrays.asList(new Integer[] { 10,20,30,40 }));
        t.myStringSet = new TreeSet();
        t.myStringSet.addAll(Arrays.asList(new String [] { "zzz", "bbb", "qqq" }));
        String data = t.toString();
        System.out.println(data);
        MyArchivable s = new MyArchivable();
        BufferedReader reader = new BufferedReader(new StringReader(data));
        s.deserialize(reader);
        assertEquals(t, s);
        MyArchivable u = t.deepCopy();
        assertEquals(u, t);
        assertEquals(u, s);        
    }
    
    public void testArraysOfCollections() throws Exception {
        MyArchivable t = new MyArchivable();
        t.collectionArray = new Collection[4];
        t.collectionArray[0] = new LinkedList();
        t.collectionArray[2] = new HashSet();
        t.collectionArray[3] = new ArrayList();
        t.collectionArray[0].addAll(Arrays.asList(new Integer[] { 0,1,2 }));
        t.collectionArray[2].addAll(Arrays.asList(new String[] { "Hello", "Goodbye" } ));
        String data = t.toString();
        System.out.println(data);
        MyArchivable s = new MyArchivable();
        BufferedReader reader = new BufferedReader(new StringReader(data));
        s.deserialize(reader);
        assertEquals(t, s);
        MyArchivable u = t.deepCopy();
        assertEquals(u, t);
        assertEquals(u, s);        
        System.out.println(data);
    }
    
    void x() {
        /*
        t.myArchivable = new MyArchivable();
        t.myBoolean = true;
        t.myInt = 827;
        t.myString = "Howdy Partner!";
        t.myEnum = SomeEnum.ENUM3;
        t.myArchivableArray = new MyArchivable[3];
        t.myArchivableArray[0] = new MyArchivable();
        t.myArchivableArray[1] = new MyArchivable();
        
        String data = t.toString();
        System.out.println(data);

        String data2 = "myArchivable=cc.lib.utils.MyArchivable {\n"
                    + "myArchivable=null\n"
                    + "myBoolean=false\n"
                    + "myEnum=ENUM1\n"
                    + "myInt=0\n"
                    + "myString=null\n"
                    + "}\n"
                    + "myBoolean=true\n"
                    + "myEnum=ENUM3\n"
                    + "myInt=827\n"
                    + "myString='Howdy partner!'\n";  
        
        BufferedReader reader = new BufferedReader(new StringReader(data));
        MyArchivable s = new MyArchivable();
        s.deserialize(reader);
        PrintWriter writer = new PrintWriter(System.out);
        s.serialize(writer);
        writer .flush();
        writer.close();
        
        assertEquals(t.myBoolean, true);
        assertEquals(t.myEnum, SomeEnum.ENUM3);
        assertEquals(t.myInt, 827);
        assertEquals(t.myArchivable, new MyArchivable());
        assertEquals(s, t);
        
        MyArchivable copy = t.deepCopy();
        assertTrue(copy != t);
        /*
        assertTrue(copy.myBoolean == t.myBoolean);
        assertTrue(copy.myString == t.myString);
        assertTrue(copy.myString.equals(t.myString));
        assertTrue(copy.myEnum == t.myEnum);
        assertEquals(t, copy);
        
        t.myString += " wazzap";
        assertTrue(copy.myString != t.myString);
        assertFalse(copy.myString.equals(t.myString));
        */
    }
    
    public static class BaseClass extends Reflector {
        int baseInt = 0;
        
        static {
            addField(BaseClass.class, "baseInt");
        }
    }
    
    public static class SuperClass extends BaseClass {
        String superString = "howdy";
        
        static {
            addField(SuperClass.class, "superString");
        }
    }
    
    public void testInheritedFields() throws Exception {
        SuperClass s = new SuperClass();
        System.out.println(s.toString());
        
        String data = "baseInt=10\nsuperString=\"shutup\"";
        s.deserialize(new BufferedReader(new StringReader(data)));
        assertEquals(s.superString, "shutup");
        assertEquals(s.baseInt, 10);
    }
    
    public static class UsesOS extends Reflector<UsesOS> {
        static {
            addAllFields(UsesOS.class);
        }
        OverridesSerialize obj = new OverridesSerialize();
        OverridesSerialize [] objArr = new OverridesSerialize[3];
        Collection<OverridesSerialize> objList = new LinkedList<OverridesSerialize>();
        
        public UsesOS() {
        }
        
        void init() {
            obj = new OverridesSerialize("hello", 1);
            objArr[0] = new OverridesSerialize("goodbye", 2);
            objArr[2] = new OverridesSerialize("solong", 4);
            objList.add(new OverridesSerialize("entry1", 100));
            objList.add(new OverridesSerialize("entry2", 200));
        }
    }
    
    public void testOverridingSerialize() throws Exception {
        UsesOS uos = new UsesOS();
        uos.init();
        String text = uos.toString();
        System.out.println(text);
        UsesOS uos2 = new UsesOS();
        uos2.deserialize(text);
        System.out.println(uos2);
        assertEquals(uos, uos2);
    }

    class MyArchivableX extends MyArchivable {
        MyArchivableX() {}
    }

    public void testDerivedReflectors() throws Exception {

        Reflector.KEEP_INSTANCES = true;
        MyArchivable a = new MyArchivable();

        a.myArchivable = new MyArchivableX();

        String txt = a.toString();

        a.deserialize(txt);

        a.myArchivableArray = new MyArchivable[] {
                new MyArchivableX(),
                new MyArchivableX(),
                new MyArchivable()
        };

        txt = a.toString();

        a. deserialize(txt);

        a.my2DArchivableArray = new MyArchivable[][] {
                { new MyArchivableX(), new MyArchivableX() },
                { new MyArchivableX(), new MyArchivableX() },
        };

        txt = a.toString();

        a. deserialize(txt);

        a.myCollection = new ArrayList();
        a.myCollection.add(new MyArchivableX());
        a.myCollection.add(new MyArchivableX());

        txt = a.toString();
        a. deserialize(txt);

        a.collectionArray = new Collection[] {
            new ArrayList(),
            new HashSet()
        };

        a.collectionArray[0].add(new MyArchivableX());
        a.collectionArray[0].add(new MyArchivableX());
        a.collectionArray[1].add(new MyArchivableX());
        a.collectionArray[1].add(new MyArchivableX());

        txt = a.toString();
        a. deserialize(txt);

    }
    
    public static class MyCollection extends Reflector<MyCollection> {
    	
    	static {
    		addAllFields(MyCollection.class);
    	}
    	
    	List<SmallReflector> list = null;//new ArrayList<SmallReflector>();
    	List<SmallReflector> other = new ArrayList<SmallReflector>();
    	
    	void build() {
    		list = new ArrayList<SmallReflector>();
    		for (int i=0; i<10; i++) {
    			list.add(new SmallReflector());
    		}
    	}
    }
    
    public void testCollection() throws Exception {
    	MyCollection c = new MyCollection();
    	//c.build();
    	String data = c.toString();
        System.out.println(data);
        BufferedReader reader = new BufferedReader(new StringReader(data));
        MyCollection c2 = new MyCollection();
        c2.deserialize(reader);
        System.out.println(c2.toString());
        assertEquals(c, c2);
    }
    
    
    
    public void testCollectionOfArrays() throws Exception {
    	String s = new TestListOfArrays().toString();
    	System.out.println(s);
    	TestListOfArrays t = new TestListOfArrays();
    	t.deserialize(s);
    	System.out.println(t);
    }
    
    public static class TestListOfArrays extends Reflector<TestListOfArrays> {
    	static {
    		addAllFields(TestListOfArrays.class);
    	}
    	List<SmallReflector[]> objects = new ArrayList<SmallReflector[]>();
    	List<SmallReflector[][]> objects2 = new ArrayList<SmallReflector[][]>();
    	
    	public TestListOfArrays() {
    		SmallReflector [] arr = {
    			new SmallReflector(),
    		};
        	objects.add(arr);
        	
        	SmallReflector [][] arr2 = {
        			arr,
        			arr,
        	};
        	objects2.add(arr2);
    	}
    }
}
