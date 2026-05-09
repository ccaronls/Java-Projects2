package cc.game.zombicide.p2p

import cc.game.zombicide.NetCommandRegistryZombicide
import cc.game.zombicide.ZPlayerName
import cc.game.zombicide.ZQuests
import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import cc.lib.net.NetConnectQuality
import cc.lib.net.impl.ANetCommandFactory

object NetCommandFactoryZombicide : ANetCommandFactory() {
	init {
		NetCommandRegistryZombicide(this)
	}
}

const val ZOMBICIDE_VERSION = 1

enum class ConnectedUserType(val code: String) {
	HOST("H"),
	PLAYER("C"),
	SPECTATOR("S")
}

@NetCommand
abstract class ConnectedUser : INetCommand {
	abstract val name: String
	abstract var type: ConnectedUserType
	abstract var colorId: Int
	abstract var connected: Boolean
	abstract var status: NetConnectQuality

	override fun hashCode(): Int = colorId
}

@NetCommand
interface ConnectedUserList : INetCommand {
	val users: Array<ConnectedUser>
}

@NetCommand
interface CommAssign : INetCommand {
	val name: ZPlayerName
	val userName: String
	val colorId: Int
	val selected: Boolean
}

@NetCommand
interface SvrInit : INetCommand {
	val quest: ZQuests
	val colorId: Int
	val numPlayers: Int
	val numCharactersPerPlayer: Int
}

enum class CLButton {
	START,
	UNDO,
	COLORS
}

@NetCommand
interface ClButtonPressed : INetCommand {
	val button: CLButton
}

@NetCommand
interface SvrUpdate : INetCommand {
	val board: ByteArray
}

@NetCommand
interface SvrColorsResponse : INetCommand {
	val availableColors: IntArray
}

