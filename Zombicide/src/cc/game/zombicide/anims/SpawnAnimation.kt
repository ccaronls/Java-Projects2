package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.game.zombicide.ZBoard
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

class SpawnAnimation(actor: ZActor, board: ZBoard) : ZActorAnimation(actor, 1000) {
	val r = actor.getRect(board)
	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val dest = GRectangle(r)
		dest.top += dest.height * (1f - position)
		dest.height *= position
		g.drawImage(actor.imageId, dest)
	}
}