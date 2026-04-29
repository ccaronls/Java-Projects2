package cc.lib.reflector

import java.io.IOException
import kotlin.reflect.KProperty

/**
 * Usage:
 * var foo by dirty(0)
 *
 * when, foo is apart of a DirtyReflector class, then DirtyReflector will know
 * that something has changed if x has changed and will be exported by
 * DirtyReflector.serializeDirty
 *
 * For collections (Array, List, Set, Map, Grid) use the dirty variations
 * to get smaller serialization sizes from serializeDirty:
 */
inline fun <reified T> dirty(value: T) = DirtyDelegate(value, T::class.java)

class DirtyDelegate<V>(var value: V, val type: Class<*> = value!!::class.java) : IDirty {

	private var dirty = true
	override fun isDirty(): Boolean = dirty

	override fun markClean() {
		dirty = false
	}

	override fun serializeDirty(out: RPrintWriter, ignoreNonDirtyTypes: Boolean) = serialize(out)

	operator fun getValue(ref: DirtyReflector<*>, prop: KProperty<*>) = value

	operator fun setValue(ref: DirtyReflector<*>, prop: KProperty<*>, v: V) {
		if (v != value) {
			dirty = true
			ref.markDirty()
		}
		value = v
	}

	override fun equals(other: Any?): Boolean = when (other) {
		null -> false
		is DirtyDelegate<*> -> other.value == value
		else -> other == value
	}

	override fun toString(): String {
		return value.toString()
	}

	override fun hashCode(): Int {
		return value?.hashCode() ?: 0
	}

	fun set(newValue: Any?) {
		value = newValue as V
	}

	@kotlin.jvm.Throws(IOException::class)
	fun serialize(out: RPrintWriter) {
		value.also { v ->
			when (v) {
				is Reflector<*> -> {
					out.push()
					v.serialize(out)
					out.pop()
				}

				else -> out.println()
			}
		}
	}

	@kotlin.jvm.Throws(IOException::class)
	fun deserialize(reader: RBufferedReader, keepInstances: Boolean) {
		when (value) {
			is Reflector<*> -> with(value as Reflector<*>) {
				if (keepInstances)
					merge(reader)
				else
					deserialize(reader)
			}
			else -> Unit
		}
	}
}