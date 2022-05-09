package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZBoard

/**
 * Animate concentric arcs that have a start and end point
 */
class SonarAnimation(actor: ZActor<*>, var start: Vector2D, var end: Vector2D, val numArcs: Int, val sweepAngle: Float) : ZActorAnimation(actor, 2000) {
	var dv: Vector2D
	val startAngle: Float
	val radius: Float

	constructor(actor: ZActor<*>, board: ZBoard, targetZone: Int) : this(actor, actor.getRect().center, board.getZone(targetZone).center, 5, 20f) {}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		g.color = GColor.WHITE
		g.setLineWidth(3f)
		val radiusStep = radius / numArcs
		if (position <= .5f) {
			// draw the arcs emanating from the start
			val numArcsToDraw = Math.round(position * 2 * numArcs)
			//g.drawFilledCircle(start, radius/10);
			g.drawArc(start, radius / 10, startAngle, sweepAngle)
			var r = radiusStep
			for (i in 0 until numArcsToDraw) {
				g.drawArc(start, r, startAngle, sweepAngle)
				r += radiusStep
			}
			g.drawArc(start, position * 2 * radius, startAngle, sweepAngle)
		} else {
			// draw the arcs backward from end
			val numArcsToDraw = Math.round(2 * (1f - position) * numArcs)
			var r = numArcs * radiusStep
			for (i in 0 until numArcsToDraw) {
				g.drawArc(start, r, startAngle, sweepAngle)
				r -= radiusStep
			}
			g.drawArc(start, (position - .5f) * 2 * radius, startAngle, sweepAngle)
		}
	}

	override fun hidesActor(): Boolean {
		return false
	}

	init {
		radius = end.sub(start).mag()
		startAngle = end.sub(start).angleOf() - sweepAngle / 2
		dv = end.sub(start).scaledBy(1f / numArcs)
	}
}