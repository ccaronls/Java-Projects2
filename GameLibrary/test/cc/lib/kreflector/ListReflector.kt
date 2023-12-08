package cc.lib.kreflector

/**
 * Created by Chris Caron on 12/1/23.
 */
class ListReflector : Reflector<ListReflector>() {
	var intList: MutableList<Int> = ArrayList()

	companion object {
		init {
			addAllFields(ListReflector::class.java)
		}
	}
}