package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand
import java.io.InputStream

typealias NetCommandCreator = (InputStream) -> INetCommand

interface INetCommandFactory {

	fun <T : INetCommand> read(stream: InputStream): T

	fun register(serializedName: String, creator: NetCommandCreator)
}
