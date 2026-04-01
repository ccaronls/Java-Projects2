package cc.lib.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Counting lock. Allows for blocking a thread until some number of tasks are completed.
 *
 *
 * Aquire increments the count.
 * Release decrements a count.
 * When count gets to zero the blocked thread is notified.
 */
class KLock(private var inUse: Int = 0) {

	private val mutex = Mutex()

	private var generation = 0
	private var allReleased = CompletableDeferred<Unit>().apply { complete(Unit) }

	suspend fun acquire() {
		val gen = generation
		onAcquire(gen)
	}

	suspend fun acquireAndBlock() {
		acquire()
		block()
	}

	suspend fun acquireAndBlock(timeoutMillis: Long) {
		val gen = generation
		onAcquire(gen)
		withTimeoutOrNull(timeoutMillis) {
			block()
		}

	}

	fun release() = runBlocking {
		val gen = generation

		mutex.withLock {
			// Ignore stale releases after reset
			if (gen != generation) return@runBlocking

			inUse--
			if (inUse == 0) {
				allReleased.complete(Unit)
			}
		}
	}

	suspend fun block() {
		allReleased.await()
	}

	/**
	 * Reset to initial state.
	 */
	suspend fun reset() {
		mutex.withLock {
			generation++

			inUse = 0

			// Complete any waiters
			allReleased.complete(Unit)

			// Prepare next cycle
			allReleased = CompletableDeferred()
			allReleased.complete(Unit)
		}
	}

	private suspend fun onAcquire(gen: Int) {
		mutex.withLock {
			// If reset happened during acquire, ignore
			if (gen != generation) return

			if (inUse == 0) {
				allReleased = CompletableDeferred()
			}
			inUse++
		}
	}

	fun releaseAll() = runBlocking {
		reset()
	}
}