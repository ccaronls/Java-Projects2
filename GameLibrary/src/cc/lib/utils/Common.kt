package cc.lib.utils

import cc.lib.game.GColor
import cc.lib.game.IRectangle
import cc.lib.game.IVector2D
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.util.Stack
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextUInt
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

private var RANDOM = Random(System.currentTimeMillis())

fun setRandomSeed(seed: Long) {
	RANDOM = Random(seed)
}

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

fun <T> List<T>.appendedWith(other: Collection<T>) : MutableList<T> {
    val a = ArrayList(this)
    a.addAll(other)
    return a
}

fun <T> MutableList<T>.removeRandom() : T {
	val item = random(RANDOM)
	assert(remove(item))
	return item
}

fun Iterable<Any>.toStringArray(pretty: Boolean): Array<String> {
	return map { if (pretty) Utils.toPrettyString(it) else it.toString() }.toTypedArray()
}

fun Int.rotate(max: Int): Int {
	return rotate(1, max)
}

fun Int.rotate(amt: Int, max: Int): Int {
	require(abs(amt) <= max)
	return plus(amt + max) % max
}

fun Any?.prettify(): String {
	return Utils.toPrettyString(toString())
}

fun String.withLineNumbers(): String = lines().mapIndexed { index, s ->
	String.format("%5d %s", index + 1, s)
}.joinToString("\n")

fun String.wrap(maxChar: Int): String {
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
	return randomFloat(this)
}

/**
 * return + or - random in range 0-this
 */
fun Float.randomSigned() : Float {
	return RANDOM.nextFloat() * (this * 2) - this
}

/**
 * return random int in range 0 to this-1
 */
fun random(range: Int): Int {
	return if (range > 0) RANDOM.nextInt(range) else -1
}

/**
 * Return random int in range
 */
fun random(range: IntRange): Int {
	return RANDOM.nextInt(range)
}

/**
 * Return random unsigned int in range
 */
fun random(range: UIntRange): UInt {
	return RANDOM.nextUInt(range)
}

/**
 * Return a random number between [0-size) where the likelyhood of a number is based on the weight value
 * at that index. Example: for array[0,1,2], the possible results and their probability are:
 * 0 -> 0 out of 3
 * 1 -> 1 out of 3
 * 2 -> 2 out of 3
 */
fun IntArray.randomWeighted(): Int {
	val total = sum()
	if (total <= 0) {
		return -1
	}
	var r = RANDOM.nextInt(total)
	var i = 0
	while (i < size) {
		r -= if (get(i) <= r) {
			get(i)
		} else {
			break
		}
		i++
	}
	require(get(i) > 0)
	return i
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

fun List<Any>.prettify(): Array<String> {
	return map { it.prettify() }.toTypedArray()
}

fun <T : Comparable<T>> T.increment(steps: Int, values: Array<T>): T {
	var idx = Utils.linearSearch(values, this)
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

fun flipCoin() : Boolean = random(2) == 0

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
		return " ".repeat(1 + diff / 2) + this + " ".repeat(diff / 2)
	}

	return " ".repeat(diff / 2) + this + " ".repeat(diff / 2)
}

fun String.padEndToFit(width: Int): String {
	if (length >= width)
		return this
	return this + " ".repeat(width - length)
}

inline fun <reified T : Enum<T>> T.increment(amt: Int = 1, values: Array<T> = enumValues()): T {
	val idx = values.indexOf(this).coerceAtLeast(0)
	return values[(idx + amt + values.size) % values.size]
}

inline fun <reified T : Enum<T>> T.incrementOrNull(amt: Int = 1, values: Array<T> = enumValues()): T? {
	val idx = values.indexOf(this).coerceAtLeast(0)
	return values.getOrNull(idx + amt)
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
	scope: CoroutineContext = Dispatchers.Main,
	block: suspend CoroutineScope.() -> Unit
): Job = CoroutineScope(scope).launch { block() }

fun launchIo(
	block: suspend CoroutineScope.() -> Unit
): Job = CoroutineScope(Dispatchers.IO).launch { block() }

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
	return s.maxOfOrNull { it.key }?.let {
		s[it]
	} ?: emptyList()
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

fun <T> List<T>.randomIndex() = random(indices)

fun <T> List<T>.randomIndexOrNull() = if (isNotEmpty()) random(indices) else null

fun randomFloat(max: Number): Float = RANDOM.nextFloat() * max.toFloat()

fun randomFloatPlusOrMinus(bounds: Number): Float = RANDOM.nextFloat() * (bounds.toFloat() * 2) - bounds.toFloat()

fun randomFloat(min: Number, max: Number) = RANDOM.nextFloat() * (max.toFloat() - min.toFloat()) + min.toFloat()

fun Float.squared(): Float = this * this

fun randomFloat(range: ClosedRange<*>): Float = randomFloat(
	(range.start as Number).toFloat(),
	(range.endInclusive as Number).toFloat()
)

fun <T> Iterable<T>.contains(predicate: (T) -> Boolean): Boolean {
	forEach {
		if (predicate(it))
			return true
	}
	return false
}

fun <T> Iterable<T>.notContains(predicate: (T) -> Boolean): Boolean = contains(predicate).not()

fun unhandledCase(obj: Any?) = require(false) { "Unhandled case $obj" }

