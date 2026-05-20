package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

abstract class ChargeAttackAnimation(source: ZActor<*>, dest: ZActor<*>) : ZActorAnimation(source, 150L, 600L, 0L) {

	private val dv = dest.getRect().center.sub(source.getRect().center).scaledBy(.75f)

	final override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {
		when (phase) {
			0 -> {
				val rect = GRectangle(actor.getRect()).moveBy(dv.scaledBy(positionInPhase))
				g.drawImage(actor.imageId, rect)
			}

			1 -> {
				val rect = GRectangle(actor.getRect()).moveBy(dv.scaledBy(1f - positionInPhase))
				g.drawImage(actor.imageId, rect)
			}
		}
	}

	final override fun onPhaseStarted(g: AGraphics, phase: Int) {
		if (phase == 1)
			onCharged()
	}

	abstract fun onCharged()
}