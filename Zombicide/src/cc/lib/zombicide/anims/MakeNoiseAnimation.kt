package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IVector2D
import cc.lib.zombicide.ZAnimation

open class MakeNoiseAnimation(center: IVector2D) : ZAnimation(1000) {

    val rect: GRectangle = GRectangle(0f, 0f, 1f, 1f).withCenter(center)

    override fun draw(g: AGraphics, position: Float, dt: Float) {
        val RADIUS = rect.radius
        val numCircles = 3
        val r = RADIUS * position
        val steps = (numCircles + 1).toFloat()
        val r2 = (steps * position).toInt().toFloat() / steps
        g.color = GColor.BLACK
        g.drawCircle(rect.center, r, 3f)
        if (r2 > 0) {
            val radius = r2 * RADIUS
            val delta = (r - radius) * steps / RADIUS
            val alpha = 1 - delta
            //log.debug("alpha = %d", Math.round(alpha*100));
            g.color = GColor.BLACK.withAlpha(alpha)
            g.drawCircle(rect.center, radius, 0f)
        }
    }

}