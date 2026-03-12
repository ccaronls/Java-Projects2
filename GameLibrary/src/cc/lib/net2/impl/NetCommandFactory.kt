package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream

@NetCommand
interface ClConnect : INetCommand {
	val name: String
	val id: Int
	val version: Int
}

@NetCommand
interface ClDisconnect : INetCommand {
	val reason: String
}

@NetCommand
interface SvrConnected : INetCommand {
	val id: Int // if zero then connection denied, see message for reason
	val udpPort: Int
	val message: String
}

@NetCommand
interface SvrStopped : INetCommand

// Client <-> server property changed request
class CommProperty(val key: String, val value: Any) : INetCommand {

	// TODO: Make this manufacturable by NetCommandProcessor
	override val serializedName = _ID

	override fun write(stream: OutputStream) {
		with(stream.toDataOutputStream()) {
			writeUTF("CommProperty")
			writeUTF(key)
			when (value) {
				is Int -> {
					writeByte(1)
					writeInt(value)
				}

				is Float -> {
					writeByte(2)
					writeFloat(value)
				}

				is Boolean -> {
					writeByte(3)
					writeBoolean(value)
				}

				is Long -> {
					writeByte(4)
					writeLong(value)
				}

				is String -> {
					writeByte(5)
					writeUTF(value)
				}

				is Double -> {
					writeByte(6)
					writeDouble(value)
				}

				is ByteArray -> {
					writeByte(7)
					writeInt(value.size)
					write(value)
				}
			}
		}
	}

	companion object {

		const val _ID = "CommProperty"

		private fun parse(code: Int, input: DataInputStream): Any = when (code) {
			1 -> input.readInt()
			2 -> input.readFloat()
			3 -> input.readBoolean()
			4 -> input.readLong()
			5 -> input.readUTF()
			6 -> input.readDouble()
			7 -> input.read(ByteArray(input.readInt()))
			else -> throw NetException("Unknown code $code")
		}

		fun read(input: InputStream): CommProperty {
			with(input.toDataInputStream()) {
				return CommProperty(
					readUTF(),
					parse(readByte().toInt(), this)
				)
			}
		}
	}
}
