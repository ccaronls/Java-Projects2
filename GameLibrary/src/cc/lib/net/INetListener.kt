package cc.lib.net

interface INetListener<T> {

	fun addListener(l: T)
	fun removeListener(l: T)
	fun addWeakListener(l: T)

	fun notifyListeners(cb: suspend (T) -> Unit)

}

