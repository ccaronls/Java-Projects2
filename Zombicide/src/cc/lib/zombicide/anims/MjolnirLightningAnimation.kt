package cc.lib.zombicide.anims

import cc.lib.game.*
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZIcon
import kotlin.math.min

/**
 * Point mjolnir in dir of targets, charge up and fire
 */
open class MjolnirLightningAnimation(actor: ZActor<*>, val targets: List<IInterpolator<Vector2D>>, val dir: ZDir) : ZActorAnimation(actor, 1500L, 1000L) {


	lateinit var start : Vector2D
	lateinit var imgRect: GRectangle
	val actorRect = actor.getRect()
	lateinit var shots: Array<LightningStrand>

	override fun onStarted(g: AGraphics) {
		when (dir) {
			ZDir.NORTH -> {
				imgRect = GRectangle(actorRect).scaledBy(.5f).withCenter(actorRect.centerTop)
				start = imgRect.center.midPoint(imgRect.centerTop)
			}
			ZDir.SOUTH -> {
				imgRect = GRectangle(actorRect).scaledBy(.5f).withCenter(actorRect.centerBottom)
				start = imgRect.center.midPoint(imgRect.centerBottom)
			}
			ZDir.EAST -> {
				imgRect = GRectangle(actorRect).scaledBy(.5f).withCenter(actorRect.centerRight)
				start = imgRect.center.midPoint(imgRect.centerRight)
			}
			ZDir.WEST -> {
				imgRect = GRectangle(actorRect).scaledBy(.5f).withCenter(actorRect.centerLeft)
				start = imgRect.center.midPoint(imgRect.centerLeft)
			}
		}
		shots = Array(targets.size) {
			LightningStrand(start, targets[it], 10, 15, .4f)
		}
	}

    override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {
		g.drawImage(ZIcon.MJOLNIR.imageIds[dir.ordinal], imgRect)

	    val radius = min(imgRect.w, imgRect.h)

        when (phase) {
            0 -> {
	            g.color = GColor.WHITE.withAlpha(positionInPhase*.5f)
				g.drawFilledCircle(start, radius * positionInPhase)
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

}