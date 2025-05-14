package cc.lib.net

import cc.lib.utils.GException
import cc.lib.utils.trimmedToSize
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket

/**
 * Created by chriscaron on 3/12/18.
 *
 * ClientConnection handles the socket and threads associated with a single client
 * That has passed the handshaking test.
 *
 * Can only be created by GameServer instances.
 *
 * Execute methods remotely and wait for the return value if it has one. This should have effect of
 * making the socket connection appear transparent the the caller.
 *
 * TODO: Add retries?
 *
 * Be sure to not obfuscate those methods involved in this scheme so different versions
 * of application can remain compatible.
 *
 * Example:
 *
 * you have some object that exists on 2 systems connected by I/O stream.
 *
 * MyObjectType myObject;
 *
 * the client and server endpoints both derive from ARemoteExecutor
 *
 * On system A:
 * server = new ARemoteExecutor() { ... }
 *
 * On system B:
 * client = new ARemoteExecutor() {... }
 *
 * System register objects to be executed upon
 *
 * client.register(myObject.getClass().getSimpleName(), myObject);
 *
 * ...
 *
 * @Keep
 * class MyObjectType {
 *
 * // make sure to prevent obfuscation
 * @Keep
 * public Integer add(int a, int b) {
 * try {
 * // executeOnRemote will determine method name and class from Exception stack
 * // and bundle everything up to the client then wait for a response and return
 * // it once it arrives
 * return server.executeOnRemote(true, a, b);
 * } catch (IOException e) {
 * ...
 * }
 * return null; // good practice to return non-primitives
 * }
 * }
 */
open class ClientConnection(server: GameServer, attributes: Map<String, Any>) :
	AClientConnection(server, attributes.toMutableMap()), Runnable {

	private var socket: Socket? = null
	private var dIn: DataInputStream? = null
	private var dOut: DataOutputStream? = null
	private val outQueue: CommandQueueWriter = object : CommandQueueWriter("SRV") {
		override fun onTimeout() {
			notifyListeners { l: Listener -> l.onTimeout(this@ClientConnection) }
		}
	}
	private var connected = false

	/**
	 * Send a disconnected message to the client and shutdown the connection.
	 * @param reason
	 */
	override fun disconnect(reason: String) {
		log.debug("Disconnecting client: $name $reason")
		try {
			if (connected) {
				log.info("ClientConnection: Disconnecting client '$name'")
				connected = false
				notifyListeners { l: Listener -> l.onDisconnected(this, reason) }
				onDisconnected()
				synchronized(outQueue) {
					outQueue.clear()
					if (!disconnecting) outQueue.add(
						GameCommandType.SVR_DISCONNECT.make().setMessage(reason)
					)
				}
				disconnecting = true
				outQueue.stop() // <-- blocks until network flushed
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	open fun onDisconnected() {}

	private fun close() {
		log.debug("ClientConnection: close() ...")
		server.removeClient(this)
		connected = false
		disconnecting = false
		outQueue.stop()
		//reader.stop();
		log.debug("ClientConnection: outQueue stopped ...")
		// close output stream first to make sure it is flushed
		// https://stackoverflow.com/questions/19307011/does-close-a-socket-will-also-close-flush-the-input-output-stream
		try {
			dOut!!.close()
		} catch (ex: Exception) {
		}
		try {
			dIn!!.close()
		} catch (ex: Exception) {
		}
		try {
			socket!!.close()
		} catch (ex: Exception) {
		}
		socket = null
		dIn = null
		dOut = null
		log.debug("ClientConnection: close() DONE")
	}

	/**
	 *
	 * @return
	 */
	override val isConnected: Boolean
		get() = connected && !disconnecting

	/*
     * init connection.  should only be used by GameServer
     */
	@Throws(Exception::class)
	fun connect(socket: Socket, input: DataInputStream, out: DataOutputStream) {
		if (isConnected) {
			throw Exception("Client '$name' is already connected")
		}
		log.debug("ClientConnection: $name connection attempt ...")
		this.socket = socket
		try {
			this.dIn = input
			this.dOut = out
			start()
		} catch (e: Exception) {
			close()
			throw e
		}
		log.debug("ClientConnection: $name connected SUCCESS")
		onConnected(socket.inetAddress, socket.port)
	}

	open fun onConnected(clientAddress: InetAddress, port: Int) {}

	/**
	 * Sent a command to the remote client
	 * @param cmd
	 */
	override fun sendCommand(cmd: GameCommand) {
		log.debug("Sending command to client $name\n${cmd.toString().trimmedToSize(256)}")
		if (!isConnected) throw GException("Client $name is not connected")
		//log.debug("ClientConnection: " + getName() + "-> sendCommand: " + cmd);
		synchronized(outQueue) { outQueue.add(cmd) }
	}

	/**
	 * internal
	 */
	override fun start() {
		//reader.start();
		outQueue.start(requireNotNull(dOut))
		connected = true
		Thread(this).start()
	}

	override fun run() {
		log.debug("ClientConnection: ClientThread " + Thread.currentThread().id + " starting")
		while (isConnected) {
			try {
				processCommand(GameCommand.parse(requireNotNull(dIn)))
			} catch (e: Exception) {
				if (isConnected) {
					e.printStackTrace()
					log.error("ClientConnection: Connection with client '" + name + "' dropped: " + e.javaClass.simpleName + " " + e.message)
					disconnecting = true
					disconnect(e.message ?: e.javaClass.simpleName)
				}
				break
			}
		}
		log.debug("ClientConnection: ClientThread " + Thread.currentThread().id + " exiting")
		close()
	}

	override fun onCancelled(id: String) {
		notifyListeners { l: Listener -> l.onCancelled(this, id) }
	}
}
