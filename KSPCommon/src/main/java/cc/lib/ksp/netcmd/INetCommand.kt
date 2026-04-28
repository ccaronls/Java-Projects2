package cc.lib.ksp.netcmd

import java.io.ByteArrayOutputStream
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

					is List<*> -> {
						writeByte(13)
						writeInt(value.size)
						value.forEach {
							encode(output, it)
						}
					}

					is Enum<*> -> {
						writeByte(14)
						writeUTF(value.javaClass.canonicalName.toString())
						writeUTF(value.name)
					}

					is ISerializable -> {
						writeByte(15)
						writeUTF(value.javaClass.canonicalName.toString())
						val sw = StringWriter().also {
							it.use {
								value.serialize(it)
							}
						}
						val array = sw.buffer.toString().toByteArray()
						writeInt(array.size)
						write(array)
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

				13 -> ArrayList<Any?>().also {
					for (i in 0 until input.readInt()) {
						it.add(decode(input))
					}
				}

				14 -> {
					val enumClassName = input.readUTF()
					val enumName = input.readUTF()
					val enumClazz = INetCommand::javaClass.javaClass.classLoader.loadClass(enumClassName)
					enumClazz.enumConstants.first { (it as Enum<*>).name == enumName }
				}

				15 -> {
					val objName = input.readUTF()
					INetCommand::javaClass.javaClass.classLoader.loadClass(objName).newInstance().also { obj ->
						val len = input.readInt()
						val bytes = ByteArray(len)
						input.readFully(bytes)
						StringReader(String(bytes)).use {
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

		fun computeSizeBytes(cmd: INetCommand): Int {
			with(ByteArrayOutputStream()) {
				use {
					cmd.write(it)
				}
				return size()
			}
		}
	}
}