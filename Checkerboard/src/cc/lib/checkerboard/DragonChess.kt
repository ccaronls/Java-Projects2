package cc.lib.checkerboard

import cc.lib.game.Utils
import cc.lib.utils.Table

class DragonChess : Chess() {
	override fun init(game: Game): Array<Array<Piece>> {
		whiteSide = if (Utils.flipCoin()) Game.FAR else Game.NEAR
		// this is to enforce the 'queen on her own color square' rule
		var left = PieceType.QUEEN
		var right = PieceType.UNCHECKED_KING_IDLE
		if (whiteSide == Game.FAR) {
			right = PieceType.QUEEN
			left = PieceType.UNCHECKED_KING_IDLE
		}
		game.turn = whiteSide
		return initFromPieceTypes(arrayOf(
			Game.FAR to arrayOf(PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.DRAGON_IDLE_R, PieceType.KNIGHT_R, PieceType.BISHOP, left, right, PieceType.BISHOP, PieceType.KNIGHT_L, PieceType.DRAGON_IDLE_L, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED),
			Game.FAR to arrayOf(PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED),
			Game.NOP to arrayOf(PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP to arrayOf(PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED),
			Game.NEAR to arrayOf(PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED),
			Game.NEAR to arrayOf(PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.DRAGON_IDLE_R, PieceType.KNIGHT_R, PieceType.BISHOP, left, right, PieceType.BISHOP, PieceType.KNIGHT_L, PieceType.DRAGON_IDLE_L, PieceType.BLOCKED, PieceType.BLOCKED, PieceType.BLOCKED),
		))
	}

	override fun getRookCastleCols(game: Game): IntArray {
		return intArrayOf(3, 3 + 7)
	}

	override fun isSquareAttacked(game: Game, rank: Int, col: Int, attacker: Int): Boolean {
		if (super.isSquareAttacked(game, rank, col, attacker)) return true
		val kn = computeKDN(rank, col, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW)
		for (k in 0 until kn) {
			val kd = kdn[k]
			val dr = kd[0]
			val dc = kd[1]
			val num = Math.min(3, dr.size)
			for (i in 0 until num) {
				val rr = rank + dr[i]
				val cc = col + dc[i]
				if (testPiece(game, rr, cc, attacker, PieceType.FLAG_DRAGON)) return true
				if (game.getPiece(rr, cc).getType() !== PieceType.EMPTY) break
			}
		}
		return false
	}

	override val instructions: Table
		get() = Table().addRow("Chess Variation. Bigger board, non square and using Dragons instead of rooks. Dragons move like a queen but with a max of 3 squares.")
}