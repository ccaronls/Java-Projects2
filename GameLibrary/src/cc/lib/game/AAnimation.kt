package cc.lib.game

/**
 * General purpose animation runner.
 *
 * Example usage:
 *
 * new MyAnimation extends AAnimation<Graphics> {
 * protected void draw(Graphics g, float pos, float dt) {
 * ...
 * }
 *
 * protected void onDone() {
 * ...
 * }
 * }.start(1000); - starts the animation after 1 second has elapsed
 *
 * @author chriscaron
</Graphics> */
abstract class AAnimation<T>(durationMSecs: Long, repeats: Int = 0, oscillateOnRepeat: Boolean = false) {
	var isStartDirectionReverse = false
		private set
	var startTime: Long = 0
		private set
	var lastTime: Long
		private set
	var duration: Long = durationMSecs
		protected set(value) {
			require(state == State.PRESTART)
			require(duration > 0)
			field = value
		}
	var maxRepeats: Int
		private set
	var position = 0f
		private set
	private var state = State.PRESTART
	var isReverse = false // 1 for forward, -1 for reversed
		private set
	var isOscillateOnRepeat: Boolean
		private set
	private var curRepeat = 0

	internal enum class State {
		PRESTART,
		STARTED,
		RUNNING,
		STOPPED,
		DONE
	}

	/**
	 * Create a repeating animation with option to oscillate (play in reverse) on every other repeat
	 *
	 * @param durationMSecs duration of one loop, must be > 0
	 * @param repeats repeats=0 means none. repeats<0 means infinite. repeats>0 means fixed number of repeats
	 * @param oscillateOnRepeat when true, a loop will reverse from its current play direction on each repeat
	 */
	/**
	 * Create a repeating animation. if maxRepeats < 0 then will repeat forever until stopped.
	 *
	 * @param durationMSecs duration of one loop
	 * @param repeats repeats=0 means none. repeats<0 means infinite. repeats>0 means fixed number of repeats
	 */
	/**
	 * Create an animation that plays for a fixed time without repeats
	 * @param durationMSecs
	 */
	init {
		Utils.assertTrue(durationMSecs > 0)
		duration = durationMSecs
		maxRepeats = repeats
		isOscillateOnRepeat = oscillateOnRepeat
		lastTime = currentTimeMSecs
	}

	/**
	 * Start the animation after some delay
	 * @param delayMSecs
	 */
	fun <A : AAnimation<*>> start(delayMSecs: Long): A {
		var delayMSecs = delayMSecs
		if (delayMSecs < 0) {
			delayMSecs = 0
		}
		startTime = currentTimeMSecs + delayMSecs
		lastTime = startTime
		position = 0f
		state = State.STARTED
		return this as A
	}

	/**
	 * Start the animation immediately
	 * @return
	 */
	fun <A : AAnimation<*>> start(): A {
		start<AAnimation<T>>(0)
		return this as A
	}

	/**
	 * Start the animation in reverse direction after some delay.
	 *
	 * @param delayMSecs
	 * @return
	 */
	fun <A : AAnimation<T>> startReverse(delayMSecs: Long): A {
		start<AAnimation<T>>(delayMSecs)
		position = 1f
		isReverse = true
		isStartDirectionReverse = isReverse
		return this as A
	}

	/**
	 *
	 * @param oscillating
	 * @param <A>
	 * @return
	</A> */
	fun <A : AAnimation<T>> setOscillating(oscillating: Boolean): A {
		isOscillateOnRepeat = oscillating
		return this as A
	}

	/**
	 *
	 * @param repeats
	 * @param <A>
	 * @return
	</A> */
	fun <A : AAnimation<T>> setRepeats(repeats: Int): A {
		maxRepeats = repeats
		return this as A
	}

	/**
	 * Immediately start the animation in reverse direction
	 * @return
	 */
	fun <A : AAnimation<T>> startReverse(): A {
		return startReverse<AAnimation<T>>(0) as A
	}

	/**
	 * This function must be gauranteed to be called once after one of 'start' methods is called
	 * @return
	 */
	open val isDone: Boolean
		get() = state == State.DONE

	/**
	 * Override this if there is some rendering to do while waiting for the animation to finish
	 * initial delay
	 *
	 * @param g
	 */
	protected open fun drawPrestart(g: T) {}

	/**
	 * Call this within your rendering loop.  Animation is over when onDone() {} executed
	 *
	 * @param g
	 * returns true when draw has been called, false otherwise
	 */
	@Synchronized
	open fun update(g: T): Boolean {
		if (state == State.PRESTART) {
			System.err.println("Calling update on animation that has not been started!")
			Utils.assertTrue(false)
			return false
		}
		var dt = 0f
		val t = currentTimeMSecs
		if (state != State.STOPPED) {
			if (t < startTime) {
				drawPrestart(g)
				return false
			} else if (state == State.STARTED) {
				state = State.RUNNING
				lastTime = t
				if (isStartDirectionReverse) {
					onStartedReversed(g)
				} else {
					onStarted(g)
				}
			}
			val delta = ((t - startTime) % duration).toFloat()
			val repeats = (t - startTime) / duration
			if (maxRepeats in 0 until repeats) {
				position = if (isReverse) 0f else 1f
				stop()
			} else {
				if (isOscillateOnRepeat) {
					if (repeats % 2 == 1L) {
						isReverse = !isStartDirectionReverse
					} else {
						isReverse = isStartDirectionReverse
					}
				}
				position = if (isReverse) {
					1 - delta / duration
				} else {
					delta / duration
				}
			}
			dt = (t - lastTime).toFloat() / duration
			var r: Int
			if (curRepeat != repeat.also { r = it }) {
				onRepeat(r)
				curRepeat = r
			}
			draw(g, position, dt)
		} else {
			draw(g, position, dt)
		}
		lastTime = t
		if (state == State.STOPPED) {
			kill()
			onDone()
		}
		return true
	}

	/**
	 * If animation is running will stop where it is and call onDone on next call to update
	 */
	@Synchronized
	fun stop() {
		if (state != State.DONE) state = State.STOPPED
	}

	/**
	 * Animation state set to DONE. onDone will not be called.
	 */
	@Synchronized
	fun kill() {
		state = State.DONE
		position = if (isReverse) 0f else 1f
	}

	/**
	 * Override this to use a different time method other than System.currentTimeMillis()
	 * @return
	 */
	protected open val currentTimeMSecs: Long
		protected get() = System.currentTimeMillis()

	/**
	 * Do not call, call update
	 * @param g
	 * @param position
	 */
	protected abstract fun draw(g: T, position: Float, dt: Float)

	/**
	 * Called from update thread when animation ended. base method does nothing
	 */
	protected open fun onDone() {}

	/**
	 * Called from update thread when animation is started. base method does nothing
	 * If there is an initial delay then this will indicate the delay has expired.
	 */
	protected open fun onStarted(g: T) {}
	protected open fun onStartedReversed(g: T) {}
	protected open fun onRepeat(n: Int) {}

	val elapsedTime: Long
		get() = currentTimeMSecs - startTime
	val timeRemaining: Long
		get() = duration - elapsedTime
	val repeat: Int
		get() = ((System.currentTimeMillis() - startTime) / duration).toInt()
	val isStarted: Boolean
		get() = state == State.STARTED || state == State.RUNNING
	val isRunning: Boolean
		get() = state == State.RUNNING
}