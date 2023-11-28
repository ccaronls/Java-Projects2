package cc.game.dominos.core

import cc.lib.annotation.Keep
import cc.lib.game.GRectangle
import cc.lib.game.Utils
import cc.lib.net.AClientConnection
import cc.lib.utils.Reflector
import java.util.*

open class Player(var playerNum: Int=-1) : Reflector<Player>() {
	companion object {
		init {
			addAllFields(Player::class.java)
		}
	}

	private var name: String="Player $playerNum"
    val tiles: MutableList<Tile> = ArrayList()

	/**
	 *
	 * @return
	 */
    @JvmField
    var score = 0
	/**
	 * 0 index player num
	 *
	 * @return
	 */
	/**
	 *
	 * @param playerNum
	 */
	@JvmField
    var smart = false

	/**
	 *
	 * @return
	 */
	@Omit
	var connection: AClientConnection? = null
		private set

	open fun getName(): String {
		connection?.let {
			return "P" + (playerNum + 1) + " " + it.name
		}
		return name
	}

	/**
	 *
	 * @param conn
	 */
	fun connect(conn: AClientConnection?) {
		connection = conn
	}

	@JvmField
    @Omit
	val outlineRect = GRectangle()
	fun reset() {
		tiles.clear()
		score = 0
	}

	@Synchronized
	fun findTile(n1: Int, n2: Int): Tile? {
		for (p in tiles) {
			if (p.pip1 == n1 && p.pip2 == n2) return p
			if (p.pip2 == n1 && p.pip1 == n2) return p
		}
		return null
	}

	/**
	 * Override to change behavior. Base method does random pick of availabel choices
	 *
	 * @param game
	 * @param moves
	 * @return
	 */
	@Keep
	open fun chooseMove(game: Dominos, moves: List<Move>): Move? {
		if (connection != null && connection!!.isConnected) {
			return connection!!.executeDerivedOnRemote(MPConstants.USER_ID, true, moves)
		}
		if (smart) {
			var best: Move? = null
			var bestPts = 0
			for (m in moves) {
				val copy = game.board.deepCopy()
				copy!!.doMove(m.piece, m.endpoint, m.placment)
				val pts = copy.computeEndpointsTotal()
				if (pts % 5 == 0) {
					if (bestPts < pts) {
						bestPts = pts
						best = m
					}
				}
			}
			if (best != null) return best
		}
		return moves[Utils.rand() % moves.size]
	}

	/**
	 *
	 * @return
	 */
	open fun isPiecesVisible(): Boolean = false
}