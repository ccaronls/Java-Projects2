package cc.lib.rem.context

/**
 * Created by Chris Caron on 5/4/24.
 */
interface IRemote {

	fun executeLocally(method: String, vararg args: Any?): Any?

	fun executeRemotely(method: String, vararg args: Any?): Any?

}