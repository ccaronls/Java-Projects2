package cc.lib.zombicide.anims

import cc.lib.game.*
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import kotlin.math.roundToLong

/**
 * Created by Chris Caron on 8/18/21.
 */
open class MagicOrbAnimation(actor: ZActor<*>, end: Vector2D) : ZActorAnimation(actor, 600L, 800L) {
    val path: Vector2D
    val start: Vector2D
    val r: GRectangle = actor.getRect().scaledBy(.5f)
    val strands: Array<LightningStrand>
    val startAlpha = .9f
    val endAlpha = .3f
    val startRadius = .05f
    val endRadius = .25f
    val padding = .02f // padding between the lightning strands and the outer edge of orb
    override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {
        val orbColor = GColor.MAGENTA
	    val alpha = startAlpha + (endAlpha - startAlpha) * positionInAnimation
	    val radius = startRadius + (endRadius - startRadius) * positionInAnimation
	    g.color = orbColor.withAlpha(1f - alpha)
        when (phase) {
            0 -> {
                // orb expands over our actors head
                g.drawFilledCircle(start, radius)
            }
            1 -> {

                // draw purple orb that fades but also grows at it travels
                val center: Vector2D = start.add(path.scaledBy(position))
                g.pushMatrix()
                g.translate(center)
                g.drawFilledCircle(Vector2D.ZERO, radius)

                // draw strands of electrocution
                for (l in strands) {
                    l.draw(g, position)
                }
                g.popMatrix()
            }
        }
    }

    override fun hidesActor(): Boolean {
        return false
    }

    init {
        start = r.centerTop
        path = end.sub(start)
        strands = Array(Utils.randRange(7, 9)) {
            val i0 = Vector2D.getPolarInterpolator(Vector2D.ZERO, startRadius + padding, endRadius + padding, Utils.randFloat(360f), Utils.randFloat(360f))
            val i1 = Vector2D.getPolarInterpolator(Vector2D.ZERO, startRadius + padding, endRadius + padding, Utils.randFloat(360f), Utils.randFloat(360f))
            LightningStrand(i0, i1, 4, 7, .4f)
        }
        setDuration(1, (path.mag() * 900).roundToLong())
    }
}