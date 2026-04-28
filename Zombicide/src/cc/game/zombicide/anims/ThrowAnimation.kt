package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.game.zombicide.ZDir
import cc.game.zombicide.ZIcon
import cc.lib.game.AGraphics
import cc.lib.game.IInterpolator
import cc.lib.game.IVector2D
import cc.lib.math.Bezier
import cc.lib.math.Vector2D
import kotlin.math.roundToInt

// TODO: Consider merging Fireball, Throw, Shoot animation types which all have similar features and special characteristics like: STATIC, SPIN, DIRECTIONAL, RANDOM
open class ThrowAnimation(
	actor: ZActor<*>,
	target: IVector2D,
	val icon: ZIcon,
	arc: Float = .5f,
	duration: Long = 1000,
	val scale: Float = .5f
) : ZActorAnimation(actor, duration) {

	val curve: IInterpolator<Vector2D>
	val dir: ZDir

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val idx = (position * (icon.imageIds.size - 1)).roundToInt()
		val id = icon.imageIds[idx]
		val img = g.getImage(id)
		val rect = actor.getRect().scaledBy(scale).fit(img)
		rect.setCenter(curve.getAtPosition(position))
		g.drawImage(id, rect)
	}

	override fun hidesActor(): Boolean {
		return false
	}

	init {
		val start: Vector2D = actor.getRect().center.toImmutable()
		val end = Vector2D(target)
		dir = ZDir.getFromVector(end.sub(start))
		curve = Bezier.build(start, end, arc)
	}
}