package cc.lib.net

import cc.lib.ksp.remote.ISvrExecuteRemote

class TestNetRemoteImpl(val connection: NetTest.TestNetConnection) : NetRemoteRemote() {

	override suspend fun executeRemotelyBlocking(cmd: ISvrExecuteRemote): Any? {
		return connection.executeRemotely(cmd)
	}
}
