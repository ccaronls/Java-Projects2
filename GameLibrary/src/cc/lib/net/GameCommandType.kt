package cc.lib.net

import cc.lib.utils.NoDupesMap
import cc.lib.utils.Table
import kotlin.math.max
import kotlin.math.min

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
	private val mOrdinal = instances.size

	private class Stat {
		var min = 0L
			private set
		var max = 0L
			private set
		var avg = 0.0
			private set
		var total = 0L
			private set
		var num = 0L
			private set


		fun reset() {
			min = 0
			max = 0
			avg = 0.0
			total = 0
			num = 0
		}

		fun add(sizeBytes: Long) {
			if (total + sizeBytes < 0)
				return
			total += sizeBytes
			if (min == 0L)
				min = sizeBytes
			else
				min = min(min, sizeBytes)
			max = max(max, sizeBytes)
			num++
			avg = total.toDouble() / num
		}
	}

	private val stats = Stat()

	fun addStat(sizeBytes: Long) {
		stats.add(sizeBytes)
	}

	init {
		instances[mName] = this
	}

	/**
	 * Just like enum
	 * @return
	 */
	fun ordinal(): Int {
		return mOrdinal
	}

	override operator fun compareTo(other: GameCommandType): Int {
		return mOrdinal.compareTo(other.mOrdinal)
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
		val CL_PING = GameCommandType("CL_PING")

		@JvmField
		val CL_CONNECTION_SPEED = GameCommandType("CL_CONNECTION_SPEED")

		// report an error that occurred on the client
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

		@JvmField
		val SVR_PONG = GameCommandType("SVR_PONG")

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
			return instances[id] ?: throw java.lang.IllegalArgumentException("Unknown GameCommandType '$id'")
		}

		fun getStatsTable(): Table {
			val stats = instances.map { it.value to it.value.stats }.toMap()
			return Table()
				.addColumn("TYPE", stats.keys.toList())
				.addColumn("NUM", stats.values.map { it.num })
				.addColumn("TOTAL", stats.values.map { it.total })
				.addColumn("MIN", stats.values.map { it.min })
				.addColumn("MAX", stats.values.map { it.max })
				.addColumn("AVG", stats.values.map { String.format("%0.2f", it.avg) })
		}

		fun resetStats() {
			instances.forEach {
				it.value.stats.reset()
			}
		}
	}
}
