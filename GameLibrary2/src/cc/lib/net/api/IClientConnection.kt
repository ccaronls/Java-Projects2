import java.util.Properties

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
