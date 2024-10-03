package cc.lib.android

import android.content.DialogInterface
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.View.OnKeyListener

/**
 * Created by Chris Caron on 8/26/24.
 */
abstract class DroidKeyListener : OnKeyListener, DialogInterface.OnKeyListener {

	private var keyDownTime = 0L

	private fun onKey(down: Boolean, code: Int): Boolean {
		if (down) {
			keyDownTime = SystemClock.uptimeMillis()
		} else {
			val timeDown = SystemClock.uptimeMillis() - keyDownTime
			if (timeDown > 1000) {
				return onKeyLongClicked(code)
			} else {
				return onKeyClicked(code)
			}
		}
		return false
	}

	final override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
		return onKey(event.action == KeyEvent.ACTION_DOWN, keyCode)
	}

	final override fun onKey(dialog: DialogInterface?, keyCode: Int, event: KeyEvent): Boolean {
		return onKey(event.action == KeyEvent.ACTION_DOWN, keyCode)
	}

	abstract fun onKeyClicked(keyCode: Int): Boolean

	abstract fun onKeyLongClicked(keyCode: Int): Boolean
}