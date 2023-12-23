package cc.lib.game

/**
 * Created by Chris Caron on 8/18/21.
 */
abstract class AMultiPhaseAnimation<T> : AAnimation<T> {
	private var durations: LongArray
	private var lastPhase = -1

	protected constructor(durations: LongArray) : super(Utils.sum(durations)) {
		this.durations = durations
	}

	protected constructor(duration: Long) : super(duration) {
		durations = Utils.toLongArray(duration)
	}

	constructor(durationMSecs: Long, repeats: Int) : super(durationMSecs, repeats) {
		durations = Utils.toLongArray(durationMSecs)
	}

	constructor(durationMSecs: Long, repeats: Int, oscillateOnRepeat: Boolean) : super(durationMSecs, repeats, oscillateOnRepeat) {
		durations = Utils.toLongArray(durationMSecs)
	}

	/**
	 * Draw a phase of a complete animation. Phase will be a value between [0-durations.length)
	 *
	 * @param g
	 * @param positionInPhase value between 0-1
	 * @param positionInAnimation value between 0-1
	 * @param phase
	 */
	protected abstract fun drawPhase(g: T, positionInPhase: Float, positionInAnimation: Float, phase: Int)
	override fun draw(g: T, position: Float, dt: Float) {
		var dur: Long = 0
		for (i in durations.indices) {
			val d = durations[i] + dur
			val elapsedTime = elapsedTime
			if (elapsedTime < d) {
				val pos = (elapsedTime - dur) / durations[i].toFloat()
				if (i != lastPhase) {
					lastPhase = i
					onPhaseStarted(g, i)
				}
				drawPhase(g, pos, position, i)
				break
			}
			dur += durations[i]
		}
	}

	fun setDuration(phase: Int, duration: Long) {
		durations[phase] = duration
		this.duration = durations.sum()
	}

	fun setDurations(durations: List<Long>) {
		this.durations = durations.toLongArray()
		duration = durations.sum()
	}

	protected open fun onPhaseStarted(g: T, phase: Int) {}
}