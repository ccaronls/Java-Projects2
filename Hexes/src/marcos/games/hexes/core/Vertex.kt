package marcos.games.hexes.core

import cc.lib.game.IVector2D
import cc.lib.reflector.Reflector

class Vertex constructor(x: Number = 0, y: Number = 0) : Reflector<Vertex>(), IVector2D {

	override val x: Float = x.toFloat()
	override val y: Float = y.toFloat()
	val p = IntArray(6)
	var num = 0
		private set

	fun addPiece(index: Int) {
		p[num++] = index
	}

	override fun toString(): String {
		return "[$x,$y]"
	}

	override fun equals(obj: Any?): Boolean {
		if (this === obj) return true
		val v = obj as Vertex?
		return x == v!!.x && y == v.y
	}

	companion object {
		init {
			addAllFields(Vertex::class.java)
		}
	}
}
