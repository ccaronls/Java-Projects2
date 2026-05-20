package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.game.zombicide.ZIcon
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

class SlashedAnimation(actor: ZActor<*>) : ZActorAnimation(actor, 1000) {

	private val id = ZIcon.CLAWS.imageIds.random()
	private lateinit var r: GRectangle

	override fun onStarted(g: AGraphics, reversed: Boolean) {
		val img = g.getImage(id)
		r = actor.getRect().fit(img)
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		g.setTransparencyFilter(1f - position)
		g.drawImage(id, r)
		g.removeFilter()
	}

	override fun hidesActor(): Boolean {
		return false
	}
}