package cc.lib.net;

import cc.lib.net.api.IGameCommand
import cc.lib.reflector.RPrintWriter
import cc.lib.reflector.Reflector
import cc.lib.utils.GException
import cc.lib.utils.NoDupesMap
import cc.lib.utils.takeIfInstance
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * <pre>
 * GameCommand is the protocol used for clients and servers to talk to each other.
 *
 * Only logic to encode and decode commands and access the data is here.
 *
 * Recommended example of extending this class to keep protocol logic abstracted:
 *
 *  // all encoding/decoding logic is isolated to this class
 * class MyCommands {
 *    final static GameCommand CMD1 = new GameCommand("CMD1");
 *    final static GameCommand CMD2 = new GameCommand("CMD2");
 *
 *    interface Listener {
 *       void onCMD1(String arg1);
 *       void onCMD2(int arg1, int arg2);
 *    }
 *
 *    // server commands.  server gets an instance of the command it wishes to send.
 *    GameCommand getCMD1(String arg) {
 *       return new GameCommand(CMD1).setArg("arg", arg);
 *    }
 *
 *    GameCommand getCMD2(int a, int b) {
 *       return new GameCommand(CMD2).setArg("a", a).setArg("b", b);
 *    }
 *
 *    // this is typically called from GameClient.onCommand(cmd)
 *    public static boolean clientDecode(Listener listener, GameCommand cmd) {
 *       if (cmd.getType() == CMD1) {
 *          listener.onCMD1(cmd.getArg("arg"));
 *       } else if (cmd.getType() == CMD2) {
 *          listener.onCMD2(Integer.parseInt(cmd.getArg("a")), Integer.parseInt(cmd.getArg("b")));
 *       } else {
 *          return false; // tell caller we did not handle the command
 *       }
 *
 *       return true;
 *    }
 * }
 *  </pre>
 * @author ccaron
 *
 */
class GameCommand(val type: GameCommandType) : IGameCommand {

	private val _arguments: MutableMap<String, Any> = NoDupesMap(LinkedHashMap())

	val arguments: Map<String, Any> = _arguments

	/**
	 *
	 * @param key
	 * @return
	 */
	override fun getString(key: String, defaultValue: String): String = (arguments[key] as String?) ?: defaultValue

	override fun getBoolean(key: String, defaultValue: Boolean): Boolean = (arguments[key] as Boolean?) ?: defaultValue

	override fun getInt(key: String, defaultValue: Int): Int = (arguments[key] as Int?) ?: defaultValue

	override fun getFloat(key: String, defaultValue: Float): Float = (arguments[key] as Float?) ?: defaultValue

	override fun getLong(key: String, defaultValue: Long): Long = (arguments[key] as Long?) ?: defaultValue

	override fun getDouble(key: String, defaultValue: Double): Double = (arguments[key] as Double?) ?: defaultValue

	@Throws(Exception::class)
	override fun <T : Reflector<T>> getReflector(key: String, obj: T): T {
		obj.merge(arguments[key] as String?)
		return obj
	}

	inline fun <reified T : Reflector<T>> getReflector(key: String): T {
		return Reflector.deserializeFromString(getString(key))
	}

	@Throws(Exception::class)
	fun <T : cc.lib.kreflector.KReflector<T>> getKReflector(key: String, obj: T): T {
		obj.merge(arguments[key] as String)
		return obj
	}

	/**
	 * @return
	 */
	fun getVersion(): String = getString("version", "")
	fun getName(): String = getString("name", "")
	fun getMessage(): String = getString("message", "")

	fun setArg(nm: String, value: Any?): GameCommand {
		value?.let {
			_arguments[nm] = value
		} ?: run {
			_arguments.remove(nm)
		}
		return this
	}

	fun setName(name: String) = setArg("name", name)

	fun setMessage(message: String) = setArg("message", message)

	fun setFilter(filter: String) = setArg("filter", filter)

	fun setVersion(version: String) = setArg("version", version)

	fun setArgs(args: Map<String, Any>): GameCommand {
		_arguments.putAll(args)
		return this
	}

	fun <T : Enum<T>> setEnumList(arg: String, items: Iterable<T>): GameCommand {
		_arguments[arg] = items.joinToString { it.name }
		return this
	}

