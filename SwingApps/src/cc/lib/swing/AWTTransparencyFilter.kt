package cc.lib.swing

import java.awt.Color
import java.awt.image.RGBImageFilter

class AWTTransparencyFilter(color: Int) : RGBImageFilter() {
	var targetColor: Int

	constructor(color: Color) : this(AWTUtils.colorToInt(color)) {}

	init {
		targetColor = color and 0x00ffffff
	}

	override fun filterRGB(x: Int, y: Int, rgb: Int): Int {
		val c = rgb and 0x00ffffff
		return if (c == targetColor) {
			0
		} else rgb
	}
}