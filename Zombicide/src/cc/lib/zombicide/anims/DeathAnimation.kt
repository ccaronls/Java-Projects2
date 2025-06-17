package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation

open class DeathAnimation(a: ZActor) : ZActorAnimation(a, 2000) {
	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val rect = GRectangle(actor.getRect())
		rect.top += rect.height * position
		rect.height *= 1f - position
		val dx = rect.width * position
		rect.width += dx
		rect.left -= dx / 2
		g.drawImage(actor.imageId, rect)
	}
}