package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IInterpolator
import cc.lib.game.IVector2D
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZDir.Companion.getFromVector
import cc.lib.zombicide.ZIcon

open class ShootAnimation(actor: ZActor, duration: Long, center: IVector2D?, val icon: ZIcon) : ZActorAnimation(actor, duration) {
	val dir: ZDir
	val id: Int
	lateinit var r: GRectangle

	val path: IInterpolator<Vector2D>
	var pos: IVector2D

	override fun onStarted(g: AGraphics, reversed: Boolean) {
		val img = g.getImage(id)
		r = actor.getRect().scaledBy(.5f).fit(img)
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		pos = path.getAtPosition(position)
		g.drawImage(id, r.setCenter(pos))
	}

	override fun hidesActor(): Boolean {
		return false
	}

	init {
		val start = Vector2D(actor.center)
		val end = Vector2D(center)
		val dv: Vector2D = end.sub(start)
		dir = getFromVector(dv)
		path = Vector2D.getLinearInterpolator(start, end)
		pos = path.getAtPosition(0f)
		this.duration = (dv.mag() * duration).toLong()
		id = icon.imageIds[dir.ordinal]
	}
}