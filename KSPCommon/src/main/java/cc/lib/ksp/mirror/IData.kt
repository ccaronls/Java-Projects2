package cc.lib.ksp.mirror

/**
 * Created by Chris Caron on 5/9/24.
 *
 * Have data classes implement this so they can be identified
 */
interface IData<T> {
	fun deepCopy(): T
}