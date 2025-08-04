package cc.lib.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import kotlin.math.max
import kotlin.math.min

/**
 * Created by Chris Caron on 7/31/25.
 */
class TooltipDrawable(
	@ColorInt private var backgroundColor: Int = Color.WHITE,
	private var cornerRadius: Float = 16f,
	private var arrowWidth: Float = 20f,
	private var arrowHeight: Float = 12f,
	private var arrowSide: ArrowSide = ArrowSide.BOTTOM,
	private var arrowPosition: Float = 0.5f, // 0f=start, 1f=end
	@ColorInt private var borderColor: Int = Color.TRANSPARENT,
	private var borderWidth: Float = 0f // in pixels
) : Drawable() {

	@JvmOverloads
	constructor(context: Context, attributeSet: AttributeSet? = null) : this() {
		context.obtainStyledAttributes(attributeSet, R.styleable.TooltipDrawable).apply {
			backgroundColor = getColor(R.styleable.TooltipDrawable_tooltipBackgroundColor, backgroundColor)
			cornerRadius = getDimension(R.styleable.TooltipDrawable_tooltipCornerRadius, cornerRadius)
			arrowWidth = getDimension(R.styleable.TooltipDrawable_tooltipArrowWidth, arrowWidth)
			arrowHeight = getDimension(R.styleable.TooltipDrawable_tooltipArrowHeight, arrowHeight)
			borderColor = getColor(R.styleable.TooltipDrawable_tooltipBorderColor, borderColor)
			borderWidth = getDimension(R.styleable.TooltipDrawable_tooltipArrowWidth, borderWidth)
			recycle()
		}
	}

	enum class ArrowSide {
		TOP,
		BOTTOM,
		LEFT,
		RIGHT
	}

	private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
		color = backgroundColor
	}

	private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = borderWidth
		color = borderColor
	}

	private val path = Path()

	fun setArrowSide(side: ArrowSide) {
		arrowSide = side
		invalidateSelf()
	}

	fun setArrowPosition(position: Float) {
		arrowPosition = min(1f, max(0f, position))
		invalidateSelf()
	}

	fun setBackgroundColor(@ColorInt color: Int) {
		backgroundColor = color
		fillPaint.color = color
		invalidateSelf()
	}

	fun setBorder(@ColorInt color: Int, width: Float) {
		borderColor = color
		borderWidth = width
		strokePaint.color = color
		strokePaint.strokeWidth = width
		invalidateSelf()
	}

	override fun draw(canvas: Canvas) {
		path.reset()
		val bounds = bounds
		val left = bounds.left.toFloat()
		val top = bounds.top.toFloat()
		val right = bounds.right.toFloat()
		val bottom = bounds.bottom.toFloat()
		val width = right - left
		val height = bottom - top
		val cr2 = cornerRadius / 2

		val rect = RectF()

		when (arrowSide) {
			ArrowSide.TOP -> {
				val arrowLeft = (left + arrowPosition * width - arrowWidth / 2).coerceAtLeast(cornerRadius)
				val arrowRight = (left + arrowPosition * width + arrowWidth / 2).coerceAtMost(width - cr2)
				path.moveTo(arrowRight, top + arrowHeight)
				path.lineTo(right - cornerRadius, top + arrowHeight)
				path.arcTo(right - cornerRadius, top + arrowHeight, right, top + arrowHeight + cornerRadius, 270f, 90f, false)
				path.lineTo(right, bottom - cornerRadius)
				path.arcTo(right - cornerRadius, bottom - cornerRadius, right, bottom, 0f, 90f, false)
				path.lineTo(left + cornerRadius, bottom)
				path.arcTo(left, bottom - cornerRadius, left + cornerRadius, bottom, 90f, 90f, false)
				path.lineTo(left, top + arrowHeight + cornerRadius)
				path.arcTo(left, top + arrowHeight, left + cornerRadius, top + arrowHeight + cornerRadius, 180f, 90f, false)
				path.lineTo(arrowLeft, top + arrowHeight)
				path.rLineTo(arrowWidth / 2, -arrowHeight)
				path.close()
			}

			ArrowSide.BOTTOM -> {
				val arrowLeft = (left + arrowPosition * width - arrowWidth / 2).coerceAtLeast(cornerRadius)
				val arrowRight = (left + arrowPosition * width + arrowWidth / 2).coerceAtMost(width - cr2)
				path.moveTo(arrowLeft, bottom - arrowHeight)
				path.lineTo(left + cornerRadius, bottom - arrowHeight)
				path.arcTo(left, bottom - arrowHeight - cornerRadius, left + cornerRadius, bottom - arrowHeight, 90f, 90f, false)
				path.lineTo(left, top + cornerRadius)
				path.arcTo(left, top, left + cornerRadius, top + cornerRadius, 180f, 90f, false)
				path.lineTo(right - cornerRadius, top)
				path.arcTo(right - cornerRadius, top, right, top + cornerRadius, 270f, 90f, false)
				path.lineTo(right, bottom - cornerRadius - arrowHeight)
				path.arcTo(right - cornerRadius, bottom - cornerRadius - arrowHeight, right, bottom - arrowHeight, 0f, 90f, false)
				path.lineTo(arrowRight, bottom - arrowHeight)
				path.rLineTo(-arrowWidth / 2, arrowHeight)
				path.close()
			}

			ArrowSide.LEFT -> {
				val arrowOffset = top + cornerRadius + arrowPosition * (bounds.height() - 2 * cornerRadius - arrowWidth)
				rect.set(left + arrowHeight, top, right, bottom)
				path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
				path.moveTo(left + arrowHeight, arrowOffset)
				path.lineTo(left, arrowOffset + arrowWidth / 2)
				path.lineTo(left + arrowHeight, arrowOffset + arrowWidth)
				path.close()
			}

			ArrowSide.RIGHT -> {
				val arrowOffset = top + cornerRadius + arrowPosition * (bounds.height() - 2 * cornerRadius - arrowWidth)
				rect.set(left, top, right - arrowHeight, bottom)
				path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
				path.moveTo(right - arrowHeight, arrowOffset)
				path.lineTo(right, arrowOffset + arrowWidth / 2)
				path.lineTo(right - arrowHeight, arrowOffset + arrowWidth)
				path.close()
			}
		}

		// Draw fill
		canvas.drawPath(path, fillPaint)

		// Draw border if set
		if (borderWidth > 0f && borderColor != Color.TRANSPARENT) {
			canvas.drawPath(path, strokePaint)
		}
	}

	override fun setAlpha(alpha: Int) {
		fillPaint.alpha = alpha
		strokePaint.alpha = alpha
		invalidateSelf()
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		fillPaint.colorFilter = colorFilter
		strokePaint.colorFilter = colorFilter
		invalidateSelf()
	}

	override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
