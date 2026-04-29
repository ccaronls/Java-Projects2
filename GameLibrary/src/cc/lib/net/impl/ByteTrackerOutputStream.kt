package cc.lib.net.impl

import cc.lib.ksp.netcmd.INetCommand
import java.io.OutputStream


class CmdStat {
	var timesSent = 0
		private set
	var bytesSent = 0L
		private set

	var minBytes = Long.MAX_VALUE
		private set

	var maxBytes = 0L
		private set

	val avgBytes: Long
		get() = if (timesSent > 0) bytesSent / timesSent else 0L

	fun increment(bytes: Int) {
		timesSent++
		bytesSent += bytes
		minBytes = minBytes.coerceAtMost(bytes.toLong())
		maxBytes = maxBytes.coerceAtLeast(bytes.toLong())
	}

	companion object {
		fun header(): String = String.format("%-6s %-6s %-6s %-6s %-6s", "COUNT", "BYTES", "MIN", "MAX", "AVG")
	}

	fun line(): String = String.format("%-6d %-6d %-6d %-6d %-6d", timesSent, bytesSent, minBytes, maxBytes, avgBytes)
}


/**
 * Created by Chris Caron on 4/29/26.
 */
class ByteTrackerOutputStream(val stats: MutableMap<String, CmdStat>, val out: OutputStream) : OutputStream() {

	var count = 0
	var cmd: INetCommand? = null

	fun mark(cmd: INetCommand) {
		this.cmd = cmd
		this.count = 0
	}

	fun completed() {
		cmd?.let {
			stats.getOrPut(it.serializedName) { CmdStat() }.increment(count)
		}
	}

	override fun write(p0: Int) {
		out.write(p0)
		count++
	}

	override fun write(p0: ByteArray) {
		out.write(p0)
		count += p0.size
	}

	override fun write(p0: ByteArray, p1: Int, p2: Int) {
		out.write(p0, p1, p2)
		count += p2
	}

	override fun close() {
		out.close()
	}

	override fun flush() {
		out.flush()
	}
}