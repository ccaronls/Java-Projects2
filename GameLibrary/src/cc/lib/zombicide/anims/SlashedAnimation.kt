package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.Utils
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZIcon

class SlashedAnimation(actor: ZActor<*>) : ZActorAnimation(actor, 1000) {
	val claws = Utils.randItem(ZIcon.CLAWS.imageIds)
	var r: GRectangle = actor.rect
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