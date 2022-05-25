package cc.lib.checkerboard

/**
 * same as checkers only winner is the first to sacrifice all pieces or otherwise fail to be able to move
 */
class Suicide : Checkers() {
	override fun getWinner(game: Game): Int {
		var winner: Int
		when (super.getWinner(game).also { winner = it }) {
			Game.NEAR -> return Game.FAR
			Game.FAR -> return Game.NEAR
		}
		return winner
	}

	override fun isDraw(game: Game): Boolean {
		return super.isDraw(game)
	}

	override fun evaluate(game: Game): Long {
		return -1L * super.evaluate(game)
	}

	override val isJumpsMandatory: Boolean
		get() = true
	override val description: String
		get() = "Objective: to LOSE all your pieces"
}