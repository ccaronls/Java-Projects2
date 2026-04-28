package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.lib.game.AGraphics
import cc.lib.game.IInterpolator
import cc.lib.game.InterpolatorUtils
import cc.lib.game.LightningStrand
import cc.lib.game.Utils
import cc.lib.math.Vector2D

open class LightningAnimation2(actor: ZActor<*>, targets: List<IInterpolator<Vector2D>>) : ZActorAnimation(actor, 700L, 1000L) {
	val start0: Vector2D
	val start1: Vector2D

	// phase1 arcs between magicians hands to arc upward
	val arcs: Array<LightningStrand>
	val shots: Array<LightningStrand>
	val minArc = 0f
	val maxArc = .5f

	override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {
        when (phase) {
            0 -> {

                // draw the arc build up
                for (l in arcs) {
                    l.draw(g, position)
                }
            }
            1 -> {

                // draw the discharge
                for (l in shots) {
                    l.draw(g, position)
                }
            }
        }
    }

    override fun hidesActor(): Boolean {
        return false
    }

    override fun onPhaseStarted(g: AGraphics, phase: Int) {
        if (phase == 1) {
            onShotPhaseStarted(g)
        }
    }

    protected fun onShotPhaseStarted(g: AGraphics) {}

    init {
        start0 = actor.getRect().topLeft
        start1 = actor.getRect().topRight
        arcs = Array(Utils.randRange(3, 6)) {
            LightningStrand(start0, start1, InterpolatorUtils.linear(minArc, maxArc), 4, 7, .5f)
        }
        shots = Array(targets.size) {
            LightningStrand(start0, targets[it], 10, 15, .4f)
        }
    }
}