package cc.lib.android

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by Chris Caron on 4/4/26.
 */
class DirectionalTouchView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

	private val _direction = MutableStateFlow(Vector2D.ZERO)
	private val _touching = MutableStateFlow(false)

	val direction: StateFlow<Vector2D>
		get() = _direction

	val touching: StateFlow<Boolean>
		get() = _touching

	private val start = MutableVector2D()

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				_touching.value = true
				start.assign(event.x, event.y)
			}

			MotionEvent.ACTION_MOVE -> {
				if (_touching.value)
					_direction.value = Vector2D(event.x, event.y) - start
			}

			MotionEvent.ACTION_UP -> {
				_touching.value = false
				_direction.value = Vector2D.ZERO
			}
		}
		return true
	}

}