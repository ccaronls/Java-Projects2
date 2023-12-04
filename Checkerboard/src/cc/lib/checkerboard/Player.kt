package cc.lib.checkerboard

import cc.lib.game.Utils
import cc.lib.reflector.Reflector

open class Player(var playerNum: Int = -1, var color: Color = Color.BLACK) : Reflector<Player>() {
	companion object {
		init {
			addAllFields(Player::class.java)
		}
	}

	fun newGame() {}

	/**
	 * Override to customize behavior. Default behavior is random.
	 *
	 * @param game
	 * @param pieces
	 * @return
	 */
	open fun choosePieceToMove(game: Game, pieces: List<Piece>): Piece? {
		return pieces[Utils.rand() % pieces.size]
	}

	/**
	 * Override to customize behavior. Default behavior is random.
	 *
	 * @param game
	 * @param moves
	 * @return
	 */
	open fun chooseMoveForPiece(game: Game, moves: List<Move>): Move? {
		return moves[Utils.rand() % moves.size]
	}
}