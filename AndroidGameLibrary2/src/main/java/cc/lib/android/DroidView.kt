package cc.lib.android

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import cc.lib.game.GDimension
import cc.lib.game.IDimension

class DroidView : View, OnScaleGestureListener {
	private var isParentDroidActivity = false
	private var scaleDetector: ScaleGestureDetector? = null
	private var gestureScale = 1f
	private var minScale = 0.1f
	private var maxScale = 5f
	private var pinchCenterX = 0f
	private var pinchCenterY = 0f
	private var scaling = false

	constructor(context: Context?, touchEnabled: Boolean) : super(context) {
		isClickable = touchEnabled
		isParentDroidActivity = context is DroidActivity
	}

	constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
		isParentDroidActivity = context is DroidActivity
	}

	fun setPinchZoomEnabled(enabled: Boolean) {
		if (enabled) {
			scaleDetector = ScaleGestureDetector(context, this)
			scaleDetector!!.isQuickScaleEnabled = true
		} else {
			scaleDetector = null
			gestureScale = 1f
		}
	}

	fun setZoomScaleBound(minScale: Float, maxScale: Float) {
		this.minScale = minScale
		this.maxScale = maxScale
	}

	override fun onScale(detector: ScaleGestureDetector): Boolean {
		pinchCenterX = detector.focusX
		pinchCenterY = detector.focusY
		gestureScale = (gestureScale * detector.scaleFactor).coerceIn(minScale, maxScale)
		invalidate()
		return true
	}

	override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
		scaling = true
		return true
	}

	override fun onScaleEnd(detector: ScaleGestureDetector) {
		scaling = false
	}

	private var g: DroidGraphics? = null
	override fun onDraw(canvas: Canvas) {
		val width = canvas.width - margin * 2
		val height = canvas.height - margin * 2
		g?.setCanvas(canvas, width, height) ?: run {
			g = DroidGraphics(context, canvas, width, height)
		}
		canvas.save()
		canvas.scale(gestureScale, gestureScale, pinchCenterX, pinchCenterY)
		canvas.translate(margin.toFloat(), margin.toFloat())
		canvas.save()
		onPaint(g)
		canvas.restore()
		canvas.restore()
	}

	var tx = -1f
	var ty = -1f
	var dragging = false
	private fun checkStartDrag() {
		if (!dragging && downTime > 0) {
			dragging = true
			Log.v(TAG, "startDrag $tx x $ty from checkStartDrag")
			onDragStart(tx, ty)
		}
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (scaleDetector != null) {
			scaleDetector!!.onTouchEvent(event)
			if (scaling) return true
		}
		if (!isParentDroidActivity) return super.onTouchEvent(event)
		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				downTime = SystemClock.uptimeMillis()
				postDelayed({ checkStartDrag() }, CLICK_TIME.toLong())
				tx = event.x
				ty = event.y
				Log.v(TAG, "onTouchDown $tx x $ty")
				onTouchDown(event.x, event.y)
			}

			MotionEvent.ACTION_UP -> {
				Log.v(TAG, "onTouchUp")
				if (!dragging && SystemClock.uptimeMillis() - downTime < CLICK_TIME) {
					Log.v(TAG, "onTap")
					//removeCallbacks(touchDownRunnable);
					//touchDownRunnable = null;
					onTap(event.x, event.y)
				} else {
					onTouchUp(event.x, event.y)
				}
				if (dragging) {
					Log.v(TAG, "onDragStop")
					onDragStop(event.x, event.y)
				}
				dragging = false
				downTime = 0
				run {
					ty = -1f
					tx = ty
				}
			}

			MotionEvent.ACTION_MOVE -> {
				val dx = event.x - tx
				val dy = event.y - ty
				val d = dx * dx + dy * dy
				if (dragging || d > 100) {
					if (!dragging) {
						Log.v(TAG, "startDrag $tx x $ty from MOVE")
						onDragStart(tx, ty)
					} else {
						Log.v(TAG, "drag $tx x $ty")
						onDrag(event.x, event.y)
					}
					dragging = true
				}
			}
		}
		invalidate()
		//        postDelayed(()->invalidate(), 50);
		return true
	}

	private val CLICK_TIME = 700
	private var downTime: Long = 0
	private var margin = 0
	fun setMargin(margin: Int) {
		this.margin = margin
		postInvalidate()
	}

	val dimension: IDimension
		get() = GDimension((width - margin * 2).toFloat(), (height - margin * 2).toFloat())

	protected fun onTap(x: Float, y: Float) {
		(context as DroidActivity).onTap(x, y)
	}

	protected fun onTouchDown(x: Float, y: Float) {
		(context as DroidActivity).onTouchDown(x, y)
	}

	protected fun onTouchUp(x: Float, y: Float) {
		(context as DroidActivity).onTouchUp(x, y)
	}

	protected fun onDragStart(x: Float, y: Float) {
		(context as DroidActivity).onDragStart(x, y)
	}

	protected fun onDragStop(x: Float, y: Float) {
		(context as DroidActivity).onDragStop(x, y)
	}

	protected fun onDrag(x: Float, y: Float) {
		(context as DroidActivity).onDrag(x, y)
	}

	protected fun onPaint(g: DroidGraphics?) {
		if (isParentDroidActivity) (context as DroidActivity).onDrawInternal(g!!)
	}

	companion object {
		const val TAG = "DroidView"
	}
}
