package cc.lib.net

import cc.lib.utils.NoDupesMap

/**
 * <pre>
 * An Extendable Enum
 * We go with this scheme for the sake of extensibility since we cannot extend an enum.
 *
 * It still has the things we like about Enum like ordinal and allowing == for comparisons.
 * However we do lose the ability to use switch-case statements,
 * but this is acceptable since if-else work the same if not quite as pretty.
 *
 * Example:
 *
 * class MyGameCommandType extends GameCommandType {
 * MyGameCommandType(String name) {
 * super(name);
 * }
 *
 * static final MyGameCommandType CMD1 = new MyGameCommandType("CMD1");
 * }
 *
 * later ...
 *
 * GameCommand cmd = new GameCommand(MyGameCommandType.CMD1).setArg("x", "y");
 * cmd.write(out);
 *
 * and will arrive at destination:
 *
 * server.onClientCommand(client, cmd);
 *
 * or
 *
 * GameClient.onCommand(cmd);
</pre> *
 *
 * @author ccaron
 */
class GameCommandType(private val mName: String) : Comparable<GameCommandType> {
	private val mOrdinal: Int

	/**
	 *
	 * @param name
	 */
	init {
		mOrdinal = instances.size
		instances[mName] = this
	}

	/**
	 * Just like enum
	 * @return
	 */
	fun ordinal(): Int {
		return mOrdinal
	}

	override operator fun compareTo(arg0: GameCommandType): Int {
		return mOrdinal.compareTo(arg0.mOrdinal)
	}

	/**
	 *
	 * @return
	 */
	fun name(): String {
		return mName
	}

	override fun toString(): String {
		return mName
	}

	fun make(): GameCommand {
		return GameCommand(this)
	}

	companion object {
		private val instances: MutableMap<String, GameCommandType> = NoDupesMap(LinkedHashMap())

		// These commands are all package access only and are handled internally.
		// --------------------------------------
		// Command sent from the client
		// --------------------------------------
		// response from a waiting execOnRemote
		@JvmField
		val CL_REMOTE_RETURNS = GameCommandType("CL_REMOTE_RETURNS")

		// additional info is name and version
		@JvmField
		val CL_CONNECT = GameCommandType("CL_CONNECT")

		// no additional info
		@JvmField
		val PING = GameCommandType("PING")

		@JvmField
		val CL_CONNECTION_SPEED = GameCommandType("CL_CONNECTION_SPEED")

		// report an error that occured on the client
		@JvmField
		val CL_ERROR = GameCommandType("CL_ERROR")

		// client signals they are disconnecting
		@JvmField
		val CL_DISCONNECT = GameCommandType("CL_DISCONNECT")

		// --------------------------------------
		// commands sent from the server
		// --------------------------------------
		// confirmation command from the server that a client has connected
		@JvmField
		val SVR_CONNECTED = GameCommandType("SVR_CONNECTED")

		@JvmField
		val SVR_EXECUTE_REMOTE = GameCommandType("SVR_EXEC_REMOTE")

		// server asks client to disconnect from their end
		@JvmField
		val SVR_DISCONNECT = GameCommandType("SVR_DISCONNECT")

		// --------------------------------------
		// shared command types
		// --------------------------------------
		@JvmField
		val MESSAGE = GameCommandType("MESSAGE")

		// confirmation from server when client asks to be disconnected
		@JvmField
		val PASSWORD = GameCommandType("PASSWORD")

		@JvmField
		val PROPERTIES = GameCommandType("PROPERTIES")

		/**
		 * Just like enum
		 * @return
		 */
		fun values(): Iterable<GameCommandType> {
			return instances.values
		}

		/**
		 *
		 * @param id
		 * @return
		 * @throws IllegalArgumentException
		 */
		@JvmStatic
		@Throws(IllegalArgumentException::class)
		fun valueOf(id: String): GameCommandType {
			return instances[id] ?: throw java.lang.IllegalArgumentException("Unknown GameComamndType '$id'")
		}
	}
}
