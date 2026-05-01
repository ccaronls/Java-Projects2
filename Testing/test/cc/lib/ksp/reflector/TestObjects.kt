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
	var ref2: AReflector2 = Reflector2()
	var ref2Nullable: AReflector2? = null
}

@Reflect(className = "Reflector2")
abstract class AReflector2(var str: String = "") : IReflector {
}

@Reflect(className = "Reflector3")
abstract class AReflector3() : IReflector {

	var z = 1000

	abstract fun doIt()
}

@Reflect(className = "Reflector4")
abstract class AReflector4() : Reflector3() {

	var x = 0
	var y = 100

	var e = TestEnum.AA

	var enums = arrayOf(
		TestEnum.BB, TestEnum.CC, TestEnum.AA
	)

	var list1 = listOf(1, 2, 3)
	var list2 = listOf("a", "b", "c")
	var list3 = listOf(TestEnum.BB, TestEnum.CC, TestEnum.AA)
	var list4 = listOf(Reflector2("Ref1"), Reflector2("Ref2"), Reflector2("Ref3"))

	override fun doIt() {
		println("Im doing it OKAY!")
	}
}

