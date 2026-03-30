package cc.lib.net.impl

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
	val udpReadPort: Int
	val udpWritePort: Int
	val udpInSize: Int // max size client can send
	val udpOutSize: Int // min size client will receive
	val message: String?
}

@NetCommand
interface SvrDisconnect : INetCommand {
	val reason: String
}

// Client <-> server property changed request
@NetCommand
interface CommProperty : INetCommand {
	val key: String
	val value: Any?
}

@NetCommand
interface SvrExecute : INetCommand {
	val objId: Int
	val methodName: String
	val resultType: String?
	val params: Array<out Any?>
	val requestId: Int
}

@NetCommand
interface ClExecuteResult : INetCommand {
	val id: Int
	val result: Any?
}

@NetCommand
interface CommPing : INetCommand {
	val pingTime: Long
	val delay: Int
}