package cc.lib.game

/**
 * Created by Chris Caron on 9/28/21.
 *
 * Interpolates over several interpolators in sequence
 */
class ChainInterpolator<T>(vararg chain: IInterpolator<T>) : IInterpolator<T> {
	val chain: Array<IInterpolator<T>> = arrayOf(*chain)

	override fun getAtPosition(position: Float): T {
		val steps = 1f / chain.size
		var step = 0f
		while (step + steps < position) {
			step += steps
		}
		val idx = Math.round(step * chain.size)
		val pos = (position - step) * chain.size
		return chain[idx].getAtPosition(pos)
	}
}
