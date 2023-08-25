package cc.lib.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
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

abstract class UIComponentView<T : UIRenderer> : View, UIComponent, Runnable {
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
	private var touchDownX = 0f
	private var touchDownY = 0f

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		init(context, attrs)
	}

	constructor(context: Context) : super(context) {
		init(context, null)
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
			canvas.drawRect(0f, 0f, getWidth().toFloat(), getHeight().toFloat(), borderPaint)
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
			g.color = GColor.RED
			g.ortho()
			val textWidth = g.getTextWidth(loadingString)
			val rect: GRectangle = GRectangle(0f, 0f, GDimension(max(textWidth + 20, (getWidth() * 3 / 4).toFloat()), (getHeight() / 6).toFloat())).withCenter(Vector2D((getWidth() / 2).toFloat(), (getHeight() / 2).toFloat()))
			g.drawRect(rect, 3f)
			rect.w *= progress
			g.drawFilledRect(rect)
			val hgt = g.textHeight
			g.textHeight = rect.h * 3 / 4
			g.color = GColor.WHITE
			g.drawJustifiedString((getWidth() / 2).toFloat(), (getHeight() / 2).toFloat(), Justify.CENTER, Justify.CENTER, loadingString)
			g.textHeight = hgt
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
					invalidate()
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

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				downTime = SystemClock.uptimeMillis()
				tx = Math.round(event.x.also { touchDownX = it })
				ty = Math.round(event.y.also { touchDownY = it })
				postDelayed(this, CLICK_TIME.toLong())
			}
			MotionEvent.ACTION_UP -> {
				run {
					ty = -1
					tx = ty
					touchDownY = tx.toFloat()
					touchDownX = touchDownY
				}
				if (!dragging && SystemClock.uptimeMillis() - downTime < CLICK_TIME) {
					renderer.onClick()
				} else if (dragging) {
					renderer.onDragEnd()
				}
				dragging = false
			}
			MotionEvent.ACTION_MOVE -> {
				tx = Math.round(event.x)
				ty = Math.round(event.y)
				if (!dragging) {
					if (Utils.fastLen(event.x - touchDownX, event.y - touchDownY) > 10) {
						dragging = true
						renderer.onDragStart(event.x, event.y)
					}
				} else {
					renderer.onDragMove(event.x, event.y)
				}
			}
		}
		invalidate()
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
}