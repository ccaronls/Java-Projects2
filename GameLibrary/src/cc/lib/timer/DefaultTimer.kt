package cc.lib.timer

/**
 * Created by Chris Caron on 5/15/26.
 */
object DefaultTimer : IClock {
	override fun currentTimeMillis(): Long = System.currentTimeMillis()

	override fun nanoTime(): Long = System.nanoTime()
}