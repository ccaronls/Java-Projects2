package cc.lib.logger

/**
 * Created by chriscaron on 2/27/18.
 */
interface Logger {
	fun debug(msg: String, vararg args: Any)
	fun info(msg: String, vararg args: Any)
	fun error(msg: String, vararg args: Any)
	fun error(e: Throwable)
	fun warn(msg: String, vararg args: Any)
	fun verbose(msg: String, vararg args: Any)
}