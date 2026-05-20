package cc.lib.timer

import java.util.concurrent.atomic.AtomicLong

/**
 * A debugger-aware virtual clock.
 *
 * Behaves similarly to System.currentTimeMillis(),
 * except time does not advance while the JVM is paused
 * in a debugger breakpoint.
 *
 * Based on the assumption that will be called very frequently
 * and any long pause (250ms) is the result of a break point
 * and should be paused
 */
class DebugTimer(val sourceClock: IClock) : IClock {

	/**
	 * Virtual current time.
	 */
	override fun currentTimeMillis(): Long {
		update()
		return virtualTime.get()
	}

	/**
	 * Monotonic nanos equivalent.
	 */
	override fun nanoTime(): Long {
		update()
		return virtualNanoTime.get()
	}

	/**
	 * Resets virtual clock to current real clock.
	 */
	fun reset() {
		synchronized(lock) {
			val nowMs = sourceClock.currentTimeMillis()
			val nowNs = sourceClock.nanoTime()

			lastRealMs = nowMs
			lastRealNs = nowNs

			virtualTime.set(nowMs)
			virtualNanoTime.set(nowNs)
		}
	}

	// ------------------------------------------------------------------------

	private val lock = Any()

	private val virtualTime = AtomicLong(sourceClock.currentTimeMillis())
	private val virtualNanoTime = AtomicLong(sourceClock.nanoTime())

	private var lastRealMs = sourceClock.currentTimeMillis()
	private var lastRealNs = sourceClock.nanoTime()

	/**
	 * If elapsed exceeds this threshold,
	 * assume we were stopped in debugger.
	 */
	private val DEBUG_PAUSE_THRESHOLD_MS = 250L

	/**
	 * Maximum amount of elapsed time to accept
	 * in a single update.
	 */
	private val MAX_FRAME_ADVANCE_MS = 100L

	private fun update() {
		synchronized(lock) {
			val nowMs = sourceClock.currentTimeMillis()
			val nowNs = sourceClock.nanoTime()

			val deltaMs = nowMs - lastRealMs
			val deltaNs = nowNs - lastRealNs

			lastRealMs = nowMs
			lastRealNs = nowNs

			val acceptedMs = when {
				deltaMs < 0 -> 0
				deltaMs > DEBUG_PAUSE_THRESHOLD_MS -> 0
				else -> minOf(deltaMs, MAX_FRAME_ADVANCE_MS)
			}

			val acceptedNs = when {
				deltaNs < 0 -> 0
				deltaMs > DEBUG_PAUSE_THRESHOLD_MS -> 0
				else -> deltaNs
			}

			virtualTime.addAndGet(acceptedMs)
			virtualNanoTime.addAndGet(acceptedNs)
		}
	}
}