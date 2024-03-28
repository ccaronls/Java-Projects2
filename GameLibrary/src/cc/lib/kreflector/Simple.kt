package cc.lib.kreflector

import cc.lib.utils.DirtyGrid

@Reflect
class Simple(var x: Int = 0, var y: Int = 0) : Reflector<Simple>()

@Reflect
class TestDirty : DirtyReflector<TestDirty>() {

	var testBool: Boolean by DirtyDelegate(true)
	var testInt: Int by DirtyDelegate(20)
	var testLong: Long by DirtyDelegate(0L)
	var testFloat: Float by DirtyDelegate(10F)
	var testStr: String by DirtyDelegate("")
	var testVec: Simple by DirtyDelegate(Simple())
	var testIntList = DirtyList(ArrayList<Int>())
	var testMap = DirtyMap(HashMap<String, Int>())
	var testListList = DirtyList(ArrayList<DirtyList<Simple>>())
	var testMapOfVectors = DirtyMap(HashMap<String, Simple>())
	var testGrid: DirtyGrid<String>? by DirtyDelegate(null, DirtyGrid::class.java)
}

@Reflect
class TestDirty2 : DirtyReflector<TestDirty2>() {
	val dirty = TestDirty()
}

@Reflect
class TestDirty3 : DirtyReflector<TestDirty2>() {
	val dirty = TestDirty()
	val dirty2 = TestDirty2()
}
