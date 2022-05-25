package cc.lib.checkerboard

import cc.lib.utils.Reflector
import cc.lib.utils.Table

/**
 * Base class for logical rules to govern a checkerboard style game.
 */
abstract class Rules : Reflector<Rules>() {
	/**
	 * setup pieces and choose side.
	 * @param game
	 */
	abstract fun init(game: Game) : Array<Array<Piece>>

	fun initFromPieceTypes(board: Array<Pair<Int, Array<PieceType>>>) : Array<Array<Piece>> {
		return board.mapIndexed { rank, pair ->
			Array(pair.second.size) { column ->
				Piece(rank, column, if (pair.second[column].flag == 0) Game.NOP else pair.first, pair.second[column])
			}
		}.toTypedArray()
	}

	/**
	 * Return a color for the side
	 * @param side
	 * @return
	 */
	abstract fun getPlayerColor(side: Int): Color

	/**
	 * Perform the logical move
	 * @param game
	 * @param move
	 */
	abstract fun executeMove(game: Game, move: Move)

	/**
	 * return the playerNum >= 0 is there is a winner, < 0 otherwise.
	 * @param game
	 * @return
	 */
	abstract fun getWinner(game: Game): Int

	/**
	 * Return true if the current state of the game is a tie
	 * @param game
	 * @return
	 */
	abstract fun isDraw(game: Game): Boolean

	/**
	 * Return the heuristic value of the board for the current move
	 * @param game
	 * @param move
	 * @return
	 */
	abstract fun evaluate(game: Game): Long

	/**
	 * return a list of available moves
	 * @param game
	 * @return
	 */
	abstract fun computeMoves(game: Game): List<Move>

	/**
	 * perform the inverse of executeMove. Use state stored in the move itself to speed the operation.
	 * @param game
	 * @param m
	 */
	abstract fun reverseMove(game: Game, m: Move)

	/**
	 * Some text to display to the user on how to play.
	 * @return
	 */
	abstract val instructions: Table
}