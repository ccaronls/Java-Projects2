package cc.lib.utils

import cc.lib.game.IRectangle
import cc.lib.game.IVector2D
import cc.lib.math.MutableVector2D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.Objects
import java.util.Stack
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

fun <T> MutableMap<T, Int>.increment(obj: T, amt: Int = 1) {
	when (val count = get(obj)) {
		null -> put(obj, amt)
		else -> put(obj, count + amt)
	}
}

fun <T> List<T>.appendedWith(other: Collection<T>): MutableList<T> {
	val a = ArrayList(this)
	a.addAll(other)
	return a
}

fun <T> MutableList<T>.removeRandom(): T {
	val item = random()
	remove(item).also {
		assert(it)
	}
	return item
}

fun Any?.prettify(): String = toPrettyString(this)

fun Iterable<Any>.toStringArray(pretty: Boolean): Array<String> {
	return map { if (pretty) it.prettify() else it.toString() }.toTypedArray()
}

fun Int.rotate(max: Int): Int {
	return plus(1) % max
}

fun Int.random(): Int {
	if (this < 1)
		return 0
	return Random.nextInt(this)
}

/**
 * Return a value in the range: [0 - counts.length) where the weight of
 * each values weight is in weights[]
 *
 * @param weights
 * @return
 */
fun chooseRandomFromSet(vararg weights: Int): Int {
	val total = weights.sum()
	if (total <= 0)
		return -1

	var r = total.randomPositive()
	var i = 0
	while (i < weights.size) {
		r -= if (weights[i] <= r) {
			weights[i]
		} else {
			break
		}
		i++
	}
	assert(weights[i] > 0)
	return i
}

/**
 * Return a random number between [0-size) where the likelyhood of a number is based on the weight value
 * at that index. Example: for array[0,1,2], the possible results and their probability are:
 * 0 -> 0 out of 3
 * 1 -> 1 out of 3
 * 2 -> 2 out of 3
 */
fun IntArray.randomWeighted(): Int {
	return chooseRandomFromSet(*this)
}

/**
 *
 */
fun IntArray.randomWeighted(vararg items: Any): Any {
	return items[randomWeighted().coerceIn(items.indices)]
}

/**
 * Returns true if object is null or 'empty'.
 * If o is a string, then empty means at least 1 non whitespace char.
 * If o is a collection then empty means sie() == 0
 * If o is an array then empty means length == 0
 * otherwise not empty
 *
 * @param o
 * @return
 */
fun Any?.isEmpty(): Boolean {
	if (this == null) return true
	if (this is String) return trim { it <= ' ' }.isEmpty()
	if (this is Collection<*>) return this.isEmpty()
	if (javaClass.isArray) return java.lang.reflect.Array.getLength(this) == 0
	assert(false) { "isEmpty not compatible of object of type: $javaClass" }
	System.err.println("isEmpty does not know about class $javaClass")
	return false
}

fun Any?.isNotEmpty() = !isEmpty()

/**
 * Shift all array elements to the left and return item shifted off the front (zeroth element)
 * @param arr
 * @param <T>
 * @return
</T> */
fun <T> Array<T>.popFirst(): T {
	val top = get(0)
	for (i in 0 until size - 1) {
		set(i, get(i + 1))
	}
	return top
}

fun List<Any>.prettify(): Array<String> {
	return map { it.prettify() }.toTypedArray()
}

/**
 * Sort a secondary array based on a primary array
 *
 * @param primary
 * @param target
 * @param comp
 * @param <T>
 * @param <S>
</S></T> */
fun <T, S> bubbleSort(primary: Array<T>, target: Array<S>, comp: Comparator<T>) {
	bubbleSort(primary, target, Math.min(primary.size, target.size), comp)
}

fun <T> Array<T>.swap(idx0: Int, idx1: Int) {
	val tmp = get(idx0)
	set(idx0, get(idx1))
	set(idx1, tmp)
}

