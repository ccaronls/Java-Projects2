package cc.game.dominos.core

import cc.lib.game.GRectangle
import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.remote.IRemote2
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteContext
import cc.lib.ksp.remote.RemoteFunction
import cc.lib.net.AClientConnection

@Mirror
interface IDominosPlayer : Mirrored {
	val name: String
	val tiles: MutableList<Tile>
	var smart: Boolean
	var score: Int
	var playerNum: Int
}

@Remote
abstract class DRemotePlayer : DominosPlayerImpl(), IRemote2 {
	@RemoteFunction
	abstract suspend fun chooseMove(moves: List<Move>): Move?
}

open class Player(playerNum: Int = -1) : DRemotePlayerRemote() {

	init {
		this.playerNum = playerNum
		this.name = "Player $playerNum"
		this.smart = false
		this.score = 0
	}

	override var context: RemoteContext? = null

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
				val copy = game.board.deepCopy<Board>()
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

class NetPlayer(connection: AClientConnection) : Player() {

	init {
		context = connection
		name = "P" + (playerNum + 1) + " " + name
	}

	override suspend fun chooseMove(game: Dominos, moves: List<Move>): Move? {
		return super.chooseMove(moves)
	}
}