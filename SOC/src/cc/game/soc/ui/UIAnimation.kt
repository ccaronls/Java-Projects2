package cc.game.soc.ui

import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.utils.Lock

/**
 * Animation that blocks SOC thread until finished
 * @author chriscaron
 */
abstract class UIAnimation internal constructor(duration: Long, repeats:Int=0, oscillateOnRepeat:Boolean=false) : AAnimation<AGraphics>(duration, repeats, oscillateOnRepeat) {

	private val lock = Lock()

	public override fun onDone() {
		lock.releaseAll()
	}

	public fun block() {
		lock.acquireAndBlock()
	}

	public fun block(delay: Long) {
		lock.acquireAndBlock(delay)
	}
}

abstract class UIAnimation2 internal constructor(duration: Long) : AAnimation<APGraphics>(duration, 0) {

	private val lock = Lock()

	public override fun onDone() {
		lock.releaseAll()
	}

	public fun block() {
		lock.acquireAndBlock()
	}

	public fun block(delay: Long) {
		lock.acquireAndBlock(delay)
	}
}