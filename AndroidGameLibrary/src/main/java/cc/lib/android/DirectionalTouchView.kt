package cc.lib.android

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D

/**
 * Created by Chris Caron on 4/4/26.
 */
class DirectionalTouchView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

	interface Listener {
		fun onDirection(dir: Vector2D)
	}

	private var touching = false
	private val start = MutableVector2D()

	var listener: Listener? = null

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				touching = true
				start.assign(event.x, event.y)
			}

			MotionEvent.ACTION_MOVE -> {
				if (touching)
					listener?.onDirection(Vector2D(event.x, event.y) - start)
			}

			MotionEvent.ACTION_UP -> {
				touching = false
				listener?.onDirection(Vector2D.ZERO)
			}
		}
		return true
	}

	override fun onDetachedFromWindow() {
		listener = null
		super.onDetachedFromWindow()
	}
}