package cc.lib.reflector

import kotlin.reflect.KProperty

class DirtyDelegate<V>(var value: V) {

	operator fun getValue(ref: DirtyReflector<*>, prop: KProperty<*>) = value
	operator fun setValue(ref: DirtyReflector<*>, prop: KProperty<*>, v: V) {
		if (v != value) {
			ref.setDirty()
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
		return value.hashCode()
	}
}