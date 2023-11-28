package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.IInterpolator
import cc.lib.math.Bezier
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZDir

/**
 * Created by Chris Caron on 8/31/21.
 */
class DeflectionAnimation(actor: ZActor, val imageId: Int, dir: ZDir) : ZActorAnimation(actor, 500) {
	val arc: IInterpolator<Vector2D>
	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val img = g.getImage(imageId)
		val pos = arc.getAtPosition(position)
		val rect = actor.getRect().scaledBy(.5f).fit(img)
		g.drawImage(imageId, rect.fit(img).setCenter(pos))
	}

	override fun hidesActor(): Boolean {
		return false
    }

    init {
        val start: Vector2D = actor.getRect().center
        val end: Vector2D = actor.getRect().centerBottom.add(.5f * dir.dx, 0f)
        arc = Bezier.build(start, end, .5f)
    }
}