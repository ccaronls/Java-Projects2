package cc.lib.net.tcp

import cc.lib.net.GameCommand
import cc.lib.net.api.IGameCommand
import cc.lib.net.base.AClientConnection
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Created by Chris Caron on 6/3/25.
 */
class TCPClientConnection(parent: TCPGameServer) : AClientConnection(parent) {

	private val writeChannel = Channel<GameCommand>(Channel.UNLIMITED)

	@Throws(IOException::class)
	suspend fun run(
		reader: DataInputStream,
		writer: DataOutputStream,
		attributes: Map<String, Any>
	) {
		_properties.putAll(attributes)
		coroutineScope {
			launch {
				try {
					while (isActive) {
						writeChannel.receive().write(writer)
					}
				} catch (e: Exception) {
					log.error("write error ${e.message}")
					cancel()
				}
			}

			launch {
				try {
					while (isActive) {
						process(GameCommand.parse(reader))
					}
				} catch (e: Exception) {
					log.error("read error ${e.message}")
					cancel()
				}
			}
		}.join()
	}

	override val name: String
		get() = properties.get("name") as String

	override fun send(cmd: IGameCommand) = runBlocking {
		writeChannel.send(cmd as GameCommand)
	}
}