package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IVector2D
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZIcon

/**
 * Created by Chris Caron on 10/14/24.
 */
open class ThrustAnimation(actor: ZActor, center: IVector2D, icon: ZIcon) : ZActorAnimation(actor, 200L, 600L) {
	var dir: ZDir
	val id: Int
	val dv: IVector2D
	val start: IVector2D
	val end: IVector2D
	lateinit var r: GRectangle

	init {
		start = Vector2D(actor.center)
		end = Vector2D(center)
		dv = end.sub(start)
		dir = ZDir.getFromVector(dv)
		id = icon.imageIds[dir.ordinal]
	}

	override fun onStarted(g: AGraphics, reversed: Boolean) {
		val img = g.getImage(id)
		r = actor.getRect().scaledBy(.5f).fit(img)
	}

	override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {
		when (phase) {
			0 -> g.drawImage(id, r.movedBy(dv.scaledBy(positionInPhase)))
			1 -> g.drawImage(id, r.movedBy(dv.scaledBy(1f - positionInPhase)))
		}
	}

	override fun hidesActor(): Boolean {
		return false
	}

}