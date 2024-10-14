package cc.lib.utils

import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import com.google.gson.stream.JsonReader

@Mirror
interface IStopWatch : Mirrored {
	var curTime: Long
	val isStarted: Boolean
}

open class StopWatch(
	startTime: Long = 0,
	pauseTime: Long = 0,
	curTime: Long = 0,
	deltaTime: Long = 0,
	lastCaptureTime: Long = 0
) : StopWatchImpl() {

	var startTime: Long = startTime
		private set
	var pauseTime: Long = pauseTime
		private set
	var deltaTime: Long = deltaTime
		get() = if (isStarted) deltaTime else 0
		private set
	var lastCaptureTime: Long = lastCaptureTime
		private set

	init {
		this.curTime = curTime
	}

	/**
	 * Start the stopwatch.  MUST be the first call
	 */
	fun start() {
		curTime = 0
		startTime = clockMiliseconds
		isStarted = true
		unpause()
	}

	fun stop() {
		isStarted = false
	}

	val isPaused: Boolean
		get() = pauseTime > 0

	/**
	 * Pause the stop watch.  getTime/getDeltaTime will not advance until unpause called.
	 */
	fun pause() {
		if (!isPaused) {
			pauseTime = clockMiliseconds
		}
	}

	/**
	 * Capture the current time and delta time.  Must be called before calling getTme, getDeltaTiime
	 */
	fun capture() {
		if (isStarted && pauseTime == 0L) {
			val t = clockMiliseconds
			curTime += t - startTime
			startTime = t
			deltaTime = curTime - lastCaptureTime
			lastCaptureTime = curTime
		}
	}

	/**
	 * Resume the stop watch if paused
	 */
	fun unpause() {
		if (pauseTime > 0) {
			startTime = clockMiliseconds
			pauseTime = 0
		}
	}

	val time: Long
		/**
		 * Get the time as of last call to capture()
		 * @return
		 */
		get() = if (isStarted) curTime else 0

	protected open val clockMiliseconds: Long
		/**
		 * Override this to use a different clock mechanism if desired.
		 * Default uses System.currentTimeMillis()
		 * @return
		 */
		protected get() = System.currentTimeMillis()


	override fun fromGson(reader: JsonReader, __name: String) {
		super.fromGson(reader, __name)
		if (isStarted) {
			startTime = clockMiliseconds
			pauseTime = startTime
		}
	}

}
