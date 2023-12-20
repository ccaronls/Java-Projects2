package cc.lib.zombicide

import cc.lib.game.GRectangle


abstract class ZActorAnimation : ZAnimation {
    val actor: ZActor

	constructor(actor: ZActor, vararg durations: Long) : super(*durations) {
		this.actor = actor
	}

	constructor(actor: ZActor, durationMSecs: Long, repeats: Int) : super(durationMSecs, repeats) {
		this.actor = actor
	}

    var rect: GRectangle? = null

    open fun hidesActor(): Boolean {
        return true
    }
}