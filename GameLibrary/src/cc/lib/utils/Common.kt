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

fun <T> List<T>.mergeWith(other: Collection<T>) : MutableList<T> {
    val a = ArrayList(this)
    a.addAll(other)
    return a
}

fun assertTrue(expr: Boolean, msg: String = "") {
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

fun Int.rotate(max: Int) : Int {
    return plus(1) % max
}
