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
class KLock() {

	private var inUse = 0
	private val mutex = Mutex()

	private var generation = 0
	private var allReleased = CompletableDeferred<Unit>().apply { complete(Unit) }


	suspend fun acquire(num: Int = 1) {
		val gen = generation
		onAcquire(gen, num)
	}

	suspend fun acquireAndBlock() {
		acquire()
		block()
	}

	suspend fun acquireAndBlock(timeoutMillis: Long, onTimeout: () -> Unit = {}) {
		val gen = generation
		onAcquire(gen, 1)
		withTimeoutOrNull(timeoutMillis) {
			block()
		} ?: onTimeout()
	}

	suspend fun block(timeoutMillis: Long, onTimeout: () -> Unit = {}) {
		withTimeoutOrNull(timeoutMillis) {
			block()
		} ?: onTimeout()
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

	private suspend fun onAcquire(gen: Int, num: Int) {
		mutex.withLock {
			// If reset happened during acquire, ignore
			if (gen != generation) return

			if (inUse == 0) {
				allReleased = CompletableDeferred()
			}
			inUse += num
		}
	}

	fun releaseAll() = runBlocking {
		reset()
	}
}