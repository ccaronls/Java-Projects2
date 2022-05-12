package cc.lib.utils

import cc.lib.game.Utils

fun <T> MutableMap<T, Int>.increment(obj: T, amt: Int) {
    when (val count = get(obj)) {
        null -> put(obj, amt)
        else -> put(obj, count + amt)
    }
}

fun Char.repeat(times: Int) : String {
    var buf = StringBuffer()
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

inline fun assertTrue(expr: Boolean, msg: String = "") {
    if (!expr) {
        throw GException("Assertion Failed $msg")
    }
}

fun <T> MutableList<T>.removeRandom() : T {
    val item = random()
    assertTrue(remove(item))
    return item
}

fun Iterable<Any>.toStringArray(pretty: Boolean) : Array<String> {
    return map { if (pretty) Utils.toPrettyString(it) else it.toString() }.toTypedArray()
}

inline fun Int.rotate(max: Int) : Int {
    return plus(1) % max
}

inline fun prettify(a:Any) : String {
	return Utils.toPrettyString(a)
}

inline fun String.wrap(maxChar: Int) : String {
	return Utils.wrapTextWithNewlines(this, maxChar)
}

inline fun isEqual(a: Any?, b: Any?) : Boolean {
	return Utils.isEquals(a, b)
}

inline fun hashCode(vararg objects: Any?) : Int {
	return Utils.hashCode(objects)
}

/**
 * Return random in range 0 to this
 */
inline fun Float.random() : Float {
	return Utils.randFloat(this)
}

/**
 * return + or - random in range 0-this
 */
inline fun Float.randomSigned() : Float {
	return Utils.randFloatX(this)
}

/**
 * return random int in range 0 to this-1
 */
inline fun random(range: Int) : Int {
	return if (range > 0) Utils.rand() % range else -1
}

/**
 * Return random int in range
 */
inline fun random(range: IntRange) : Int {
	return Utils.randRange(range.first, range.last)
}

/**
 *
 */
inline fun <T> List<T>.random() : T {
	return get(random(size))
}

/**
 * Return a random number between [0-size) where the likelyhood of a number is based on the weight value
 * at that index. Example: for array[0,1,2], the possible results and their probability are:
 * 0 -> 0 out of 3
 * 1 -> 1 out of 3
 * 2 -> 2 out of 3
 */
inline fun IntArray.randomWeighted() : Int {
	return Utils.chooseRandomFromSet(*this)
}

/**
 *
 */
inline fun isEmpty(obj: Any?) : Boolean {
	return Utils.isEmpty(obj)
}

/**
 * left rotate all elems in the array and return the first elem shifted off.
 */
inline fun IntArray.popFirst(fillLastElem: Int=0) : Int  {
	return Utils.popFirst(this).also {
		set(size -1, fillLastElem)
	}
}

inline fun <R,T> List<R>.join(elems: Iterator<T>) : List<Pair<R,T>> {
	val list = mutableListOf<Pair<R,T>>()
	var index = 0
	elems.forEach { r ->
		if (index < size) {
			list.add(Pair(get(index), r))
			index ++
		}
	}
	return list
}

inline fun List<Any>.prettify() : Array<String> {
	return map { prettify(it) }.toTypedArray()
}

inline fun <T: Comparable<T>> T.increment(steps: Int, values: Array<T>) : T {
	var idx = Utils.linearSearch(values, this)
	idx = (idx + steps) % values.size
	return values[idx]
}