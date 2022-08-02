package cc.lib.checkerboard

import cc.lib.game.Utils
import cc.lib.utils.GException
import java.util.*
import kotlin.collections.ArrayList

class Ugolki : Checkers() {
	companion object {
		private val FAR_POSITIONS = arrayOf(intArrayOf(0, 3), intArrayOf(0, 4), intArrayOf(0, 5), intArrayOf(1, 3), intArrayOf(1, 4), intArrayOf(1, 5))
		private val NEAR_POSITIONS = arrayOf(intArrayOf(4, 0), intArrayOf(4, 1), intArrayOf(4, 2), intArrayOf(5, 0), intArrayOf(5, 1), intArrayOf(5, 2))
		private val OTHER_POSITIONS: Array<IntArray> by lazy {
			val list = ArrayList<IntArray>()
			for (i in 0..5) {
				for (ii in 0..5) {
					if (!isInArray(i, ii, FAR_POSITIONS) && !isInArray(i, ii, NEAR_POSITIONS)) {
						list.add(intArrayOf(i, ii))
					}
				}
			}
			list.toTypedArray()
		}
		fun isInArray(i0: Int, i1: Int, arr: Array<IntArray>): Boolean {
			for (i in arr.indices) {
				if (arr[i][0] == i0 && arr[i][1] == i1) return true
			}
			return false
		}

	}

	override fun init(game: Game): Array<Array<Piece>> {
		game.turn = if (Utils.flipCoin()) Game.FAR else Game.NEAR
		return Array(6) { rank ->
			Array(6) { col ->
				Piece(rank, col)
			}
		}.also { board ->
			for (pos in FAR_POSITIONS) {
				board[pos[0]][pos[1]] = Piece(pos[0], pos[1], Game.FAR, PieceType.DAMA_MAN)
			}
			for (pos in NEAR_POSITIONS) {
				board[pos[0]][pos[1]] = Piece(pos[0], pos[1], Game.NEAR, PieceType.DAMA_MAN)
			}
		}
	}

	override fun getPlayerColor(side: Int): Color {
		return if (side == Game.FAR) Color.BLACK else Color.WHITE
	}

	override val isNoCaptures: Boolean
		get() = true
	override val isKingPieces: Boolean
		get() = false

	override fun isDraw(game: Game): Boolean {
		return false // draw game not possible in Ukogli
	}

	override fun getWinner(game: Game): Int {
		if (isWinner(game, Game.NEAR)) {
			return Game.NEAR
		}
		return if (isWinner(game, Game.FAR)) {
			Game.FAR
		} else -1
	}

	fun isWinner(game: Game, side: Int): Boolean {
		when (side) {
			Game.FAR -> {
				for (pos in NEAR_POSITIONS) {
					val p = game.getPiece(pos[0], pos[1])
					if (p.playerNum != side) return false
				}
			}
			Game.NEAR -> {
				for (pos in FAR_POSITIONS) {
					val p = game.getPiece(pos[0] shl 8 or pos[1])
					if (p.playerNum != side) return false
				}
			}
			else      -> throw GException("Unhandled case $side")
		}
		return true
	}

	override fun canJumpSelf(): Boolean {
		return true
	}

	override fun evaluate(game: Game): Long {
		val playerNum = game.turn
		val targetPositions: MutableList<IntArray> = ArrayList()
		if (playerNum == Game.NEAR) targetPositions.addAll(Arrays.asList(*FAR_POSITIONS)) else targetPositions.addAll(Arrays.asList(*NEAR_POSITIONS))
		var totalD = 0
		for (p in game.getPieces(playerNum)) {
			val clIdx = getClosest(p.position, targetPositions)
			val pos = targetPositions.removeAt(clIdx)
			totalD += dist(pos, p.position)
		}
		assert(totalD > 0)
		return (Int.MAX_VALUE - totalD).toLong()
	}

