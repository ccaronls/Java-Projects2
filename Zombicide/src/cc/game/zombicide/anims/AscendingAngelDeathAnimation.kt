package cc.game.zombicide.anims

import cc.game.zombicide.ZCharacter
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle

class AscendingAngelDeathAnimation(a: ZCharacter) : DeathAnimation(a) {
    override fun draw(g: AGraphics, position: Float, dt: Float) {
        super.draw(g, position, dt)
        val rect = GRectangle(actor.getRect())
        rect.top -= rect.height * 3 * position
        g.setTransparencyFilter(.5f - position / 3)
        g.drawImage(actor.imageId, rect)
        g.removeFilter()
    }

	override fun hidesActor(): Boolean {
		return true
	}

	override fun onDone() {
		(actor as ZCharacter).setFallen(true)
	}

    init {
        duration = 4000
    }
}