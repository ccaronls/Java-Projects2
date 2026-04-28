package cc.lib.net.impl

import cc.lib.net.INetContext
import cc.lib.net.INetListener
import cc.lib.utils.contains
import cc.lib.utils.weakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.net.NetworkInterface

const val NET_DEBUG = false

const val PRIME = 37039
const val DISPLAY_NAME = "displayName"
const val DISCOVERY_PACKET_SIZE = 256
const val DISCOVERY_PORT = 9999
const val DISCOVERY_REFRESH_PERIOD = 3000L // millis

class NetException(msg: String) : IOException(msg)

fun InputStream.toDataInputStream() = if (this is DataInputStream) this else DataInputStream(this)
fun OutputStream.toDataOutputStream() = if (this is DataOutputStream) this else DataOutputStream(this)

fun DataOutputStream.writeEnum(e: Enum<*>) {
	writeShort(e.ordinal)
}

inline fun <reified T : Enum<T>> DataInputStream.readEnum(): T {
	return enumValues<T>()[readShort().toInt()]
}

fun DataOutputStream.writeBooleans(vararg bools: Boolean) {
	require(bools.size <= 32)
	writeByte(bools.size)
	writeInt(boolsToInt(*bools))
}

fun DataInputStream.readBooleans(setter: (bools: BooleanArray) -> Unit) {
	val num = readByte().toInt()
	require(num <= 32)
	setter(boolsFromInt(readInt(), num))
}

fun boolsToInt(vararg bools: Boolean): Int {
	var flag = 0
	bools.forEachIndexed { index, b ->
		val i = if (b) (1 shl index) else 0
		flag = flag or i
	}
	return flag
}

fun boolsFromInt(flag: Int, cnt: Int): BooleanArray {
	return BooleanArray(cnt) { i ->
		val b = flag and (1 shl i)
		b != 0
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
			_context?.sendTCP(CommPropertyImpl(key, value))
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

fun findMyIp(): InetAddress? {
	fun <T> Iterator<T>.toList(): List<T> {
		val l = mutableListOf<T>()
		forEach {
			l.add(it)
		}
		return l
	}

	return NetworkInterface.getNetworkInterfaces().iterator().toList().map {
		it.inetAddresses.toList()
	}.flatten().firstOrNull { it.hostAddress.startsWith("192.") }
}

// TODO: Allow for scope as  a parameter so we dont block network thread
class NetListener<T>(val scope: CoroutineScope) : INetListener<T> {

	private val listeners = mutableSetOf<T>()
	private val weakListeners = mutableListOf<WeakReference<T>>()
	override fun addListener(l: T) {
		listeners.add(l)
	}

	override fun removeListener(l: T) {
		listeners.remove(l)
	}

	override fun addWeakListener(l: T) {
		if (weakListeners.contains { it.get() == l })
			return
		weakListeners.add(WeakReference(l))
	}

	override fun notifyListeners(cb: suspend (T) -> Unit) {
		scope.launch {
			listeners.forEach {
				cb(it)
			}
			weakListeners.removeAll { it.get() == null }
			weakListeners.forEach { wr ->
				wr.get()?.let {
					cb(it)
				}
			}
		}
	}

}