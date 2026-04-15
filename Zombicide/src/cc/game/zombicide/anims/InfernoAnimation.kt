package cc.game.zombicide.anims

import cc.game.zombicide.ZAnimation
import cc.game.zombicide.ZIcon
import cc.lib.game.AGraphics
import cc.lib.game.IRectangle

/**
 * Engulf some number of rectangles in flames
 */
open class InfernoAnimation(private val rects: List<IRectangle>) : ZAnimation(2000) {
    var index = 0f

    constructor(vararg rect: IRectangle) : this(listOf(*rect))

    override fun draw(g: AGraphics, position: Float, dt: Float) {
        for (rect in rects) {
            val idx: Int = index.toInt() % ZIcon.FIRE.imageIds.size
            g.drawImage(ZIcon.FIRE.imageIds[idx], rect)
            index += .2f
        }
    }
}