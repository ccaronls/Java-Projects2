package cc.lib.net

import cc.lib.game.Utils
import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.utils.GException
import cc.lib.utils.Lock
import java.io.DataOutputStream
import java.util.LinkedList
import java.util.Queue

/**
 * Allows for queueing of commands so we are never waiting for the network to write
 * @author ccaron
 */
open class CommandQueueWriter(logPrefix: String) : Runnable {
	private val log: Logger
	private val queue: Queue<GameCommand> = LinkedList()
	private var running = false
	private var timeout = 10000
	private lateinit var out: DataOutputStream
	private val lock = Lock()
	private val stopLock = Lock()

	init {
		log = LoggerFactory.getLogger(logPrefix, CommandQueueWriter::class.java)
	}

	fun setTimeout(timeout: Int) {
		this.timeout = timeout
		lock.release()
	}

	@Synchronized
	fun start(out: DataOutputStream) {
		if (!running) {
			this.out = out
			running = true
			lock.releaseAll()
			lock.reset()
			Thread(this).start()
		} else {
			log.error("start called when already running!")
		}
	}

	override fun run() {
		log.debug("thread starting")
		var errors = 0
		while (errors < 5 && (running || !queue.isEmpty())) {
			var cmd: GameCommand? = null
			try {
				if (queue.isEmpty()) {
					lock.acquire(1)
					lock.block(timeout.toLong()) { onTimeout() }
				}
				if (!queue.isEmpty()) {
					synchronized(queue) { cmd = queue.peek() }
					log.debug("Writing command: " + cmd!!.type)
					cmd!!.write(out)
					out.flush()
					synchronized(queue) { queue.remove() }
					errors = 0
				}
			} catch (e: Exception) {
				errors++
				log.error("ERROR: $errors Problem sending command: $cmd")
				e.printStackTrace()
				Utils.waitNoThrow(this, 500)
			}
		}
		// signal waiting stop() call that we done.
		stopLock.release()
		log.debug("thread exiting")
	}

	/**
	 * Block until all commands sent.  No new commands will be accepted.
	 */
	fun stop() {
		log.debug("Stopping")
		try {
			// block for up to 5 seconds for the remaining commands to get sent
			if (queue.size > 0) {
				log.debug("Wait for queue to flush")
				lock.release()
				stopLock.acquireAndBlock(5000)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		running = false
		synchronized(queue) { queue.clear() }
		lock.releaseAll()
		log.debug("Stopped")
	}

	/**
	 * Push a command to the outbound queue.
	 * throw an error if the queue not running
	 * @param cmd
	 * @throws Exception
	 */
	fun add(cmd: GameCommand) {
		if (!running) throw GException("commandQueue is not running")
		synchronized(queue) { queue.add(cmd) }
		lock.release()
	}

	/**
	 * Clear out any pending things
	 */
	fun clear() {
		synchronized(queue) { queue.clear() }
	}

	/**
	 * Called when the specified timeout has been reached without a command
	 * being sent.  This could be used to support a keep-alive scheme.
	 */
	protected open fun onTimeout() {}
}
