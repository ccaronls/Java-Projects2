package cc.lib.net.api

import cc.lib.reflector.Reflector

/**
 * Created by Chris Caron on 6/3/25.
 */
interface IGameCommand {
	fun getString(s: String, defaultValue: String = ""): String

	fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

	fun getInt(key: String, defaultValue: Int = 0): Int

	fun getFloat(key: String, defaultValue: Float = 0f): Float

	fun getLong(key: String, defaultValue: Long = 0): Long

	fun getDouble(key: String, defaultValue: Double = 0.0): Double

	fun <T : Reflector<T>> getReflector(key: String, obj: T): T
}