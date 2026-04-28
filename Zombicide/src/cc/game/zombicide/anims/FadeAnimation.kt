package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.lib.game.AGraphics
import cc.lib.game.IRectangle

/**
 * Created by Chris Caron on 10/14/24.
 */
open class FadeAnimation(actor: ZActor<*>, duration: Long, val iconId: Int) : ZActorAnimation(actor, duration) {

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