/**
 *
 * @param primary
 * @param target
 * @param length
 * @param comp
 * @param <T>
 * @param <S>
</S></T> */
fun <T, S> bubbleSort(primary: Array<T>, target: Array<S>, length: Int, comp: Comparator<T>) {
	var length = length
	assert(target.size >= length && primary.size >= length)
	var swapped = false
	do {
		swapped = false
		for (ii in 1 until length) {
			val c = comp.compare(primary[ii - 1], primary[ii])
			if (c < 0) {
				primary.swap(ii - 1, ii)
				target.swap(ii - 1, ii)
				swapped = true
			}
		}
		length--
	} while (swapped)
}

/**
 *
 * @param primary
 * @param target
 * @param <T>
 * @param <S>
</S></T> */
fun <T : Comparable<T>?, S> bubbleSort(primary: Array<T>, target: Array<S>) {
	bubbleSort(primary, target, Math.min(primary.size, target.size), false)
}

/**
 *
 * @param primary
 * @param target
 * @param length
 * @param <T>
 * @param <S>
</S></T> */
fun <T : Comparable<T>?, S> bubbleSort(primary: Array<T>, target: Array<S>, length: Int) {
	bubbleSort(primary, target, length, false)
}

/**
 *
 * @param primary
 * @param target
 * @param descending
 * @param <T>
 * @param <S>
</S></T> */
fun <T : Comparable<T>?, S> bubbleSort(primary: Array<T>, target: Array<S>, descending: Boolean) {
	bubbleSort(primary, target, Math.min(primary.size, target.size), descending)
}

/**
 *
 * @param primary
 * @param target
 * @param length
 * @param descending
 * @param <T>
 * @param <S>
</S></T> */
fun <T : Comparable<T>?, S> bubbleSort(primary: Array<T>, target: Array<S>, length: Int, descending: Boolean) {
	assert(target.size >= primary.size)
	if (primary.size < 2) return  // already sorted
	var swapped: Boolean
	var n = primary.size
	do {
		swapped = false
		for (ii in 1 until n) {
			var c = primary[ii - 1]!!.compareTo(primary[ii])
			if (descending) {
				c = -c
			}
			if (c > 0) {
				primary.swap(ii - 1, ii)
				target.swap(ii - 1, ii)
				swapped = true
			}
		}
		n--
	} while (swapped)
}

/**
 *
 * @param arr
 * @param key
 * @param <T>
 * @return
</T> */
fun <T> linearSearch(arr: Array<T>, key: T): Int {
	return linearSearch(arr, key, arr.size)
}

/**
 *
 * @param arr
 * @param key
 * @param len
 * @param <T>
 * @return
</T> */
fun <T> linearSearch(arr: Array<T>, key: T, len: Int): Int {
	for (i in 0 until len) {
		if (Objects.equals(arr[i], key)) return i
	}
	return -1
}


