package cc.lib.android

import androidx.lifecycle.*

/**
 * Created by Chris Caron on 12/7/22.
 */

inline fun <T> LiveData<T>.observeWhile(
	lifecycleOwner: LifecycleOwner,
	crossinline onChanged: (T) -> Boolean
) : Observer<T> = object : Observer<T> {
	override fun onChanged(t: T) {
		if (!onChanged.invoke(t)) {
			removeObserver(this)
		}
	}
}.also {
	observe(lifecycleOwner, it)
}

fun <IN1, IN2, OUT> combine(in1 : LiveData<IN1>, in2 : LiveData<IN2>, combiner : (IN1?, IN2?) -> OUT?) : LiveData<OUT> {
	return object : MediatorLiveData<OUT>(), Observer<Any?> {
		var ignore = true
		init {
			addSource(in1, this)
			addSource(in2, this)
			ignore = false
			update()
		}

		override fun onChanged(t: Any?) {
			if (!ignore)
				update()
		}

		fun update() {
			value = combiner(in1.value, in2.value)
		}
	}
}

fun <IN1, IN2, IN3, OUT> combine(in1 : LiveData<IN1>, in2 : LiveData<IN2>, in3 : LiveData<IN3>, combiner : (IN1?, IN2?, IN3?) -> OUT?) : LiveData<OUT> {
	return object : MediatorLiveData<OUT>(), Observer<Any?> {
		var ignore = true
		init {
			addSource(in1, this)
			addSource(in2, this)
			addSource(in3, this)
			ignore = false
			update()
		}

		override fun onChanged(t: Any?) {
			if (!ignore)
				update()
		}

		fun update() {
			value = combiner(in1.value, in2.value, in3.value)
		}
	}
}

fun MutableLiveData<Int>.increment(amt : Int) {
	postValue((value?:0) + amt)
}

class TransformedLiveData<IN, OUT>(data : LiveData<IN>, converter : (v : IN) -> OUT) : MediatorLiveData<OUT>() {
	init {
		addSource(data) {
			value = converter(it)
		}
	}
}

fun <T> Collection<LiveData<T>>.combine(combiner : (List<T>) -> T) : LiveData<T> {
	return object : MediatorLiveData<T>(), Observer<T> {
		var ignore = true
		init {
			forEach {
				addSource(it, this)
			}
			ignore = false
			update()
		}

		override fun onChanged(t: T) {
			if (!ignore)
				update()
		}

		fun update() {
			value = combiner(map { it.value }.filter { it != null } as List<T>)
		}

		override fun onInactive() {
			super.onInactive()
			forEach { it.removeObserver(this) }
		}
	}
}
