package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.game.zombicide.ZBoard
import cc.game.zombicide.ZIcon
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.math.Vector2D

open class MeleeAnimation(actor: ZActor, board: ZBoard) : ZActorAnimation(actor, 400) {
	val id: Int = ZIcon.SLASH.imageIds.random()

	private val r: GRectangle = actor.getRect(board).scaledBy(1.3f).moveBy(Vector2D.newRandom(.1f))

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