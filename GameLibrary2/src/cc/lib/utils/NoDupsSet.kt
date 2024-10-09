package cc.lib.utils

/**
 * Created by Chris Caron on 4/26/22.
 */
class NoDupsSet : HashSet<Any?>() {
	override fun add(o: Any?): Boolean {
		require(!contains(o)) { "Cannot add duplicate object:$o" }
		return super.add(o)
	}
}
