package cc.lib.ksp.mirror

import kotlinx.serialization.KSerializer

/**
 * Created by Chris Caron on 5/9/24.
 *
 * kotlin data classes cannot be identified my KSP, so, in order for Mirror to work,
 * data classes that are apart of mirror class hierarchy must implement IData so they
 * can be identified and handled correctly.
 *
 * Also, data classes must use @Serializer
 */
interface IData<T> {
	/**
	 * All implementations simply need to be:
	 *
	 * <code>fun deepCopy() = copy()</code>
	 *
	 * 'copy()' is generated at compile time by the data class system and
	 * takes arguments with default values, therefore we cannot override
	 */
	fun deepCopy(): T

	/**
	 * All implementations simply need to be:generated into companion object of this type
	 *
	 * <code>fun getSerializer() = serializer()</code>
	 *
	 * Like 'copy' serializer is g
	 */
	fun getSerializer(): KSerializer<*>
}