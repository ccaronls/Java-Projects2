package cc.lib.android

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
