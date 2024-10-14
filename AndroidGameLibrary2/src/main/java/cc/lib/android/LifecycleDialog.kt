package cc.lib.android

import android.app.Dialog
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Created by Chris Caron on 3/25/23.
 */
abstract class LifecycleDialog<VM : ViewModel>(context: Context, modelClass: Class<VM>, themeRes: Int = 0) :
	Dialog(context, themeRes), LifecycleOwner, ViewModelStoreOwner {

	private val lifecycleRegistry = LifecycleRegistry(this)
	private val _viewModelStore by lazy {
		ViewModelStore()
	}

	open val viewModel: VM by lazy {
		ViewModelProvider(viewModelStore, object : ViewModelProvider.Factory {
			override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.newInstance()
		}).get(modelClass)
	}

	override val lifecycle: Lifecycle = lifecycleRegistry

	override fun onStart() {
		super.onStart()
		lifecycleRegistry.currentState = Lifecycle.State.STARTED
	}

	override fun onStop() {
		lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
		super.onStop()
	}

	override val viewModelStore: ViewModelStore = _viewModelStore
}