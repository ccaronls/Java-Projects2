package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.zombicide.ZAnimation

open class OverlayTextAnimation(val text: String, type: Int) : ZAnimation(3000) {
    private val dyType: Int = type % 3
    override fun draw(g: AGraphics, position: Float, dt: Float) {
        val cx = (g.viewportWidth / 2).toFloat()
        val cy0 = (g.viewportHeight / 2).toFloat()
        val cy1 = when (dyType) {
            1 -> (g.viewportHeight / 2).toFloat()
            2 -> (g.viewportHeight * 2 / 3).toFloat()
            else -> (g.viewportHeight / 3).toFloat()
        }
        val minHeight = 32f
        val maxHeight = 48f
        var color = GColor.RED
        if (position > .5f) {
            val alpha = 1f - 2f * (position - .5f)
            color = color.withAlpha(alpha)
        }
        g.color = color
        val curHeight = g.textHeight
        g.textHeight = minHeight + (maxHeight - minHeight) * position
        val cy = cy0 + (cy1 - cy0) * position
        g.drawJustifiedString(cx, cy, Justify.CENTER, Justify.BOTTOM, text)
        g.textHeight = curHeight
    }

}