fun Boolean.toString(whenTrue: String, whenFalse: String) = if (this) whenTrue else whenFalse

/**
 * Give string representation of Boolean as ON / OFF
 */
fun Boolean.toOnOffStr() = if (this) "ON" else "OFF"

fun <K, V> noDupesMapOf(vararg pairs: kotlin.Pair<K, V>): Map<K, V> = NoDupesMap<K, V>(LinkedHashMap()).also {
	for ((k, v) in pairs) {
		it[k] = v
	}
}

/**
 * Give this pair if both components are non null
 */
inline fun <S, T> kotlin.Pair<S?, T?>.hasBothOrNull(): kotlin.Pair<S, T>? {
	return if (first != null && second != null) kotlin.Pair(first!!, second!!) else null
}

/**
 * Give this Triple when all components are non-null
 */
inline fun <R, S, T> Triple<R?, S?, T?>.hasAllOrNull(): Triple<R, S, T>? {
	return if (first != null && second != null && third != null) Triple(first!!, second!!, third!!) else null
}

/**
 * Give this string prepended with color code to be compatible with 'drawAnnotatedString'
 */
fun String.annotated(color: GColor): String = String.format("%s%s", color.toString(), this)

/**
 * Get enum associated with this ordinal
 */
inline fun <reified E : Enum<E>> Int.toEnum(): E = enumValues<E>()[this]

/**
 * Get subject of this with dot and trailing chars stripped or this if no dot found
 */
fun String.stripFileExtension(): String = lastIndexOf('.').takeIf { it > 0 }?.let {
	substring(0, it)
} ?: this

fun <T> Collection<T>.randomWeighted(weight: (T) -> Int): T {
	val items = map { Pair(it, weight(it).coerceAtLeast(0)) }
	val sum = items.sumOf { it.second }
	if (sum <= 0)
		return random()
	var which = random(sum)
	items.filter { it.second > 0 }.forEach {
		if (it.second >= which)
			return it.first
		which -= it.second
	}
	return items.last().first
}

/**
 * Return a value that is an interpolation between a set of values.
 *
 * Factor to be value between 0-1
 */
fun <T> interpolate(factor: Float, value0: T, vararg values: T, interpolator: (Float, T, T) -> T): T {
	val factor = factor.coerceIn(0f, 1f)
	if (values.isEmpty())
		return value0
	if (values.size == 1)
		return interpolator(factor, value0, values[0])
	val f = factor * values.size
	val secondIndex = f.toInt().coerceIn(values.indices)
	val first = values.getOrNull(secondIndex - 1) ?: value0
	val second = values[secondIndex]
	val fraction = f - floor(f)
	val f2 = (f - (values.size - 1)).coerceAtLeast(0f) + fraction
	return interpolator(f2, first, second)
}

/**
 * Run 'main' method on a class in a new process
 */
fun Class<*>.runNewProcess(vararg args: String): Process {
	val javaHome = System.getProperty("java.home")
	val javaBin = "$javaHome/bin/java"
	val classpath = java.lang.System.getProperty("java.class.path")
	val className: String = name
	val builder = ProcessBuilder(javaBin, "-cp", classpath, className, *args)
	return builder.start()
}

fun String.toClass(): Class<*> = Class.forName(this)

fun Array<MutableVector2D>.computeBezierPoints(p0: IVector2D, p1: IVector2D, p2: IVector2D, p3: IVector2D) {
	val steps: Int = size - 1
	val step = 1.0f / steps
	var t = 0f
	for (pt in 0 until steps) {
		val fW = 1 - t
		val fA = fW * fW * fW
		val fB = 3 * t * fW * fW
		val fC = 3 * t * t * fW
		val fD = t * t * t
		val fX: Float = fA * p0.x + fB * p1.x + fC * p2.x + fD * p3.x
		val fY: Float = fA * p0.y + fB * p1.y + fC * p2.y + fD * p3.y
		get(pt).assign(fX, fY)
		t += step
	}
	get(steps).assign(p3)
}

/**
 * Give a value that is a poser of 2 and greater than or equal this
 */
fun Number.nearestPowerOf2(): Int {
	return 2.0.pow(ceil(ln(toDouble()) / ln(2.0))).roundToInt()
}

/**
 * delay with option to early exit. The Completable
 * should be called with 'false' for early out, otherwise
 * will be called with true internally
 */
suspend fun CoroutineScope.delayOrSignal(
	timeMillis: Long
): CompletableDeferred<Boolean> {
	val signal = CompletableDeferred<Boolean>()

	launch {
		select {
			onTimeout(timeMillis) {
				// Normal delay completed
				signal.complete(true)
			}
			signal.onAwait {
				// Completed early via signal
			}
		}
	}

	return signal
}

fun String.toInetAddress(): InetAddress = InetAddress.getByName(this)

fun Boolean.doIf(cond: Boolean, cb: () -> Unit) {
	if (this == cond) cb()
}

inline fun <reified T : Enum<T>> Enum<T>.ifIs(value: T, then: () -> Unit) {
	if (this == value) then()
}

fun String.formatSafe(msg: String, vararg params: Any?): String {
	try {
		return String.format(msg, *params)
	} catch (e: Throwable) {
		println("${LoggerFactory.RED} Cannot format '$msg'\n" + e.message)
		return msg
	}
}