package cc.lib.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * Counting lock. Allows for blocking a thread until some number of tasks are completed.
 *
 *
 * Aquire increments the count.
 * Release decrements a count.
 * When count gets to zero the blocked thread is notified.
 */
class KLock {
	var holders = 0
		private set

	// allows us to differentiate when a 'block' has released due to notify of timeout
	var isNotified = false
		private set

	private var continuation: CancellableContinuation<Any?>? = null
	private val mutex = Mutex()

	fun acquire() {
		holders++
	}

	fun acquire(num: Int) {
		require(holders == 0) { "Holders [$holders] != 0" }
		holders = num
	}

	constructor()
	constructor(holders: Int) {
		this.holders = holders
	}

	suspend fun block(waitTimeMillis: Long = 0, onTimeout: (() -> Unit)? = null) {
		require(!(onTimeout != null && waitTimeMillis <= 0)) { "Cannot have timeout on infinite wait" }
		//require(!isBlocking) { "Already blocking" }
		isNotified = false
		if (holders > 0) {
			suspendCancellableCoroutine {
				it.invokeOnCancellation {
					continuation = null
					holders = 0
					isNotified = false
					if (mutex.isLocked)
						mutex.unlock()
				}
				continuation = it
			}
			holders = 0
			if (!isNotified) {
				onTimeout?.invoke()
			}
		}
	}

	suspend fun acquireAndBlock(timeout: Long = 0, onTimeout: (() -> Unit)? = null) {
		mutex.withLock {
			holders++
		}
		block(timeout, onTimeout)
	}

	fun releaseAll() {
		isNotified = true
		continuation?.resume(null)
		continuation = null
	}

	fun release() = runBlocking {
		mutex.withLock {
			if (holders > 0) holders--
		}
		if (holders == 0) {
			isNotified = true
			continuation?.resume(null)
			continuation = null
		}
	}

	fun reset() {
		continuation?.cancel()
	}
}
