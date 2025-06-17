package cc.lib.kreflector

import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.utils.FileUtils
import cc.lib.utils.GException
import cc.lib.utils.Grid
import cc.lib.utils.SomeEnum
import junit.framework.TestCase
import org.junit.Assert
import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import java.util.*

class KReflectorTest : TestCase() {
	@Throws(Exception::class)
	override fun setUp() {
		super.setUp()
		Utils.setDebugEnabled()
		println("Start Test: $name")
		println("--------------------------------------------------------")
	}

	@Throws(Exception::class)
	fun testLists() {
		val r = ListKReflector()
		println(r.toString())
		r.intList.add(2)
		println(r.toString())
		val r2 = ListKReflector()
		r2.deserialize(r.toString())
		println(r2.toString())
		assertEquals(1, r2.intList.size)
		r2.intList.addAll(Arrays.asList(1, 2, 3))
		println(r2.toString())
		r2.deserialize(r.toString())
		println(r2.toString())
		assertEquals(1, r2.intList.size)
		r.deserialize(r2.toString())
		assertTrue(r.deepEquals(r2))
	}

	@Throws(Exception::class)
	fun testAllNull() {
		val t = MyArchivable()
		val data = t.toString()
		println(t.toStringNumbered())
		val s = MyArchivable()
		s.deserialize(data)
		assertEquals(t, s)
		val u = t.deepCopy()
		assertEquals(u, t)
		assertEquals(u, s)
	}

	@Throws(Exception::class)
	fun testSerializeRawObject() {
		val str = "this is a test string for the purpose of NOT!!!"
		val writer = StringWriter()
		cc.lib.reflector.Reflector.serializeObject(str, PrintWriter(writer))
		println(writer.buffer.toString())
		val reader =
			cc.lib.reflector.Reflector.deserializeObject<String>(BufferedReader(StringReader(writer.buffer.toString())))
		assertEquals(reader, str)
		writer.buffer.setLength(0)
		val r: KReflector<*> = SmallKReflector()
		KReflector.serializeObject(r, PrintWriter(writer))
		println(writer.buffer.toString())
		val rin = KReflector.deserializeObject<KReflector<*>>(BufferedReader(StringReader(writer.buffer.toString())))
		assertEquals(rin, r)
		writer.buffer.setLength(0)
		val m: MutableMap<String, Int> = HashMap()
		m["hello"] = 1
		m["goodbye"] = 2
		KReflector.serializeObject(m, PrintWriter(writer))
		println(writer.buffer.toString())
		val min = KReflector.deserializeObject<Map<*, *>>(BufferedReader(StringReader(writer.buffer.toString())))!!
		assertTrue(m.size == min.size)
		for (s in m.keys) {
			assertEquals(m[s], min[s])
		}
		val e = SomeEnum.ENUM1
		var eout = KReflector.serializeObject(e)!!
		println(eout)
		val e2 = KReflector.deserializeFromString<SomeEnum>(eout)
		assertEquals(e, e2)
		val el = arrayOf(
			SomeEnum.ENUM2,
			SomeEnum.ENUM3
		)
		eout = KReflector.serializeObject(el)!!
		println(eout)
		val el2 = KReflector.deserializeFromString<Array<SomeEnum>>(eout)
		assertTrue(Arrays.equals(el, el2))
	}

	@Throws(Exception::class)
	fun testSerializeStringArray() {
		val arr = arrayOf(
			"a", "b", "c"
		)
		val str = StringWriter()
		KReflector.serializeObject(arr, PrintWriter(str))
		println(str.buffer.toString())
		val arr2 = KReflector.deserializeObject<Array<String>>(BufferedReader(StringReader(str.buffer.toString())))
		assertTrue(Arrays.equals(arr, arr2))
	}

	@Throws(Exception::class)
	fun testSerializeStringArray2D() {
		val arr = arrayOf(arrayOf("a", "b", "c"), arrayOf("d", "e", "f"))
		val str = StringWriter()
		KReflector.serializeObject(arr, PrintWriter(str))
		println(str.buffer.toString())
		val arr2 = KReflector.deserializeObject<Array<Array<String>>>(BufferedReader(StringReader(str.buffer.toString())))!!
		println("Deserialized array:")
		KReflector.serializeObject(arr2, PrintWriter(System.out))
		assertTrue(arr.size == arr2.size)
		for (i in arr.indices) assertTrue(Arrays.equals(arr[i], arr2[i]))
	}

