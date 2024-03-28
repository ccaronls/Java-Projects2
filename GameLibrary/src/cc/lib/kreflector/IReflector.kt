package cc.lib.kreflector

/**
 * Created by Chris Caron on 12/8/23.
 */
interface IReflector<T> : IDirty {

	fun isImmutable(): Boolean = false

	//fun copyFrom(source : T) : T = Reflector.copy(source, this)

	//fun deepCopy() : T = Reflector.deepCopy(this as T)!!
}