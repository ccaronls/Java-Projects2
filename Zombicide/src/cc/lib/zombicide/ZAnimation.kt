package cc.lib.zombicide

import cc.lib.game.AGraphics
import cc.lib.game.AMultiPhaseAnimation

import cc.lib.utils.GException

abstract class ZAnimation : AMultiPhaseAnimation<AGraphics> {
    constructor(vararg durations: Long) : super(durations) {}
    constructor(durationMSecs: Long) : super(durationMSecs) {}
    constructor(durationMSecs: Long, repeats: Int) : super(durationMSecs, repeats) {}
    constructor(durationMSecs: Long, repeats: Int, oscilateOnRepeat: Boolean) : super(durationMSecs, repeats, oscilateOnRepeat) {}

    protected override fun drawPhase(g: AGraphics, position: Float, phase: Int) {
        throw GException("Unhandled")
    }
}