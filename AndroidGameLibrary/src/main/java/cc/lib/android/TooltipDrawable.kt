package cc.lib.android

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import kotlin.math.max
import kotlin.math.min

/**
 * Created by Chris Caron on 7/31/25.
 */
class TooltipDrawable(
	@ColorInt private var backgroundColor: Int,
	private var cornerRadius: Float = 16f,
	private var arrowWidth: Float = 20f,
	private var arrowHeight: Float = 12f,
	private var arrowSide: ArrowSide = ArrowSide.BOTTOM,
	private var arrowPosition: Float = 0.5f, // 0f=start, 1f=end
	@ColorInt private var borderColor: Int = Color.TRANSPARENT,
	private var borderWidth: Float = 0f // in pixels
) : Drawable() {

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

		val arrowOffset: Float
		val rect = RectF()

		when (arrowSide) {
			ArrowSide.TOP -> {
				arrowOffset = left + cornerRadius + arrowPosition * (bounds.width() - 2 * cornerRadius - arrowWidth)
				rect.set(left, top + arrowHeight, right, bottom)
				path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
				path.moveTo(arrowOffset, top + arrowHeight)
				path.lineTo(arrowOffset + arrowWidth / 2, top)
				path.lineTo(arrowOffset + arrowWidth, top + arrowHeight)
				path.close()
			}

			ArrowSide.BOTTOM -> {
				arrowOffset = left + cornerRadius + arrowPosition * (bounds.width() - 2 * cornerRadius - arrowWidth)
				rect.set(left, top, right, bottom - arrowHeight)
				path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
				path.moveTo(arrowOffset, bottom - arrowHeight)
				path.lineTo(arrowOffset + arrowWidth / 2, bottom)
				path.lineTo(arrowOffset + arrowWidth, bottom - arrowHeight)
				path.close()
			}

			ArrowSide.LEFT -> {
				arrowOffset = top + cornerRadius + arrowPosition * (bounds.height() - 2 * cornerRadius - arrowWidth)
				rect.set(left + arrowHeight, top, right, bottom)
				path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
				path.moveTo(left + arrowHeight, arrowOffset)
				path.lineTo(left, arrowOffset + arrowWidth / 2)
				path.lineTo(left + arrowHeight, arrowOffset + arrowWidth)
				path.close()
			}

			ArrowSide.RIGHT -> {
				arrowOffset = top + cornerRadius + arrowPosition * (bounds.height() - 2 * cornerRadius - arrowWidth)
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
