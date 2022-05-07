package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IVector2D

import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZIcon

open class FireballAnimation(actor: ZActor<*>, end: IVector2D?) : ZActorAnimation(actor, 500) {
    val path: Vector2D
    val start: Vector2D
    val r: GRectangle = actor.rect.scaledBy(.5f)
	override fun draw(g: AGraphics, position: Float, dt: Float) {
        val id = ZIcon.FIREBALL.imageIds.random()
        val img = g.getImage(id)
        //GRectangle rect = attacker.getRect(board).scaledBy(.5f).fit(img);
        val pos: Vector2D = start.add(path.scaledBy(position))
        val r = r.fit(img).setCenter(pos)
        g.drawImage(id, r)
    }

    override fun hidesActor(): Boolean {
        return false
    }

    init {
	    start = r.center
        path = Vector2D(end).sub(start)
        duration = Math.round(path.mag() * 700).toLong()
    }
}