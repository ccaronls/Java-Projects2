package cc.lib.net

import java.util.Properties

/**
 * Created by Chris Caron on 12/4/23.
 */
interface ICommand {

	/**
	 * Code <= 0 are reserved
	 */
	val code: Byte

	/**
	 *
	 */
	val args: MutableMap<String, Any>
}

interface IGameClient {

	enum class ConnectionQuality {
		SLOW,
		FAIR,
		GOOD
	}

	interface Listener {

		fun onCommand(cmd: ICommand)

		fun onDisconnected(reason: String)
	}

	/**
	 * Connect to a server
	 */
	fun connect(address: String, port: Int)

	/**
	 * Disconnect from the server.
	 */
	fun disconnect()

	/**
	 * Send a command to the server
	 */
	fun send(command: ICommand)

	/**
	 * Get the connection quality
	 */
	val connectQuality: ConnectionQuality

	/**
	 * Set a property. If the client is connected then the server will have
	 * onPropertyUpdated with updated values. Otherwise properties will be
	 * given on next connect. Setting a property to null will remove the property.
	 */
	fun setProperty(key: String, obj: Any?)

	/**
	 * Add / remove listeners
	 */
	val listeners: MutableSet<Listener>

	/**
	 * General place to save info about this client like version, device name, display name etc.
	 * Setting a property while connected will cause the onPropertyUpdated callback on the
	 * ClientConnection and will have the new set of properties
	 */
	val properties: Map<String, Any>
}

interface IGameServer {

	interface Listener {
		fun onNewConnection(connection: IClientConnection)

		fun onStopped()
	}

	/**
	 * Start listening for client connections
	 */
	fun listen()

	/**
	 * Send disconnect to all clients and shutdown
	 */
	fun stop()

	/**
	 * Send command to all connected clients
	 */
	fun broadcast(command: ICommand)

	val listeners: MutableSet<Listener>

	val connections: List<IClientConnection>
}

interface IClientConnection {

	interface Listener {

		/**
		 * Client has reconnected
		 */
		fun onReconnect()

		/**
		 * Client has disconnected
		 */
		fun onDisconnect()

		fun onCommand(command: ICommand)

		/**
		 * Client has updated a property
		 */
		fun onPropertiesChanged()
	}

	/**
	 * Client can be kicked from game and subsequent connect
	 * attempts will be denied
	 */
	fun setKicked(kicked: Boolean)

	/**
	 * Send a command to a client. Will cause onCommand to trigger for listeners of the GameClient
	 */
	fun sendCommand(command: ICommand)

	/**
	 * Add or remove listeners to this connection
	 */
	val listeners: MutableSet<Listener>

	/**
	 * Mirrored from properties in client
	 */
	val properties: Properties
}

interface INetFactory {
	fun createServer(): IGameServer

	fun createClient(): IGameClient

	fun createCommand(code: Byte): ICommand
}