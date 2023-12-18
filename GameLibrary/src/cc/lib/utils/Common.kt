package cc.lib.utils

import cc.lib.game.Utils
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

fun <T> MutableMap<T, Int>.increment(obj: T, amt: Int) {
    when (val count = get(obj)) {
	    null -> put(obj, amt)
        else -> put(obj, count + amt)
    }
}

fun Char.repeat(times: Int) : String {
    val buf = StringBuffer(times)
    for (i in 0 until times) {
        buf.append(this)
    }
    return buf.toString()
}

fun <T> List<T>.appendedWith(other: Collection<T>) : MutableList<T> {
    val a = ArrayList(this)
    a.addAll(other)
    return a
}

fun <T> MutableList<T>.removeRandom() : T {
    val item = random()
    assert(remove(item))
    return item
}

fun Iterable<Any>.toStringArray(pretty: Boolean) : Array<String> {
    return map { if (pretty) Utils.toPrettyString(it) else it.toString() }.toTypedArray()
}

fun Int.rotate(max: Int) : Int {
    return plus(1) % max
}

fun Any?.prettify(): String {
	return Utils.toPrettyString(toString())
}

fun String.wrap(maxChar: Int) : String {
	return Utils.wrapTextWithNewlines(this, maxChar)
}

fun isEqual(a: Any?, b: Any?) : Boolean {
	return Utils.isEquals(a, b)
}

fun hashCode(vararg objects: Any?) : Int {
	return Utils.hashCode(objects)
}

/**
 * Return random in range 0 to this
 */
fun Float.random() : Float {
	return Utils.randFloat(this)
}

/**
 * return + or - random in range 0-this
 */
fun Float.randomSigned() : Float {
	return Utils.randFloatX(this)
}

/**
 * return random int in range 0 to this-1
 */
fun random(range: Int) : Int {
	return if (range > 0) Utils.rand() % range else -1
}

/**
 * Return random int in range
 */
fun random(range: IntRange) : Int {
	return Utils.randRange(range.first, range.last)
}

/**
 *
 */
fun <T> List<T>.random() : T {
	return get(random(size))
}

/**
 * Return a random number between [0-size) where the likelyhood of a number is based on the weight value
 * at that index. Example: for array[0,1,2], the possible results and their probability are:
 * 0 -> 0 out of 3
 * 1 -> 1 out of 3
 * 2 -> 2 out of 3
 */
fun IntArray.randomWeighted() : Int {
	return Utils.chooseRandomFromSet(*this)
}

/**
 *
 */
fun isEmpty(obj: Any?) : Boolean {
	return Utils.isEmpty(obj)
}

/**
 * left rotate all elems in the array and return the first elem shifted off.
 */
fun IntArray.popFirst(fillLastElem: Int = 0) : Int  {
	return Utils.popFirst(this).also {
		set(size - 1, fillLastElem)
	}
}

fun <R, T> List<R>.join(elems: Iterator<T>) : List<Pair<R, T>> {
	val list = mutableListOf<Pair<R, T>>()
	var index = 0
	elems.forEach { r ->
		if (index < size) {
			list.add(Pair(get(index), r))
			index ++
		}
	}
	return list
}

fun List<Any>.prettify() : Array<String> {
	return map { it.prettify() }.toTypedArray()
}

fun <T : Comparable<T>> T.increment(steps: Int, values: Array<T>) : T {
	var idx = Utils.linearSearch(values, this)
	idx = (idx + steps) % values.size
	return values[idx]
}

fun <K, V> MutableMap<K, V>.removeAll(predicate: (entry: MutableMap.MutableEntry<K, V>) -> Boolean) {
	with(iterator()) {
		while (hasNext()) {
			if (predicate.invoke(next()))
				remove()
		}
	}
}

fun flipCoin() : Boolean = random(2) == 0

fun String.toFile() = File(this)

fun assert(expr: Boolean, msg: String) {
	if (!expr) {
		throw AssertionError(msg)
	}
}

