package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.game.zombicide.ZIcon
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

class ShieldBlockAnimation(actor: ZActor<*>) : ZActorAnimation(actor, 1000) {
	private lateinit var r: GRectangle

	override fun onStarted(g: AGraphics, reversed: Boolean) {
		val id = ZIcon.SHIELD.imageIds[0]
		val img = g.getImage(id)
		r = actor.getRect().fit(img).scaledBy(.5f)
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val id = ZIcon.SHIELD.imageIds[0]
		g.setTransparencyFilter(1f - position)
        g.drawImage(id, r)
        g.removeFilter()
    }

    override fun hidesActor(): Boolean {
        return false
    }
}