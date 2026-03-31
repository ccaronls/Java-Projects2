package cc.lib.net.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.net.INetCommandFactory
import cc.lib.net.NetCommandCreator
import cc.lib.net.NetCommandRegistryGameLib
import java.io.DataInputStream
import java.io.InputStream

/**
 * Created by Chris Caron on 3/6/26.
 *
 * Projects should have a single object that inherits this class and has an init
 * to load the commands specific to that project. The name of the registry to
 * be built comes from ksp parameter '"net_command_registry_name"'
 *
 * For instance, for project 'MyGame' we my have:
 * ksp {
 *  arg("net_command_registry_name", "MyGame")
 * }
 *
 * and some commands
 *
 * @NetCommand
 * MyGameCmd1 ...
 *
 * @NetCommand
 * MyGameCmd2 ...
 *
 * Will generate a registry:
 * NetCommandRegistryMyGame
 *
 * so you should have:
 *
 * object MyGameCommandFactory : ANetCommandFactory() {
 *    init { NetCommandRegistryMyGame(this) }
 * }
 *
 * Then pass this object into you NetClient/Server objects as the factory param
 */
abstract class ANetCommandFactory : INetCommandFactory {

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
				return (it(this) as T)
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