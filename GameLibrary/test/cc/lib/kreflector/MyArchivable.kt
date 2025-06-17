package cc.lib.kreflector

import cc.lib.utils.SomeEnum
import java.util.LinkedList

open class MyArchivable : KReflector<MyArchivable>() {
	var myEnum = SomeEnum.ENUM1
	var myString: String? = null
	var myInt = 0
	var myInteger: Int? = null
	var myBool = false
	var myBoolean: Boolean? = null
	var myFloat = 0f
	var myFloatingNum: Float? = null
	var myLong: Long = 0
	var myLongNum: Long? = null
	var myDouble = 0.0
	var myDoubleNum: Double? = null

	@JvmField
	var myArchivable: MyArchivable? = null

	@JvmField
	var myList: MutableList<String> = LinkedList()
	var myArray = intArrayOf(1, 2, 3, 4, 5)

	@JvmField
	var myStringArray = arrayOf("a", null, "b", "c")
	var myEmptyStringArray = arrayOf<String>()
	var myNullStringArray: Array<String>? = null

	@JvmField
	var myIntArray = intArrayOf(1, 2, 3, 4)
	var myEmptyIntArray = intArrayOf()
	var myNullIntArray: IntArray? = null
	var myFloatArray = floatArrayOf(10f, 11f, 12f, 13f, 14f)
	var myEmptyFloatArray = floatArrayOf()
	var myNullFloatArray: FloatArray? = null
	var myLongArray = longArrayOf(1000, 2000, 3000, 4000)
	var myEmptyLongArray = longArrayOf()
	var myNullLongArray: LongArray? = null
	var myDoubleArray = doubleArrayOf(11111.0, 22222.0, 33333.0, 444444.0, 555555.0)
	var myEmptyDoubleArray = doubleArrayOf()
	var myNullDoubleArray: DoubleArray? = null
	var myBooleanArray = booleanArrayOf(true, false, false, true)
	var myEmptyBooleanArray: BooleanArray = BooleanArray(0) { false }
	var myNullBooleanArray: BooleanArray? = null
	var myEnumArray = arrayOf(SomeEnum.ENUM2, SomeEnum.ENUM3, SomeEnum.ENUM1)
	var myEmptyEnumArray = arrayOf<SomeEnum>()
	var myNullEnumArray: Array<SomeEnum>? = null

	@JvmField
	var myArchivableArray: Array<KReflector<*>>? = null
	var myEmptyArchivableArray = arrayOf<KReflector<*>>()
	var myNullArchivableArray: Array<KReflector<*>>? = null

	@JvmField
	var my2DArchivableArray: Array<Array<KReflector<*>>>? = null

	@JvmField
	var my2DIntArray = arrayOfNulls<IntArray>(3)
	var my2DNullStringArray: Array<Array<String>?> = arrayOfNulls(3)

	@JvmField
	var my3DDoubleArray: Array<Array<DoubleArray?>?> = arrayOfNulls(4)

	@JvmField
	var myCollection: Collection<*>? = null

	@JvmField
	var myIntList: Collection<Int> = LinkedList()

	@JvmField
	var myStringSet: Collection<String> = HashSet()

	@JvmField
	var collectionArray = arrayOf<Collection<*>?>(
		ArrayList<Any>()
	)
	var collectionArray2D = arrayOf(arrayOf<Collection<*>>(ArrayList<Any?>(), ArrayList<Any?>()), arrayOf<Collection<*>>(HashSet<Any?>(), HashSet<Any?>()))
	var collectionArray3D = arrayOf<Array<Array<Collection<*>>>>()
	fun populate() {
		myList.add("A")
		myList.add("B")
		myList.add("C")
		my2DIntArray[0] = intArrayOf(1, 2, 3)
		my2DIntArray[1] = intArrayOf(4, 5, 6)
		my2DIntArray[2] = intArrayOf(7, 8, 9)
		for (i in my3DDoubleArray.indices) {
			my3DDoubleArray[i] = Array(3) { ii ->
				DoubleArray(3) { iii ->
					(i * ii * iii).toDouble()
				}
			}
		}
	}

	companion object {
		init {
			addAllFields(MyArchivable::class.java)
			/*
        try {
            Field [] fields = MyArchivable.class.getDeclaredFields();
            for (Field f: fields) {
                addField(MyArchivable.class, f.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/

			/*
        addField(MyArchivable.class, "myStringArray");
        addField(MyArchivable.class, "myEmptyStringArray");
        addField(MyArchivable.class, "myNullStringArray");
        addField(MyArchivable.class, "myIntArray");
        addField(MyArchivable.class, "myEmptyIntArray");
        addField(MyArchivable.class, "myNullIntArray");
        addField(MyArchivable.class, "myFloatArray");
        addField(MyArchivable.class, "myEmptyFloatArray");
        addField(MyArchivable.class, "myNullFloatArray");
        addField(MyArchivable.class, "myLongArray");
        addField(MyArchivable.class, "myEmptyLongArray");
        addField(MyArchivable.class, "myNullLongArray");
        addField(MyArchivable.class, "myDoubleArray");
        addField(MyArchivable.class, "myEmptyDoubleArray");
        addField(MyArchivable.class, "myNullDoubleArray");
        addField(MyArchivable.class, "myBooleanArray");
        addField(MyArchivable.class, "myEmptyBooleanArray");
        addField(MyArchivable.class, "myNullBooleanArray");
        addField(MyArchivable.class, "myEnumArray");
        addField(MyArchivable.class, "myEmptyEnumArray");
        addField(MyArchivable.class, "myNullEnumArray");
        addField(MyArchivable.class, "myArchivableArray");
        addField(MyArchivable.class, "myEmptyArchivableArray");
        addField(MyArchivable.class, "myNullArchivableArray");
        addField(MyArchivable.class, "myEnum");
        addField(MyArchivable.class, "myString");
        addField(MyArchivable.class, "myInt");
        addField(MyArchivable.class, "myBoolean");
        addField(MyArchivable.class, "myFloat");
        addField(MyArchivable.class, "myDouble");
        addField(MyArchivable.class, "myArchivable");
        addField(MyArchivable.class, "my2DIntArray");
        addField(MyArchivable.class, "my2DNullStringArray");
        addField(MyArchivable.class, "my3DDoubleArray");
        addField(MyArchivable.class, "myIntList");
        addField(MyArchivable.class, "myStringSet");*/
		}
	}
}