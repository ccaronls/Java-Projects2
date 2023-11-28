package cc.lib.net

interface SharedContext {

}

enum class SharedPropertyType {
	BOOL,
	INT,
	FLOAT,
	STRING,
	SHARED
}

interface SharedProperty<T> {
	fun get(): T
	fun set(value: T)
}

/**
 * Created by Chris Caron on 11/13/23.
 */
class SharedObject(context: SharedContext) {


	fun addSharedProperty(name: String, type: SharedPropertyType): SharedProperty<*> {
		TODO("Not yet implemented")
	}

}

class SharedList<T>(context: SharedContext) : MutableList<T> {

	private val list = mutableListOf<T>()

	override val size: Int
		get() = list.size

	override fun contains(element: T): Boolean = list.contains(element)

	override fun containsAll(elements: Collection<T>): Boolean = list.containsAll(elements)

	override fun get(index: Int): T {
		TODO("Not yet implemented")
	}

	override fun indexOf(element: T): Int {
		TODO("Not yet implemented")
	}

	override fun isEmpty(): Boolean {
		TODO("Not yet implemented")
	}

	override fun iterator(): MutableIterator<T> {
		TODO("Not yet implemented")
	}

	override fun lastIndexOf(element: T): Int {
		TODO("Not yet implemented")
	}

	override fun add(element: T): Boolean {
		TODO("Not yet implemented")
	}

	override fun add(index: Int, element: T) {
		TODO("Not yet implemented")
	}

	override fun addAll(index: Int, elements: Collection<T>): Boolean {
		TODO("Not yet implemented")
	}

	override fun addAll(elements: Collection<T>): Boolean {
		TODO("Not yet implemented")
	}

	override fun clear() {
		TODO("Not yet implemented")
	}

	override fun listIterator(): MutableListIterator<T> {
		TODO("Not yet implemented")
	}

	override fun listIterator(index: Int): MutableListIterator<T> {
		TODO("Not yet implemented")
	}

	override fun remove(element: T): Boolean {
		TODO("Not yet implemented")
	}

	override fun removeAll(elements: Collection<T>): Boolean {
		TODO("Not yet implemented")
	}

	override fun removeAt(index: Int): T {
		TODO("Not yet implemented")
	}

	override fun retainAll(elements: Collection<T>): Boolean {
		TODO("Not yet implemented")
	}

	override fun set(index: Int, element: T): T {
		TODO("Not yet implemented")
	}

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
		TODO("Not yet implemented")
	}
}