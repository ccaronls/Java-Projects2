package cc.lib.zombicide.anims

import cc.lib.game.*
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZAnimation
import cc.lib.zombicide.ui.UIZBoardRenderer

class HoverMessage(board: UIZBoardRenderer<*>, private val msg: String, private val center: IVector2D) : ZAnimation(3000) {
    private val dv: Vector2D
    private lateinit var hJust: Justify

	override fun onStarted(g: AGraphics) {
		val tv: Vector2D = g.transform(center)
		val width = g.getTextWidth(msg) / 2
		hJust = if (tv.X() + width > g.viewportWidth) {
			Justify.RIGHT
		} else if (tv.X() - width < 0) {
			Justify.LEFT
		} else {
			Justify.CENTER
		}
	}

    override fun draw(g: AGraphics, position: Float, dt: Float) {
        val v: Vector2D = Vector2D(center).add(dv.scaledBy(position))
        g.color = GColor.YELLOW.withAlpha(1f - position)
        g.drawJustifiedString(v, hJust, Justify.CENTER, msg)
    }

    init {
        dv = board.zoomedRect.center.subEq(center)
                .rotateEq(Utils.randFloatX(30f))
                .normalizedEq().scaleEq(.5f)
    }
}