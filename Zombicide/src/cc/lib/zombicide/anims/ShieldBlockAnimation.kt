package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZIcon

class ShieldBlockAnimation(actor: ZActor) : ZActorAnimation(actor, 1000) {
	private lateinit var r: GRectangle

	override fun onStarted(g: AGraphics) {
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