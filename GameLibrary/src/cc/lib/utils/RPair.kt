package cc.lib.utils

import cc.lib.game.Utils
import cc.lib.reflector.Reflector
import java.util.Arrays

/**
 * Created by Chris Caron on 5/2/25.
 */
class RPair<FIRST : Reflector<FIRST>, SECOND : Reflector<SECOND>>(
	val first: FIRST, val second: SECOND
) : Reflector<RPair<FIRST, SECOND>>() {

	companion object {
		init {
			addAllFields(RPair::class.java)
		}
	}

	override fun toString(): String {
		return "Pair{" +
			"first=" + first +
			", second=" + second +
			'}'
	}

	override fun equals(o: Any?): Boolean {
		if (this === o) return true
		if (o == null || javaClass != o.javaClass) return false
		val pair = o as Pair<*, *>
		return Utils.isEquals(first, pair.first) &&
			Utils.isEquals(second, pair.second)
	}

	override fun hashCode(): Int {
		return Arrays.hashCode(Utils.toArray<Any>(first, second))
	}

}