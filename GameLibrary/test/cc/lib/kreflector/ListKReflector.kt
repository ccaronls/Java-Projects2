package cc.lib.kreflector

/**
 * Created by Chris Caron on 12/1/23.
 */
class ListKReflector : KReflector<ListKReflector>() {
	var intList: MutableList<Int> = ArrayList()

	companion object {
		init {
			addAllFields(ListKReflector::class.java)
		}
	}
}