package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand

interface INetContext {

	suspend fun sendTCP(vararg cmds: INetCommand)
}