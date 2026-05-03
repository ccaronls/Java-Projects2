package cc.lib.ksp.reflector

enum class TestEnum {
	AA,
	BB,
	CC
}

/**
 * Created by Chris Caron on 4/30/26.
 */
@Reflect(className = "Reflector1")
abstract class AReflector1 : IReflector {

	var foo = 0
	var bar = ""
	var ints = intArrayOf()
	var floats = floatArrayOf()
	var longs = longArrayOf()
	var doubles = doubleArrayOf()
	var bools = booleanArrayOf()
	var objects: Array<IReflector> = arrayOf()
	var ref2: AReflector2 = Reflector2Impl()
	var ref2Nullable: AReflector2? = null
}

@Reflect
abstract class AReflector2(var str: String = "") : IReflector {
}

@Reflect
abstract class AReflector3() : IReflector {

	var z = 1000

	abstract fun doIt()
}

@Reflect(className = "Reflector4")
abstract class AReflector4() : Reflector3Impl() {

	var x = 0
	var y = 0

	var e = TestEnum.AA

	var enums = arrayOf<TestEnum>()

	var list1 = listOf<Int>()
	var list2 = listOf<String>()
	var list3 = listOf<TestEnum>()
	var list4 = listOf<AReflector2>()

	var map1 = mapOf<String, Int>()


	override fun doIt() {
		println("Im doing it OKAY!")
	}
}

@Reflect
abstract class AReflector5() : AReflector3() {

	var x: Int? = null
	var y: Float? = null

	var e: TestEnum? = null

	var enums: Array<TestEnum>? = null
	var ints: IntArray? = null
	var refs: Array<IReflector>? = null
	var refs2: Array<IReflector?>? = null

	var list1: List<Int>? = null
	var list2: List<String>? = null
	var list3: List<TestEnum>? = null
	var list4: List<AReflector2>? = null
	var list5: List<AReflector2?>? = null

	var map1: Map<String, Int>? = null


	override fun doIt() {
		println("Im doing it OKAY!")
	}
}
