package cc.lib.reflector

class DirtyHashMap<K, V> : HashMap<K, V>(), IDirty {
	private var dirty = false

	override fun isDirty(): Boolean {
		if (dirty)
			return true
		values.forEach {
			if (it is IDirty) {
				if (it.isDirty) {
					dirty = true
					return true
				}
			}
		}
		return false
	}

	override fun markClean() {
		dirty = false
		values.forEach {
			if (it is IDirty) {
				it.markClean()
			}
		}
	}

	@Throws(Exception::class)
	override fun serializeDirty(out: RPrintWriter) {
		/*
		if (isDirty) {
			// write whole thing
			Reflector.serializeMap(this, out)
		} else {
			for (it : Map.Entry<*,*> in entries) {
				if (it.value is IDirty) {
					if ((it.value as IDirty).isDirty) {
						continue
					}
				}
				Reflector.serialzeObjectType(it.key, out)
				Reflector.serializeObject(it.key, out, true, false)
				if (it.value == null) {
					out.println("null")
				} else {
					Reflector.serialzeObjectType(it.value, out)
					Reflector.serializeObject(it.value, out, true, true)
				}
			}
		}*/
	}

	override fun clear() {
		dirty = dirty || size > 0
		super.clear()
	}

	override fun put(key: K, value: V): V? {
		dirty = true
		return super.put(key, value)
	}

	override fun putAll(from: Map<out K, V>) {
		dirty = true
		super.putAll(from)
	}

	override fun remove(key: K): V? {
		return super.remove(key).also {
			dirty = dirty || it != null
		}
	}
/*
	override fun remove(key: K, value: V): Boolean {
		return super.remove(key, value).also {
			dirty = dirty || it
		}
	}

	override fun replaceAll(function: BiFunction<in K, in V, out V>) {
		super.replaceAll(function)
	}

	override fun putIfAbsent(key: K, value: V): V? {
		return super.putIfAbsent(key, value)
	}

	override fun replace(key: K, oldValue: V, newValue: V): Boolean {
		return super.replace(key, oldValue, newValue)
	}

	override fun replace(key: K, value: V): V? {
		return super.replace(key, value)
	}

	override fun merge(key: K, value: V, remappingFunction: BiFunction<in V, in V, out V?>): V? {
		return super.merge(key, value, remappingFunction)
	}*/
}

/*
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
}*/
