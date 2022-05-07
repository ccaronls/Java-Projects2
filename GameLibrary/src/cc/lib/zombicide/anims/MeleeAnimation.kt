package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZBoard
import cc.lib.zombicide.ZIcon

open class MeleeAnimation(actor: ZActor<*>, board: ZBoard) : ZActorAnimation(actor, 400) {
    val id: Int = ZIcon.SLASH.imageIds.random()

	private val r: GRectangle = actor.rect.scaledBy(1.3f).moveBy(Vector2D.newRandom(.1f))

	override fun draw(g: AGraphics, position: Float, dt: Float) {
        val img = g.getImage(id)
        g.setTransparencyFilter(1f - position)
        g.drawImage(id, r.fit(img))
        g.removeFilter()
    }

    override fun hidesActor(): Boolean {
        return false
    }

}