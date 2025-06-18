package marcos.games.hexes.core

import cc.lib.game.Utils
import cc.lib.reflector.Reflector
import java.util.Arrays

open class Player : Reflector<Player>() {
	private val pieces = IntArray(Shape.entries.size)

	@JvmField
	var score = 0
	fun getShapeCount(shape: Shape): Int {
		return pieces[shape.ordinal]
	}

	fun decrementPiece(shape: Shape) {
		pieces[shape.ordinal] -= 1
	}

	fun init() {
		Utils.println("player.init")
		pieces[Shape.NONE.ordinal] = 0
		pieces[Shape.DIAMOND.ordinal] = 6
		pieces[Shape.TRIANGLE.ordinal] = 6
		pieces[Shape.HEXAGON.ordinal] = 6
		score = 0
	}

	fun countPieces(): Int {
		return Utils.sum(pieces)
	}

	/**
	 * Return an int from the options list
	 * @param hexes
	 * @param choices
	 * @return
	 */
	open fun choosePiece(hexes: Hexes, choices: List<Int>): Int {
		Utils.println("choosePiece pieces=" + Arrays.toString(pieces))
		val choice = choices[Utils.rand() % choices.size]
		Utils.println("choosePiece pieces=" + Arrays.toString(pieces))
		return choice
	}

	open fun chooseShape(hexes: Hexes, choices: Array<Shape>): Shape {
		return choices[Utils.rand() % choices.size]
	}

	companion object {
		init {
			addAllFields(Player::class.java)
		}
	}
}
