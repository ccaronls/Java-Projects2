package cc.lib.checkerboard

/**
 * Variation on Russian checkers where captured pieces are not removed but placed under the jumping piece making for taller and taller stacks.
 * If a stacked piece is jumnped, only the top checker is removed causing pieces to be released.
 */
class Columns : Checkers() {
	override val isStackingCaptures: Boolean
		get() = true

	override fun canJumpSelf(): Boolean {
		return false
	}

	override fun canMenJumpBackwards(): Boolean {
		return true
	}

	override val isJumpsMandatory: Boolean
		get() = true
	override val isFlyingKings: Boolean
		get() = true
	override val description: String
		get() = """
	    	A single piece may not move backward unless it has reached the opponent's king-row as a single man (whether or not covered and uncovered on the way), after which it is inverted or marked and may at any future time in the game move either backward or forward.
	    	A stack composed of two or more checkers of the same or different colors may move either backward or forward. A single piece confronted with a double jump may, after completion of the first jump, when it becomes a double, continue jumping in any direction.
	    	The same stack may not be jumped twice in the same series of jumps.
	    	If a jump is possible it must be made. There is no huffing or alternative penalty.
	    	The game is drawn if a player remains in possession of a man and cannot legally move.
	    	""".trimIndent()

	override fun isDraw(game: Game): Boolean {
		return if (game.getMoves().size == 0) true else super.isDraw(game)
	}

	override fun evaluate(game: Game): Long {
		var value: Long = 0
		for (p in game.getPieces(game.turn)) {
			for (i in 0 until p.stackSize) {
				if (p.getStackAt(i) == game.turn) value++ else break
			}
		}
		for (p in game.getPieces(Game.getOpponent(game.turn))) {
			// how many stacks of the top of the opponent player
			var cnt = 0
			for (i in 0 until p.stackSize) {
				if (p.getStackAt(i) == game.turn) {
					cnt++
				} else {
					value--
					if (cnt > 0) {
						break
					}
				}
			}
		}
		return value
	}
}