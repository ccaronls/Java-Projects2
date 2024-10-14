package cc.lib.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Spring slider is a widget that the user can slide from a base position where is will spring back to 0 after the user releases
 * @author chriscaron
 */
class SpringSlider : View {
	interface OnSliderChangedListener {
		fun sliderMoved(slider: SpringSlider?, position: Float)
	}

	private var buttonRadius = 0f
	private var sliderLength = 0f
	private var horizontal = false
	private var buttonIcon: Drawable? = null
	var sliderPosition = 0f
		private set
	private val springAccel = 0.01f
	private var springVelocity = 0f
	private var buttonThickness = 5.0f
	private var backgroundColor = Color.CYAN
	private var buttonColor = Color.RED
	private var targetPosition = 0f
	var onSliderChangedListener: OnSliderChangedListener? = null
	private fun init(context: Context, attrs: AttributeSet?) {
		val a = context.obtainStyledAttributes(attrs, R.styleable.SpringSlider)
		buttonRadius = a.getDimension(R.styleable.SpringSlider_buttonRadius, buttonRadius)
		horizontal = a.getBoolean(R.styleable.SpringSlider_horizontal, horizontal)
		buttonIcon = a.getDrawable(R.styleable.SpringSlider_buttonIcon)
		buttonThickness = a.getDimension(R.styleable.SpringSlider_buttonThickness, buttonThickness)
		backgroundColor = a.getColor(R.styleable.SpringSlider_backgroundColor, backgroundColor)
		buttonColor = a.getColor(R.styleable.SpringSlider_buttonColor, buttonColor)
		a.recycle()
	}

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
		init(context, attrs)
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		init(context, attrs)
	}

	constructor(context: Context?) : super(context)

	var paint = Paint()
	override fun onDraw(canvas: Canvas) {
		val radius = buttonRadius
		if (background == null) {
			paint.color = backgroundColor
			paint.style = Paint.Style.FILL
			if (horizontal) {
				canvas.drawCircle(radius, radius, radius, paint)
				canvas.drawCircle(radius + sliderLength, radius, radius, paint)
				canvas.drawRect(radius, 0f, radius + sliderLength, radius * 2, paint)
			} else {
				canvas.drawCircle(radius, radius, radius, paint)
				canvas.drawCircle(radius, radius + sliderLength, radius, paint)
				canvas.drawRect(0f, radius, radius * 2, radius + sliderLength, paint)
			}
		}
		if (buttonIcon == null) {
			paint.style = Paint.Style.STROKE
			paint.strokeWidth = buttonThickness
			paint.color = buttonColor
			if (horizontal) {
				canvas.drawCircle(radius + sliderLength * sliderPosition, radius, radius, paint)
			} else {
				canvas.drawCircle(radius, radius + sliderLength * (1 - sliderPosition), radius, paint)
			}
		} else {
			buttonIcon!!.setColorFilter(buttonColor, PorterDuff.Mode.MULTIPLY)
			if (horizontal) buttonIcon!!.setBounds(
				Math.round(sliderLength * sliderPosition),
				0,
				Math.round(sliderLength * sliderPosition + radius * 2),
				Math.round(radius * 2)
			) else buttonIcon!!.setBounds(
				0,
				height - Math.round(sliderLength * sliderPosition + radius * 2),
				Math.round(radius * 2),
				height - Math.round(sliderLength * sliderPosition)
			)
			buttonIcon!!.draw(canvas)
		}
		if (sliderPosition > 0 && targetPosition != sliderPosition) {
			springVelocity += springAccel
			sliderPosition -= springVelocity
			if (sliderPosition < 0) sliderPosition = 0f
			invalidate()
			if (onSliderChangedListener != null) onSliderChangedListener!!.sliderMoved(this, sliderPosition)
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		var width = MeasureSpec.getSize(widthMeasureSpec)
		var height = MeasureSpec.getSize(heightMeasureSpec)
		if (buttonRadius <= 0) {
			if (horizontal) {
				// width must be greater than height
				if (height < 10) height = 10
				if (width < height * 2) width = height * 2
				buttonRadius = (height / 2).toFloat()
				sliderLength = width - buttonRadius * 2
			} else {
				// height must be greater then width
				if (width < 10) width = 10
				if (height < width * 2) height = width * 2
				buttonRadius = (width / 2).toFloat()
				sliderLength = height - buttonRadius * 2
			}
		} else {
			if (horizontal) {
				height = Math.round(buttonRadius * 2)
				if (width < height * 2) width = height * 2
			} else {
				width = Math.round(buttonRadius * 2)
				if (height < width * 2) height = width * 2
			}
		}
		setMeasuredDimension(width, height)
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.action) {
			MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
				if (horizontal) {
					val x = event.x
					if (x < buttonRadius) {
						sliderPosition = 0f
						targetPosition = sliderPosition
					} else if (x > buttonRadius + sliderLength) {
						sliderPosition = 1f
						targetPosition = sliderPosition
					} else {
						sliderPosition = (x - buttonRadius) / sliderLength
						targetPosition = sliderPosition
					}
				} else {
					val y = event.y
					if (y < buttonRadius) {
						sliderPosition = 1f
						targetPosition = sliderPosition
					} else if (y > buttonRadius + sliderLength) {
						sliderPosition = 0f
						targetPosition = sliderPosition
					} else {
						sliderPosition = 1f - (y - buttonRadius) / sliderLength
						targetPosition = sliderPosition
					}
				}
				if (onSliderChangedListener != null) onSliderChangedListener!!.sliderMoved(this, sliderPosition)
			}

			MotionEvent.ACTION_UP -> {
				targetPosition = 0f
				springVelocity = 0f
			}

			else -> return false
		}
		invalidate()
		return true
	}
}
