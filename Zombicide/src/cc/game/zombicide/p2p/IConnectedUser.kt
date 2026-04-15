package cc.game.zombicide.p2p

import cc.lib.game.GColor
import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import cc.lib.net.NetConnectQuality

@NetCommand
interface IConnectedUser : INetCommand {
	val name: String
	val color: GColor
	val connected: Boolean
	val status: NetConnectQuality
	val startUser: Boolean
}