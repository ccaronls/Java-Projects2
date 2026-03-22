package cc.lib.ksp.netcmd

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.StringReader
import java.io.StringWriter

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
					null -> writeByte(0)
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

					is IntArray -> {
						writeByte(8)
						writeInt(value.size)
						value.forEach {
							writeInt(it)
						}
					}

					is FloatArray -> {
						writeByte(9)
						writeInt(value.size)
						value.forEach {
							writeFloat(it)
						}
					}

					is LongArray -> {
						writeByte(10)
						writeInt(value.size)
						value.forEach {
							writeLong(it)
						}
					}

					is DoubleArray -> {
						writeByte(11)
						writeInt(value.size)
						value.forEach {
							writeDouble(it)
						}
					}

					is ShortArray -> {
						writeByte(12)
						writeInt(value.size)
						value.forEach {
							writeShort(it.toInt())
						}
					}

					is ISerializable -> {
						writeByte(13)
						writeUTF(value.javaClass.canonicalName.toString())
						StringWriter().use {
							value.serialize(it)
							writeUTF(it.buffer.toString())
						}
					}

					else -> throw IllegalArgumentException("Don't know how to encode ${value.javaClass}")
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

				8 -> IntArray(input.readInt()) {
					input.readInt()
				}

				9 -> FloatArray(input.readInt()) {
					input.readFloat()
				}

				10 -> LongArray(input.readInt()) {
					input.readLong()
				}

				11 -> DoubleArray(input.readInt()) {
					input.readDouble()
				}

				12 -> ShortArray(input.readInt()) {
					input.readShort()
				}

				13 -> {
					INetCommand::javaClass.javaClass.classLoader.loadClass(input.readUTF()).newInstance().also { obj ->
						StringReader(input.readUTF()).use {
							(obj as ISerializable).deserialize(it)
						}
					}
				}

				else -> throw IOException("Unknown code $code")
			}
		}

		fun print(value: Any?): String {
			fun String.quotify(): String = "\"$this\""
			return (value as? ByteArray)?.joinToString(
				prefix = "[",
				postfix = "]",
				limit = 16,
				truncated = "..."
			) ?: (value as? IntArray)?.joinToString(
				prefix = "[",
				postfix = "]",
			) ?: (value as? FloatArray)?.joinToString(
				prefix = "[",
				postfix = "]",
			) ?: (value as? LongArray)?.joinToString(
				prefix = "[",
				postfix = "]",
			) ?: (value as? DoubleArray)?.joinToString(
				prefix = "[",
				postfix = "]",
			) ?: (value as? ShortArray)?.joinToString(
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