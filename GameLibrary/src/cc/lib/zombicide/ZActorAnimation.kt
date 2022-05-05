package cc.lib.zombicide

import cc.lib.annotation.CallSuper
import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.Utils

abstract class ZActorAnimation : ZAnimation {
    @JvmField
    val actor: ZActor<*>
    private var next: ZActorAnimation? = null

    constructor(actor: ZActor<*>, vararg durations: Long) : super(*durations) {
        this.actor = actor
    }

    constructor(actor: ZActor<*>, durationMSecs: Long, repeats: Int) : super(durationMSecs, repeats) {
        this.actor = actor
    }

    @CallSuper
    override fun onDone() {
        next?.let {
            actor.animation = it
            it.start<AAnimation<AGraphics>>()
        }
    }

    override fun isDone(): Boolean {
        return next?.isDone?:super.isDone()
    }

    fun add(anim: ZActorAnimation) {
        next?.add(anim) ?:run {
            next = anim
        }
    }

    open val rect: GRectangle?
        get() = null

    open fun hidesActor(): Boolean {
        return true
    }
}