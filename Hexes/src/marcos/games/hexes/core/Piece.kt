package marcos.games.hexes.core

import cc.lib.reflector.Reflector

class Piece : Reflector<Piece?> {
	@JvmField
	var player = 0 // 0 means not assigned to a player
	val v = IntArray(3) //v0, v1, v2; // v0, v1 is always the base.  v1, v2 is always right side.  v2,v0 is always left side

	@JvmField
	var type = Shape.NONE // type of piece.  Never NONE when assigned to a player
	var groupId = 0 // group id
	var groupShape = Shape.NONE // group shape type

	constructor()
	internal constructor(v0: Int, v1: Int, v2: Int) {
		v[0] = v0
		v[1] = v1
		v[2] = v2
	}

	override fun toString(): String {
		return "[" + v[0] + "," + v[1] + "," + v[2] + "] type=" + type + " player=" + player + " id=" + groupId + " shape=" + groupShape
	}

	companion object {
		init {
			addAllFields(Piece::class.java)
		}
	}
}