	@Throws(Exception::class)
	fun testArrays() {
		val t = MyArchivable()
		t.my2DIntArray = arrayOf(intArrayOf(10, 20), intArrayOf(30, 40), intArrayOf(50, 60, 70))
		t.my3DDoubleArray =
			arrayOf(arrayOf(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0), doubleArrayOf(6.0, 7.0, 8.0, 9.0, 10.0)), arrayOf(doubleArrayOf(10.0, 20.0, 30.0), doubleArrayOf(40.0, 50.0, 60.0)))
		t.myIntArray = intArrayOf(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3)
		t.myStringArray = arrayOf("Hello", "goodbye", "so long")
		val data = t.toString()
		println(data)
		val s = MyArchivable()
		val reader = BufferedReader(StringReader(data))
		s.deserialize(reader)
		assertEquals(t, s)
		val u = t.deepCopy()
		assertEquals(u, t)
		assertEquals(u, s)
	}

	@Throws(Exception::class)
	fun testCollections() {
		val t = MyArchivable()
		t.myList = ArrayList()
		t.myList.addAll(Arrays.asList(*arrayOf("a", "B", "c", "dddd")))
		t.myIntList = LinkedList<Int>().also {
			it.addAll(Arrays.asList(*arrayOf(10, 20, 30, 40)))
		}
		t.myStringSet = TreeSet<String>().also {
			it.addAll(Arrays.asList(*arrayOf("zzz", "bbb", "qqq")))
		}
		val data = t.toString()
		println(t.toStringNumbered())
		val s = MyArchivable()
		val reader = BufferedReader(StringReader(data))
		s.deserialize(reader)
		assertEquals(t, s)
		val u = t.deepCopy()
		assertEquals(u, t)
		assertEquals(u, s)
	}

	@Throws(Exception::class)
	fun testArraysOfCollections() {
		val t = MyArchivable()
		t.collectionArray = arrayOfNulls(4)
		t.collectionArray[0] = LinkedList<Any?>().also {
			it.addAll(listOf(0, 1, 2))
		}
		t.collectionArray[2] = HashSet<Any?>().also {
			it.addAll(Arrays.asList(*arrayOf("Hello", "Goodbye")))
		}
		t.collectionArray[3] = ArrayList<Any?>()
		val data = t.toString()
		println(data)
		val s = MyArchivable()
		s.deserialize(data)
		assertEquals(t, s)
		val u = t.deepCopy()
		assertEquals(u, t)
		assertEquals(u, s)
		println(data)
	}

	fun x() {
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
        / *
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

	open class BaseClass : KReflector<BaseClass?>() {
		var baseInt = 0

		companion object {
			init {
				addField(BaseClass::class.java, "baseInt")
			}
		}
	}

	class SuperClass : BaseClass() {
		var superString: String? = "howdy"

		companion object {
			init {
				addField(SuperClass::class.java, "superString")
			}
		}
	}

	@Throws(Exception::class)
	fun testInheritedFields() {
		val s = SuperClass()
		println(s.toString())
		val data = "baseInt=10\nsuperString=\"shutup\""
		s.deserialize(BufferedReader(StringReader(data)))
		assertEquals(s.superString, "shutup")
		assertEquals(s.baseInt, 10)
	}

	class UsesOS : KReflector<UsesOS?>() {
		var obj = OverridesSerialize()
		var objArr = arrayOfNulls<OverridesSerialize>(3)
		var objList: MutableCollection<OverridesSerialize> = LinkedList()
		fun init() {
			obj = OverridesSerialize("hello", 1)
			objArr[0] = OverridesSerialize("goodbye", 2)
			objArr[2] = OverridesSerialize("solong", 4)
			objList.add(OverridesSerialize("entry1", 100))
			objList.add(OverridesSerialize("entry2", 200))
		}

		companion object {
			init {
				addAllFields(UsesOS::class.java)
			}
		}
	}

	@Throws(Exception::class)
	fun testOverridingSerialize() {
		val uos = UsesOS()
		uos.init()
		val text = uos.toString()
		println(text)
		val uos2 = UsesOS()
		uos2.deserialize(text)
		println(uos2)
		assertEquals(uos, uos2)
	}

	class MyArchivableX : MyArchivable() {
		init {
			addAllFields(MyArchivableX::class.java)
		}
	}

	@Throws(Exception::class)
	fun testDerivedReflectors() {
		val a = MyArchivable()
		a.myArchivable = MyArchivableX()
		var txt = a.toString()
		a.deserialize(txt)
		a.myArchivableArray = arrayOf(
			MyArchivableX(),
			MyArchivableX(),
			MyArchivable()
		)
		txt = a.toString()
		a.deserialize(txt)
		a.my2DArchivableArray = arrayOf(
			arrayOf(MyArchivableX(), MyArchivableX()),
			arrayOf(MyArchivableX(), MyArchivableX())
		)
		txt = a.toString()
		a.deserialize(txt)
		a.myCollection = ArrayList<Any?>().also {
			it.add(MyArchivableX())
			it.add(MyArchivableX())
		}
		txt = a.toString()
		println(txt)
		a.deserialize(txt)
		a.collectionArray = arrayOf(
			ArrayList<Any?>().also {
				it.add(MyArchivableX())
				it.add(MyArchivableX())
			},
			HashSet<Any?>().also {
				it.add(MyArchivableX())
				it.add(MyArchivableX())
			}
		)
		txt = a.toString()
		a.deserialize(txt)
	}

	@Throws(Exception::class)
	fun testCollectionArray2() {
		val a = MyArchivable()
		a.collectionArray = arrayOf(
			ArrayList<Any?>(Arrays.asList<MyArchivableX?>(MyArchivableX(), MyArchivableX())),
			HashSet<Any?>(Arrays.asList<MyArchivableX?>(MyArchivableX(), MyArchivableX())))
		val s = a.toString()
		println(a.toStringNumbered())
		val a2 = MyArchivable()
		a2.deserialize(s)
		assertTrue(a.deepEquals(a2))
	}

	@Throws(Exception::class)
	fun testCollectionArray() {
		val c = arrayOf<Collection<*>>(
			ArrayList<Any?>(Arrays.asList("hello", "goodbye")),
			ArrayList<Any?>(Arrays.asList(4, 8, 1, 0)))
		val s = KReflector.serializeObject(c)!!
		println(s)
		val c2 = KReflector.deserializeFromString<Array<Collection<*>>>(s)!!
		assertEquals(c.size, c2.size)
		assertEquals(c[0], c2[0])
		assertEquals(c[1], c2[1])
		assertTrue(KReflector.isEqual(c, c2))
	}

	class MyCollection : KReflector<MyCollection?>() {
		var list: MutableList<SmallKReflector>? = null //new ArrayList<SmallReflector>();
		var other: List<SmallKReflector> = ArrayList<SmallKReflector>()
		fun build() {
			list = ArrayList<SmallKReflector>()
			for (i in 0..9) {
				list!!.add(SmallKReflector())
			}
		}

		companion object {
			init {
				addAllFields(MyCollection::class.java)
			}
		}
	}

	@Throws(Exception::class)
	fun testCollection() {
		val c = MyCollection()
		//c.build();
		val data = c.toString()
		println(data)
		val reader = BufferedReader(StringReader(data))
		val c2 = MyCollection()
		c2.deserialize(reader)
		println(c2.toString())
		assertEquals(c, c2)
	}

	@Throws(Exception::class)
	fun testCollectionOfArrays() {
		val s = TestListOfArrays().toString()
		println(s)
		val t = TestListOfArrays()
		t.deserialize(s)
		println(t)
	}

	class TestListOfArrays : KReflector<TestListOfArrays?>() {
		var objects: MutableList<Array<SmallKReflector>> = ArrayList<Array<SmallKReflector>>()
		var objects2: MutableList<Array<Array<SmallKReflector>>> = ArrayList<Array<Array<SmallKReflector>>>()

		init {
			val arr: Array<SmallKReflector> = arrayOf<SmallKReflector>(
				SmallKReflector())
			objects.add(arr)
			val arr2: Array<Array<SmallKReflector>> = arrayOf<Array<SmallKReflector>>(
				arr,
				arr)
			objects2.add(arr2)
		}

		companion object {
			init {
				addAllFields(TestListOfArrays::class.java)
			}
		}
	}

	@Throws(Exception::class)
	fun testSerializeSpecialChars() {
		val str = "\n\t!@#$%^&*()-_=+[]{};':\",.<>/?"
		val out = KReflector.serializeObject(str)!!
		println(out)
		val reader = KReflector.deserializeFromString<String>(out)
		assertEquals(str, reader)
	}

	// NOPE, cannot handle
	// NOPE, cannot handle...too difficult, would require each element be surronded with a type descriptor. This would be hard
	//  to make work everywhere
	@Throws(Exception::class)
	fun xtestSerializeObjectArray() {
		val arr = arrayOf(
			"hello",
			15,
			BaseClass()
		)
		val out = StringWriter()
		KReflector.serializeObject(arr, PrintWriter(out))
		println(out.buffer.toString())
	}

	@Throws(Exception::class)
	fun testArraysOfNullableInts() {
		val a = arrayOfNulls<Int>(10)
		val b = arrayOf(1, 2, 3, 4, null, 6, 7)
		println("a=" + KReflector.serializeObject(a))
		println("b=" + KReflector.serializeObject(b))
		val x = KReflector.deserializeFromString<Array<Int>>(KReflector.serializeObject(b)!!)!!
		println("x=" + KReflector.serializeObject(x))
		val l: MutableList<*> = ArrayList<Any?>().also {
			it.addAll(Arrays.asList(1, 2, 3, 4))
		}
		println("l=" + KReflector.serializeObject(l))
	}

	@Throws(Exception::class)
	fun testDiff() {
		val a = BaseClass()
		val b = BaseClass()
		val c = SuperClass()
		a.baseInt = 10
		b.baseInt = 10
		c.baseInt = 5
		c.superString = null
		a.baseInt = 10
		b.baseInt = 11
		a.merge(b.toString())
		assertEquals(a.baseInt, 11)
	}

	@Throws(Exception::class)
	fun testDeserializeEnclosedEnum() {
		val a = SimpleObject()
		a.myEnum2 = SimpleObject.MyEnum.A
		val s = a.toString()
		println("s=$s")
		val b = SimpleObject()
		b.deserialize(s)
	}

	internal class SimpleObject : KReflector<SimpleObject?>() {
		enum class MyEnum {
			A,
			B,
			C
		}

		var myInt = 10
		var myFloat = 2.5f
		var myLong = 24738504127385701L
		var myDouble = 0.23498752083475
		var myEnum: SomeEnum? = null
		var myEnumArray: Array<SomeEnum?>? = null
		var myEnum2: MyEnum? = null
		var myIntList: List<Int>? = null
		var myObj: SimpleObject? = null
		var myObjArray: Array<SimpleObject>? = null
		var myIntArray: IntArray? = null
		var myObjList: Vector<SimpleObject>? = null
		var myColorList: List<GColor>? = null
		var myStrStrMap: Map<String, String>? = null
		var myEnumMap: Map<MyEnum, MyEnum>? = null
		var myEnumList: List<MyEnum>? = null

		companion object {
			init {
				addAllFields(SimpleObject::class.java)
				registerClass(MyEnum::class.java)
			}
		}
	}

	@Throws(Exception::class)
	fun testMaps() {
		val map: MutableMap<Int, Int> = HashMap()
		var serialized = KReflector.serializeObject(map)!!
		var map2 = KReflector.deserializeFromString<Map<Int?, Int>>(serialized)!!
		assertEquals(0, map2.size)
		map[0] = 1
		serialized = KReflector.serializeObject(map)!!
		map2 = KReflector.deserializeFromString(serialized)!!
		assertEquals(1, map2.size)
		assertEquals(1, map2[0]!!.toInt())
		map[1] = 2
		serialized = KReflector.serializeObject(map)!!
		map2 = KReflector.deserializeFromString(serialized)!!
		assertEquals(2, map2.size)
		assertEquals(1, map2[0]!!.toInt())
		assertEquals(2, map2[1]!!.toInt())
	}

	internal class Generic<T> : KReflector<Generic<T>>() {
		var array1d: Array<T>? = null
		var array2d: Array<Array<T>>? = null
	}

	fun testShouldFail() {
		val g = Generic<Int?>()
		g.array1d = arrayOfNulls(1)
		g.array2d = Array(1) { arrayOfNulls(1) }
		try {
			KReflector.addAllFields(Generic::class.java)
			fail()
		} catch (e: GException) {
		}
	}

	@Throws(Exception::class)
	fun testArrayOrEnums() {
		val e = arrayOf(
			SomeEnum.ENUM3,
			SomeEnum.ENUM1
		)
		val s = KReflector.serializeObject(e)!!
		println(s)
		val e2 = KReflector.deserializeFromString<Array<SomeEnum>>(s)
		println(Arrays.toString(e2))
		Assert.assertArrayEquals(e, e2)
	}

	@Throws(Exception::class)
	fun testEnumList() {
		val l: MutableList<SimpleObject.MyEnum> = ArrayList()
		l.add(SimpleObject.MyEnum.A)
		l.add(SimpleObject.MyEnum.A)
		KReflector.registerClass(SimpleObject.MyEnum::class.java)
		val result = KReflector.serializeObject(l)!!
		println("result = $l")
		val ll = KReflector.deserializeFromString<List<SimpleObject.MyEnum>>(result)
		println("ll = $ll")
	}

	@Throws(Exception::class)
	fun testEnumMap() {
		KReflector.registerClass(SimpleObject.MyEnum::class.java)
		val m: MutableMap<SimpleObject.MyEnum, SimpleObject.MyEnum?> = HashMap()
		m[SimpleObject.MyEnum.A] = SimpleObject.MyEnum.B
		m[SimpleObject.MyEnum.B] = SimpleObject.MyEnum.A
		m[SimpleObject.MyEnum.C] = null
		val result = KReflector.serializeObject(m)!!
		println(result)
		val mm = KReflector.deserializeFromString<Map<*, *>>(result)!!
		assertEquals(mm[SimpleObject.MyEnum.A], SimpleObject.MyEnum.B)
		assertEquals(mm[SimpleObject.MyEnum.B], SimpleObject.MyEnum.A)
		assertNull(mm[SimpleObject.MyEnum.C])
		println(mm)
	}

	@Throws(Exception::class)
	fun testEnumAsField() {
		val r = SmallKReflector()
		System.out.println(r.toStringNumbered())
	}

	@Throws(Exception::class)
	fun testSerializeCollectionsList() {
		//Reflector.registerClass(Collections.synchronizedList(new ArrayList()).getClass(), "java.util.Collections.SynchronizedRandomAccessList");
		//Reflector.registerConstructor("java.util.Collections.SynchronizedRandomAccessList", () -> Collections.synchronizedList(new ArrayList()));
		var list = Utils.toList(
			Utils.toList("Hello", "Goodbye"),
			Utils.toList("So Long", "Farewell"))
		val str = KReflector.serializeObject(list)!!
		println("str=$str")
		list = KReflector.deserializeFromString(str)
	}

	@Throws(Exception::class)
	fun testMergeArraysNull() {
		val a = SimpleObject()
		val b = SimpleObject()
		a.myEnumArray = arrayOfNulls(100)
		b.merge(a.toString())
		assertEquals(a, b)
		a.myEnumArray?.set(50, SomeEnum.ENUM1)
		b.merge(a.toString())
		assertEquals(a, b)
	}

	@Throws(Exception::class)
	fun testGrid() {
		val grid = Grid<String?>(2, 2)
		grid[0, 0] = "Hello"
		grid[1, 1] = "Goodbye"
		println("grid=$grid")
		val copy = grid.deepCopy()
		grid[0, 1] = "Whatev"
		grid[0, 0] = null
		println("grid=$grid")
		copy.merge(grid.toString())
		println("grid=$grid")
		println("copy=$copy")
		assertEquals(grid, copy)
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
	@Throws(Exception::class)
	fun testGColor() {
		println(GColor.BLUE.toStringNumbered())
	}

	@Throws(Exception::class)
	fun testEnumCollections() {
		val a = SimpleObject()
		a.myEnumList = Utils.toList(SimpleObject.MyEnum.A, SimpleObject.MyEnum.B, SimpleObject.MyEnum.C)
		var str = a.toString()
		println("a=$str")
		val arr = arrayOf(
			a, a
		)
		val b = SimpleObject()
		b.deserialize(str)
		assertEquals(a, b)
		assertEquals(a.checksum, b.checksum)
		str = KReflector.serializeObject(arr)!!
		println("a=$str")
		KReflector.dump()
		val barr = KReflector.deserializeFromString<Array<SimpleObject>>(str)
		assertTrue(Arrays.equals(arr, barr))
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
	@Throws(Exception::class)
	fun testLoadFromNewProcess() {
		val a = SimpleObject()
		val b = SimpleObject()
		a.myObjList = Vector()
		a.myObjList!!.add(b)
		if (KReflector.STRIP_PACKAGE_QUALIFIER) a.deserialize(FileUtils.openFileOrResource("testLoadFromNewProcess.txt"))
		/*
        b.myEnumArray = Utils.toArray(SomeEnum.ENUM1, SomeEnum.ENUM2, SomeEnum.ENUM3);
        String str = a.toString();
        System.out.println(str);
        a.saveToFile(new File("testResources/testLoadFromNewProcess.txt"));
         */
	}

	@Throws(Exception::class)
	fun testSerializeArrays() {
		val arr = arrayOf(4)
		val strx = KReflector.serializeObject(arr)!!
		println(strx)
		val arr2: Array<Int> = KReflector.deserializeFromString(strx)!!
		assertTrue(Arrays.equals(arr, arr2))
	}
}