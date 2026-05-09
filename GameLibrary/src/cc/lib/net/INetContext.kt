package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand
import kotlinx.coroutines.CoroutineScope

interface INetContext {

	val scope: CoroutineScope
	fun sendTCP(vararg cmds: INetCommand)
}