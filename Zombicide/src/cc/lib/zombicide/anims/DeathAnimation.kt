package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation

open class DeathAnimation(a: ZActor<*>) : ZActorAnimation(a, 2000) {
    override fun draw(g: AGraphics, position: Float, dt: Float) {
        val rect = GRectangle(actor.getRect())
        rect.y += rect.h * position
        rect.h *= 1f - position
        val dx = rect.w * position
        rect.w += dx
        rect.x -= dx / 2
        g.drawImage(actor.imageId, rect)
    }
}