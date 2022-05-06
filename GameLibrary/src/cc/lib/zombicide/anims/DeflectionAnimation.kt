package cc.lib.zombicide.anims

import cc.lib.zombicide.ZActor
import cc.lib.game.GRectangle
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZActorAnimation
import cc.lib.game.IInterpolator
import cc.lib.math.Vector2D
import cc.lib.game.AGraphics
import cc.lib.math.Bezier

/**
 * Created by Chris Caron on 8/31/21.
 */
class DeflectionAnimation(actor: ZActor<*>, val imageId: Int, val r: GRectangle, dir: ZDir) : ZActorAnimation(actor, 500) {
    val arc: IInterpolator<Vector2D>
    override fun draw(g: AGraphics, position: Float, dt: Float) {
        val img = g.getImage(imageId)
        val pos = arc.getAtPosition(position)
        g.drawImage(imageId, r.fit(img).setCenter(pos))
    }

    override fun hidesActor(): Boolean {
        return false
    }

    init {
        val start: Vector2D = actor.rect.center
        val end: Vector2D = actor.rect.centerBottom.add(.5f * dir.dx, 0f)
        arc = Bezier.build(start, end, .5f)
    }
}