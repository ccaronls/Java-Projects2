package cc.lib.android

import android.app.Activity
import android.app.AlertDialog
import android.app.Application.ActivityLifecycleCallbacks
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import cc.lib.utils.takeIfInstance
import cc.lib.utils.weakReference

abstract class SpinnerTask<T>(_context: Activity) : CoroutinesAsyncTask<T, Int, Any>(_context.localClassName),
	ActivityLifecycleCallbacks {
	private var dialog: ProgressDialog? = null
	private val context by weakReference(_context)
	private var result: Any? = null

	init {
		context?.takeIfInstance<Activity>()?.let {
			it.application.registerActivityLifecycleCallbacks(this)
		}
	}

	override fun onCancelled(o: Any?) {
		super.onCancelled(o)
	}

	override fun onCancelled() {
		dialog?.dismiss()
		onCompleted()
	}

	override fun onPostExecute(o: Any?) {
		dialog?.dismiss()
		if (!isCancelled) {
			if (o != null && o is Exception) {
				onError(o)
			} else {
				onSuccess()
			}
			onCompleted()
		}
	}

	/**
	 * Gets called for all situations: SUCCESS, ERROR, CANCELLED
	 */
	protected open fun onCompleted() {}
	protected open fun onError(e: Exception) {
		e.printStackTrace()
		context?.let {
			it.runOnUiThread {
				AlertDialog.Builder(it).setTitle("Error")
					.setMessage(
						"""
		An error occured: ${e.javaClass.simpleName}
		${e.message}
		""".trimIndent()
					)
					.setNegativeButton("Ok", null).show()
			}
		}
	}

	fun postExecute(args: T? = null) {
		context?.runOnUiThread { execute(args) }
	}

	override fun onPreExecute() {
		context?.apply {
			if (!isFinishing && !isDestroyed) {
				dialog = ProgressDialog(this).also {
					it.setMessage(progressMessage)
					it.setCancelable(false) // prevents cancel from back button
					it.isIndeterminate = true
					it.setCanceledOnTouchOutside(false) // prevents cancel from random touches
					if (isCancellable) {
						it.setButton(
							DialogInterface.BUTTON_NEGATIVE,
							getString(R.string.popup_button_cancel)
						) { dialog: DialogInterface, which: Int -> onCancelButtonClicked(dialog, which) }
					}
					it.show()
				}
			}
		}
	}

	protected open var progressMessage: String?
		protected get() = null
		set(message) {
			dialog?.setMessage(message)
		}

	@Throws(Throwable::class)
	protected abstract suspend fun doIt(args: T?)
	protected open fun onSuccess() {}
	override suspend fun doInBackground(args: T?): Any? {
		try {
			doIt(args)
		} catch (e: Throwable) {
			return e
		}
		return null
	}

	val isCancellable: Boolean
		get() = true

	fun canInterruptOnCancel(): Boolean {
		return false
	}

	protected fun setResult(result: Any?) {
		this.result = result
	}

	fun <T> getResult(): T? {
		return result as T?
	}

	private fun onCancelButtonClicked(dialog: DialogInterface, which: Int) {
		cancel(canInterruptOnCancel())
		onCancelButtonClicked()
		dialog.dismiss()
	}

	protected open fun onCancelButtonClicked() {}
	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
	override fun onActivityStarted(activity: Activity) {}
	override fun onActivityResumed(activity: Activity) {}
	override fun onActivityPaused(activity: Activity) {}
	override fun onActivityStopped(activity: Activity) {
		cancel(true)
	}

	override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
	override fun onActivityDestroyed(activity: Activity) {}
}
