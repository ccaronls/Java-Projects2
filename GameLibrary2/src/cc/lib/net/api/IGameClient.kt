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