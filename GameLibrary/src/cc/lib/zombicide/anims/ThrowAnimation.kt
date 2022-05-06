package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IInterpolator
import cc.lib.game.IVector2D
import cc.lib.math.Bezier
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZDir.Companion.getFromVector
import cc.lib.zombicide.ZIcon
import kotlin.math.roundToInt

// TODO: Consider merging Fireball, Throw, Shoot animation types which all have similar features and special characteristics like: STATIC, SPIN, DIRECTIONAL, RANDOM
open class ThrowAnimation(actor: ZActor<*>, target: IVector2D, val icon: ZIcon, arc:Float, duration:Long) : ZActorAnimation(actor, duration) {

	constructor(actor: ZActor<*>, target: IVector2D, icon: ZIcon) : this(actor, target, icon, .5f, 1000)

	val curve: IInterpolator<Vector2D>
	val dir: ZDir

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val idx = (position * (icon.imageIds.size - 1)).roundToInt()
		val id = icon.imageIds[idx]
		val img = g.getImage(id)
		val rect = actor.rect.scaledBy(.5f).fit(img)
		rect.setCenter(curve.getAtPosition(position))
		g.drawImage(id, rect)
	}

	override fun hidesActor(): Boolean {
		return false
	}

	init {
		val start: Vector2D = actor.rect.center
		val end = Vector2D(target)
		dir = getFromVector(end.sub(start))
		curve = Bezier.build(start, end, arc)
	}
}