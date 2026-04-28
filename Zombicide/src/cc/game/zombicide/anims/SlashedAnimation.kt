package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.game.zombicide.ZIcon
import cc.lib.game.AGraphics

class SlashedAnimation(actor: ZActor<*>) : ZActorAnimation(actor, 1000) {
	val claws = ZIcon.CLAWS.imageIds.random()
	var r = actor.getRect()
	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val img = g.getImage(claws)
		g.setTransparencyFilter(1f - position)
		g.drawImage(claws, r.fit(img))
		g.removeFilter()
	}

	override fun hidesActor(): Boolean {
		return false
	}
}