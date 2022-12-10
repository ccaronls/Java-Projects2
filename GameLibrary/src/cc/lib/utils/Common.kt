package cc.lib.utils

import cc.lib.game.Utils
import cc.lib.math.Vector2D
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> MutableMap<T, Int>.increment(obj: T, amt: Int) {
    when (val count = get(obj)) {
	    null -> put(obj, amt)
        else -> put(obj, count + amt)
    }
}

fun Char.repeat(times: Int) : String {
    val buf = StringBuffer()
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

fun prettify(a: Any) : String {
	return Utils.toPrettyString(a)
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
	return map { prettify(it) }.toTypedArray()
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

open class DirtyReflector<T> : Reflector<T>() {

	@Omit
	val dirtyFields = mutableSetOf<KProperty<*>>()

	override fun isDirty(reset: Boolean): Boolean {
		return dirtyFields.isNotEmpty().also {
			if (reset)
				dirtyFields.clear()
		}
	}

	override fun serializeDirty(out: MyPrintWriter) {
		out.push()
		dirtyFields.forEach { property ->
			out.print("${property.name}=")
			serializeObject(property.getter.call(this), out)
		}
		out.pop()
	}
}

abstract class DirtyDelegate<V>(var value: V) {

	operator fun getValue(ref: DirtyReflector<*>, prop: KProperty<*>) = value
	operator fun setValue(ref: DirtyReflector<*>, prop: KProperty<*>, v: V) {
		if (v != value) {
			ref.dirtyFields.add(prop)
		}
		value = v
	}

	abstract fun setValueFromString(str: String)

	override fun equals(other: Any?): Boolean {
		return if (other is DirtyDelegate<*>) other.value == value else false
	}

	override fun toString(): String {
		return value.toString()
	}

	override fun hashCode(): Int {
		return value.hashCode()
	}
}

class DirtyDelegateInt(value: Int) : DirtyDelegate<Int>(value) {
	override fun setValueFromString(str: String) {
		value = str.toInt()
	}
}

class DirtyDelegateFloat(value: Float) : DirtyDelegate<Float>(value) {
	override fun setValueFromString(str: String) {
		value = str.toFloat()
	}
}

class DirtyDelegateLong(value: Long) : DirtyDelegate<Long>(value) {
	override fun setValueFromString(str: String) {
		value = str.toLong()
	}
}

class DirtyDelegateString(value: String) : DirtyDelegate<String>(value) {
	override fun setValueFromString(str: String) {
		value = str
	}
}

class DirtyDelegateReflector<T : Reflector<T>>(value: T, val parser: (String) -> T) : DirtyDelegate<T>(value) {
	override fun setValueFromString(str: String) {
		value = parser(str)
	}
}

class DirtyDelegateVector2D(value: Vector2D) : DirtyDelegate<Vector2D>(value) {
	override fun setValueFromString(str: String) {
		value = Vector2D.parse(str)
	}
}

fun <T> weakReference(tIn : T? = null) : ReadWriteProperty<Any?, T?> = object : ReadWriteProperty<Any?, T?> {
	var t = WeakReference(tIn)

	override fun getValue(thisRef: Any?, property: KProperty<*>): T? = t.get()

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
		t = WeakReference(value)
	}
}