fun <T : Comparable<T>> T.increment(steps: Int, values: Array<T>): T {
	var idx = linearSearch(values, this)
	idx = (idx + steps) % values.size
	return values[idx]
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

fun flipCoin(): Boolean = Random.nextInt(2) == 0

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

fun List<IRectangle>.midPointOrNull(): IVector2D? {
	if (isEmpty())
		return null
	val v = MutableVector2D()
	forEach {
		v.addEq(it.center)
	}
	v.scaleEq(size.toFloat())
	return v
}

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

private var PRETTY_STRING_PATTERN: Pattern? = null

private val PRETTY_CACHE = LRUCache<String, String>(256)

/**
 * 1. Strip extension if any
 * 2. Replace [_]+ (underscores) with a space
 * 3. Make whole strings lowercase with first letter capitalized
 *
 * @param obj
 * @return
 */
fun toPrettyString(obj: Any?): String {
	if (obj == null) return "null"
	val str = obj.toString()
	var cached = PRETTY_CACHE[str]
	if (cached != null) return cached
	val pretty: String = (str.replace("[_]+".toRegex(), " ").trim { it <= ' ' }).stripExtension()
	if (PRETTY_STRING_PATTERN == null) PRETTY_STRING_PATTERN = Pattern.compile("([A-Za-z][a-zA-Z]+)|([IiAa])")
	val us = PRETTY_STRING_PATTERN!!.matcher(pretty)
	val result = StringBuffer()
	var begin = 0
	while (us.find()) {
		val s = us.group().lowercase(Locale.getDefault())
		if (result.isNotEmpty() && result[result.length - 1] != ' ' && pretty[begin] != ' ') result.append(" ")
		result.append(pretty.substring(begin, us.start()))
		begin = us.end()
		if (result.isNotEmpty() && result[result.length - 1] != ' ') result.append(" ")
		result.append(s[0].uppercaseChar()).append(s.substring(1))
	}
	if (begin >= 0 && begin < pretty.length) {
		if (result.isNotEmpty() && result[result.length - 1] != ' ' && pretty[begin] != ' ') result.append(" ")
		result.append(pretty.substring(begin))
	}
	cached = result.toString()
	PRETTY_CACHE[str] = cached
	return cached
}

/**
 *
 * @param txt
 * @param maxChars
 * @return
 */
fun String.wrap(maxChars: Int): Array<String> {
	assert(maxChars > 1)
	if (maxChars < 1) return arrayOf(this)
	val lines: MutableList<String> = java.util.ArrayList()
	var newline = indexOf('\n')
	var str: String = if (newline >= 0) substring(0, newline) else this
	while (true) {
		while (str.length > maxChars) {
			var spc = str.indexOf(' ')
			if (spc < 0 || spc > maxChars - 1) {
				lines.add(str.substring(0, maxChars - 1) + "-")
				str = str.substring(maxChars - 1)
			} else {
				while (true) {
					val nxt = str.indexOf(' ', spc + 1)
					if (nxt < 0 || nxt > maxChars) {
						break
					}
					spc = nxt
				}
				lines.add(str.substring(0, spc))
				str = str.substring(spc + 1)
			}
		}
		lines.add(str)
		if (newline >= 0) {
			val end = indexOf('\n', newline + 1)
			if (end > newline) {
				str = substring(newline + 1, end)
				newline = end
			} else {
				str = substring(newline + 1)
				newline = -1
			}
		} else {
			break
		}
	}
	return lines.toTypedArray<String>()
}

fun <T> Array<T>.copyFrom(vararg items: T) {
	repeat(size.coerceAtMost(items.size)) {
		set(it, items[it])
	}
}

fun FloatArray.copyFrom(vararg items: Float) {
	repeat(size.coerceAtMost(items.size)) {
		set(it, items[it])
	}
}

fun IntArray.copyFrom(vararg items: Int) {
	repeat(size.coerceAtMost(items.size)) {
		set(it, items[it])
	}
}

fun Int.randomPositive() = Random.nextInt(this)

fun Float.randomPositive() = Random.nextFloat() * this

fun Float.randomPositiveOrNegative() = Random.nextFloat() * (this * 2) - this

fun randRange(min: Int, max: Int): Int = Random.nextInt(max - min + 1) + min

fun randRange(range: IntRange): Int = Random.nextInt(range)

fun String.repeat(times: Int): String = StringBuffer().also {
	for (i in 0 until times)
		it.append(this)
}.toString()

/**
 * Rotates in place array elements
 *
 * example:
 * [0,1,2].rotate(1) -> [2, 1, 0]
 * [0,1,2].rotate(-1) -> [1,2,0]
 */
fun <T> Array<T>.rotate(shift: Int): Array<T> {
	var shift = shift
	while (shift > size)
		shift -= size
	while (shift < 0)
		shift += size
	while (shift > 0) {
		for (i in 0 until size - 1)
			swap(i, i + 1)
	}
	return this
}