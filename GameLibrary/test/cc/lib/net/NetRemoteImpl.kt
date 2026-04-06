package cc.lib.net

import cc.lib.ksp.remote.ISvrExecuteRemote

class NetRemoteImpl(val connection: INetConnection) : NetRemoteRemote() {

	override suspend fun executeRemotelyBlocking(cmd: ISvrExecuteRemote): Any? {
		return connection.executeRemotely(cmd)
	}
}
