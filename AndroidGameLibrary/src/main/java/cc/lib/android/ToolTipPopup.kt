package cc.lib.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView

/**
 * Created by Chris Caron on 7/31/25.
 */
class ToolTipPopup private constructor(
	private val popupWindow: PopupWindow
) {

	fun dismiss() {
		popupWindow.dismiss()
	}


	companion object {


		@SuppressLint("ResourceType")
		fun show(
			context: Context,
			anchorView: View,
			text: String,
			_gravity: Int? = null, // default to compute automatically
			onClick: (() -> Unit)? = null
		): ToolTipPopup {

			// Read defaults from theme
			//val themeValues = getTooltipStyle(context)

			val attrs = intArrayOf(
				R.attr.tooltipBackgroundColor,
				R.attr.tooltipTextColor,
				R.attr.tooltipBorderColor,
				R.attr.tooltipBorderWidth,
				R.attr.tooltipCornerRadius,
				R.attr.tooltipArrowWidth,
				R.attr.tooltipArrowHeight
			)

			// Use theme defaults if caller doesn't provide a value
			val ta = context.obtainStyledAttributes(attrs)
			var idx = 0
			val backgroundColor = ta.getColor(idx++, Color.parseColor("#6200EE"))
			val textColor = ta.getColor(idx++, Color.WHITE)
			val borderColor = ta.getColor(idx++, Color.WHITE)
			val borderWidthPx = ta.getDimension(idx++, 2f)
			val cornerRadiusPx = ta.getDimension(idx++, 20f)
			val arrowWidthPx = ta.getDimension(idx++, 24f)
			val arrowHeightPx = ta.getDimension(idx++, 16f)
			ta.recycle()
			// Inflate layout
			val inflater = LayoutInflater.from(context)
			val popupView = inflater.inflate(R.layout.tooltip_popup, null)
			val tooltipTextView = popupView.findViewById<TextView>(R.id.tooltipText)
			val container = popupView.findViewById<ViewGroup>(R.id.tooltipContainer)

			tooltipTextView.text = text
			tooltipTextView.setTextColor(textColor)

			// Create PopupWindow (unmeasured yet)
			val popupWindow = PopupWindow(
				popupView,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				true
			)
			popupWindow.isOutsideTouchable = true
			popupWindow.setBackgroundDrawable(null)

			// Measure popup to get size
			popupView.measure(
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
			)
			val popupWidth = popupView.measuredWidth
			val popupHeight = popupView.measuredHeight

			// Get screen size
			val displayMetrics = context.resources.displayMetrics
			val screenWidth = displayMetrics.widthPixels
			val screenHeight = displayMetrics.heightPixels

			// Get anchor position
			val (anchorX, anchorY) = anchorView.getLocationOnScreen()
			val anchorCenterX = anchorX + anchorView.width / 2
			val anchorCenterY = anchorY + anchorView.height / 2

			val gravity: Int = _gravity ?: run {
				if (anchorY > screenHeight / 2)
					Gravity.TOP
				else
					Gravity.BOTTOM
			}

			// Default arrow settings
			var arrowSide = TooltipDrawable.ArrowSide.TOP
			var arrowPosition = 0.5f

			// Decide popup position based on gravity
			var popupX = anchorCenterX - popupWidth / 2
			var popupY = anchorCenterY - popupHeight / 2

			when (gravity) {
				Gravity.TOP -> {
					popupY = anchorY - popupHeight - arrowHeightPx.toInt()
					arrowSide = TooltipDrawable.ArrowSide.BOTTOM
				}

				Gravity.BOTTOM -> {
					popupY = anchorY + anchorView.height + arrowHeightPx.toInt()
					arrowSide = TooltipDrawable.ArrowSide.TOP
				}

				Gravity.START, Gravity.LEFT -> {
					popupX = anchorX - popupWidth - arrowHeightPx.toInt()
					arrowSide = TooltipDrawable.ArrowSide.RIGHT
				}

				Gravity.END, Gravity.RIGHT -> {
					popupX = anchorX + anchorView.width + arrowHeightPx.toInt()
					arrowSide = TooltipDrawable.ArrowSide.LEFT
				}
			}

			// Keep popup on screen horizontally
			if (popupX < 0) popupX = 0
			if (popupX + popupWidth > screenWidth) popupX = screenWidth - popupWidth

			// Keep popup on screen vertically
			if (popupY < 0) popupY = 0
			if (popupY + popupHeight > screenHeight) popupY = screenHeight - popupHeight

			// Compute arrow position based on anchor relative to popup
			arrowPosition = when (arrowSide) {
				TooltipDrawable.ArrowSide.TOP, TooltipDrawable.ArrowSide.BOTTOM -> {
					((anchorCenterX - popupX - arrowWidthPx / 2) / popupWidth)
						.coerceIn(0.1f, 0.9f) // keep arrow inside bounds
				}

				TooltipDrawable.ArrowSide.LEFT, TooltipDrawable.ArrowSide.RIGHT -> {
					((anchorCenterY - popupY - arrowWidthPx / 2) / popupHeight)
						.coerceIn(0.1f, 0.9f)
				}
			}

			// Set background drawable with computed arrow
			val tooltipDrawable = TooltipDrawable(
				backgroundColor = backgroundColor,
				cornerRadius = cornerRadiusPx,
				arrowWidth = arrowWidthPx,
				arrowHeight = arrowHeightPx,
				arrowSide = arrowSide,
				arrowPosition = arrowPosition,
				borderColor = borderColor,
				borderWidth = borderWidthPx
			)
			container.background = tooltipDrawable

			// Set click handler
			tooltipTextView.setOnClickListener {
				onClick?.invoke()
				popupWindow.dismiss()
			}

			// Show popup
			popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, popupX, popupY)

			return ToolTipPopup(popupWindow)
		}
	}
}
