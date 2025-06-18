package marcos.games.hexes.core

import cc.lib.reflector.Reflector
import java.util.LinkedList

class IdGenerator : Reflector<IdGenerator?>() {
	private val removed = LinkedList<Int>()
	private var counter = 1
	fun nextId(): Int {
		return if (removed.isEmpty()) counter++ else removed.remove()
	}

	fun putBack(id: Int) {
		if (id > 0 && !removed.contains(id)) removed.add(id)
	}

	companion object {
		init {
			addAllFields(IdGenerator::class.java)
		}
	}
}
