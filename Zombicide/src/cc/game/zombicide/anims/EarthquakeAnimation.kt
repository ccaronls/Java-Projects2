package cc.game.zombicide.anims

import cc.game.zombicide.ZActor
import cc.game.zombicide.ZActorAnimation
import cc.lib.game.AGraphics
import cc.lib.utils.randomSigned

open class EarthquakeAnimation : ZActorAnimation {
    val target: ZActor

	constructor(actor: ZActor, dur: Long = 2000) : super(actor, dur) {
		target = actor
	}

	constructor(target: ZActor, owner: ZActor, dur: Long) : super(owner, dur) {
		this.target = target
	}

    override fun draw(g: AGraphics, position: Float, dt: Float) {
        g.pushMatrix()
        g.translate(((1f - position) / 8).randomSigned(), 0f)
        g.drawImage(target.imageId, target.getRect())
        g.popMatrix()
    }

    override fun hidesActor(): Boolean {
        return false
    }
}