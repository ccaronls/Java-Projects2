package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.game.zombicide.ZIcon
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

/**
 * Created by Chris Caron on 5/15/26.
 */
class GravestoneAnimation(actor: ZActor<*>) : ZActorAnimation(actor, 2000) {
	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val img = g.getImage(ZIcon.GRAVESTONE.imageIds[0])
		val rect = GRectangle(actor.getRect().fit(img))
		rect.top += rect.height * (1f - position)
		rect.height *= position
		g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect)
	}
}