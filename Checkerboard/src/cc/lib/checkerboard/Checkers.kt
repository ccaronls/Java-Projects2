package cc.lib.checkerboard

import cc.lib.game.Utils
import cc.lib.math.CMath
import cc.lib.utils.GException
import cc.lib.utils.Table

open class Checkers : Rules() {

	override fun init(game: Game): Array<Array<Piece>> {
		game.turn = if (Utils.flipCoin()) Game.FAR else Game.NEAR
		return initFromPieceTypes(arrayOf(
			Game.FAR to arrayOf(PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY),
			Game.FAR to arrayOf(PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER),
			Game.FAR to arrayOf(PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NEAR to arrayOf(PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER),
			Game.NEAR to arrayOf(PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY),
			Game.NEAR to arrayOf(PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER)
		))
	}

	override fun computeMoves(game: Game): List<Move> {
		val moves: MutableList<Move> = ArrayList()
		var numJumps = 0
		for (rank in 0 until game.ranks) {
			for (col in 0 until game.columns) {
				with (game.getPiece(rank, col)) {
					if (playerNum == game.turn)
						numJumps += computeMovesForSquare(game, rank, col, null, moves)
				}
			}
		}
		if (numJumps > 0 && (isJumpsMandatory || isMaxJumpsMandatory)) {
			// remove non-jumps
			var it = moves.iterator()
			var maxDepth = 0
			while (it.hasNext()) {
				val m = it.next()
				when (m.moveType) {
					MoveType.JUMP, MoveType.FLYING_JUMP -> {
						if (isMaxJumpsMandatory) {
							m.jumpDepth = findMaxDepth(game.turn, game, m)
							maxDepth = Math.max(maxDepth, m.jumpDepth)
						} else if (m.jumped >= 0) {
							val p = game.getPiece(m.jumped)
							if (p.playerNum == m.playerNum) break // remove jumps of our own pieces
						}
						continue
					}
					else -> Unit
				}
				it.remove()
			}
			if (isMaxJumpsMandatory) {
				it = moves.iterator()
				while (it.hasNext()) {
					val m = it.next()
					if (m.jumpDepth < maxDepth) {
						it.remove()
					}
				}
			}
		}
		return moves
	}

	private fun findMaxDepth(playerNum: Int, game: Game, m: Move): Int {
		if (m.playerNum != playerNum) return 0
		game.movePiece(m)
		game.clearPiece(m.capturedPosition)
		//executeMove(game, m);
		var max = 0
		val moves: MutableList<Move> = ArrayList()
		val pos = m.end
		val rnk = pos shr 8
		val col = pos and 0xff
		val numJumps = computeMovesForSquare(game, rnk, col, m, moves)
		if (numJumps > 0) {
			for (m2 in moves) {
				when (m2.moveType) {
					MoveType.JUMP, MoveType.FLYING_JUMP -> max = Math.max(max, 1 + findMaxDepth(playerNum, game, m2))
					else -> Unit
				}
			}
		}
		reverseMove(game, m)
		return max
	}

	private fun computeMovesForSquare(game: Game, rank: Int, col: Int, parent: Move?, moves: MutableList<Move>): Int {
		val p = game.getPiece(rank, col)
		if (p.playerNum != game.turn) throw GException("Logic Error: Should not be able to move opponent piece")
		val startSize = moves.size
		var numJumps = if (p.getType().isFlying) {
			computeFlyingKingMoves(game, p, rank, col, parent, moves)
		} else {
			computeMenKingMoves(game, p, rank, col, parent, moves)
		}
		p.numMoves = moves.size - startSize
		return numJumps
	}

	override fun getWinner(game: Game): Int {
		return if (game.getMoves().size == 0) game.opponent else -1
	}

	override fun isDraw(game: Game): Boolean {
		// if down to only 2 kings, one of each color, then game is a draw
		var numNear = 0
		var numFar = 0
		for (p in game.getPieces(-1)) {
			when (p.getType()) {
				PieceType.KING, PieceType.FLYING_KING, PieceType.DAMA_KING -> if (p.playerNum == Game.NEAR && ++numNear > 1) return false else if (p.playerNum == Game.FAR && ++numFar > 1) return false
				PieceType.CHECKER, PieceType.DAMA_MAN, PieceType.CHIP_4WAY -> return false
				else                                                       -> throw GException("Unhandled case: " + p.getType())
			}
		}
		return numNear == 1 && numFar == 1
	}

	fun computeMenKingMoves(game: Game, p: Piece, rank: Int, col: Int, parent: Move?, moves: MutableList<Move>): Int {
		var numJumps = 0
		var jdr: IntArray? = null
		var jdc: IntArray? = null
		var dr: IntArray? = null
		var dc: IntArray? = null
		var pt = p.getType()
		if (pt === PieceType.CHECKER && p.isStacked) pt = PieceType.KING
		when (pt) {
			PieceType.KING -> {
				run {
					dr = PIECE_DELTAS_DIAGONALS_R
					jdr = dr
				}
				run {
					dc = PIECE_DELTAS_DIAGONALS_C
					jdc = dc
				}
			}
			PieceType.CHECKER -> {
				if (p.playerNum == Game.NEAR) {
					// negative
					dr = PIECE_DELTAS_DIAGONALS_NEAR_R
					dc = PIECE_DELTAS_DIAGONALS_NEAR_C
				} else { // red
					// positive
					dr = PIECE_DELTAS_DIAGONALS_FAR_R
					dc = PIECE_DELTAS_DIAGONALS_FAR_C
				}
				if (canMenJumpBackwards()) {
					jdr = PIECE_DELTAS_DIAGONALS_R
					jdc = PIECE_DELTAS_DIAGONALS_C
				} else {
					jdr = dr
					jdc = dc
				}
			}
			PieceType.DAMA_MAN -> {
				if (p.playerNum == Game.NEAR) {
					// negative
					dr = PIECE_DELTAS_3WAY_NEAR_R
					dc = PIECE_DELTAS_3WAY_NEAR_C
				} else { // red
					// positive
					dr = PIECE_DELTAS_3WAY_FAR_R
					dc = PIECE_DELTAS_3WAY_FAR_C
				}
				if (canMenJumpBackwards()) {
					jdr = PIECE_DELTAS_4WAY_R
					jdc = PIECE_DELTAS_4WAY_C
				} else {
					jdr = dr
					jdc = dc
				}
			}
			PieceType.CHIP_4WAY, PieceType.DAMA_KING -> {
				run {
					dr = PIECE_DELTAS_4WAY_R
					jdr = dr
				}
				run {
					dc = PIECE_DELTAS_4WAY_C
					jdc = dc
				}
			}
			else -> TODO("Unhandled piece  $p")
		}

		// check for jumps
		for (i in jdr!!.indices) {
			val rdr = rank + jdr!![i]
			val cdc = col + jdc!![i]
			val rdr2 = rank + jdr!![i] * 2
			val cdc2 = col + jdc!![i] * 2
			if (!game.isOnBoard(rdr, cdc)) continue
			if (!game.isOnBoard(rdr2, cdc2)) continue
			if (parent != null) {
				val pos = parent.start
				val srnk = pos shr 8
				val scol = pos and 0xff
				if (rdr2 == srnk && cdc2 == scol) continue  // cannot make 180 degree turns
			}
			val cap = game.getPiece(rdr, cdc)
			val t = game.getPiece(rdr2, cdc2)
			if (t.getType() !== PieceType.EMPTY) continue
			if (canJumpSelf() && cap.playerNum == game.turn) {
				moves.add(
					Move(MoveType.JUMP, game.turn)
						.setStart(rank, col, p.getType())
						.setEnd(rdr2, cdc2, p.getType())
						.setJumped(rdr, cdc)
				)
				//numJumps++;
			} else if (!cap.isCaptured && cap.playerNum == game.opponent) {
				val mv = Move(MoveType.JUMP, game.turn)
					.setStart(rank, col, p.getType())
					.setEnd(rdr2, cdc2, p.getType())
					.setJumped(rdr, cdc)
				if (!isNoCaptures) mv.setCaptured(rdr, cdc, cap.getType())
				moves.add(mv)
				numJumps++
			}
		}

		// check for slides
		if (parent == null && !(isJumpsMandatory && p.numMoves > 0)) {
			for (i in dr!!.indices) {
				val rdr = rank + dr!![i]
				val cdc = col + dc!![i]
				if (!game.isOnBoard(rdr, cdc)) continue
				// t is piece one unit away in this direction
				val t = game.getPiece(rdr, cdc) ?: throw GException("Null piece at [$rdr,$cdc]")
				if (t.getType() === PieceType.EMPTY) {
					moves.add(Move(MoveType.SLIDE, game.turn).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType()))
					//new Move(MoveType.SLIDE, rank, col, rdr, cdc, getTurn()));
				}
			}
		}
		return numJumps
	}

