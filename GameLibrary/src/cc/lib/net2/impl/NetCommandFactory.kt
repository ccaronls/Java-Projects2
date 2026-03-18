package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand

@NetCommand
interface ClConnect : INetCommand {
	val name: String
	val id: Int
	val version: Int
}

@NetCommand
interface ClDisconnect : INetCommand {
	val reason: String
}

@NetCommand
interface SvrConnected : INetCommand {
	val id: Int // if zero then connection denied, see message for reason
	val udpPort: Int
	val message: String
}

@NetCommand
interface SvrStopped : INetCommand

// Client <-> server property changed request
@NetCommand
interface CommProperty : INetCommand {
	val key: String
	val value: Any?
}

@NetCommand
interface SvrExecute : INetCommand {
	val methodName: String
	val resultType: String?
	val params: Array<out Any?>
}

@NetCommand
interface ClExecuteResult : INetCommand {
	val result: Any?
}