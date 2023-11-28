package cc.lib.zombicide.anims

import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZAnimation

/**
 * Allow for running multiple animations with some delay in between each starting position
 *
 */
open class GroupAnimation(actor: ZActor, val hidesActor: Boolean = false) : ZActorAnimation(actor, 1) {
	private val group: MutableList<Pair<ZAnimation, Int>> = ArrayList()

	@Synchronized
	fun addAnimation(animation: ZAnimation): GroupAnimation {
		return addAnimation(0, animation)
	}

	/**
	 * IMPORTANT!: Make sure to have all animations added before starting!
	 * @param animation
     */
    @Synchronized
    fun addAnimation(delay: Int, animation: ZAnimation): GroupAnimation {
        require(!isStarted)
        group.add(Pair(animation, delay))
        return this
    }

	fun addSequentially(animation: ZAnimation) : GroupAnimation {
		val delay = group.sumBy { it.first.duration.toInt() }
		addAnimation(delay, animation)
		return this
	}

    override fun isDone(): Boolean {
        for (a in group) {
            if (!a.first.isDone) return false
        }
        return super.isDone()
    }

    override fun draw(g: AGraphics, position: Float, dt: Float) {
        require(false) // should not get called
    }

    @Synchronized
    override fun update(g: AGraphics): Boolean {
        val it = group.iterator()
        while (it.hasNext()) {
            val p = it.next()
            if (p.first.isDone) it.remove() else if (!p.first.isStarted) {
                //a.start();
                if (currentTimeMSecs - startTime >= p.second) {
                    p.first.start<AAnimation<AGraphics>>()
                }
            } else {
                p.first.update(g)
            }
        }
        if (group.isEmpty()) {
            kill()
            onDone()
            return true
        }
        return false
    }

    override fun hidesActor(): Boolean {
        return hidesActor
    }
}