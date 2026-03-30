package cc.lib.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ResettableLazy<T>(private val initializer: () -> T) : ReadWriteProperty<Any?, T> {

	private var lazyValue: Lazy<T>? = null

	override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return lazyValue?.value ?: run {
			val v = lazy(initializer)
			lazyValue = v
			return v.value
		}
	}

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		lazyValue = lazyOf(value)
	}

	fun reset() {
		lazyValue = null
	}
}

fun <T> resettableLazy(initializer: () -> T): ResettableLazy<T> {
	return ResettableLazy(initializer)
}