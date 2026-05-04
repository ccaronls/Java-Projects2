package cc.lib.ksp.reflex

enum class TestEnum {
	AA,
	BB,
	CC
}

/**
 * Created by Chris Caron on 4/30/26.
 */
@Reflex(className = "Reflex1")
abstract class AReflex1 : IReflex {

	var foo = 0
	var bar = ""
	var ints = intArrayOf()
	var floats = floatArrayOf()
	var longs = longArrayOf()
	var doubles = doubleArrayOf()
	var bools = booleanArrayOf()
	var objects: Array<IReflex> = arrayOf()
	var ref2: AReflex2 = Reflex2Impl()
	var ref2Nullable: AReflex2? = null
}

@Reflex
abstract class AReflex2(var str: String = "") : IReflex {
}

@Reflex
abstract class AReflex3() : IReflex {

	var z = 1000

	abstract fun doIt()
}

@Reflex(className = "Reflex4")
abstract class AReflex4() : Reflex3Impl() {

	var x = 0
	var y = 0

	var e = TestEnum.AA

	var enums = arrayOf<TestEnum>()

	var list1 = listOf<Int>()
	var list2 = listOf<String>()
	var list3 = listOf<TestEnum>()
	var list4 = listOf<AReflex2>()

	var map1 = mapOf<String, Int>()


	override fun doIt() {
		println("Im doing it OKAY!")
	}
}

@Reflex
abstract class AReflex5() : AReflex3() {

	var x: Int? = null
	var y: Float? = null

	var e: TestEnum? = null

	var enums: Array<TestEnum>? = null
	var ints: IntArray? = null
	var refs: Array<IReflex>? = null
	var refs2: Array<IReflex?>? = null

	var list1: List<Int>? = null
	var list2: List<String>? = null
	var list3: List<TestEnum>? = null
	var list4: List<AReflex2>? = null
	var list5: List<AReflex2?>? = null

	var map1: Map<String, Int>? = null


	override fun doIt() {
		println("Im doing it OKAY!")
	}
}

@Reflex
abstract class AReflex6 : IReflex {
	var map = mapOf(
		"hello" to listOf(1, 2, 3),
		"goodbye" to listOf(1, 2, 3)
	)

	var arr = arrayOf(
		Reflex4(), Reflex4(), Reflex4()
	)
}