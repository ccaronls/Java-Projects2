package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation

/**
 * Created by Chris Caron on 8/30/21.
 */
open class EmptyAnimation(actor: ZActor, duration: Long) : ZActorAnimation(actor, duration) {

	constructor(actor: ZActor) : this(actor, 1)

	override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {
		// do nothing
	}

	override fun hidesActor(): Boolean {
		return false
	}
}