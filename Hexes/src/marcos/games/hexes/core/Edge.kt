package marcos.games.hexes.core

import cc.lib.reflector.Reflector

class Edge : Reflector<Edge>, Comparable<Edge> {
	val from: Int
	val to: Int
	var num = 0
		private set
	val p = IntArray(2) // index to pieces adjacent to

	constructor() {
		to = 0
		from = to
	}

	internal constructor(from: Int, to: Int) {
		if (from == to) throw RuntimeException("Illegal edge")
		if (from < to) {
			this.from = from
			this.to = to
		} else {
			this.to = from
			this.from = to
		}
	}

	fun addPiece(index: Int) {
		p[num++] = index
	}

	override fun compareTo(e: Edge): Int {
		return if (from == e.from) to - e.to else from - e.from
	}

	override fun equals(obj: Any?): Boolean {
		val e = obj as Edge?
		return if (e === this) true else from == e!!.from && to == e.to
	}

	override fun toString(): String {
		return "$from->$to"
	}

	companion object {
		init {
			addAllFields(Edge::class.java)
		}
	}
}
