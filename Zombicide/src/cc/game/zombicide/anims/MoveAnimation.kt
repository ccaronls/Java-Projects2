package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.math.Vector2D

class MoveAnimation(actor: ZActor<*>, val start: GRectangle, val end: GRectangle, speed: Long) : ZActorAnimation(actor, speed) {

	init {
		rect = GRectangle(actor.enclosingRect())
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val dv0 = end.topLeft.sub(start.topLeft)
		val dv1 = end.bottomRight.sub(start.bottomRight)
		val topLeft: Vector2D = start.topLeft.add(dv0.scaledBy(position))
		val bottomRight: Vector2D = start.bottomRight.add(dv1.scaledBy(position))
		rect = GRectangle(topLeft, bottomRight)
		actor.draw(g)
	}
}