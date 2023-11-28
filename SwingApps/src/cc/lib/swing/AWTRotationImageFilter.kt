package cc.lib.swing

import java.awt.image.RGBImageFilter

internal class AWTRotationImageFilter(val source: IntArray, val degrees: Int, val srcWid: Int, val srcHgt: Int, val dstWid: Int, val dstHgt: Int) : RGBImageFilter() {
	override fun filterRGB(x: Int, y: Int, rgb: Int): Int {
		var sx = 0
		var sy = 0
		when (degrees) {
			0 -> {}
			90 -> {
				sx = y
				sy = x
			}
			180 -> {
				sx = srcWid - x
				sy = srcHgt - y
			}
			270 -> {
				sx = srcHgt - y
				sy = srcWid - x
			}
		}
		return source[sx + sy * srcWid]
	}
}