package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.IRectangle
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation

/**
 * Created by Chris Caron on 10/14/24.
 */
open class FadeAnimation(actor: ZActor, duration: Long, val iconId: Int) : ZActorAnimation(actor, duration) {

	lateinit var iconRect: IRectangle

	override fun onStarted(g: AGraphics, reversed: Boolean) {
		iconRect = actor.getRect().fit(g.getImage(iconId))
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		g.setTransparencyFilter(position)
		g.drawImage(iconId, iconRect)
		g.removeFilter()
	}

	override fun hidesActor(): Boolean = false
}