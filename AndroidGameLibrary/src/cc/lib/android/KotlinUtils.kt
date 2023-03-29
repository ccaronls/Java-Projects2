package cc.lib.android

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

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

fun <IN1, IN2, OUT> combine(in1 : LiveData<IN1>, in2 : LiveData<IN2>, combiner : (IN1, IN2) -> OUT) : LiveData<OUT> {
	return object : MediatorLiveData<OUT>() {
		init {
			addSource(in1) { t ->
				in2.value?.let {
					value = combiner(t, it)
				}
			}
			addSource(in2) { t ->
				in1.value?.let {
					value = combiner(it, t)
				}
			}
		}
	}
}
/*
fun <K,V> MutableMap<K,V>.getOrPut(key : K, defaultValue : V) : V {
	get(key)?.let {
		return it
	}
	put(key, defaultValue)
	return defaultValue
}*/