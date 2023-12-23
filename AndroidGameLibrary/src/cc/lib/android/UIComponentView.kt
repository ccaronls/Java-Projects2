package cc.lib.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import cc.lib.game.*
import cc.lib.math.Vector2D
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

abstract class UIComponentView<T : UIRenderer> : View, UIComponent, Runnable, ScaleGestureDetector.OnScaleGestureListener {

	private val TAG = this::class.java.simpleName

	private lateinit var g: DroidGraphics
	private var initialized = false
	private var tx = -1
	private var ty = -1
	lateinit var renderer: T
		private set
	private var borderThickness = 0f
	private var borderColor = 0
	private val borderPaint = Paint()
	private val CLICK_TIME = 700
	private var downTime: Long = 0
	private var touchDownX = 0
	private var touchDownY = 0
	private var scaleGestureDetector: ScaleGestureDetector? = null
	private var scaling = false

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		init(context, attrs)
	}

	constructor(context: Context) : super(context) {
		init(context, null)
	}

	fun enablePinchZoom() {
		scaleGestureDetector = ScaleGestureDetector(context, this)
	}

	protected fun init(context: Context, attrs: AttributeSet?) {
		val a = context.obtainStyledAttributes(attrs, R.styleable.UIComponentView)
		borderThickness = a.getDimension(R.styleable.UIComponentView_borderThickness, borderThickness)
		borderColor = a.getColor(R.styleable.UIComponentView_borderColor, borderColor)
		borderPaint.style = Paint.Style.STROKE
		borderPaint.strokeWidth = borderThickness
		borderPaint.color = borderColor
		a.recycle()
	}

	protected open fun getProgress(): Float = 1f

	protected open suspend fun loadAssets(g: DroidGraphics) {}
	protected open fun preDrawInit(g: DroidGraphics) {}
	var loadAssetsRunnable: Job? = null
	protected open fun onLoading() {}
	protected open fun onLoaded() {}
	override fun onDraw(canvas: Canvas) {
		val progress = getProgress()
		val width = (width - borderThickness * 2).roundToInt()
		val height = (height - borderThickness * 2).roundToInt()
		if (!initialized) {
			val BACK: GColor
			BACK = if (background is ColorDrawable) {
				GColor((background as ColorDrawable).color)
			} else {
				GColor.LIGHT_GRAY
			}
			g = object : DroidGraphics(context, canvas, width, height) {
				override fun getBackgroundColor(): GColor {
					return BACK
				}
			}
			g.setCaptureModeSupported(!isInEditMode)
			preDrawInit(g)
			initialized = true
		} else {
			g.setCanvas(canvas, width, height)
		}
		if (borderThickness > 0) {
			canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
			g.translate(borderThickness, borderThickness)
		}
		if (progress < 1) {
			if (!isInEditMode && loadAssetsRunnable == null) {
				onLoading()
				loadAssetsRunnable = CoroutineScope(Dispatchers.Main).launch {
					loadAssets(g)
					post { onLoaded() }
				}
			}
			g.ortho()
			val textWidth = g.getTextWidth(loadingString)
			val rect: GRectangle = GRectangle(0f, 0f, GDimension(max(textWidth + 20, (width * 3 / 4).toFloat()), (height / 6).toFloat())).withCenter(Vector2D((width / 2).toFloat(), (height / 2).toFloat()))

			val hgt = g.textHeight
			g.textHeight = rect.h * 3 / 4
			g.color = GColor.WHITE
			g.drawWrapString((width / 2f), (height / 2f) - rect.h / 2f, rect.w, Justify.CENTER, Justify.BOTTOM, loadingString)
			g.textHeight = hgt

			g.color = GColor.RED
			g.drawRect(rect, 3f)
			rect.w *= progress
			g.drawFilledRect(rect)
		} else if (!isInEditMode) {
			loadAssetsRunnable = null
			val prev = renderer.getMinDimension()
			try {
				renderer.draw(g, tx, ty)
			} catch (e: Exception) {
				e.printStackTrace()
			}
			val next = renderer.getMinDimension()
			if (next != prev) {
				if (isResizable) {
					requestLayout()
					postInvalidate()
				}
			}
		}
		g.translate(-borderThickness, -borderThickness)
	}

	open val loadingString : String
		get() = "LOADING"

	val isResizable: Boolean
		get() {
			val lp = layoutParams
			return lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT
		}
	var dragging = false
	override fun run() {
		if (!dragging && touchDownX >= 0) {
			renderer.onDragStart(touchDownX, touchDownY)
			dragging = true
		}
	}

	final override fun onTouchEvent(event: MotionEvent): Boolean {
		if (!::renderer.isInitialized)
			return false
		scaleGestureDetector?.onTouchEvent(event)
		if (scaling)
			return true

		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				downTime = SystemClock.uptimeMillis()
				tx = event.x.roundToInt().also { touchDownX = it }
				ty = event.y.roundToInt().also { touchDownY = it }
				renderer.onTouch(tx, ty)
				postDelayed(this, CLICK_TIME.toLong())
			}
			MotionEvent.ACTION_UP -> {
				ty = -1
				tx = -1
				touchDownY = tx
				touchDownX = ty
				if (!dragging && SystemClock.uptimeMillis() - downTime < CLICK_TIME) {
					performClick()
					renderer.onClick()
				} else if (dragging) {
					renderer.onDragEnd()
				} else {
					renderer.onTouchUp(event.x.roundToInt(), event.y.roundToInt())
				}
				dragging = false
			}
			MotionEvent.ACTION_MOVE -> {
				tx = event.x.roundToInt()
				ty = event.y.roundToInt()
				if (!dragging) {
					if (Utils.fastLen(event.x - touchDownX, event.y - touchDownY) > 10) {
						dragging = true
						renderer.onDragStart(tx, ty)
					}
				} else {
					renderer.onDragMove(tx, ty)
				}
			}
		}
		postInvalidate()
		return true
	}

	override fun redraw() {
		postInvalidate()
	}

	override fun setRenderer(r: UIRenderer) {
		renderer = r as T
	}

	override fun getViewportLocation(): Vector2D {
		val loc = IntArray(2)
		getLocationOnScreen(loc)
		return Vector2D(loc[0].toFloat(), loc[1].toFloat())
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		if (isInEditMode) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			return
		}
		var width = MeasureSpec.getSize(widthMeasureSpec)
		var height = MeasureSpec.getSize(heightMeasureSpec)
		val wSpec = MeasureSpec.getMode(widthMeasureSpec)
		val hSpec = MeasureSpec.getMode(heightMeasureSpec)
		val dim = renderer.getMinDimension()
		when (wSpec) {
			MeasureSpec.AT_MOST -> width = Math.min(width, Math.round(dim.width))
			MeasureSpec.UNSPECIFIED -> width = Math.round(dim.width)
			MeasureSpec.EXACTLY -> {
			}
		}
		when (hSpec) {
			MeasureSpec.AT_MOST -> height = Math.min(height, Math.round(dim.height))
			MeasureSpec.UNSPECIFIED -> height = Math.round(dim.height)
			MeasureSpec.EXACTLY -> {
			}
		}
		super.setMeasuredDimension(width, height)
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		g.releaseBitmaps()
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		if (::renderer.isInitialized)
			renderer.onSizeChanged(w, h)
	}

	final override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
		if (dragging) {
			renderer.onDragEnd()
		}
		renderer.onDragStart(detector.focusX.roundToInt(), detector.focusY.roundToInt())
		scaling = true
		return true
	}

	final override fun onScaleEnd(detector: ScaleGestureDetector) {
		scaling = false
		dragging = false
		renderer.onDragEnd()
	}

	final override fun onScale(detector: ScaleGestureDetector): Boolean {
		if (::renderer.isInitialized && detector.isInProgress && detector.scaleFactor > 0.1f) {
			renderer.onDragMove(detector.focusX.roundToInt(), detector.focusY.roundToInt())
			renderer.onZoom(1f / detector.scaleFactor)
		}
		return true
	}
}