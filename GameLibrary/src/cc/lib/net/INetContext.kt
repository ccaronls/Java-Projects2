package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand
import kotlinx.coroutines.CoroutineScope

interface INetListener<T> {

	fun addListener(l: T)
	fun removeListener(l: T)
	fun addWeakListener(l: T)

	fun notifyListeners(cb: suspend (T) -> Unit)

}


interface INetContext {

	val scope: CoroutineScope
	fun sendTCP(vararg cmds: INetCommand)
}