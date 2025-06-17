package cc.lib.reflector;

import junit.framework.TestCase;

import org.junit.Assert;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.FileUtils;
import cc.lib.utils.GException;
import cc.lib.utils.Grid;
import cc.lib.utils.SomeEnum;

import static java.lang.System.out;

public class KReflectorTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Utils.setDebugEnabled();
        out.println("Start Test: " + getName());
        out.println("--------------------------------------------------------");
    }

    public void testLists() throws Exception {
        ListReflector r = new ListReflector();
        out.println(r.toString());
        r.intList.add(2);
        out.println(r.toString());
        ListReflector r2 = new ListReflector();
        r2.deserialize(r.toString());
        out.println(r2.toString());
        assertEquals(1, r2.intList.size());
        r2.intList.addAll(Arrays.asList(1, 2, 3));
        out.println(r2.toString());
        r2.deserialize(r.toString());
        out.println(r2.toString());
        assertEquals(1, r2.intList.size());
        r.deserialize(r2.toString());
        assertTrue(r.deepEquals(r2));
    }

    public void testAllNull() throws Exception {
        MyArchivable t = new MyArchivable();
        String data = t.toString();
        System.out.println(t.toStringNumbered());
        MyArchivable s = new MyArchivable();
        s.deserialize(data);
        assertEquals(t, s);
        MyArchivable u = t.deepCopy();
        assertEquals(u, t);
        assertEquals(u, s);
    }

    public void testSerializeRawObject() throws Exception {
        String str = "this is a test string for the purpose of NOT!!!";
        StringWriter writer = new StringWriter();
        Reflector.serializeObject(str, new PrintWriter(writer));
        System.out.println(writer.getBuffer().toString());

        String in = Reflector.deserializeObject(new BufferedReader(new StringReader(writer.getBuffer().toString())));
        assertEquals(in, str);

        writer.getBuffer().setLength(0);
        Reflector r = new SmallReflector();
        Reflector.serializeObject(r, new PrintWriter(writer));
        System.out.println(writer.getBuffer().toString());

        Reflector rin = Reflector.deserializeObject(new BufferedReader(new StringReader(writer.getBuffer().toString())));
        assertEquals(rin, r);

        writer.getBuffer().setLength(0);
        Map<String, Integer> m = new HashMap();
        m.put("hello", 1);
        m.put("goodbye", 2);
        Reflector.serializeObject(m, new PrintWriter(writer));
        System.out.println(writer.getBuffer().toString());

        Map min = Reflector.deserializeObject(new BufferedReader(new StringReader(writer.getBuffer().toString())));

        assertTrue(m.size() == min.size());
        for (String s : m.keySet()) {
            assertEquals(m.get(s), min.get(s));
        }

        SomeEnum e = SomeEnum.ENUM1;
        String eout = Reflector.serializeObject(e);
        System.out.println(eout);
        SomeEnum e2 = Reflector.deserializeFromString(eout);
        assertEquals(e, e2);

        SomeEnum[] el = {
                SomeEnum.ENUM2,
                SomeEnum.ENUM3
        };
        eout = Reflector.serializeObject(el);
        System.out.println(eout);
        SomeEnum[] el2 = Reflector.deserializeFromString(eout);
        assertTrue(Arrays.equals(el, el2));
    }

    public void testSerializeStringArray() throws Exception {
        String[] arr = {
                "a", "b", "c"
        };

        StringWriter str = new StringWriter();
        Reflector.serializeObject(arr, new PrintWriter(str));
        System.out.println(str.getBuffer().toString());

        String[] arr2 = Reflector.deserializeObject(new BufferedReader(new StringReader(str.getBuffer().toString())));
        assertTrue(Arrays.equals(arr, arr2));
    }

    public void testSerializeStringArray2D() throws Exception {
        String[][] arr = {
                {"a", "b", "c"},
                {"d", "e", "f"}
        };

        StringWriter str = new StringWriter();
        Reflector.serializeObject(arr, new PrintWriter(str));
        System.out.println(str.getBuffer().toString());

        String[][] arr2 = Reflector.deserializeObject(new BufferedReader(new StringReader(str.getBuffer().toString())));
        System.out.println("Deserialized array:");
        Reflector.serializeObject(arr2, new PrintWriter(System.out));

        assertTrue(arr.length == arr2.length);
        for (int i = 0; i < arr.length; i++)
            assertTrue(Arrays.equals(arr[i], arr2[i]));
    }


    public void testArrays() throws Exception {
        MyArchivable t = new MyArchivable();
        t.my2DIntArray = new int[][]{
                new int[]{10, 20},
                new int[]{30, 40},
                new int[]{50, 60, 70},
        };
        t.my3DDoubleArray = new double[][][]{
                new double[][]{
                        new double[]{1, 2, 3, 4, 5},
                        new double[]{6, 7, 8, 9, 10}
                },
                new double[][]{
                        new double[]{10, 20, 30},
                        new double[]{40, 50, 60}
                }
        };
        t.myIntArray = new int[]{1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3};
        t.myStringArray = new String[]{"Hello", "goodbye", "so long"};
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
        t.myList.addAll(Arrays.asList(new String[]{"a", "B", "c", "dddd"}));
        t.myIntList = new LinkedList();
        t.myIntList.addAll(Arrays.asList(new Integer[]{10, 20, 30, 40}));
        t.myStringSet = new TreeSet();
        t.myStringSet.addAll(Arrays.asList(new String[]{"zzz", "bbb", "qqq"}));
        String data = t.toString();
        System.out.println(t.toStringNumbered());
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
        t.collectionArray[0].addAll(Arrays.asList(new Integer[]{0, 1, 2}));
        t.collectionArray[2].addAll(Arrays.asList(new String[]{"Hello", "Goodbye"}));
        String data = t.toString();
        System.out.println(data);
        MyArchivable s = new MyArchivable();
        s.deserialize(data);
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

    public static class BaseClass extends Reflector<BaseClass> {
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
        OverridesSerialize[] objArr = new OverridesSerialize[3];
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

    public static class MyArchivableX extends MyArchivable {

        static {
            addAllFields(MyArchivableX.class);
        }

        public MyArchivableX() {
        }
    }

    public void testDerivedReflectors() throws Exception {

        MyArchivable a = new MyArchivable();

        a.myArchivable = new MyArchivableX();

        String txt = a.toString();

        a.deserialize(txt);

        a.myArchivableArray = new MyArchivable[]{
                new MyArchivableX(),
                new MyArchivableX(),
                new MyArchivable()
        };

        txt = a.toString();

        a.deserialize(txt);

        a.my2DArchivableArray = new MyArchivable[][]{
                {new MyArchivableX(), new MyArchivableX()},
                {new MyArchivableX(), new MyArchivableX()},
        };

        txt = a.toString();

        a.deserialize(txt);

        a.myCollection = new ArrayList();
        a.myCollection.add(new MyArchivableX());
        a.myCollection.add(new MyArchivableX());

        txt = a.toString();
        out.println(txt);
        a.deserialize(txt);

        a.collectionArray = new Collection[]{
                new ArrayList(),
                new HashSet()
        };

        a.collectionArray[0].add(new MyArchivableX());
        a.collectionArray[0].add(new MyArchivableX());
        a.collectionArray[1].add(new MyArchivableX());
        a.collectionArray[1].add(new MyArchivableX());

        txt = a.toString();
        a.deserialize(txt);

    }

    public void testCollectionArray2() throws Exception {
        MyArchivable a = new MyArchivable();
        a.collectionArray = new Collection[]{
                new ArrayList(Arrays.asList(new MyArchivableX(), new MyArchivableX())),
                new HashSet(Arrays.asList(new MyArchivableX(), new MyArchivableX())),
        };
        String s = a.toString();
        out.println(a.toStringNumbered());
        MyArchivable a2 = new MyArchivable();
        a2.deserialize(s);


        assertTrue(a.deepEquals(a2));
    }

    public void testCollectionArray() throws Exception {
        Collection[] c = new Collection[]{
                new ArrayList(Arrays.asList("hello", "goodbye")),
                new ArrayList(Arrays.asList(4, 8, 1, 0)),
        };
        String s = Reflector.serializeObject(c);
        out.println(s);
        Collection[] c2 = Reflector.deserializeFromString(s);

        assertEquals(c.length, c2.length);
        assertEquals(c[0], c2[0]);
        assertEquals(c[1], c2[1]);

        assertTrue(Reflector.isEqual(c, c2));
    }

    public static class MyCollection extends Reflector<MyCollection> {

        static {
            addAllFields(MyCollection.class);
        }

        List<SmallReflector> list = null;//new ArrayList<SmallReflector>();
        List<SmallReflector> other = new ArrayList<SmallReflector>();

        void build() {
            list = new ArrayList<SmallReflector>();
            for (int i = 0; i < 10; i++) {
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
            SmallReflector[] arr = {
                    new SmallReflector(),
            };
            objects.add(arr);

            SmallReflector[][] arr2 = {
                    arr,
                    arr,
            };
            objects2.add(arr2);
        }
    }

    public void testSerializeSpecialChars() throws Exception {
        String str = "\n\t!@#$%^&*()-_=+[]{};':\",.<>/?";

        String out = Reflector.serializeObject(str);
        System.out.println(out);

        String in = Reflector.deserializeFromString(out);

        assertEquals(str, in);
    }

    // NOPE, cannot handle
    // NOPE, cannot handle...too difficult, would require each element be surronded with a type descriptor. This would be hard
    //  to make work everywhere
    public void xtestSerializeObjectArray() throws Exception {
        Object[] arr = {
                "hello",
                15,
                new BaseClass()
        };

        StringWriter out = new StringWriter();
        Reflector.serializeObject(arr, new PrintWriter(out));
        System.out.println(out.getBuffer().toString());
    }

    public void testArraysOfNullableInts() throws Exception {

        Integer[] a = new Integer[10];

        Integer[] b = new Integer[]{1, 2, 3, 4, null, 6, 7};

        System.out.println("a=" + Reflector.serializeObject(a));
        System.out.println("b=" + Reflector.serializeObject(b));

        Integer[] x = Reflector.deserializeFromString(Reflector.serializeObject(b));
        System.out.println("x=" + Reflector.serializeObject(x));

        List l = new ArrayList();
        l.addAll(Arrays.asList(1, 2, 3, 4));
        System.out.println("l=" + Reflector.serializeObject(l));
    }

    public void testDiff() throws Exception {
        BaseClass a = new BaseClass();
        BaseClass b = new BaseClass();
        SuperClass c = new SuperClass();

        a.baseInt = 10;
        b.baseInt = 10;
        c.baseInt = 5;
        c.superString = null;

        a.baseInt = 10;
        b.baseInt = 11;

        a.merge(b.toString());
        assertEquals(a.baseInt, 11);
    }

    public void testDeserializeEnclosedEnum() throws Exception {

        SimpleObject a = new SimpleObject();
        a.myEnum2 = SimpleObject.MyEnum.A;
        String s = a.toString();
        System.out.println("s=" + s);


        SimpleObject b = new SimpleObject();
        b.deserialize(s);
    }

    static class SimpleObject extends Reflector<SimpleObject> {

        public enum MyEnum {
            A, B, C
        }

        static {
            addAllFields(SimpleObject.class);
            registerClass(MyEnum.class);
        }

        int myInt = 10;
        float myFloat = 2.5f;
        long myLong = 24738504127385701L;
        double myDouble = 0.23498752083475;
        SomeEnum myEnum;
        SomeEnum[] myEnumArray;
        MyEnum myEnum2;
        List<Integer> myIntList = null;

        SimpleObject myObj = null;
        SimpleObject[] myObjArray = null;
        int[] myIntArray = null;

        Vector<SimpleObject> myObjList = null;
        List<GColor> myColorList = null;
        Map<String, String> myStrStrMap = null;

        Map<MyEnum, MyEnum> myEnumMap = null;
        List<MyEnum> myEnumList = null;
    }

    public void testMaps() throws Exception {

        Map<Integer, Integer> map = new HashMap<>();
        String serialized = Reflector.serializeObject(map);
        Map<Integer, Integer> map2 = Reflector.deserializeFromString(serialized);
        assertEquals(0, map2.size());
        map.put(0, 1);
        serialized = Reflector.serializeObject(map);
        map2 = Reflector.deserializeFromString(serialized);
        assertEquals(1, map2.size());
        assertEquals(1, map2.get(0).intValue());
        map.put(1, 2);
        serialized = Reflector.serializeObject(map);
        map2 = Reflector.deserializeFromString(serialized);
        assertEquals(2, map2.size());
        assertEquals(1, map2.get(0).intValue());
        assertEquals(2, map2.get(1).intValue());

    }

    static class Generic<T> extends Reflector<Generic<T>> {
        T[] array1d;
        T[][] array2d;
    }

    public void testShouldFail() {
        Generic<Integer> g = new Generic<Integer>();

        g.array1d = new Integer[1];
        g.array2d = new Integer[1][1];

        try {
            Reflector.addAllFields(Generic.class);
            fail();
        } catch (GException e) {
        }
    }

    public void testArrayOrEnums() throws Exception {

        SomeEnum[] e = {
                SomeEnum.ENUM3,
                SomeEnum.ENUM1
        };

        String s = Reflector.serializeObject(e);
        System.out.println(s);

        SomeEnum[] e2 = Reflector.deserializeFromString(s);

        System.out.println(Arrays.toString(e2));

        Assert.assertArrayEquals(e, e2);

    }

    public void testEnumList() throws Exception {

        List<SimpleObject.MyEnum> l = new ArrayList<>();
        l.add(SimpleObject.MyEnum.A);
        l.add(SimpleObject.MyEnum.A);

        Reflector.registerClass(SimpleObject.MyEnum.class);
        String result = Reflector.serializeObject(l);

        System.out.println("result = " + l);

        List<SimpleObject.MyEnum> ll = Reflector.deserializeFromString(result);

        System.out.println("ll = " + ll);
    }

    public void testEnumMap() throws Exception {

        Reflector.registerClass(SimpleObject.MyEnum.class);

        Map<SimpleObject.MyEnum, SimpleObject.MyEnum> m = new HashMap<>();

        m.put(SimpleObject.MyEnum.A, SimpleObject.MyEnum.B);
        m.put(SimpleObject.MyEnum.B, SimpleObject.MyEnum.A);
        m.put(SimpleObject.MyEnum.C, null);
        String result = Reflector.serializeObject(m);

        System.out.println(result);

        Map mm = Reflector.deserializeFromString(result);

        assertEquals(mm.get(SimpleObject.MyEnum.A), SimpleObject.MyEnum.B);
        assertEquals(mm.get(SimpleObject.MyEnum.B), SimpleObject.MyEnum.A);
        assertNull(mm.get(SimpleObject.MyEnum.C));

        System.out.println(mm);
    }

    public void testEnumAsField() throws Exception {

        SmallReflector r = new SmallReflector();
        System.out.println(r.toStringNumbered());

    }


    public void testSerializeCollectionsList() throws Exception {
        //Reflector.registerClass(Collections.synchronizedList(new ArrayList()).getClass(), "java.util.Collections.SynchronizedRandomAccessList");
        //Reflector.registerConstructor("java.util.Collections.SynchronizedRandomAccessList", () -> Collections.synchronizedList(new ArrayList()));

        List<List<String>> list = Utils.toList(
                Utils.toList("Hello", "Goodbye"),
                Utils.toList("So Long", "Farewell"));

        String str = Reflector.serializeObject(list);
        System.out.println("str=" + str);

        list = Reflector.deserializeFromString(str);
    }

    public void testMergeArraysNull() throws Exception {
        SimpleObject a = new SimpleObject();
        SimpleObject b = new SimpleObject();

        a.myEnumArray = new SomeEnum[100];
        b.merge(a.toString());
        assertEquals(a, b);

        a.myEnumArray[50] = SomeEnum.ENUM1;
        b.merge(a.toString());
        assertEquals(a, b);

    }

    public void testGrid() throws Exception {
        Grid<String> grid = new Grid<>(2, 2);

        grid.set(0, 0, "Hello");
        grid.set(1, 1, "Goodbye");

        System.out.println("grid=" + grid);

        Grid<String> copy = grid.deepCopy();
        grid.set(0, 1, "Whatev");
        grid.set(0, 0, null);
        System.out.println("grid=" + grid);

        copy.merge(grid.toString());
        System.out.println("grid=" + grid);
        System.out.println("copy=" + copy);
        assertEquals(grid, copy);

    }
/*
    public void testMergableVector() throws Exception {

        MergableVector<Integer> v = new MergableVector<>();
        MergableVector<Integer> copy = new MergableVector<>();

        String diff = copy.diff(v);
        System.out.println(diff);
        copy.merge(diff);
        assertEquals(v, copy);

        v.setSize(3);

        diff = copy.diff(v);
        System.out.println(diff);
        copy.merge(diff);
        assertEquals(v, copy);

        v.set(1, 100);
        diff = copy.diff(v);
        System.out.println(diff);
        copy.merge(diff);
        assertEquals(v, copy);

        copy.setSize(5);
        diff = copy.diff(v);
        System.out.println(diff);
        copy.merge(diff);
        assertEquals(v, copy);

        String str = copy.toString();
        MergableVector vv = new MergableVector();
        vv.deserialize(str);

        assertEquals(vv, copy);
    }*/

    public void testGColor() throws Exception {
        System.out.println(GColor.BLUE.toStringNumbered());
    }

    public void testEnumCollections() throws Exception {

        SimpleObject a = new SimpleObject();
        a.myEnumList = Utils.toList(SimpleObject.MyEnum.A, SimpleObject.MyEnum.B, SimpleObject.MyEnum.C);

        String str = a.toString();
        System.out.println("a=" + str);

        SimpleObject[] arr = {
                a, a
        };

        SimpleObject b = new SimpleObject();
        b.deserialize(str);

        assertEquals(a, b);
        assertEquals(a.getChecksum(), b.getChecksum());

        str = Reflector.serializeObject(arr);
        System.out.println("a=" + str);

        Reflector.dump();
        SimpleObject[] barr = Reflector.deserializeFromString(str);
        assertTrue(Arrays.equals(arr, barr));
    }
/*
    static class TestA extends Reflector<TestA> {

        static {
            addAllFields(TestA.class);
        }

        TestObject a = new TestObject();
        cc.lib.utils.pkgb.TestObject b = new cc.lib.utils.pkgb.TestObject();
    }

    public void testPkgCollision() throws Exception {

        // test that reflector objects of same name but different packages are handled
        TestA a = new TestA();
        a.a.x = "Hello X";
        a.b.y = "Goodbye Y";
        System.out.println("a=" + a.toString());

        TestA b = new TestA();
        b.deserialize(a.toString());
        System.out.println("b=" + a.toString());

        assertEquals(a, b);
        assertEquals(a.getChecksum(), b.getChecksum());
    }*/

    public void testLoadFromNewProcess() throws Exception {

        SimpleObject a = new SimpleObject();
        SimpleObject b = new SimpleObject();
        a.myObjList = new Vector<>();
        a.myObjList.add(b);
        if (Reflector.STRIP_PACKAGE_QUALIFIER)
            a.deserialize(FileUtils.openFileOrResource("testLoadFromNewProcess.txt"));
        /*
        b.myEnumArray = Utils.toArray(SomeEnum.ENUM1, SomeEnum.ENUM2, SomeEnum.ENUM3);
        String str = a.toString();
        System.out.println(str);
        a.saveToFile(new File("testResources/testLoadFromNewProcess.txt"));
         */

    }

    public void testSerializeArrays() throws Exception {
        Integer[] arr = {4};

        String strx = Reflector.serializeObject(arr);
        System.out.println(strx);

        Integer[] arr2 = Reflector.deserializeFromString(strx);

        assertTrue(Arrays.equals(arr, arr2));
    }
}
