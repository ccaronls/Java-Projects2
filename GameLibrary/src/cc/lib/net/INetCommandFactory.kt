package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand
import java.io.InputStream

typealias NetCommandCreator = (InputStream, INetCommandFactory) -> INetCommand

interface INetCommandFactory {

	fun <T : INetCommand> read(stream: InputStream, factory: INetCommandFactory): T

	fun register(serializedName: String, creator: NetCommandCreator)
}
