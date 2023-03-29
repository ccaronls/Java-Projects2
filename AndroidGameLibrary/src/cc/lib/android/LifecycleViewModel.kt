package cc.lib.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel

/**
 * Created by Chris Caron on 3/24/23.
 */
abstract class LifecycleViewModel : ViewModel(), LifecycleOwner {

	private val lifecycleRegistry by lazy {
		LifecycleRegistry(this).also {
			it.currentState = Lifecycle.State.STARTED
		}
	}

	override fun getLifecycle(): Lifecycle = lifecycleRegistry

	override fun onCleared() {
		lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
	}
}