fun String.appendDelimited(delim: String, obj: Any) : String {
	if (isEmpty()) {
		return obj.toString()
	}
	return this + delim + obj.toString()
}

fun <T : Collection<*>> takeIfNotEmpty(collection: T?) : T? {
	return collection?.takeIf { it.isNotEmpty() }
}

fun <T> weakReference(tIn : T? = null) : ReadWriteProperty<Any?, T?> = object : ReadWriteProperty<Any?, T?> {
	var t = WeakReference(tIn)

	override fun getValue(thisRef: Any?, property: KProperty<*>): T? = t.get()

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
		t = WeakReference(value)
	}
}

fun Long.toHMS() : IntArray {
	var secs = this
	var mins = secs / 60
	secs -= mins * 60
	val hours = mins / 60
	mins -= hours * 60
	return intArrayOf(hours.toInt(), mins.toInt(), secs.toInt())
}

fun Int.toHMS() : IntArray = toLong().toHMS()

/**
 * For values between 0 and 1 will return a string between '0%' to '100%'
 */
fun Float.toPercentString() : String = String.format("%d%%", (100f*this).roundToInt())

fun computeFloydWarshallDistanceMatrix(numCells : Int, predicate : (from: Int, to: Int) -> Boolean): Array<IntArray> {
	val numV = numCells
	val INF = Int.MAX_VALUE / 2 - 1
	val dist = Array(numV) { IntArray(numV) }
	for (i in 0 until numV) {
		for (ii in 0 until numV) {
			dist[i][ii] = INF
		}
		dist[i][i] = 0
	}
	for (i in 0 until numCells) {
		for (ii in i+1 until numCells) {
			require (i != ii)
			if (predicate.invoke(i, ii)) {
				dist[i][ii] = 1
				dist[ii][i] = 1
			}
		}
	}
	for (k in 0 until numV) {
		for (i in 0 until numV) {
			for (j in 0 until numV) {
				dist[i][j] = Math.min(dist[i][k] + dist[k][j], dist[i][j])
			}
		}
	}
	return dist
}

fun <T> Stack<T>.peekOrNull() : T? {
	if (size == 0)
		return null
	return peek()
}

fun <K, V> MutableMap<K, V>.getOrSet(key: K, creator: () -> V): V {
	get(key)?.let {
		return it
	}
	with(creator()) {
		set(key, this)
		return this
	}
}

fun <T> List<T>.getOrNull(index: Int) : T? {
	if (index in 0 until size)
		return get(index)
	return null
}


fun String.trimmedToSize(maxLen : Int) : String {
	if (length > maxLen) {
		return substring(0, maxLen)
	}
	return this
}

fun Boolean.toInt() : Int = if (this) 1 else 0

fun Int.toBoolean() : Boolean = this != 0

inline fun <reified T> Any.takeIfInstance() : T? = if (this is T) this else null

fun String.padToFit(width: Int) : String {
	val diff = width - length
	if (diff <= 0)
		return this
	// for odd remainders, give more padding to the front
	if (diff % 2 == 1) {
		return padStart(1 + diff / 2).padEnd(diff / 2)
	}

	return padStart(diff / 2).padEnd(diff / 2)
}

inline fun <reified T : Enum<T>> T.increment(amt: Int, values: Array<T> = enumValues()): T {
	val idx = values.indexOf(this).coerceAtLeast(0)
	return values[(idx + amt + values.size) % values.size]
}

inline fun <T, S> Iterable<T>.forEachAs(action: (S) -> Unit) {
	(this as Iterable<S>).forEach(action)
}

inline fun <T, S> T.transform(func: (T) -> S): S = func(this)

inline fun KMutableProperty<Boolean>.toggle() {
	setter.call(getter.call().not())
}

fun Float.formated(fmt: String): String = String.format(fmt, toString())

fun String.trimQuotes(): String = trimStart(' ', '\"').trimEnd(' ', '\"')