	private fun evaluate_old(game: Game, move: Move): Long {
		var numNearPiecesInPlace = 0
		var numFarPiecesInPlace = 0
		val nearPositions = arrayOfNulls<IntArray>(NEAR_POSITIONS.size)
		val farPositions = arrayOfNulls<IntArray>(FAR_POSITIONS.size)
		val farPiecesNotInPlace: MutableList<Piece> = ArrayList()
		val nearPiecesNotInPlace: MutableList<Piece> = ArrayList()
		var numNearPositions = 0
		var numFarPositions = 0
		for (r in 0 until game.ranks) {
			for (c in 0 until game.columns) {
				val p = game.getPiece(r, c)
				if (p.getType() === PieceType.EMPTY) continue
				assert(p.getType() === PieceType.DAMA_MAN, {"Invalid piece" })
				when (p.playerNum) {
					Game.NEAR, Game.FAR -> {
					}
				}
			}
		}
		for (pos in NEAR_POSITIONS) {
			val p = game.getPiece(pos[0], pos[1])
			if (p.getType() === PieceType.EMPTY) continue
			if (p.getType() !== PieceType.DAMA_MAN) throw GException("Invalid piece")
			when (p.playerNum) {
				Game.FAR -> numFarPiecesInPlace++
				Game.NEAR -> {
					nearPiecesNotInPlace.add(p)
					nearPositions[numNearPositions++] = pos
				}
			}
		}
		for (pos in FAR_POSITIONS) {
			val p = game.getPiece(pos[0], pos[1])
			if (p.getType() === PieceType.EMPTY) continue
			if (p.getType() !== PieceType.DAMA_MAN) throw GException("Invalid piece")
			when (p.playerNum) {
				Game.NEAR -> numNearPiecesInPlace++
				Game.FAR -> {
					farPiecesNotInPlace.add(p)
					farPositions[numFarPositions++] = pos
				}
			}
		}
		for (pos in OTHER_POSITIONS) {
			val p = game.getPiece(pos[0], pos[1])
			if (p.getType() === PieceType.EMPTY) continue
			if (p.getType() !== PieceType.DAMA_MAN) throw GException("Invalid piece")
			when (p.playerNum) {
				Game.NEAR -> nearPiecesNotInPlace.add(p)
				Game.FAR -> farPiecesNotInPlace.add(p)
			}
		}
		var nearValue = (1000 * numNearPiecesInPlace).toLong()
		var farValue = (1000 * numFarPiecesInPlace).toLong()
		for (p in nearPiecesNotInPlace) {
			nearValue -= distBetween(p.position, farPositions, numFarPositions).toLong()
		}
		for (p in farPiecesNotInPlace) {
			farValue -= distBetween(p.position, nearPositions, numNearPositions).toLong()
		}
		return if (move.playerNum == Game.NEAR) {
			nearValue - farValue
		} else {
			farValue - nearValue
		}
	}

	fun distBetween(pos: Int, positions: Array<IntArray?>, num: Int): Int {
		if (num == 0) return 0
		var minD = Int.MAX_VALUE
		for (i in 0 until num) {
			val p1 = positions[i]
			val d = dist(p1, pos)
			minD = Math.min(minD, d)
		}
		return minD
	}

	fun getClosest(pos: Int, positions: List<IntArray>): Int {
		assert(positions.size > 0)
		val cl = positions[0]
		val p0r = pos shr 8
		val p0c = pos and 0xff
		var minD = Math.abs(p0r - cl[0]) + Math.abs(p0c - cl[1])
		for (i in positions.indices) {
			val p1 = positions[i]
			val d = dist(p1, pos)
			minD = Math.min(minD, d)
		}
		return minD
	}

	fun dist(p0: IntArray?, pos: Int): Int {
		val posrank = pos shr 8
		val poscol = pos and 0xff
		return Math.abs(p0!![0] - posrank) + Math.abs(p0[1] - poscol)
	}

	override val description: String
		get() = "Objective: move all your pieces into opponents starting section"
}