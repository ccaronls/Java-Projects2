package cc.lib.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ResettableLazy<T>(private val initializer: () -> T) : ReadWriteProperty<Any?, T> {

	private var lazyValue: Lazy<T>? = null

	override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		if (lazyValue == null) {
			lazyValue = lazy(initializer)
		}
		return lazyValue!!.value
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