	/**
	 *
	 * @param arg
	 * @param enumType
	 * @return
	 */
	inline fun <reified T : Enum<T>> getEnumList(arg: String): List<T> {
		return (arguments[arg] as? String)?.let {
			it.split("[, ]+").map {
				enumValueOf(it.trim())
			}
		} ?: emptyList()
	}

	inline fun <reified T : Enum<T>> getEnum(arg: String, defaultValue: T? = null): T? {
		return arguments[arg]?.takeIfInstance<String>()?.let {
			enumValueOf<T>(it)
		} ?: defaultValue
	}

	/**
	 *
	 * @param arg
	 * @param items
	 * @return
	 */
	fun setIntList(arg: String, items: Iterable<Int>): GameCommand {
		_arguments[arg] = items.map { it.toString() }.joinToString()
		return this
	}

	fun getIntList(arg: String): List<Int> = (arguments[arg] as? String)?.let {
		return it.split("[, ]").map {
			it.toInt()
		}
	} ?: emptyList()

	@Throws(IOException::class)
	fun write(dout: DataOutputStream) {
		dout.writeUTF(type.name())
		dout.writeInt(arguments.size)
		for (value in arguments.entries) {
			dout.writeUTF(value.key)
			when (value.value) {
				null -> dout.writeByte(TYPE_NULL)
				is Boolean -> {
					dout.writeByte(TYPE_BOOL)
					dout.writeBoolean(value.value as Boolean)
				}

				is Int -> {
					dout.writeByte(TYPE_INT)
					dout.writeInt(value.value as Int)
				}

				is Long -> {
					dout.writeByte(TYPE_LONG)
					dout.writeLong(value.value as Long)
				}

				is Float -> {
					dout.writeByte(TYPE_FLOAT)
					dout.writeFloat(value.value as Float)
				}

				is Double -> {
					dout.writeByte(TYPE_DOUBLE)
					dout.writeDouble(value.value as Double)
				}

				is String -> {
					dout.writeByte(TYPE_STRING)
					dout.writeUTF(value.value as String)
				}

				is Reflector<*> -> {
					dout.writeByte(TYPE_REFLECTOR);
					val out = ByteArrayOutputStream()
					(value.value as Reflector<*>).serialize(RPrintWriter(out))
					dout.writeInt(out.size());
					dout.write(out.toByteArray());
				}

				is cc.lib.kreflector.KReflector<*> -> {
					dout.writeByte(TYPE_KREFLECTOR);
					val out = ByteArrayOutputStream()
					(value.value as cc.lib.kreflector.KReflector<*>).serialize(RPrintWriter(out))
					dout.writeInt(out.size());
					dout.write(out.toByteArray());
				}

				else -> {
					dout.writeByte(TYPE_STRING);
					dout.writeUTF(value.value.toString());
				}
			}
		}
		dout.flush()
	}

	companion object {
		const val TYPE_NULL = 0
		const val TYPE_BOOL = 1
		const val TYPE_INT = 2
		const val TYPE_LONG = 3
		const val TYPE_FLOAT = 4
		const val TYPE_DOUBLE = 5
		const val TYPE_STRING = 6
		const val TYPE_REFLECTOR = 7
		const val TYPE_KREFLECTOR = 7

		@Throws(Exception::class)
		fun parse(din: DataInputStream): GameCommand {
			val cmd = din.readUTF()
			val type = GameCommandType.valueOf(cmd)
			val command = GameCommand(type)
			val numArgs = din.readInt()
			for (i in 0 until numArgs) {
				val key = din.readUTF()
				val itype = din.readUnsignedByte()
				when (itype) {
					TYPE_NULL -> Unit
					TYPE_BOOL -> command.setArg(key, din.readBoolean())
					TYPE_INT -> command.setArg(key, din.readInt())
					TYPE_LONG -> command.setArg(key, din.readLong())
					TYPE_FLOAT -> command.setArg(key, din.readFloat())
					TYPE_DOUBLE -> command.setArg(key, din.readDouble())
					TYPE_STRING -> command.setArg(key, din.readUTF())
					TYPE_REFLECTOR -> {
						val len = din.readInt()
						val data = ByteArray(len)
						din.readFully(data);
						command.setArg(key, String(data, Charsets.UTF_8));
					}

					else -> throw GException("Unhandled type $itype")
				}
			}
			return command
		}
	}

	override fun toString(): String = "$type : $arguments"


}
