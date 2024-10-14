package cc.lib.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import cc.lib.game.GColor

class DPadView : ImageView {
	interface OnDpadListener {
		fun dpadPressed(view: DPadView?, dir: PadDir?)
		fun dpadReleased(view: DPadView?, dir: PadDir?)
	}

	private var cx = 0f
	private var cy = 0f
	private var tx = 0f
	private var ty = 0f
	var dx = 0f
		private set
	var dy = 0f
		private set
	private val touched = false

	enum class PadDir(val flag: Int) {
		LEFT(1),
		RIGHT(2),
		UP(4),
		DOWN(8)
	}

	private val paint = Paint()
	private var downFlag = 0
	val pressure = 0f
	private var listener: OnDpadListener? = null
	val isLeftPressed: Boolean
		get() = touched && downFlag and PadDir.LEFT.flag != 0
	val isRightPressed: Boolean
		get() = touched && downFlag and PadDir.RIGHT.flag != 0
	val isUpPressed: Boolean
		get() = touched && downFlag and PadDir.UP.flag != 0
	val isDownPressed: Boolean
		get() = touched && downFlag and PadDir.DOWN.flag != 0

	fun setOnDpadListener(listener: OnDpadListener?) {
		this.listener = listener
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val width = MeasureSpec.getSize(widthMeasureSpec)
		val height = MeasureSpec.getSize(heightMeasureSpec)
		var dim = if (width > height) height else width
		val max = if (width > height) heightMeasureSpec else widthMeasureSpec
		dim = roundUpToTile(dim, 1, max)
		setMeasuredDimension(dim, dim)
	}

	private fun roundUpToTile(dimension: Int, tileSize: Int, maxDimension: Int): Int {
		return Math.min((dimension + tileSize - 1) / tileSize * tileSize, maxDimension)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		if (BuildConfig.DEBUG) {
			paint.color = GColor.GREEN.toARGB()
			paint.strokeWidth = 4f
			canvas.drawCircle(cx + dx, cy + dy, 10f, paint)
		}
		val RECT_DIM = 16f
		paint.color = GColor.GREEN.toARGB()
		paint.style = Paint.Style.FILL
		paint.strokeWidth = 3f
		if (isDownPressed) {
			val left = width / 2 - RECT_DIM / 2
			val right = width / 2 + RECT_DIM / 2
			val bottom = (height / 2 + width / 2).toFloat()
			val top = bottom - RECT_DIM
			canvas.drawRect(left, top, right, bottom, paint)
		}
		if (isLeftPressed) {
			val left = 0f
			val top = height / 2 - RECT_DIM / 2
			val bottom = top + RECT_DIM
			canvas.drawRect(left, top, RECT_DIM, bottom, paint)
		}
		if (isRightPressed) {
			val left = width - RECT_DIM
			val right = width.toFloat()
			val top = height / 2 - RECT_DIM / 2
			val bottom = top + RECT_DIM
			canvas.drawRect(left, top, right, bottom, paint)
		}
		if (isUpPressed) {
			val left = width / 2 - RECT_DIM / 2
			val right = width / 2 + RECT_DIM / 2
			val top = (height / 2 - width / 2).toFloat()
			val bottom = top + RECT_DIM
			canvas.drawRect(left, top, right, bottom, paint)
		}
		canvas.drawLine(cx, cy, cx + dx, cy + dy, paint)
	}

	private fun init() {
		//setOnTouchListener(this);
	}

	constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
		init()
	}

	constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
		init()
	}

	constructor(context: Context?) : super(context) {
		init()
	}

	fun doTouch(event: MotionEvent, x: Float, y: Float) {
		var touched = false
		when (event.action) {
			MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> touched = true
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {}
			else -> return
		}
		if (!touched) {
			dy = 0f
			dx = dy
		} else {
			tx = x
			ty = y
			cx = (width / 2).toFloat() // + view.getLeft()
			cy = (height / 2).toFloat() // + view.getTop()
			dx = tx - cx
			dy = ty - cy
			var flag = 0
			val x0 = (width / 3).toFloat()
			val x1 = x0 * 2
			val y0 = (height / 2 - width / 3).toFloat()
			val y1 = (height / 2 + width / 3).toFloat()
			if (tx < x0) {
				flag = flag or PadDir.LEFT.flag
			} else if (tx > x1) {
				flag = flag or PadDir.RIGHT.flag
			}
			if (ty < y0) {
				flag = flag or PadDir.UP.flag
			} else if (ty > y1) {
				flag = flag or PadDir.DOWN.flag
			}
			if (listener != null) {
				if (flag and PadDir.LEFT.flag != 0) {
					if (downFlag and PadDir.LEFT.flag == 0) listener!!.dpadPressed(this, PadDir.LEFT)
				} else {
					if (downFlag and PadDir.LEFT.flag != 0) listener!!.dpadReleased(this, PadDir.LEFT)
				}
				if (flag and PadDir.RIGHT.flag != 0) {
					if (downFlag and PadDir.RIGHT.flag == 0) listener!!.dpadPressed(this, PadDir.RIGHT)
				} else {
					if (downFlag and PadDir.RIGHT.flag != 0) listener!!.dpadReleased(this, PadDir.RIGHT)
				}
				if (flag and PadDir.UP.flag != 0) {
					if (downFlag and PadDir.UP.flag == 0) listener!!.dpadPressed(this, PadDir.UP)
				} else {
					if (downFlag and PadDir.UP.flag != 0) listener!!.dpadReleased(this, PadDir.UP)
				}
				if (flag and PadDir.DOWN.flag != 0) {
					if (downFlag and PadDir.DOWN.flag == 0) listener!!.dpadPressed(this, PadDir.DOWN)
				} else {
					if (downFlag and PadDir.DOWN.flag != 0) listener!!.dpadReleased(this, PadDir.DOWN)
				}
			}
			downFlag = flag
		}
		invalidate()
	}
}
