package cc.lib.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import java.util.Stack
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

fun <T> MutableMap<T, Int>.increment(obj: T, amt: Int = 1) {
	when (val count = get(obj)) {
		null -> put(obj, amt)
		else -> put(obj, count + amt)
	}
}

fun Char.repeat(times: Int): String {
	val buf = StringBuffer(times)
	for (i in 0 until times) {
		buf.append(this)
	}
	return buf.toString()
}

fun <T> List<T>.appendedWith(other: Collection<T>): MutableList<T> {
	val a = ArrayList(this)
	a.addAll(other)
	return a
}

fun <T> MutableList<T>.removeRandom(): T {
	val item = random()
	assert(remove(item))
	return item
}

fun Int.rotate(max: Int): Int {
	return plus(1) % max
}


fun <R, T> List<R>.join(elems: Iterator<T>): List<Pair<R, T>> {
	val list = mutableListOf<Pair<R, T>>()
	var index = 0
	elems.forEach { r ->
		if (index < size) {
			list.add(Pair(get(index), r))
			index++
		}
	}
	return list
}

fun Int.increment(max: Int, amt: Int = 1): Int = plus(amt).mod(max)

fun <K, V> MutableMap<K, V>.removeAll(predicate: (entry: MutableMap.MutableEntry<K, V>) -> Boolean) {
	with(iterator()) {
		while (hasNext()) {
			if (predicate.invoke(next()))
				remove()
		}
	}
}

fun flipCoin(): Boolean = (Math.random().toInt() % 2) == 0

fun String.toFile() = File(this)

fun assert(expr: Boolean, msg: String) {
	if (!expr) {
		throw AssertionError(msg)
	}
}

fun String.appendDelimited(delim: String, obj: Any): String {
	if (isEmpty()) {
		return obj.toString()
	}
	return this + delim + obj.toString()
}

fun <T : Collection<*>> takeIfNotEmpty(collection: T?): T? {
	return collection?.takeIf { it.isNotEmpty() }
}

fun <T> weakReference(tIn: T? = null): ReadWriteProperty<Any?, T?> = object : ReadWriteProperty<Any?, T?> {
	var t = WeakReference(tIn)

	override fun getValue(thisRef: Any?, property: KProperty<*>): T? = t.get()

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
		t = WeakReference(value)
	}
}

fun Long.toHMS(): IntArray {
	var secs = this
	var mins = secs / 60
	secs -= mins * 60
	val hours = mins / 60
	mins -= hours * 60
	return intArrayOf(hours.toInt(), mins.toInt(), secs.toInt())
}

fun Int.toHMS(): IntArray = toLong().toHMS()

/**
 * For values between 0 and 1 will return a string between '0%' to '100%'
 */
fun Float.toPercentString(): String = String.format("%d%%", (100f * this).roundToInt())

fun <T> Stack<T>.peekOrNull(): T? {
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

fun <T> List<T>.getOrNull(index: Int): T? {
	if (index in 0 until size)
		return get(index)
	return null
}


fun String.trimmedToSize(maxLen: Int): String {
	if (length > maxLen) {
		return substring(0, maxLen)
	}
	return this
}

fun Boolean.toInt(): Int = if (this) 1 else 0

fun Int.toBoolean(): Boolean = this != 0

inline fun <reified T> Any.takeIfInstance(): T? = if (this is T) this else null

fun String.padToFit(width: Int): String {
	val diff = width - length
	if (diff <= 0)
		return this
	// for odd remainders, give more padding to the front
	if (diff % 2 == 1) {
		return " ".repeat(1 + diff / 2) + this + " ".repeat(diff / 2)
	}

	return " ".repeat(diff / 2) + this + " ".repeat(diff / 2)
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

fun Float.formatted(fmt: String): String = String.format(fmt, toString())

fun String.trimQuotes(): String = trimStart(' ', '\"').trimEnd(' ', '\"')

fun launchIn(
	scope: CoroutineContext = Dispatchers.Default,
	block: suspend CoroutineScope.() -> Unit
): Job = CoroutineScope(scope).launch { block() }

@Throws
inline fun fatal(msg: String): Nothing {
	exitProcess(1)
	error(msg)
}

/**
 * Return whether any of elements of other exist in this array
 *
 * [0,1,2,3,4].containsAny([5,5,5,5,5,0]) -> true
 */
fun <T> Array<T>.containsAny(other: Iterable<T>): Boolean {
	other.forEach {
		if (contains(it))
			return true
	}
	return false
}

/**
 * Splits a collection into 2 parts. Result is a pair with positive results in first and negative results in second
 *
 * [0,1,2,3,4,5,6,7].splitFilter {
 *    it < 5
 * } -> Pair([0,1,2,3,4], [5,6,7]]
 */
fun <T> Iterable<T>.splitFilter(predicate: (T) -> Boolean): kotlin.Pair<Iterable<T>, Iterable<T>> {
	val positive = mutableListOf<T>()
	val negative = mutableListOf<T>()
	forEach {
		if (predicate(it)) {
			positive.add(it)
		} else {
			negative.add(it)
		}
	}
	return kotlin.Pair(positive, negative)
}

fun <T> Iterable<T>.splitFilterIndexed(predicate: (Int, T) -> Boolean): kotlin.Pair<Iterable<T>, Iterable<T>> {
	val positive = mutableListOf<T>()
	val negative = mutableListOf<T>()
	forEachIndexed { index, it ->
		if (predicate(index, it)) {
			positive.add(it)
		} else {
			negative.add(it)
		}
	}
	return kotlin.Pair(positive, negative)
}

fun CharArray.asString() = String(this)

fun <T> test(expr: Boolean, ifTrue: T, ifFalse: T): T = if (expr) ifTrue else ifFalse

fun <T> MutableList<T>.clearAndAddAll(items: Iterable<T>) {
	clear()
	addAll(items)
}

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?): T? {
	return try {
		name?.let {
			java.lang.Enum.valueOf(T::class.java, it)
		}
	} catch (e: IllegalArgumentException) {
		null
	}
}

/**
 * Returns all elements that represent the max from an unordered collection
 *
 * for items [1,2,3,3,3] -> [3,3,3]
 */
inline fun <T, R : Comparable<R>> Collection<T>.allMaxOf(selector: (T) -> R): List<T> {
	val s: Map<R, List<T>> = groupBy(selector)
	val max = s.maxOfOrNull { it.key } ?: return emptyList()
	return s[max]!!
}

/**
 * Returns all elements that represent the min from an unordered collection
 *
 * for items [1,1,1,2,2,3] -> [1,1,1]
 */
inline fun <T, R : Comparable<R>> Collection<T>.allMinOf(selector: (T) -> R): List<T> {
	val s: Map<R, List<T>> = groupBy(selector)
	val min = s.minOfOrNull { it.key } ?: return emptyList()
	return s[min]!!
}

inline fun <T0, T1> notNull(t0: T0?, t1: T1?, block: (T0, T1) -> Unit) {
	if (t0 != null && t1 != null) block(t0, t1)
}
