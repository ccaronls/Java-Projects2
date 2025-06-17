package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZBoard

class SpawnAnimation(actor: ZActor, board: ZBoard) : ZActorAnimation(actor, 1000) {
	val r = actor.getRect(board)
	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val dest = GRectangle(r)
		dest.top += dest.height * (1f - position)
		dest.height *= position
		g.drawImage(actor.imageId, dest)
	}
}