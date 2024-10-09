package cc.lib.game

abstract class AImage : IDimension {
	abstract val pixels: IntArray
	abstract fun draw(g: AGraphics, x: Float, y: Float)
}
