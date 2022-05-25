package cc.lib.checkerboard

import cc.lib.game.Utils
import cc.lib.utils.GException

/**
 * http://www.geekyhobbies.com/kings-court-1986-board-game-review-and-rules/
 */
class KingsCourt : Checkers() {
	override fun init(game: Game): Array<Array<Piece>> {
		game.turn = if (Utils.flipCoin()) Game.NEAR else Game.FAR
		var pnum = Game.NEAR
		return Array(8) { r ->
			Array(8) { c ->
				if (isInCourt(r, c))
					Piece(r, c)
				else {
					Piece(r, c, pnum, PieceType.CHIP_4WAY).also {
						pnum = (pnum + 1) % 2
					}
				}
			}
		}
	}

	var numNear = 0
	var numFar = 0
	override fun getWinner(game: Game): Int {
		// if a player has no pieces in the court, they are a loser
		numNear = 0
		numFar = 0
		for (r in 2..5) {
			for (c in 2..5) {
				val p = game.getPiece(r, c)
				if (p.getType() !== PieceType.EMPTY) {
					if (p.playerNum == Game.NEAR) {
						numNear++
					} else {
						numFar++
					}
				}
			}
		}
		if (game.moveHistoryCount < 3) return -1
		if (numNear == 0 && numFar == 0) return -1
		if (numNear > 0 && numFar > 0) return -1
		return if (numNear > 0) Game.NEAR else Game.FAR
	}

	override fun computeMoves(game: Game): List<Move> {
		val moves = super.computeMoves(game).toMutableList()
		if (game.moveHistoryCount < 2) {
			// remove anything with a jump or capture.
			val it = moves.iterator()
			while (it.hasNext()) {
				val m = it.next()
				if (m.hasCaptured()) {
					it.remove()
				}
			}
			if (moves.size < 1) throw GException("Bad state")
		}
		// visit moves and remove any that cause us to lose
		getWinner(game)
		when (game.turn) {
			Game.NEAR -> if (numNear > 1) {
				return moves
			}
			Game.FAR -> if (numFar > 1) {
				return moves
			}
		}
		val it = moves.iterator()
		while (it.hasNext()) {
			val m = it.next()
			if (m.hasEnd() && !isInCourt(m.end)) {
				it.remove()
			}
		}
		return moves
	}

	override fun evaluate(game: Game): Long {
		if (game.isDraw()) return 0
		var winner: Int
		when (game.getWinnerNum().also { winner = it }) {
			Game.NEAR, Game.FAR -> return if (winner == game.turn) Long.MAX_VALUE else Long.MIN_VALUE
		}
		var value: Long = 0 // no its not game.getMoves().size(); // move options is good
		//if (move.hasCaptured())
		//    value += 1000;
		for (r in 0 until game.ranks) {
			for (c in 0 until game.columns) {
				val p = game.getPiece(r, c)
				val scale = if (p.playerNum == game.turn) 1 else -1
				when (p.getType()) {
					PieceType.EMPTY -> {
					}
					PieceType.CHIP_4WAY -> value += (scale * (10 - distToCourtTable[r][c])).toLong()
					else                -> throw GException("Unhandled case '" + p.getType() + "'")
				}
			}
		}
		return value
	}

	override val isKingPieces: Boolean
		get() = false
	override val description: String
		get() = "Objective is to have only your own pieces in the court (center region). On first move each play moves a piece onto the court and must keep at least one piece there or else they lose."

	companion object {
		fun isInCourt(pos: Int): Boolean {
			val r = pos shr 8
			val c = pos and 0xff
			return r >= 2 && c >= 2 && r <= 5 && c <= 5
		}

		fun isInCourt(r: Int, c: Int): Boolean {
			return r >= 2 && c >= 2 && r <= 5 && c <= 5
		}

		// table that gives the number of slides neccessary to get to the court from a current [r.c]
		var distToCourtTable = arrayOf(intArrayOf(4, 3, 2, 2, 2, 2, 3, 4), intArrayOf(3, 2, 1, 1, 1, 1, 2, 3), intArrayOf(2, 1, 0, 0, 0, 0, 1, 2), intArrayOf(2, 1, 0, 0, 0, 0, 1, 2), intArrayOf(2, 1, 0, 0, 0, 0, 1, 2), intArrayOf(2, 1, 0, 0, 0, 0, 1, 2), intArrayOf(3, 2, 1, 1, 1, 1, 2, 3), intArrayOf(4, 3, 2, 2, 2, 2, 3, 4))
	}
}