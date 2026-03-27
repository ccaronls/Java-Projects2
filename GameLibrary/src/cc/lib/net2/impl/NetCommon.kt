package cc.lib.net2.impl

import cc.lib.net2.INetContext
import cc.lib.utils.weakReference
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val PRIME = 37039

class NetException(msg: String) : IOException(msg)

fun InputStream.toDataInputStream() = if (this is DataInputStream) this else DataInputStream(this)
fun OutputStream.toDataOutputStream() = if (this is DataOutputStream) this else DataOutputStream(this)

/**
 * Not to be confused with 'readFully' (which doesn't do what it name implies)
 * readUntilFull continuously calls 'read' until the array id full
 */
fun InputStream.readUntilFull(array: ByteArray) {
	var bytesRead = read(array)
	if (bytesRead >= 0) {
		while (bytesRead < array.size) {
			val b = read(array, bytesRead, array.size - bytesRead)
			if (b < 0)
				break;
			bytesRead += b
		}
	}
}

fun getSecretCode(): Long {
	val t = System.currentTimeMillis().and(0x00000000ffffffff)
	val s = (t + PRIME) % PRIME
	return s.shl(32).or(t)
}

fun validateSecretCode(x: Long): Boolean {
	val t = x.and(0x00000000ffffffff)
	val sc = x.shr(32)
	val s = (t + PRIME) % PRIME
	return s == sc
}

class MirroredHashMap(context: INetContext, vararg lockedKeys: String) : HashMap<String, Any?>() {

	private val _context by weakReference(context)
	private val _lockedKeys = lockedKeys

	override fun put(key: String, value: Any?): Any? {
		if (key in _lockedKeys)
			throw IllegalArgumentException("key $key is locked")
		val orig = get(key)
		if (orig != value) {
			super.put(key, value)
			runBlocking {
				_context?.sendTCP(CommPropertyImpl(key, value))
			}
		}
		return orig
	}

	fun update(key: String, value: Any?): Boolean {
		if (get(key) != value) {
			if (value == null) {
				super.remove(key)
			} else {
				super.put(key, value)
			}
			return true
		}
		return false
	}
}

fun MutableMap<String, Any?>.toggle(key: String) {
	(get(key) as? Boolean)?.let {
		put(key, !it)
	}
}
