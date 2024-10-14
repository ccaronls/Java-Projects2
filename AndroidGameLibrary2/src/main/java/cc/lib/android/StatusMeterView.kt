package cc.lib.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import java.util.LinkedList

/**
 * This view can be used to show a meter bar, gps status is a typical usage.
 * Can be any one of 4 orientations.
 * @author chriscaron
 */
class StatusMeterView : View {
	private val METER_COLOR_INDEX_DEFAULT = 0
	private val METER_COLOR_INDEX_POOR = 1
	private val METER_COLOR_INDEX_FAIR = 2
	private val METER_COLOR_INDEX_GOOD = 3
	private val NUM_METER_COLORS_INDICES = 4 // MUST BE LAST!

	enum class MeterStyle {
		ONE_COLOR,
		MULTI_COLOR
	}

	var direction = DIRECTION_LEFT_TO_RIGHT
	var meterTotal = 8
	var meterPoor = 8 / 3
	var meterFair = 8 * 2 / 3
	private var textViewMeterValueId = -1
	val meterColors = IntArray(NUM_METER_COLORS_INDICES)
	var meterThickness = 0f
	var meterSpacing = 5f
	private var meterValue = 0f
	var meterMaxValue = 1f
	private var rounded = false
	private val paint = Paint()
	private val rectF = RectF()
	private val rectI = Rect()
	var length = 0f
	var barDrawable: Drawable? = null
	var meterStyle: MeterStyle? = null
	private var animatePeaks = false
	private var peak = 0f
	private fun init(context: Context, attrs: AttributeSet?) {
		val a = context.obtainStyledAttributes(attrs, R.styleable.StatusMeterView)
		direction = a.getInt(R.styleable.StatusMeterView_direction, DIRECTION_LEFT_TO_RIGHT)
		meterColors[0] = a.getColor(R.styleable.StatusMeterView_meterColor, Color.BLACK)
		meterColors[1] = a.getColor(R.styleable.StatusMeterView_poorColor, Color.RED)
		meterColors[2] = a.getColor(R.styleable.StatusMeterView_fairColor, Color.YELLOW)
		meterColors[3] = a.getColor(R.styleable.StatusMeterView_goodColor, Color.GREEN)
		meterTotal = a.getInt(R.styleable.StatusMeterView_meterTotal, meterTotal)
		meterFair = a.getInt(R.styleable.StatusMeterView_meterFair, meterTotal * 2 / 3)
		textViewMeterValueId = a.getResourceId(R.styleable.StatusMeterView_textView, -1)
		meterMaxValue = a.getFloat(R.styleable.StatusMeterView_meterMaxValue, meterMaxValue)
		setMeterValue(a.getFloat(R.styleable.StatusMeterView_meterValue, meterValue))
		rounded = a.getBoolean(R.styleable.StatusMeterView_rounded, false)
		val id = a.getResourceId(R.styleable.StatusMeterView_drawable, -1)
		if (id != -1) {
			barDrawable = resources.getDrawable(id)
		}
		meterThickness = a.getDimension(R.styleable.StatusMeterView_meterThickness, 0f)
		meterSpacing = a.getDimension(R.styleable.StatusMeterView_meterSpacing, 0f)
		meterStyle = MeterStyle.entries[a.getInt(
			R.styleable.StatusMeterView_meterStyle,
			MeterStyle.ONE_COLOR.ordinal
		)]
		animatePeaks = a.getBoolean(R.styleable.StatusMeterView_animatePeaks, false)
		a.recycle()
		paint.style = Paint.Style.FILL
		paint.isAntiAlias = true
	}

