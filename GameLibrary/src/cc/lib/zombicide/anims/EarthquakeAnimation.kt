package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.Utils
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation

open class EarthquakeAnimation : ZActorAnimation {
    val target: ZActor<*>

    constructor(actor: ZActor<*>) : super(actor, 2000) {
        target = actor
    }

    constructor(target: ZActor<*>, owner: ZActor<*>, dur: Long) : super(owner, dur) {
        this.target = target
    }

    override fun draw(g: AGraphics, position: Float, dt: Float) {
        g.pushMatrix()
        g.translate(Utils.randFloatX((1f - position) / 8), 0f)
        g.drawImage(target.imageId, target.rect)
        g.popMatrix()
    }

    override fun hidesActor(): Boolean {
        return false
    }
}