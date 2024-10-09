package cc.lib.game

/**
 * Interface for a MiniMaxDescision Tree
 * @param <M>
</M> */
interface IGame<M : IMove> {
	/**
	 * This will push of the game
	 * @param move
	 */
	fun executeMove(move: M)

	/**
	 * This will pop state
	 * @return
	 */
	fun undo(): M

	/**
	 * Get all the moves available for this state
	 * @return
	 */
	fun getMoves(): List<M>

	/**
	 * Get the current player turn
	 * @return
	 */
	var turn: Int
	fun getWinnerNum(): Int
	fun isDraw(): Boolean
}