package cc.game.dominos.core

/**
 * Created by chriscaron on 2/14/18.
 */
open class PlayerUser : Player {
	var chosenMove: Move? = null
	val usable = HashSet<Tile>()
	val moves: MutableList<Move> = ArrayList()

	constructor() {}
	constructor(playerNum: Int) : super(playerNum) {}

	override suspend fun chooseMove(game: Dominos, moves: List<Move>): Move? {
		clearMoves()
		this.moves.addAll(moves)
		for (m in moves) {
			usable.add(m.piece)
		}
		game.redraw()
		game.gameLock.acquireAndBlock()
		if (chosenMove != null)
			usable.clear()
		return chosenMove
	}

	override fun isPiecesVisible(): Boolean {
		return true
	}

	protected fun clearMoves() {
		moves.clear()
		usable.clear()
		chosenMove = null
	}

}