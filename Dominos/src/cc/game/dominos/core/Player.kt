package cc.game.dominos.core

import cc.lib.game.GRectangle
import cc.lib.ksp.remote.IRemote
import cc.lib.reflector.Reflector


open class Player(playerNum: Int = -1) : Reflector<Player>(), IRemote {

	var name = "Player $playerNum"
	val tiles = mutableListOf<Tile>()
	var smart = false
	var score = 0
	var playerNum = playerNum

	val outlineRect = GRectangle()

	fun reset() {
		tiles.clear()
		score = 0
	}

	fun findTile(n1: Int, n2: Int): Tile? {
		for (p in tiles) {
			if (p.pip1 == n1 && p.pip2 == n2) return p
			if (p.pip2 == n1 && p.pip1 == n2) return p
		}
		return null
	}

	open suspend fun chooseMove(game: Dominos, moves: List<Move>): Move? {
		if (smart) {
			return moves.maxByOrNull { m ->
				val copy = game.board.deepCopy()
				copy.doMove(m.piece, m.endpoint, m.placement)
				copy.computeEndpointsTotal().takeIf { it % 5 == 0 } ?: 0
			}
		}
		return moves.random()
	}

	/**
	 *
	 * @return
	 */
	open fun isPiecesVisible(): Boolean = false
}

class NetPlayer(val connection: IDominosConnection) : Player() {

	init {
		name = "P" + (playerNum + 1) + " " + name
	}

	override suspend fun chooseMove(game: Dominos, moves: List<Move>): Move? {
		return connection.chooseMove(moves)
	}
}