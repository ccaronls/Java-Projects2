package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.utils.randomSigned

import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation

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