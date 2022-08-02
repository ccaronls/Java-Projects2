package cc.game.soc.android

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import cc.game.soc.core.Board
import cc.game.soc.ui.*
import cc.lib.android.DroidGraphics
import cc.lib.game.GColor
import cc.lib.math.Vector2D
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

/**
 * Created by chriscaron on 3/2/18.
 */
class SOCView<T : UIRenderer> : View, UIComponent {
	var g: DroidGraphics? = null
	var tx = -1
	var ty = -1

    lateinit var _renderer: T

	private inner class DelayedTouchDown internal constructor(ev: MotionEvent) : Runnable {
		val x: Float
		val y: Float
		override fun run() {
			//onTouchDown(x, y);
			_renderer.onDragStart(Math.round(x).toFloat(), Math.round(y).toFloat())
			touchDownRunnable = null
		}

		init {
			x = ev.x
			y = ev.y
		}
	}

	private val CLICK_TIME = 700
	private var downTime: Long = 0
	private var touchDownRunnable: Runnable? = null

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		init(context, attrs)
	}

	constructor(context: Context) : super(context) {
		init(context, null)
	}

	private fun init(context: Context, attrs: AttributeSet?) {
		when (id) {
			R.id.soc_barbarian -> {
				val r = UIBarbarianRenderer(this)
				val border = resources.getDimension(R.dimen.border_thin)
				r.initAssets(R.drawable.barbarians_tile, R.drawable.barbarians_piece)
				_renderer = r as T
			}
			R.id.soc_board -> {
				val board = Board()
				board.generateDefaultBoard()
				val r: UIBoardRenderer = object : UIBoardRenderer(this@SOCView) {
					override fun onClick() {
						// disable this method since it is hard to pull off easily on a device. Use accept button only.
						//super.doClick();
					}
				}

				r.board = board
				r.initImages(R.drawable.desert,
					R.drawable.water,
					R.drawable.gold,
					R.drawable.undiscoveredtile,
					R.drawable.foresthex,
					R.drawable.hillshex,
					R.drawable.mountainshex,
					R.drawable.pastureshex,
					R.drawable.fieldshex,
					R.drawable.knight_basic_inactive,
					R.drawable.knight_basic_active,
					R.drawable.knight_strong_inactive,
					R.drawable.knight_strong_active,
					R.drawable.knight_mighty_inactive,
					R.drawable.knight_mighty_active) //, R.drawable.card_frame);

				_renderer = r as T
			}
			R.id.soc_dice -> _renderer = UIDiceRenderer(this, true) as T
			R.id.soc_player_1, R.id.soc_player_2, R.id.soc_player_3, R.id.soc_player_4, R.id.soc_player_5, R.id.soc_player_6 -> _renderer = UIPlayerRenderer(this) as T
			R.id.soc_event_cards -> _renderer = UIEventCardRenderer(this) as T
			R.id.soc_console -> _renderer = UIConsoleRenderer(this) as T
		}
	}

	override fun onDraw(canvas: Canvas) {
		if (g == null) {
			g = object : DroidGraphics(context, canvas, width, height) {
				override fun getBackgroundColor(): GColor {
					return GColor.GRAY
				}
			}
			g?.setCaptureModeSupported(!isInEditMode)
		} else {
			g?.setCanvas(canvas, width, height)
		}
		val prev = _renderer.getMinDimension()
		_renderer.draw(g, tx, ty)
		val next = _renderer.getMinDimension()
		if (next != prev) {
			if (isResizable) {
				requestLayout()
				invalidate()
			}
		}
	}

	val isResizable: Boolean
		get() {
			val lp = layoutParams
			return lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT
		}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				downTime = SystemClock.uptimeMillis()
				tx = Math.round(event.x)
				ty = Math.round(event.y)
			}
			MotionEvent.ACTION_UP -> {
				run {
					ty = -1
					tx = ty
				}
				if (SystemClock.uptimeMillis() - downTime < CLICK_TIME) {
					removeCallbacks(touchDownRunnable)
					touchDownRunnable = null
					_renderer.onClick()
				} else {
					_renderer.onDragEnd()
				}
			}
			MotionEvent.ACTION_MOVE -> {
				tx = Math.round(event.x)
				ty = Math.round(event.y)
				if (touchDownRunnable == null) {
					_renderer.onDragStart(event.x, event.y)
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
		_renderer = r as T
	}

	fun getRenderer() : T {
		return _renderer
	}

	override fun getViewportLocation(): Vector2D {
		val loc = IntArray(2)
		getLocationOnScreen(loc)
		return Vector2D(loc[0].toFloat(), loc[1].toFloat())
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		var width = MeasureSpec.getSize(widthMeasureSpec)
		var height = MeasureSpec.getSize(heightMeasureSpec)
		val wSpec = MeasureSpec.getMode(widthMeasureSpec)
		val hSpec = MeasureSpec.getMode(heightMeasureSpec)
		val dim = _renderer.minDimension
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
		if (g != null) {
			g!!.releaseBitmaps()
		}
	}
}