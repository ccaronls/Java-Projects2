package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZIcon

class SlashedAnimation(actor: ZActor<*>) : ZActorAnimation(actor, 1000) {
	val claws = ZIcon.CLAWS.imageIds.random()
	var r: GRectangle = actor.getRect()
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