	/*
    flying kings move any distance along unblocked diagonals, and may capture an opposing man any distance away
    by jumping to any of the unoccupied squares immediately beyond it.

    There are 2 cases to consider determined by: isCaptureAtEndEnabled()
    1> (Simple) Pieces are removed from the board as they are jumped or
    2> (Complex) Jumped pieces remain on the board until the turn is complete, in which case,
       it is possible to reach a position in a multi-jump move where the flying king is blocked
       from capturing further by a piece already jumped.
    */
	fun computeFlyingKingMoves(game: Game, p: Piece, rank: Int, col: Int, parent: Move?, moves: MutableList<Move>): Int {
		var numJumps = 0
		val d = Math.max(game.ranks, game.columns)
		val dr: IntArray
		val dc: IntArray
		when (p.getType()) {
			PieceType.FLYING_KING -> {
				dr = PIECE_DELTAS_DIAGONALS_R
				dc = PIECE_DELTAS_DIAGONALS_C
			}
			PieceType.DAMA_KING -> {
				dr = PIECE_DELTAS_4WAY_R
				dc = PIECE_DELTAS_4WAY_C
			}
			else                  -> throw GException("Unhandled case")
		}
		for (i in 0..3) {
			var mt = MoveType.SLIDE
			var captured: Piece? = null
			var capturedRank = 0
			var capturedCol = 0
			var ii = 1
			if (parent != null) {
				// we are in a multijump, so search forward for the piece to capture
				val pos = parent.start
				val srnk = pos shr 8
				val scol = pos and 0xff
				val ddr = CMath.signOf((srnk - rank).toFloat())
				val ddc = CMath.signOf((scol - col).toFloat())
				if (ddr == dr[i] && ddc == dc[i]) continue  // cannot go backwards
				while (ii <= d) {

					// square we are moving too
					val rdr = rank + dr[i] * ii
					val cdc = col + dc[i] * ii
					if (!game.isOnBoard(rdr, cdc)) break
					val t = game.getPiece(rdr, cdc)
					if (t.getType() === PieceType.EMPTY) {
						ii++
						continue
					}
					if (t.isCaptured) break // cannot jump a piece we already did
					if (t.playerNum == game.opponent) {
						captured = t
						ii++
						capturedRank = rdr
						capturedCol = cdc
					}
					break
					ii++
				}
				if (captured == null) continue
				mt = MoveType.FLYING_JUMP
			}
			while (ii < d) {


				// square we are moving too
				val rdr = rank + dr[i] * ii
				val cdc = col + dc[i] * ii
				if (!game.isOnBoard(rdr, cdc)) break

				// t is piece one unit away in this direction
				val t = game.getPiece(rdr, cdc)
				if (t.getType() === PieceType.EMPTY) {
					if (captured == null) moves.add(Move(mt, game.turn).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType())) else moves.add(Move(mt, game.turn).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType()).setCaptured(capturedRank, capturedCol, captured.getType()))
					if (mt == MoveType.FLYING_JUMP) numJumps++
					ii++
					continue
				}
				if (mt != MoveType.SLIDE) break
				if (t.playerNum != game.opponent) break
				mt = MoveType.FLYING_JUMP
				captured = t
				capturedRank = rdr
				capturedCol = cdc
				ii++
			}
		}
		return numJumps
	}

	override fun executeMove(game: Game, move: Move) {
		if (move.playerNum != game.turn) throw GException()
		var isKinged = false
		var isDamaKing = false
		var p = game.getPiece(move.start)
		// clear everyone all moves
		val epos = move.end
		val ernk = epos shr 8
		val ecol = epos and 0xff
		if (move.hasEnd()) {
			if (isKingPieces) {
				isKinged = !p.isStacked && p.getType() === PieceType.CHECKER && game.getStartRank(game.opponent) == ernk
				isDamaKing = p.getType() === PieceType.DAMA_MAN && game.getStartRank(game.opponent) == ernk
			}
			p = game.movePiece(move)
		}
		when (move.moveType) {
			MoveType.SLIDE -> {
				if (isKinged) {
					game.getMovesInternal().add(Move(MoveType.STACK, move.playerNum).setStart(ernk, ecol, p.getType()).setEnd(ernk, ecol, if (isFlyingKings) PieceType.FLYING_KING else PieceType.KING))
					return
				}
				if (isDamaKing) {
					game.getMovesInternal().add(Move(MoveType.STACK, move.playerNum).setStart(ernk, ecol, p.getType()).setEnd(ernk, ecol, PieceType.DAMA_KING))
					return
				}
				endTurnPrivate(game)
				return
			}
			MoveType.END -> {
				endTurnPrivate(game)
				return
			}
			MoveType.FLYING_JUMP, MoveType.JUMP -> {
				if (move.hasCaptured()) {
					if (isStackingCaptures) {
						val capturing = game.getPiece(move.end)
						val captured = game.getPiece(move.capturedPosition)
						// capturing end stack becomes start stack
						capturing.addStackBottom(captured.playerNum)
						if (!captured.isStacked) {
							game.clearPiece(captured.position)
						} else {
							captured.removeStackTop()
							game.countPieces()
						}
						captured.isCaptured = false
					} else if (isCaptureAtEndEnabled) {
						game.getPiece(move.capturedPosition).isCaptured = true
					} else {
						game.clearPiece(move.capturedPosition)
					}
				}
				if (isKinged) {
					game.getMovesInternal().add(Move(MoveType.STACK, move.playerNum).setStart(ernk, ecol, p.getType()).setEnd(ernk, ecol, if (isFlyingKings) PieceType.FLYING_KING else PieceType.KING))
				} else if (isDamaKing) {
					game.getMovesInternal().add(Move(MoveType.STACK, move.playerNum).setStart(ernk, ecol, p.getType()).setEnd(ernk, ecol, PieceType.DAMA_KING))
				} else {
					computeMovesForSquare(game, ernk, ecol, move, game.getMovesInternal())
					p = game.getPiece(ernk, ecol)
					if (p.numMoves == 0) {
						endTurnPrivate(game)
					} else if (!isJumpsMandatory) {
						game.getMovesInternal().add(Move(MoveType.END, move.playerNum).setStart(ernk, ecol, p.getType()))
					}
				}
			}
			MoveType.STACK -> {
				game.getPiece(move.start).setType(move.endType!!)
				if (game.getPreviousMove(move.playerNum)?.hasCaptured() == true) {
					// we cannot king if we can still jump.
					computeMovesForSquare(game, ernk, ecol, move, game.getMovesInternal())
					val pp = game.getPiece(move.end)
					if (pp.numMoves == 0) {
						endTurnPrivate(game)
					} else if (!isJumpsMandatory) {
						game.getMovesInternal().add(Move(MoveType.END, move.playerNum).setStart(ernk, ecol, p.getType()))
					}
				} else {
					endTurnPrivate(game)
				}
			}
			else -> TODO("Unhandled piece ${move.moveType}")
		}
	}

	fun endTurnPrivate(game: Game) {
		val captured: MutableList<Int> = ArrayList()
		if (!isNoCaptures) {
			for (i in 0 until game.ranks) {
				for (ii in 0 until game.columns) {
					val p = game.getPiece(i, ii)
					if (p.isCaptured) {
						captured.add(p.position)
					}
				}
			}
			if (isCaptureAtEndEnabled && captured.size > 0) {
				for (pos in captured) {
					game.clearPiece(pos)
				}
			}
		}
		game.nextTurn()
	}

	override fun getPlayerColor(side: Int): Color {
		when (side) {
			Game.FAR -> return Color.RED
			Game.NEAR -> return Color.BLACK
		}
		return Color.WHITE
	}

	override fun evaluate(game: Game): Long {
		var value: Long = 0
//		if (move.hasCaptured()) value += 2
		for (p in game.getPieces(-1)) {
			val scale = if (p.playerNum == game.turn) 1 else -1
			value += when (p.getType()) {
				PieceType.CHECKER, PieceType.DAMA_MAN -> (2 * scale).toLong()
				PieceType.KING -> (10 * scale).toLong()
				PieceType.FLYING_KING, PieceType.DAMA_KING -> (50 * scale).toLong()
				else                                       -> throw GException("Unhandled case '" + p.getType() + "'")
			}
		}
		return value
	}

	override fun reverseMove(game: Game, m: Move) {
		val p: Piece
		when (m.moveType) {
			MoveType.END -> {
			}
			MoveType.SLIDE, MoveType.FLYING_JUMP, MoveType.JUMP -> {
				p = game.getPiece(m.end)
				if (m.hasCaptured()) {
					if (isStackingCaptures) {
						val captured = game.getPiece(m.capturedPosition)
						if (!p.isStacked) throw GException("Logic Error: Capture must result in stacked piece")
						if (captured.getType() === PieceType.EMPTY) {
							captured.playerNum = p.removeStackBottom()
							captured.setType(PieceType.CHECKER)
						} else {
							captured.addStackTop(p.removeStackBottom())
						}
						captured.isCaptured = false
						game.countPieces()
					} else {
						val oppNum = Game.getOpponent(m.playerNum)
						if (isCaptureAtEndEnabled) {
							val pc = game.getPiece(m.capturedPosition)
							if (pc.getType().flag == 0)
								game.setPiece(m.capturedPosition, oppNum, m.capturedType!!) else pc.isCaptured = false
							//game.getPiece(m.getLastCaptured().getPosition()).setCaptured(false);
							// iterate backward through the move history and uncapture the pieces while
							// keeping their 'captured' flag true
							for (o in game.moveHistory) {
								if (o.playerNum == oppNum) break
								if (o.hasCaptured()) {
									game.setPiece(o.capturedPosition, oppNum, o.capturedType!!).isCaptured = true
								}
							}
						} else {
							game.setPiece(m.capturedPosition, oppNum, m.capturedType!!)
						}
					}
				}
				game.copyPiece(m.end, m.start)
				game.clearPiece(m.end)
			}
			MoveType.STACK -> game.getPiece(m.start).setType(m.startType!!)
			else -> throw GException("Unhandled case '" + m.moveType + "'")
		}
		game.turn = m.playerNum
	}

	open fun canJumpSelf(): Boolean {
		return true // true for traditional checkers
	}

	open fun canMenJumpBackwards(): Boolean {
		return false // true for international/russian draughts
	}

	/**
	 * Men/King must jump when possible
	 * @return
	 */
	open val isJumpsMandatory: Boolean
		get() = false

	/**
	 * Men/King must take moves that lead to most jumps
	 * @return
	 */
	open val isMaxJumpsMandatory: Boolean
		get() = false
	open val isCaptureAtEndEnabled: Boolean
		get() = false
	open val isFlyingKings: Boolean
		get() = false
	open val isNoCaptures: Boolean
		get() = false
	open val isKingPieces: Boolean
		get() = true
	open val isStackingCaptures: Boolean
		get() = false
	override val instructions: Table
		get() {
			val tab = Table()
				.addRow("Can Jump Self", canJumpSelf())
				.addRow("Men Jump Backwards", canMenJumpBackwards())
				.addRow("Must Jump When Possible", isJumpsMandatory)
				.addRow("Must make maximum Jumps", isMaxJumpsMandatory)
				.addRow("Flying Kings", isFlyingKings)
				.addRow("Captures at the end", isCaptureAtEndEnabled)
			return Table("Classic game of " + javaClass.simpleName)
				.addRow(description)
				.addRow(tab)
		}
	open val description: String?
		get() = "Objective to capture all other player's pieces or prevent them from bieing able to move."

	companion object {
		private val PIECE_DELTAS_DIAGONALS_R = intArrayOf(1, 1, -1, -1)
		private val PIECE_DELTAS_DIAGONALS_C = intArrayOf(-1, 1, -1, 1)
		private val PIECE_DELTAS_DIAGONALS_NEAR_R = intArrayOf(-1, -1)
		private val PIECE_DELTAS_DIAGONALS_NEAR_C = intArrayOf(-1, 1)
		private val PIECE_DELTAS_DIAGONALS_FAR_R = intArrayOf(1, 1)
		private val PIECE_DELTAS_DIAGONALS_FAR_C = intArrayOf(-1, 1)
		private val PIECE_DELTAS_4WAY_R = intArrayOf(1, -1, 0, 0)
		private val PIECE_DELTAS_4WAY_C = intArrayOf(0, 0, -1, 1)
		private val PIECE_DELTAS_3WAY_NEAR_R = intArrayOf(-1, 0, 0)
		private val PIECE_DELTAS_3WAY_NEAR_C = intArrayOf(0, -1, 1)
		private val PIECE_DELTAS_3WAY_FAR_R = intArrayOf(1, 0, 0)
		private val PIECE_DELTAS_3WAY_FAR_C = intArrayOf(0, -1, 1)
	}
}