package cc.lib.net

import kotlinx.coroutines.runBlocking

class NetRemoteImpl(val connection: INetConnection) : NetRemoteRemote() {

	override fun executeRemotely(method: String, resultType: Class<*>?, vararg args: Any?): Any? {
		return runBlocking {
			connection.executeRemotely(0, method, resultType, args)
		}
	}
}
