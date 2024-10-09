package cc.lib.net

import cc.lib.crypt.Cypher
import cc.lib.crypt.EncryptionInputStream
import cc.lib.crypt.EncryptionOutputStream
import cc.lib.crypt.HuffmanEncoding
import cc.lib.utils.launchIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * A Game server is a server that handles normal connection/handshaking and maintains
 * a set of ClientConnections.  The base version should be minimal with only those
 * methods to listen for connections accept and handshake with the client and maintain
 * the connections.
 *
 * The Base Game Server Provides:
 * - Handshaking with new connections
 * - Managing Connection timeouts and reconnects.
 * - Managing older client versions.
 * - Managing Protocol Encryption
 * - Executing methods on a game object (see GameCommon)
 *
 * Override protected methods to create custom behaviors
 *
 * @author ccaron
 */
class GameServer(
	serverName: String,
	listenPort: Int,
	serverVersion: String,
	private val cypher: Cypher?,
	maxConnections: Int
) : AGameServer(serverName, listenPort, serverVersion, maxConnections) {
	@JvmField
	var TIMEOUT = 20000

	@JvmField
	var PING_FREQ = 10000

	private var socketListener: SocketListener? = null
	private var counter: HuffmanEncoding? = null
	private var listenJob: Job? = null

	/**
	 * Create a server and start listening.  When cypher is not null, then then server will only
	 * allow encrypted clients.
	 *
	 * @param serverName the name of this server as seen by the clients
	 * @param listenPort port to listen on for new connections
	 * @param serverVersion version of this service to use to check compatibility with clients
	 * @param cypher used to encrypt the dialog. can be null.
	 * @param maxConnections max number of clients to allow to be connected
	 * @throws IOException
	 * @throws Exception
	 */
	init {
		if (cypher == null) {
			log.warn("NULL CYPHER NOT A GOOD IDEA FOR RELEASE!")
		}
	}

	/**
	 * Start listening for connections
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun listen() {
		val socket = ServerSocket(port)
		socket.soTimeout = 5000
		socketListener = SocketListener(socket)
		listenJob = launchIn(Dispatchers.IO) {
			socketListener!!.run()
		}
	}

	/**
	 *
	 * @return
	 */
	override val isRunning: Boolean
		get() = socketListener != null


	/**
	 * Disconnect all clients and stop listening.  Will block until all clients have closed their sockets.
	 */
	override fun stop() = runBlocking {
		log.info("GameServer: Stopping server: $this")
		if (socketListener != null) {
			socketListener!!.stop()
			socketListener = null
			disconnectingLock.reset()
			synchronized(clients) {
				for (c in clients.values) {
					if (c.isConnected) {
						disconnectingLock.acquire()
						try {
							c.disconnect("Game Stopped")
						} catch (e: Throwable) {
							e.printStackTrace()
						}
					}
				}
			}
			disconnectingLock.block(10000) { log.warn("Unclean stoppage") } // give clients a chance to disconnect from their end
			clear()
		}
		listenJob?.cancel()
		listenJob = null
		if (counter != null) {
			log.info("------------------------------------------------------------------------------------------------------")
			log.info("******************************************************************************************************")
			log.info("------------------------------------------------------------------------------------------------------")
			log.info(counter!!.encodingAsCode)
			log.info("------------------------------------------------------------------------------------------------------")
			log.info("******************************************************************************************************")
			log.info("------------------------------------------------------------------------------------------------------")
			counter = null
		}
		notifyListeners { l: Listener -> l.onServerStopped() }
	}

	private inner class SocketListener(var socket: ServerSocket) {
		var running = true
		fun stop() {
			running = false
			try {
				socket.close()
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}

		suspend fun run() {
			log.info("GameServer: Thread started listening for connections")
			running = true
			while (running) {
				try {
					log.debug("accepting connections")
					val client = socket.accept()
					log.debug(
						"""New Client connect:
   remote address=%s
   local address=%s
   keep alive=%s
   OOD inline=%s
   send buf size=%s
   recv buf size=%s
   reuse addr=%s
   tcp nodelay=%s
   SO timeout=%s
   SO linger=%s""",
						client.remoteSocketAddress,
						client.localAddress,
						client.keepAlive,
						client.oobInline,
						client.sendBufferSize,
						client.receiveBufferSize,
						client.reuseAddress,
						client.tcpNoDelay,
						client.soTimeout,
						client.soLinger
					)
					launchIn(Dispatchers.IO) {
						HandshakeThread(client).run()
					}
				} catch (e: SocketTimeoutException) {
					// ignore
					log.debug("Timeout")
				} catch (e: SocketException) {
					if (running)
						log.error(e)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			log.info("SocketListener thread exiting")
		}
	}


	fun close(socket: Socket, input: InputStream, output: OutputStream) {
		try {
			input.close()
		} catch (ex: Throwable) {
		}
		try {
			output.close()
		} catch (ex: Throwable) {
		}
		try {
			socket.close()
		} catch (ex: Throwable) {
		}
	}

	private inner class HandshakeThread(val socket: Socket) {
		init {
			socket.soTimeout = TIMEOUT // give a few seconds latency
			socket.keepAlive = true
			socket.tcpNoDelay = true
		}

		suspend fun run() {
			var dIn: DataInputStream? = null
			var out: DataOutputStream? = null
			try {
				log.info("GameServer: Start handshake with new connection")
				if (cypher != null) {
					dIn = DataInputStream(
						EncryptionInputStream(
							BufferedInputStream(socket.getInputStream()),
							cypher
						)
					)
					out = DataOutputStream(
						EncryptionOutputStream(
							BufferedOutputStream(socket.getOutputStream()),
							cypher
						)
					)
				} else {
					counter = HuffmanEncoding()
					dIn = DataInputStream(BufferedInputStream(socket.getInputStream()))
					out = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
				}
				val magic = dIn.readLong()
				if (magic != 87263450972L) throw ProtocolException("Unknown client")
				val cmd = GameCommand.parse(dIn)
				log.debug("Parsed incoming command: $cmd")
				val clientVersion = cmd.getVersion()
				if (clientVersion.isBlank()) {
					GameCommand(GameCommandType.MESSAGE).setMessage("ERROR: Missing required 'version' attribute")
						.write(out)
					throw ProtocolException("Broken Protocol.  Expected clientVersion field in cmd: $cmd")
				}
				clientVersionCompatibilityTest(clientVersion, mVersion)
				if (password?.isNotEmpty() == true) {
					GameCommand(GameCommandType.PASSWORD).write(out)
					val pswd = GameCommand.parse(dIn)
					if (password != pswd.getString("password")) throw ProtocolException("Bad Password")
				}
				var conn: AClientConnection? = null
				var reconnection = false
				synchronized(clients) {
					if (cmd.type == GameCommandType.CL_CONNECT) {
						val name = cmd.getString("name")
						conn = clients[name]?.let {
							if (it.isConnected) {
								//new GameCommand(GameCommandType.SVR_MESSAGE).setMessage("ERROR: A client with the name '" + name + "' is already connected").write(out);
								throw ProtocolException("client with name already exists")
							}
							if (it.isKicked) {
								throw ProtocolException("Client Banned")
							}
							reconnection = true
							it
						} ?: run {
							if (clients.size >= maxConnections) {
								throw java.net.ProtocolException("Max client connections reached")
							}
							ClientConnection(this@GameServer, cmd.arguments)
						}
						(conn as ClientConnection).let {
							it.connect(socket, dIn, out)
							it.setAttributes(cmd.arguments)
						}
						clients.put(name, conn!!)
					} else {
						throw ProtocolException("Handshake failed: Invalid client command: $cmd")
					}
				}
				GameCommand(GameCommandType.SVR_CONNECTED)
					.setName(name)
					.setArg("keepAlive", PING_FREQ).write(out)

				// TODO: Add a password feature when server prompts for password before conn.connect is called.
				conn?.let {
					log.debug("GameServer: Client ${it.name} " + if (reconnection) "reconnected" else "connected")
					if (reconnection) {
						it.notifyListeners { l: AClientConnection.Listener ->
							l.onReconnected(it)
						}
					} else {
						notifyListeners { l: Listener -> l.onConnected(it) }
					}
				} ?: log.error("Null connection")

				//new GameCommand(GameCommandType.SVR_CONNECTED).setArg("keepAlive", clientReadTimeout).write(out);
				//log.debug("GameServer: Client " + name + " connected");

				// send the client the main menu
			} catch (e: ProtocolException) {
				try {
					e.printStackTrace()
					log.error(e)
					out?.let {
						GameCommandType.SVR_DISCONNECT.make().setMessage(e.message ?: "Unknown Error").write(it)
						it.flush()
					}
				} catch (ex: Exception) {
					ex.printStackTrace()
				}
				close(socket, dIn ?: socket.getInputStream(), out ?: socket.getOutputStream())
			} catch (e: Throwable) {
				e.printStackTrace()
				log.error(e)
				close(socket, dIn ?: socket.getInputStream(), out ?: socket.getOutputStream())
			}
		}
	}
}