	constructor(context: Context?) : super(context)
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
		init(context, attrs)
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		init(context, attrs)
	}

	private var lastUpdateTime: Long = 0
	private var peakOffsetRampDown = 0f
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas) // draw background if any
		var numMeters = Math.round(meterValue / meterMaxValue * meterTotal)
		val secondsToFallEntireLength = 300 // higher number result in a quicker move of peak toward 0 

		// The below number have been tuned to look like iOS 24/7 as of 12/15/14
		if (peak < numMeters) {
			peak = numMeters.toFloat()
			peakOffsetRampDown = (Math.random() * 2 + 1).toFloat()
		}
		if (animatePeaks && peak > numMeters) {
			var rampDown = Math.round(peak - peakOffsetRampDown)
			if (numMeters < rampDown) {
				numMeters = rampDown
				rampDown = (rampDown + (Math.random() * 0.5 + 0.5).toFloat()).toInt()
			}
		}
		var meterColor = METER_COLOR_INDEX_DEFAULT
		meterColor = if (numMeters <= meterPoor) {
			METER_COLOR_INDEX_POOR
		} else if (numMeters <= meterFair) {
			METER_COLOR_INDEX_FAIR
		} else {
			METER_COLOR_INDEX_GOOD
		}
		paint.color = meterColors[meterColor]
		var drawable: Drawable? = null
		if (barDrawable != null) {
			drawable = barDrawable!!.constantState!!.newDrawable()
		}
		var sx = 0f
		var sy = 0f
		var dx = 0f
		var dy = 0f
		var w = 0f
		var h = 0f
		when (direction) {
			DIRECTION_LEFT_TO_RIGHT -> {
				sx = paddingLeft.toFloat()
				sy = paddingTop.toFloat()
				dy = 0f
				dx = meterThickness + meterSpacing
				w = meterThickness
				h = (height - paddingTop - paddingBottom).toFloat()
			}

			DIRECTION_RIGHT_TO_LEFT -> {
				sy = paddingTop.toFloat()
				dy = 0f
				dx = -(meterThickness + meterSpacing)
				sx = paddingLeft + length - meterThickness
				w = meterThickness
				h = (height - paddingTop - paddingBottom).toFloat()
			}

			DIRECTION_TOP_TO_BOTTOM -> {
				sx = paddingLeft.toFloat()
				sy = paddingTop.toFloat()
				dx = 0f
				dy = meterThickness + meterSpacing
				w = (width - paddingLeft - paddingRight).toFloat()
				h = meterThickness
			}

			DIRECTION_BOTTOM_TO_TOP -> {
				sx = paddingLeft.toFloat()
				sy = paddingTop + length - meterThickness
				dx = 0f
				dy = -(meterThickness + meterSpacing)
				w = (width - paddingLeft - paddingRight).toFloat()
				h = meterThickness
			}

			else -> throw RuntimeException("Unhandled direction")
		}
		var meter = 0
		val radius: Float = if (rounded) meterThickness else 0f
		while (meter < numMeters) {
			if (meterStyle == MeterStyle.MULTI_COLOR) {
				meterColor =
					if (meter <= meterPoor) METER_COLOR_INDEX_POOR else if (meter <= meterFair) METER_COLOR_INDEX_FAIR else METER_COLOR_INDEX_GOOD
				paint.color = meterColors[meterColor]
			}
			val x = sx + dx * meter
			val y = sy + dy * meter
			rectF[x, y, x + w] = y + h
			rectI[Math.round(x), Math.round(y), Math.round(x + w)] = Math.round(y + h)
			if (drawable != null) {
				drawable.bounds = rectI
				drawable.setColorFilter(meterColors[meterColor], PorterDuff.Mode.MULTIPLY)
				drawable.draw(canvas)
			} else if (radius > 0) {
				canvas.drawRoundRect(rectF, radius, radius, paint)
			} else {
				canvas.drawRect(rectF, paint)
			}
			meter++
		}
		paint.color = meterColors[METER_COLOR_INDEX_DEFAULT]
		meterColor = METER_COLOR_INDEX_DEFAULT
		while (meter < meterTotal) {
			val x = sx + dx * meter
			val y = sy + dy * meter
			rectF[x, y, x + w] = y + h
			rectI[Math.round(x), Math.round(y), Math.round(x + w)] = Math.round(y + h)
			if (drawable != null) {
				drawable.bounds = rectI
				drawable.setColorFilter(meterColors[meterColor], PorterDuff.Mode.MULTIPLY)
				drawable.draw(canvas)
			} else if (radius > 0) {
				canvas.drawRoundRect(rectF, radius, radius, paint)
			} else {
				canvas.drawRect(rectF, paint)
			}
			meter++
		}
		if (animatePeaks && lastUpdateTime != 0L && peak > 1) {
			val dt = (SystemClock.uptimeMillis() - lastUpdateTime).toFloat() / 1000
			val peakChange = dt * secondsToFallEntireLength / meterTotal.toFloat()
			peak -= peakChange
			val x = sx + dx * Math.round(peak)
			val y = sy + dy * Math.round(peak)
			rectF[x, y, x + w] = y + h
			rectI[Math.round(x), Math.round(y), Math.round(x + w)] = Math.round(y + h)
			if (meterStyle == MeterStyle.MULTI_COLOR) {
				meterColor =
					if (peak <= meterPoor) METER_COLOR_INDEX_POOR else if (peak <= meterFair) METER_COLOR_INDEX_FAIR else METER_COLOR_INDEX_GOOD
				paint.color = meterColors[meterColor]
			}
			if (drawable != null) {
				drawable.setColorFilter(meterColors[meterColor], PorterDuff.Mode.MULTIPLY)
				drawable.bounds = rectI
				drawable.draw(canvas)
			} else if (radius > 0) {
				canvas.drawRoundRect(rectF, radius, radius, paint)
			} else {
				canvas.drawRect(rectF, paint)
			}
		}
		if (peak > 1 && animatePeaks && !isInEditMode) {
			lastUpdateTime = SystemClock.uptimeMillis()
			postInvalidate()
		} else {
			lastUpdateTime = 0
		}
	}

	val isVertical: Boolean
		/**
		 *
		 * @return
		 */
		get() = direction == DIRECTION_TOP_TO_BOTTOM || direction == DIRECTION_BOTTOM_TO_TOP

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		var widthMeasureSpec = widthMeasureSpec
		var heightMeasureSpec = heightMeasureSpec
		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)
		var widthSpec = MeasureSpec.getSize(widthMeasureSpec)
		var heightSpec = MeasureSpec.getSize(heightMeasureSpec)
		if (barDrawable != null && meterThickness == 0f) {
			meterThickness = if (isVertical) {
				barDrawable!!.intrinsicHeight.toFloat()
			} else {
				barDrawable!!.intrinsicWidth.toFloat()
			}
		}

		// TODO: If we are unspecified, then return a desired dimension
		do {
			if (meterThickness > 0) {
				if (meterSpacing <= 0) {
					meterSpacing = meterThickness / 3
				}
				var desiredLength = Math.round(meterThickness * meterTotal + meterSpacing * (meterTotal - 1))
				if (widthMode == MeasureSpec.UNSPECIFIED && !isVertical) {
					widthMeasureSpec = MeasureSpec.makeMeasureSpec(desiredLength, widthMode)
				} else if (heightMode == MeasureSpec.UNSPECIFIED && isVertical) {
					heightMeasureSpec = MeasureSpec.makeMeasureSpec(desiredLength, heightMode)
				} else if (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED) {
					// exit out with current specs
					desiredLength = 0
				} else break
				super.onMeasure(widthMeasureSpec, heightMeasureSpec)
				return
			}
		} while (false)


		//Log.d("StatsuMeterView", "onMeasure: wMode=" + getModeString(widthMode) + " width=" + widthSpec + " heightMode=" + getModeString(heightMode) + " height=" + heightSpec);
		length =
			if (isVertical) (heightSpec - paddingTop - paddingBottom).toFloat() else (widthSpec - paddingLeft - paddingRight).toFloat()

		// we want to know the meter thickness and spacing to fill the length of the canavs
		//
		// algebra
		//
		// l = length (known)
		// mt = meter thickness (this is what we want to know)
		// ms = meter spacing = mt/3 (derived)
		// t = meter total (known)
		//
		// l = t*mt + (t-1)*ms
		//   = t*mt + (t-1)*mt/3
		//   = (t + (t-1)/3) * mt;
		// mt = l / (t + t/3 - 1/3)
		var actualLength = 0f
		for (i in 0..1) {
			if (meterSpacing > 0) {
				if (meterThickness <= 0) {
					meterThickness = (length - (meterTotal - 1) * meterSpacing) / meterTotal
				}
				//eclipseLog(this, "onMeasure: meterTotal=" + meterTotal + " length=" + length + " meterThickness = "+ meterThickness);
			} else {
				if (meterThickness <= 0) {
					meterThickness = length / (meterTotal.toFloat() + meterTotal.toFloat() / 3 - 1f / 3)
				}
				meterSpacing = meterThickness / 3
			}

			// check that we are in the maxWidth
			actualLength = meterThickness * meterTotal + meterSpacing * (meterTotal - 1)
			if (meterThickness > 0 && actualLength - 0.01 <= length) break

			//adtLog(this, "onMeasure: constraint not met, redo");
			meterThickness = 0f
			meterSpacing = 0f
		}

		//adtLog(this, "length=" + length + " actualLength=" + actualLength);
		if (isVertical) {
			if (heightMode != MeasureSpec.EXACTLY) {
				length = actualLength
				heightSpec = actualLength.toInt() + paddingTop + paddingBottom
			}
			if (widthMode != MeasureSpec.EXACTLY) {
				widthSpec = Math.max(suggestedMinimumWidth, heightSpec / 4)
			}
		} else {
			if (widthMode != MeasureSpec.EXACTLY) {
				length = actualLength
				widthSpec = actualLength.toInt() + paddingLeft + paddingRight
			}
			if (heightMode != MeasureSpec.EXACTLY) {
				heightSpec = Math.max(suggestedMinimumHeight, widthSpec / 4)
			}
		}
		setMeasuredDimension(widthSpec, heightSpec)
	}

	/**
	 *
	 * @param value
	 */
	fun setMeterValue(value: Float) {
		var value = value
		if (value < 0) value = 0f else if (value > meterMaxValue) value = meterMaxValue
		meterValue = value
		if (textViewMeterValueId >= 0) {
			val tv = this.rootView.findViewById<View>(textViewMeterValueId) as TextView
			if (tv != null) tv.text = String.format("%.2f", meterValue)
		}
		invalidate()
	}

	override fun setVisibility(visibility: Int) {
		super.setVisibility(visibility)
		if (textViewMeterValueId >= 0) {
			val tv = this.rootView.findViewById<View>(textViewMeterValueId) as TextView
			if (tv != null) tv.visibility = visibility
		}
	}

	override fun setEnabled(enabled: Boolean) {
		super.setEnabled(enabled)
		if (textViewMeterValueId >= 0) {
			val tv = this.rootView.findViewById<View>(textViewMeterValueId) as TextView
			if (tv != null) tv.isEnabled = enabled
		}
	}

	/**
	 *
	 * @return
	 */
	fun getMeterValue(): Float {
		return meterValue
	}

	fun getModeString(specMode: Int): String {
		when (specMode) {
			MeasureSpec.UNSPECIFIED -> return "UNSPECIFIED"
			MeasureSpec.EXACTLY -> return "EXACTLY"
			MeasureSpec.AT_MOST -> return "AT_MOST"
		}
		return "UNKNOWN"
	}

	companion object {
		private const val DIRECTION_LEFT_TO_RIGHT = 0
		private const val DIRECTION_RIGHT_TO_LEFT = 1
		private const val DIRECTION_TOP_TO_BOTTOM = 2
		private const val DIRECTION_BOTTOM_TO_TOP = 3

		// ADT Debug logging support
		var lines = LinkedList<String>()
		var msgCount = 0
	}
}
