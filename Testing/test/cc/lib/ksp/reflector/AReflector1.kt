package cc.lib.ksp.reflector

/**
 * Created by Chris Caron on 4/30/26.
 */
@Reflect(className = "Reflector1")
abstract class AReflector1 : IReflector {

	protected var foo = 0
	protected var bar = "hello"
	protected var ints = intArrayOf(1, 2, 3)
	protected var floats = floatArrayOf(4f, 5f, 6f)
	protected var longs = longArrayOf(100L, 200L, 300L)
	protected var doubles = doubleArrayOf(0.001, 0.002, 0.003)
	protected var bools = booleanArrayOf(true, false, true, false)
	protected var objects: Array<IReflector> = arrayOf(
		Reflector2().also { it.str = "a" },
		Reflector2().also { it.str = "b" },
		Reflector2().also { it.str = "c" },
	)
	protected var ref2: AReflector2 = Reflector2()
//	protected var ref2Nullable : Reflector2? = null
}

@Reflect(className = "Reflector2")
abstract class AReflector2() : IReflector {
	var str: String = "hello"
}