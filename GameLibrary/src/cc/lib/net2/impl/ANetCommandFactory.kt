package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetCommandFactory
import cc.lib.net2.NetCommandCreator
import cc.lib.net2.NetCommandRegistryGameLib
import java.io.DataInputStream
import java.io.InputStream

/**
 * Created by Chris Caron on 3/6/26.
 */
abstract class ANetCommandFactory(name: String) : INetCommandFactory {

	private val logger = LoggerFactory.getLoggerForName(name)

	private val registrar = object : HashMap<String, (DataInputStream) -> INetCommand>() {
		override fun put(key: String, value: (DataInputStream) -> INetCommand): ((DataInputStream) -> INetCommand)? {
			if (get(key) != null)
				throw IllegalArgumentException("Duplicate entry '$key'")
			return super.put(key, value)
		}
	}

	override fun <T : INetCommand> read(stream: InputStream): T {
		with(stream.toDataInputStream()) {
			val cmd = readUTF()
			registrar[cmd]?.let {
				return (it(this) as T).also {
					logger.debug("read $it")
				}
			} ?: throw NetException("Unknown command $cmd")
		}
	}

	override fun register(name: String, creator: NetCommandCreator) {
		registrar[name] = creator
	}

	init {
		NetCommandRegistryGameLib(this)
	}

}