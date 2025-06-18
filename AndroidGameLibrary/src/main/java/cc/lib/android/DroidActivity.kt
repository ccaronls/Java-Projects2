package cc.lib.android

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.ViewGroup
import cc.lib.game.GColor

/**
 * Created by chriscaron on 2/13/18.
 *
 * Convenience class for getting fullscreen game up without a layout file.
 *
 * Just override the draw method
 *
 */
abstract class DroidActivity : CCActivityBase() {
	lateinit var graphics: DroidGraphics
	override lateinit var content: DroidView

	/**
	 * Called after onCreate
	 * @return
	 */
	var topBar: ViewGroup? = null
		private set
	private var margin = 0
	private var initialized = false

	/**
	 *
	 * @param margin
	 */
	fun setMargin(margin: Int) {
		this.margin = margin
		content!!.postInvalidate()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		graphics = object : DroidGraphics(this, null, 0, 0) {
			override fun getBackgroundColor(): GColor = GColor.BLACK
		}
		setContentView(contentViewId)
		content = findViewById(R.id.droid_view)
		topBar = findViewById(R.id.top_bar_layout)
	}

	protected open val contentViewId: Int
		protected get() = R.layout.droid_activity

	override fun onPause() {
		super.onPause()
	}

	override fun onDestroy() {
		if (graphics != null) graphics.releaseBitmaps()
		initialized = false
		super.onDestroy()
	}

	override fun onBackPressed() {
		super.onBackPressed()
	}

	fun onDrawInternal(g: DroidGraphics) {
		if (!isFinishing) {
			if (initialized) {
				onDraw(g)
			} else {
				onInit(g)
				initialized = true
				redraw()
			}
		}
	}

	protected abstract fun onDraw(g: DroidGraphics)
	protected open fun onInit(g: DroidGraphics) {}
	open fun onTap(x: Float, y: Float) {}
	open fun onTouchDown(x: Float, y: Float) {}
	open fun onTouchUp(x: Float, y: Float) {}
	open fun onDrag(x: Float, y: Float) {}
	open fun onDragStart(x: Float, y: Float) {}
	open fun onDragStop(x: Float, y: Float) {}
	private var currentDialog: AlertDialog? = null
	protected open val dialogTheme: Int
		protected get() = R.style.DialogTheme

	override fun newDialogBuilder(): AlertDialog.Builder {
		val previous = currentDialog
		val builder = object : AlertDialog.Builder(this, dialogTheme) {
			override fun show(): AlertDialog {
				currentDialog?.dismiss()
				return super.show().also {
					currentDialog = it
					onDialogShown(it)
				}
			}
		}.setCancelable(false)
		if (shouldDialogAddBackButton() && currentDialog != null && currentDialog!!.isShowing) {
			builder.setNeutralButton(R.string.popup_button_back) { dialog: DialogInterface?, which: Int ->
				currentDialog = previous
				previous!!.show()
			}
		}
		return builder
	}

	protected open fun shouldDialogAddBackButton(): Boolean {
		return true
	}

	protected open fun onDialogShown(d: Dialog) {}
	fun dismissCurrentDialog() {
		if (currentDialog != null) {
			currentDialog!!.dismiss()
			currentDialog = null
		}
	}

	val isCurrentDialogShowing: Boolean
		get() = currentDialog != null && currentDialog!!.isShowing

	fun redraw() {
		content!!.postInvalidate()
	}
}
