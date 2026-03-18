package cc.lib.ksp.netcmd

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Created by Chris Caron on 3/2/26.
 */

interface INetCommand {

	val serializedName: String

	@Throws(IOException::class)
	fun write(stream: OutputStream)

	companion object {

		fun encode(output: DataOutputStream, value: Any?) {
			with(output) {
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

					else -> writeByte(0)
				}
			}
		}

		fun decode(input: DataInputStream): Any? {
			return when (val code = input.readByte().toInt()) {
				0 -> null
				1 -> input.readInt()
				2 -> input.readFloat()
				3 -> input.readBoolean()
				4 -> input.readLong()
				5 -> input.readUTF()
				6 -> input.readDouble()
				7 -> ByteArray(input.readInt()).also {
					input.read(it)
				}

				else -> throw IOException("Unknown code $code")
			}
		}

		fun print(value: Any?): String {
			fun String.quotify(): String = "\"$this\""
			return (value as? ByteArray)?.joinToString(
				prefix = "[",
				postfix = "]",
			) ?: (value as? Array<*>)?.joinToString(
				prefix = "[",
				postfix = "]",
			) ?: (value as? String)?.quotify()
			?: value?.toString() ?: "null"
		}
	}
}