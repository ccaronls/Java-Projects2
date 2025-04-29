package cc.lib.ksp.binaryserializer

import java.nio.ByteBuffer

fun ByteBuffer.writeBoolean(value: Boolean) = put(if (value) 1 else 0)

fun ByteBuffer.readBoolean() = get().toInt() != 0

fun ByteBuffer.writeByte(value: Int) {
	require(value in Byte.MIN_VALUE..Byte.MAX_VALUE)
	put(value.toByte())
}

fun ByteBuffer.readByte(): Int = get().toInt()

fun ByteBuffer.writeUByte(value: Int) {
	require(value in 0..UByte.MAX_VALUE.toInt())
	put(value.toByte())
}

fun ByteBuffer.readUByte(): Int = get().toInt() and 0xff

fun ByteBuffer.writeShort(value: Int) {
	require(value in Short.MIN_VALUE..Short.MAX_VALUE)
	putShort(value.toShort())
}

fun ByteBuffer.readShort() = getShort().toInt()

fun ByteBuffer.writeUShort(value: Int) {
	require(value in 0..UShort.MAX_VALUE.toInt())
	putShort(value.toShort())
}

fun ByteBuffer.readUShort(): Int = getShort().toInt() and 0xffff

fun ByteBuffer.writeInt(value: Int) = putInt(value)

fun ByteBuffer.readInt(): Int = getInt()

fun ByteBuffer.writeUInt(value: UInt) {
	require(value in UInt.MIN_VALUE..UInt.MAX_VALUE)
	putInt(value.toInt())
}

fun ByteBuffer.readUInt(): UInt = getInt().toUInt() and 0xffffffffu

fun ByteBuffer.writeFloat(value: Float) = putFloat(value)

fun ByteBuffer.readFloat(): Float = getFloat()

fun ByteBuffer.writeLong(value: Long) = putLong(value)

fun ByteBuffer.readLong(): Long = getLong()

fun ByteBuffer.writeULong(value: ULong) = putLong(value.toLong())

fun ByteBuffer.readULong(): ULong = getLong().toULong() and 0xffffffffffffffffu

fun ByteBuffer.writeString(s: String) {
	val array = s.toByteArray()
	writeShort(array.size)
	put(array)
}

fun ByteBuffer.readString(): String {
	val len = readUShort()
	val buffer = ByteArray(len)
	get(buffer)
	return String(buffer)
}