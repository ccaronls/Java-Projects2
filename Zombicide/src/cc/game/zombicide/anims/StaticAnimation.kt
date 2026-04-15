package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

/**
 * Created by Chris Caron on 9/1/21.
 */
class StaticAnimation @JvmOverloads constructor(actor: ZActor, duration: Long, val imageId: Int, val r: GRectangle, val fadeOut: Boolean = false) : ZActorAnimation(actor, duration) {
	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val img = g.getImage(imageId)
		if (fadeOut) {
			g.setTransparencyFilter(1f - position)
		}
		g.drawImage(imageId, r.fit(img))
		if (fadeOut) g.removeFilter()
	}

	override fun hidesActor(): Boolean {
		return false
	}
}