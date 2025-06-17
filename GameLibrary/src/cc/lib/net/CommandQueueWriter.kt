package cc.lib.net

import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.utils.GException
import cc.lib.utils.launchIn
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.DataOutputStream
import java.io.IOException

/**
 * Allows for queueing of commands so we are never waiting for the network to write
 * @author ccaron
 */
open class CommandQueueWriter(logPrefix: String) {
	private val log: Logger
	private val queue = Channel<GameCommand>(32)
	private var timeout = 10000L
	private val scope = CoroutineScope(Dispatchers.IO + CoroutineName(logPrefix))
	private var job: Job? = null
	private var out: DataOutputStream? = null

	var running = false
		private set

	init {
		log = LoggerFactory.getLogger(logPrefix, CommandQueueWriter::class.java)
	}

	fun setTimeout(timeout: Number) {
		this.timeout = timeout.toLong()
	}

	@Synchronized
	fun start(out: DataOutputStream) {
		if (!running) {
			this.out = out
			running = true
			job = scope.launch {
				var errors = 0
				while ((running || !queue.isEmpty) && errors < 5) {
					val cmd: GameCommand? = withTimeoutOrNull(timeout) {
						queue.receive()
					} ?: run {
						onTimeout()
						null
					}
					try {
						cmd?.let {
							it.write(out)
							out.flush()
							errors = 0
						}
					} catch (e: IOException) {
						if (!running)
							break
						errors++
						log.error("ERROR: $errors Problem sending command: $cmd")
					} catch (e: Exception) {
						break
					}
				}
				queue.close()
			}
		} else {
			log.error("start called when already running!")
		}
	}

	/**
	 * Block until all commands sent.  No new commands will be accepted.
	 */
	@Synchronized
	fun stop(flush: Boolean) = runBlocking {
		log.debug("Stopping")
		running = false
		job?.cancel()
		job = null
		out?.takeIf { flush }?.let { out ->
			while (!queue.isEmpty) {
				queue.receive().write(out)
			}
			out.flush()
		}
		out = null
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
		launchIn {
			if (!queue.isClosedForSend)
				queue.send(cmd)
		}
	}

	/**
	 * Called when the specified timeout has been reached without a command
	 * being sent.  This could be used to support a keep-alive scheme.
	 */
	protected open fun onTimeout() {}
}
