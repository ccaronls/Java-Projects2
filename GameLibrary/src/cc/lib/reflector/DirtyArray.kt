package cc.lib.reflector

import java.util.Vector

/**
 * Created by Chris Caron on 4/25/26.
 *
 * DirtyCollections are designed to reduce transport sizes.
 * When T is a DirtyReflector, then only the indices of the changed objects are serialized by
 * serializeDirty and deserialized by merge.
 *
 * When T is a common type (Int, String) then when any of the items change, then the whole array
 * is marked dirty and all are serialized / deserialize
 *
 * Note: serializeDirty / merge are incompatible with serialize/deserialize (TODO: Fix)
 */
class DirtyArray<T> @JvmOverloads constructor(
	size: Int = 0,
	filler: ((Int) -> T)? = null
) : DirtyReflector<DirtyArray<T>>(), Iterable<T> {

	private val changedIndices = mutableSetOf<Int>()

	private val array = Vector<T>(size).also {
		for (i in 0 until size) {
			it.add(filler?.invoke(i))
			changedIndices.add(i)
		}
	}

	val size: Int
		get() = array.size

	override fun iterator(): Iterator<T> = array.iterator()

	override fun markClean() {
		super.dirty = false
		array.forEach {
			(it as? IDirty)?.markClean()
		}
		changedIndices.clear()
	}

	override fun serializeDirty(out: RPrintWriter, ignoreNonDirtyTypes: Boolean) {
		out.p("size=$size").println()
		array.forEachIndexed { index, it ->
			if (it is IDirty) {
				if (it.isDirty) {
					out.p("$index=${getCanonicalName(it.javaClass)} ")
					out.push()
					it.serializeDirty(out, ignoreNonDirtyTypes)
					out.pop()
				}
			} else if (changedIndices.contains(index) || (!ignoreNonDirtyTypes && isDirty)) {
				out.p("$index=")
				serializeObject(it, out)
			}
		}
	}

	override fun merge(input: RBufferedReader) {
		fun String.parse(startsWith: String, parser: (String) -> Any): Any {
			if (!startsWith(startsWith))
				throw Exception("Expected $startsWith but got $this")
			return parser(substring(startsWith.length))
		}

		val s = input.readLineOrEOF()?.parse("size=") { Integer.valueOf(it) } as Int
		if (size > 0 && s != size) {
			throw Exception("Cannot merge incoming DirtyArray unless sizes match. Incoming size ($s) and existing is ($size)")
		} else if (size == 0) {
			array.setSize(s)
		}

		while (true) {
			input.markDepth()
			try {
				val line = input.readLineOrEOF() ?: break
				val parts = line.split("=")
				if (parts.size < 2)
					throw Exception("Parse ${parts.size} parts but expected 2")
				val idx = parts[0].toInt()
				if (parts[1] == "null") {
					array[idx] = null
				} else {
					val type = getClassForName(parts[1])
					val obj = parse(array[idx], type, input, true)
					array[idx] = obj as T
				}
			} finally {
				input.restoreDepth()
			}
		}
	}

	operator fun get(idx: Int): T = array[idx]

	operator fun set(idx: Int, obj: T) {
		if (array[idx] != obj) {
			array[idx] = obj
			changedIndices.add(idx)
			markDirty()
		}
	}

	fun fill(filler: (Int) -> T) {
		for (i in 0 until size) {
			array[i] = filler(i)
		}
	}
}

inline fun <reified T> dirtyArrayOf(vararg values: T): DirtyArray<T> {
	return DirtyArray(values.size) { values[it] }
}

inline fun <reified T> List<T>.toDirtyArray(): DirtyArray<T> {
	return DirtyArray(size) {
		get(it)
	}
}

inline fun <reified T> Array<T>.toDirtyArray(): DirtyArray<T> {
	return DirtyArray(size) {
		get(it)
	}
}