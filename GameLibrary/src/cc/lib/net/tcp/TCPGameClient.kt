package cc.lib.net.tcp

import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import cc.lib.net.ProtocolException
import cc.lib.net.api.IGameCommand
import cc.lib.net.base.AGameClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket

/**
 * Created by Chris Caron on 6/3/25.
 */
class TCPGameClient(val name: String, val version: String) : AGameClient() {

	private var running = false
	private val requestChannel = Channel<GameCommand>(Channel.UNLIMITED)
	private var readJob: Job? = null
	private var writeJob: Job? = null

	override fun connect(iPaddress: String, port: Int) {
		if (running)
			return
		running = true
		CoroutineScope(Dispatchers.IO).launch {
			while (running) {
				try {
					val socket = Socket(iPaddress, port)
					log.debug("Connected to server")
					communicateWithServer(socket)
				} catch (e: Exception) {
					log.warn("Connection failed: ${e.message}. Retrying in 5s")
					delay(5000)
				}
			}
		}
	}

	override fun send(command: IGameCommand) = runBlocking {
		requestChannel.send(command as GameCommand)
	}

	private suspend fun communicateWithServer(socket: Socket) {
		socket.use {
			val reader = DataInputStream(it.getInputStream())
			val writer = DataOutputStream(it.getOutputStream())

			writer.writeLong(87263450972L)
			GameCommandType.CL_CONNECT.make()
				.setArgs(properties)
				.setArg("name", name)
				.setArg("version", version)
				.write(writer)

			val cmd = GameCommand.parse(reader)
			if (cmd.type != GameCommandType.SVR_CONNECTED)
				throw ProtocolException("Expecting SVR_CONNECTED, got: ${cmd.type}")

			val pingFrequency = cmd.getLong("pingFrequency", 0)
			if (pingFrequency == 0L)
				throw ProtocolException("No ping frequency property")

			val index = cmd.getInt("index", -1)
			if (index < 0)
				throw ProtocolException("No index property")

			readJob = coroutineScope {
				launch {
					try {
						while (true) {
							val serverCommand = withTimeoutOrNull(10000L) {
								GameCommand.parse(reader)
							} ?: run {
								log.warn("Server unresponsive. Closing.")
								cancel()
								return@launch
							}

							processCommand(serverCommand)

							log.debug("Server: $serverCommand")
						}
					} catch (e: IOException) {
						log.warn("read error: ${e.message}")
					}
				}
			}

			writeJob = coroutineScope {
				launch {
					try {
						while (true) {
							requestChannel.receive().write(writer)
						}
					} catch (e: IOException) {
						log.warn("write error: ${e.message}")
					}
				}
			}

			try {
				while (readJob?.isActive == true && writeJob?.isActive == true) {
					val cmd = withTimeoutOrNull(pingFrequency) {
						requestChannel.receive()
					} ?: GameCommandType.CL_PING.make()
						.setArg("time", System.currentTimeMillis())
					cmd.write(writer)
					writer.flush()
					delay(pingFrequency)
				}
			} finally {
				GameCommandType.CL_DISCONNECT.make().write(writer)
				writer.flush()
				log.debug("Client sent disconnect message")
			}
		}
	}

	override fun close() {
		running = false
		readJob?.cancel()
		super.close()
	}
}