package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

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