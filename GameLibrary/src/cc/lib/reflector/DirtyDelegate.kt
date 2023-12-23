package cc.lib.reflector

import cc.lib.utils.trimQuotes
import java.io.IOException
import kotlin.reflect.KProperty

class DirtyDelegate<V>(var value: V, val type: Class<*> = value!!::class.java) {

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
		return value?.hashCode() ?: 0
	}

	fun set(newValue: String, keepInstances: Boolean) {
		if (type.isAssignableFrom(Boolean::class.javaObjectType)) {
			value = newValue.toBoolean() as V
		} else if (type.isAssignableFrom(String::class.javaObjectType)) {
			value = newValue.trimQuotes() as V
		} else if (type.isAssignableFrom(Int::class.javaObjectType)) {
			value = newValue.toInt() as V
		} else if (type.isAssignableFrom(Long::class.javaObjectType)) {
			value = newValue.toLong() as V
		} else if (type.isAssignableFrom(Float::class.javaObjectType)) {
			value = newValue.toFloat() as V
		} else if (Reflector::class.java.isAssignableFrom(type)) {
			if (newValue == null || newValue == "null") {
				value = null as V
			} else {
				if (!keepInstances || value == null || Reflector.isImmutable(value)) {
					value = Reflector.getClassForName(newValue.split(" ")[0]).newInstance() as V
				}
			}
		} else TODO("$newValue Not implemented for type ${type}")
	}

	@kotlin.jvm.Throws(IOException::class)
	fun serialize(out: RPrintWriter, printObjects: Boolean) {
		when (value) {
			is Reflector<*> -> with(value as Reflector<*>) {
				out.push()
				serialize(out)
				out.pop()
			}
			else -> out.println()
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