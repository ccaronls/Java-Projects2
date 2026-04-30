package cc.lib.ksp.reflector

/**
 * Created by Chris Caron on 4/30/26.
 */
@Reflect(className = "Reflector1")
abstract class AReflector1 : IReflector {

	protected var foo = 0
	protected var bar = "hello"
	protected var array = intArrayOf(1, 2, 3)
	protected var objects: Array<IReflector> = arrayOf(
		Reflector2().also { it.str = "a" },
		Reflector2().also { it.str = "b" },
		Reflector2().also { it.str = "c" },
	)
}

@Reflect(className = "Reflector2")
abstract class AReflector2() : IReflector {
	var str: String = "hello"
}