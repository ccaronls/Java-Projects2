package cc.game.soc.core

/**
 * Created by chriscaron on 3/27/18.
 */
interface StringResource {
	fun getString(format: String, vararg args: Any): String {
		return String.format(format, *args)
	}
}