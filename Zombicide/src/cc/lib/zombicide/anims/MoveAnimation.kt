package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation

class MoveAnimation(actor: ZActor, val start: GRectangle, val end: GRectangle, speed: Long) : ZActorAnimation(actor, speed) {

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