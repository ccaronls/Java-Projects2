package cc.lib.checkerboard

import cc.lib.reflector.Omit
import cc.lib.utils.GException
import cc.lib.utils.Table

import cc.lib.utils.flipCoin
import java.util.*

/**
 * Created by chriscaron on 10/10/17.
 */
open class Chess : Rules() {
	companion object {
		const val DELTAS_N = 0
		const val DELTAS_S = 1
		const val DELTAS_E = 2
		const val DELTAS_W = 3
		const val DELTAS_NE = 4
		const val DELTAS_NW = 5
		const val DELTAS_SE = 6
		const val DELTAS_SW = 7
		const val DELTAS_KNIGHT = 8
		const val DELTAS_KING = 9
		const val NUM_DELTAS = 10

		init {
			addAllFields(Chess::class.java)
		}
	}

	@JvmField
    protected var whiteSide = -1
	private var timerLength: Long = 0
	private var timerFar: Long = 0
	private var timerNear: Long = 0

	@Omit
	private var startTimeMS: Long = 0
	@Omit
	private var cachesInitialized = false

	fun setTimer(seconds: Int) {
		timerNear = (seconds * 1000).toLong()
		timerFar = timerNear
		timerLength = timerFar
		startTimeMS = 0
	}

	fun timerTick(game: Game, uptimeMillis: Long) {
		if (startTimeMS <= 0) startTimeMS = uptimeMillis
		val dt = uptimeMillis - startTimeMS
		when (game.turn) {
			Game.FAR -> timerFar -= dt
			Game.NEAR -> timerNear -= dt
		}
		startTimeMS = uptimeMillis
	}

	fun getTimerLength(): Int {
		return (timerLength / 1000).toInt()
	}

	fun getTimerFar(): Int {
		return (timerFar / 1000).toInt()
	}

	fun getTimerNear(): Int {
		return (timerNear / 1000).toInt()
	}

	fun isTimerExpired(game: Game): Boolean {
		if (timerLength <= 0) return false
		if (game.turn == Game.FAR && timerFar <= 0) return true
		return if (game.turn == Game.NEAR && timerNear <= 0) true else false
	}

	override fun init(game: Game): Array<Array<Piece>> {
		whiteSide = if (flipCoin()) Game.FAR else Game.NEAR
		// this is to enforce the 'queen on her own color square' rule
		var left = PieceType.QUEEN
		var right = PieceType.UNCHECKED_KING_IDLE
		if (whiteSide == Game.FAR) {
			right = PieceType.QUEEN
			left = PieceType.UNCHECKED_KING_IDLE
		}
		game.turn = whiteSide
		return initFromPieceTypes(arrayOf(
			Game.FAR  to arrayOf(PieceType.ROOK_IDLE, PieceType.KNIGHT_R, PieceType.BISHOP, left, right, PieceType.BISHOP, PieceType.KNIGHT_L, PieceType.ROOK_IDLE),
			Game.FAR  to arrayOf(PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE),
			Game.NOP  to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP  to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP  to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP  to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NEAR to arrayOf(PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE, PieceType.PAWN_IDLE),
			Game.NEAR to arrayOf(PieceType.ROOK_IDLE, PieceType.KNIGHT_R, PieceType.BISHOP, left, right, PieceType.BISHOP, PieceType.KNIGHT_L, PieceType.ROOK_IDLE)
		))
	}

	override fun isDraw(game: Game): Boolean {
		// in chess, draw game if only 2 kings left or current player cannot move but is not in check
		// if down to only 2 kings, one of each color, then game is a draw. Also a king and bishop alone cannot checkmate
		var numBishops = 0
		var numPieces = 0
		val noMoves = game.getMoves().size == 0
		var inCheck = false
		for (p in game.getPieces(-1)) {
			when (p.getType()) {
				PieceType.CHECKED_KING, PieceType.CHECKED_KING_IDLE -> if (p.playerNum == game.turn) inCheck = true
				PieceType.BISHOP -> numBishops++
				PieceType.UNCHECKED_KING, PieceType.UNCHECKED_KING_IDLE, PieceType.EMPTY -> {
				}
				else -> numPieces++
			}
		}
		if (noMoves && !inCheck) return true
		if (noMoves) return false
		return if (numPieces > 0) false else numBishops < 2
	}

	override fun getWinner(game: Game): Int {
		if (game.getMoves().isEmpty()) {
			val p = findKing(game, game.turn)
			when (p!!.getType()) {
				PieceType.CHECKED_KING, PieceType.CHECKED_KING_IDLE -> return Game.getOpponent(p.playerNum)
				else -> TODO("Unhandled type ${p.getType()}")
			}
		}
		return -1
	}

	override fun executeMove(game: Game, move: Move) {
		val previous = game.getPreviousMove(game.turn)
		executeMoveInternal(game, move)
		findKing(game, move.playerNum)!!.setChecked(false)
		if (game.hasNoMoreMoves()) game.nextTurn()
		if (previous != null && previous.endType === PieceType.PAWN_ENPASSANT && game.getPiece(previous.end).getType() === PieceType.PAWN_ENPASSANT) {
			game.getPiece(previous.end).setType(PieceType.PAWN)
			move.enpassant = previous.end
		}
	}

	private fun executeMoveInternal(game: Game, move: Move) {
		var p: Piece? = null
		try {
			when (move.moveType) {
				MoveType.END ->                     // does this ever get called?
					println("!!!!!! I GOT CALLED !!!!!!!!!!!!!!!")
				MoveType.JUMP, MoveType.SLIDE -> {
					if (move.hasCaptured()) {
						game.clearPiece(move.capturedPosition)
					}
					p = game.movePiece(move)
					// check for pawn advancing
					if (p.getType() === PieceType.PAWN_TOSWAP) {
						val pos = move.end
						computeMovesForSquare(game, pos shr 8, pos and 0xff, null, game.getMovesInternal())
						return
					}
				}
				MoveType.SWAP -> game.getPiece(move.start).setType(move.endType!!)
				MoveType.CASTLE -> {
					game.movePiece(move)
					p = game.getPiece(move.castleRookStart)
					if (!p.getType().canCastleWith) {
						throw GException("Expected castleabel piece")
					}
					game.setPiece(move.castleRookEnd, move.playerNum, p.getType().nonIdled)
					game.clearPiece(move.castleRookStart)
				}
				else                          -> throw GException()
			}
		} finally {
			if (move.hasOpponentKing()) game.getPiece(move.opponentKingPos).setType(move.opponentKingTypeEnd!!)
		}
		if (p != null && timerLength > 0) {
			throw GException("I dont understand this logic")
			//game.getMovesInternal().add(new Move(MoveType.END, p.getPlayerNum()));
		}
	}

	// Return true if p.getType() is on set of types and p.getPlayerNum() equals playerNum
	fun testPiece(game: Game, rank: Int, col: Int, playerNum: Int, flag: Int): Boolean {
		//if (!game.isOnBoard(rank, col))
		//    return false;
		val p = game.board[rank][col] //rank, col);
		return p.playerNum == playerNum && p.getType().flag and flag != 0
	}

	/**
	 * Return true if playerNum is attacking the position
	 * @param rank
	 * @param col
	 * @param attacker
	 * @return
	 */
	protected open fun isSquareAttacked(game: Game, rank: Int, col: Int, attacker: Int): Boolean {
		val knightDeltas = pieceDeltas[DELTAS_KNIGHT]

		// search in the eight directions and knights whom can
		var kd = knightDeltas[rank][col]
		var dr = kd[0]
		var dc = kd[1]
		for (i in dr.indices) {
			val rr = rank + dr[i]
			val cc = col + dc[i]
			if (testPiece(game, rr, cc, attacker, PieceType.FLAG_KNIGHT)) {
				return true
			}
		}
		val adv = game.getAdvanceDir(Game.getOpponent(attacker))
		// look for pawns
		if (game.isOnBoard(rank + adv, col + 1) && testPiece(game, rank + adv, col + 1, attacker, PieceType.FLAG_PAWN)) return true
		if (game.isOnBoard(rank + adv, col - 1) && testPiece(game, rank + adv, col - 1, attacker, PieceType.FLAG_PAWN)) return true

//        int [][][] kdn = new int[8][][];
		var kn = 0

		// fan out in all eight directions looking for a opponent king
		kd = pieceDeltas[DELTAS_KING][rank][col]
		dr = kd[0] //pieceDeltas[UNCHECKED_KING.ordinal()];///NSEW_DIAG_DELTAS[0];
		dc = kd[1] //NSEW_DIAG_DELTAS[1];
		for (i in dr.indices) {
			val rr = rank + dr[i]
			val cc = col + dc[i]
			if (testPiece(game, rr, cc, attacker, PieceType.FLAG_KING)) return true
		}
		kn = computeKDN(rank, col, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
		for (k in 0 until kn) {
			kd = kdn[k]
			dr = kd[0]
			dc = kd[1]
			for (i in dr.indices) {
				val rr = rank + dr[i]
				val cc = col + dc[i]
				if (testPiece(game, rr, cc, attacker, PieceType.FLAG_ROOK_OR_QUEEN)) return true
				if (game.getPiece(rr, cc).getType() !== PieceType.EMPTY) break
			}
		}
		kn = computeKDN(rank, col, DELTAS_NE, DELTAS_SE, DELTAS_NW, DELTAS_SW)
		for (k in 0 until kn) {
			kd = kdn[k]
			dr = kd[0] //DIAGONAL_DELTAS[0];
			dc = kd[1] //DIAGONAL_DELTAS[1];
			// search DIAGonals for bishop, queen
			for (i in dr.indices) {
				val rr = rank + dr[i]
				val cc = col + dc[i]
				if (testPiece(game, rr, cc, attacker, PieceType.FLAG_BISHOP_OR_QUEEN)) return true
				if (game.getPiece(rr, cc).getType() !== PieceType.EMPTY) break
			}
		}
		return false
	}

	private fun checkForCastle(game: Game, rank: Int, kingCol: Int, rookCol: Int, moves: MutableList<Move>) {
		val king = game.getPiece(rank, kingCol)
		val rook = game.getPiece(rank, rookCol)
		if (king.getType() !== PieceType.UNCHECKED_KING_IDLE) return
		if (!rook.getType().canCastleWith) return
		// check that there are no places in between king and rook and also none of the square is attacked
		val kingEndCol: Int
		val rookEndCol: Int
		val opponent = Game.getOpponent(game.turn)
		if (rookCol > kingCol) {
			for (i in kingCol + 1 until rookCol) {
				var p: Piece?
				if (game.getPiece(rank, i).also { p = it }.getType() !== PieceType.EMPTY) return
				if (isSquareAttacked(game, rank, i, opponent)) return
			}
			kingEndCol = kingCol + 2
			rookEndCol = kingEndCol - 1
		} else {
			// long side castle
			for (i in rookCol + 1 until kingCol) {
				if (game.getPiece(rank, i).getType() !== PieceType.EMPTY) return
				if (isSquareAttacked(game, rank, i, opponent)) return
			}
			kingEndCol = kingCol - 2
			rookEndCol = kingEndCol + 1
		}
		moves.add(Move(MoveType.CASTLE, king.playerNum)
			.setStart(rank, kingCol, PieceType.UNCHECKED_KING_IDLE)
			.setEnd(rank, kingEndCol, PieceType.UNCHECKED_KING)
			.setCastle(rank, rookCol, rank, rookEndCol))
	}

	override fun computeMoves(game: Game): List<Move> {
		val moves: MutableList<Move> = ArrayList()
		for (p in game.getPieces(game.turn)) {
			computeMovesForSquare(game, p.rank, p.col, null, moves)
		}
		return moves
	}

	lateinit var castleRookCols: IntArray

	open fun getRookCastleCols(game: Game) : IntArray = intArrayOf(0, game.columns - 1)

    @Omit
	lateinit var kdn: Array<Array<IntArray>>
	fun computeKDN(rank: Int, col: Int, vararg which: Int): Int {
		kdn = Array(which.size) {
			if (pieceDeltas[which[it]][rank][col].size > 0)
				pieceDeltas[which[it]][rank][col]
			else
				emptyArray()

		}
		var n = 0
		for (w in which) {
			if (pieceDeltas[w][rank][col].size > 0)
				kdn[n++] = pieceDeltas[w][rank][col]
		}
		return n
	}

	private fun computeMovesForSquare(game: Game, rank: Int, col: Int, parent: Move?, moves: MutableList<Move>) {
		if (!cachesInitialized) {
			initCaches(game)
			cachesInitialized = true
		}
		val startNumMoves = moves.size
		val p = game.getPiece(rank, col)
		var tr: Int
		var tc: Int
		var tp: Piece
		val opponent = Game.getOpponent(p.playerNum)
		var dr: IntArray? = null
		var dc: IntArray? = null
		var kd: Array<IntArray>? = null
		var kn = 0
		var d = Math.max(game.ranks, game.columns)
		var mt = MoveType.SLIDE
		var nextType = p.getType()
		when (p.getType()) {
			PieceType.PAWN_ENPASSANT, PieceType.PAWN_IDLE, PieceType.PAWN -> {

				// check in front of us 1 space
				tr = rank + game.getAdvanceDir(p.playerNum)
				tc = col
				nextType = if (tr == game.getStartRank(opponent)) {
					PieceType.QUEEN // For simplicity, just make the pawn a queen now. PAWN_TOSWAP;
				} else {
					PieceType.PAWN
				}
				if (game.isOnBoard(tr, col) && game.getPiece(tr, col).getType() === PieceType.EMPTY) {
					moves.add(Move(MoveType.SLIDE, p.playerNum).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType))
					if (p.getType() === PieceType.PAWN_IDLE) {
						val tr2 = rank + game.getAdvanceDir(p.playerNum) * 2
						// if we have not moved yet then we may be able move 2 squares
						if (game.getPiece(tr2, col).getType() === PieceType.EMPTY) {
							moves.add(Move(MoveType.SLIDE, p.playerNum).setStart(rank, col, p.getType()).setEnd(tr2, tc, PieceType.PAWN_ENPASSANT))
						}
					}
				}
				val enpassantRank = game.getStartRank(opponent) + 3 * game.getAdvanceDir(opponent)
				// check if we can capture to upper right
				if (game.isOnBoard(tr, col + 1.also { tc = it })) {
					tp = game.getPiece(tr, tc)
					if (tp.playerNum == opponent) {
						// if this opponent is the king, then we will be 'checking' him
						moves.add(Move(MoveType.SLIDE, p.playerNum).setStart(rank, col, p.getType()).setCaptured(tr, tc, tp.getType()).setEnd(tr, tc, nextType))
					} else if (rank == enpassantRank && game.getPiece(rank, tc).also { tp = it }.getType() === PieceType.PAWN_ENPASSANT) {
						// check en passant
						moves.add(Move(MoveType.SLIDE, p.playerNum).setStart(rank, col, p.getType()).setCaptured(rank, tc, tp.getType()).setEnd(tr, tc, nextType))
					}
				}
				// check if we can capture to upper left
				if (game.isOnBoard(tr, col - 1.also { tc = it })) {
					tp = game.getPiece(tr, tc)
					if (tp.playerNum == opponent) {
						moves.add(Move(MoveType.SLIDE, p.playerNum).setStart(rank, col, p.getType()).setCaptured(tr, tc, tp.getType()).setEnd(tr, tc, nextType))
					} else if (rank == enpassantRank && game.getPiece(rank, tc).also { tp = it }.getType() === PieceType.PAWN_ENPASSANT) {
						// check enpassant
						moves.add(Move(MoveType.SLIDE, p.playerNum).setStart(rank, col, p.getType()).setCaptured(rank, tc, tp.getType()).setEnd(tr, tc, PieceType.PAWN))
					}
				}
			}
			PieceType.PAWN_TOSWAP -> {

				// see if we have one of our knights?
				for (np in Arrays.asList(PieceType.ROOK, PieceType.KNIGHT_R, PieceType.KNIGHT_L, PieceType.BISHOP, PieceType.QUEEN)) { // TODO: Have option to only allow from pieces already captured
					moves.add(Move(MoveType.SWAP, p.playerNum).setStart(rank, col, p.getType()).setEnd(rank, col, np))
				}
				return
			}
			PieceType.BISHOP -> kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW)
			PieceType.KNIGHT_L, PieceType.KNIGHT_R -> {
				kdn[kn++] = pieceDeltas[DELTAS_KNIGHT][rank][col]
				mt = MoveType.JUMP
			}
			PieceType.ROOK_IDLE -> {
				nextType = PieceType.ROOK
				kn = computeKDN(rank, col, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
			}
			PieceType.ROOK -> kn = computeKDN(rank, col, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
			PieceType.DRAGON_IDLE_L -> {
				nextType = PieceType.DRAGON_L
				kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
				d = 3
			}
			PieceType.DRAGON_L -> {
				kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
				d = 3
			}
			PieceType.DRAGON_IDLE_R -> {
				nextType = PieceType.DRAGON_R
				kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
				d = 3
			}
			PieceType.DRAGON_R -> {
				kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
				d = 3
			}
			PieceType.UNCHECKED_KING_IDLE -> {
				for (n in castleRookCols)
					checkForCastle(game, rank, col, n, moves)
				nextType = PieceType.UNCHECKED_KING
				kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
				d = 1
			}
			PieceType.CHECKED_KING_IDLE, PieceType.UNCHECKED_KING, PieceType.CHECKED_KING -> {
				nextType = PieceType.UNCHECKED_KING
				kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
				d = 1
			}
			PieceType.QUEEN -> kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W)
			else                                                                          -> throw GException("Unknown pieceType " + p.getType())
		}
		for (k in 0 until kn) {
			kd = kdn[k]
			dr = kd[0]
			dc = kd[1]
			if (dr.size != dc.size) throw GException()
			val n = Math.min(d, dr.size)
			for (i in 0 until n) {
				// search max d units in a specific direction
				tr = rank + dr[i]
				tc = col + dc[i]
				tp = game.getPiece(tr, tc)
				if (tp.playerNum == opponent) { // look for capture
					moves.add(Move(mt, p.playerNum).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType).setCaptured(tr, tc, tp.getType()))
				} else if (tp.getType() === PieceType.EMPTY) { // look for open
					moves.add(Move(mt, p.playerNum).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType))
					continue
				}
				if (mt == MoveType.SLIDE) break
			}
		}

		// now search moves and remove any that cause our king to be checked
		if (moves.size > startNumMoves) {
			val opponentKing = findKing(game, opponent) ?: throw NullPointerException()
			val opponentKingStartType = opponentKing.getType()
			val it = moves.iterator()
			var num = 0
			while (it.hasNext()) {
				val m = it.next()
				if (num++ < startNumMoves) continue
				if (!m.hasEnd()) continue
				executeMoveInternal(game, m)
				do {
					if (m.moveType != MoveType.SWAP) {
						val king = findKing(game, game.turn)
						if (isSquareAttacked(game, king!!.rank, king.col, opponent)) {
							it.remove()
							break
						}
					}
					var opponentKingEndType = opponentKingStartType
					val attacked = isSquareAttacked(game, opponentKing.rank, opponentKing.col, m.playerNum)
					when (opponentKingStartType) {
						PieceType.CHECKED_KING -> if (!attacked) opponentKingEndType = PieceType.UNCHECKED_KING
						PieceType.CHECKED_KING_IDLE -> if (!attacked) opponentKingEndType = PieceType.UNCHECKED_KING_IDLE
						PieceType.UNCHECKED_KING -> if (attacked) opponentKingEndType = PieceType.CHECKED_KING
						PieceType.UNCHECKED_KING_IDLE -> if (attacked) opponentKingEndType = PieceType.CHECKED_KING_IDLE
						else                          -> throw GException("Unhandled case:$opponentKingStartType")
					}
					m.setOpponentKingType(opponentKing.rank, opponentKing.col, opponentKingStartType, opponentKingEndType)
				} while (false)
				reverseMove(game, m)
			}
		}
	}

	@Omit
	private val kingCache = arrayOfNulls<Piece>(2)
	private fun findKing(game: Game, playerNum: Int): Piece {
		kingCache[playerNum]?.let {
			if (0 != it.getType().flag and PieceType.FLAG_KING)
				return it
		}
		for (p in game.getPieces(playerNum)) {
			if (p.getType().flag and PieceType.FLAG_KING != 0) {
				kingCache[playerNum] = p
				return p
			}
		}
		throw GException("Logic Error: Cannot find king for player $playerNum")
	}

	@Omit
	lateinit var pieceDeltas: Array<Array<Array<Array<IntArray>>>>

	fun initCaches(game: Game) {
		castleRookCols = getRookCastleCols(game)
		pieceDeltas = Array(NUM_DELTAS) {
			when (it) {
				DELTAS_KNIGHT -> computeKnightDeltas(game)
				DELTAS_N -> computeDeltas(game, -1, 0)
				DELTAS_S -> computeDeltas(game, 1, 0)
				DELTAS_E -> computeDeltas(game, 0, 1)
				DELTAS_W -> computeDeltas(game, 0, -1)
				DELTAS_NE -> computeDeltas(game, -1, 1)
				DELTAS_NW -> computeDeltas(game, -1, -1)
				DELTAS_SE -> computeDeltas(game, 1, 1)
				DELTAS_SW -> computeDeltas(game, 1, -1)
				DELTAS_KING -> computeQueenDeltas(game, 1)
				else -> throw GException("Unhandled case")
			}
		}
	}

	fun computeDeltas(game: Game, dr: Int, dc: Int): Array<Array<Array<IntArray>>> {
		val ranks = game.ranks
		val cols = game.columns
		val deltas = Array(ranks) { i ->
			Array(cols) { ii ->
				computeDeltaFor(game, i, ii, dr, dc)
			}
		}
		return deltas
	}

	private fun computeDeltaFor(game: Game, rank: Int, col: Int, dr: Int, dc: Int): Array<IntArray> {
		val max = Math.max(game.ranks, game.columns)
		val d = Array(2) { IntArray(max) }
		var n = 0
		for (i in 1 until max) {
			val r = rank + dr * i
			val c = col + dc * i
			if (!game.isOnBoard(r, c)) break
			d[0][n] = dr * i
			d[1][n] = dc * i
			n++
		}

//        Utils.assertTrue(n > 0);
		var t = d[0]
		d[0] = IntArray(n)
		System.arraycopy(t, 0, d[0], 0, n)
		t = d[1]
		d[1] = IntArray(n)
		System.arraycopy(t, 0, d[1], 0, n)
		return d
	}

	fun computeBishopDeltas(game: Game): Array<Array<Array<IntArray>>> {
		val DIAGONAL_DELTAS = arrayOf(
			intArrayOf(-1, -1, 1, 1),
			intArrayOf(-1, 1, -1, 1)
		)
		val ranks = game.ranks
		val cols = game.columns
		val deltas = Array(ranks) { i ->
			Array(cols) { ii ->
				computeDeltaFor(game, i, ii, DIAGONAL_DELTAS, Math.max(ranks, cols))
			}
		}
		return deltas
	}

	fun computeRookDeltas(game: Game): Array<Array<Array<IntArray>>> {
		val ranks = game.ranks
		val cols = game.columns
		val NSEW_DELTAS = arrayOf(
			intArrayOf(1, 0, -1, 0),
			intArrayOf(0, 1, 0, -1)
		)
		val deltas = Array(ranks) { i ->
			Array(cols) { ii ->
				computeDeltaFor(game, i, ii, NSEW_DELTAS, Math.max(ranks, cols))
			}
		}
		return deltas
	}

	fun computeQueenDeltas(game: Game, max: Int): Array<Array<Array<IntArray>>> {
		val ranks = game.ranks
		val cols = game.columns
		val NSEW_DIAG_DELTAS = arrayOf(
			intArrayOf(1, 0, -1, 0, -1, -1, 1, 1),
			intArrayOf(0, 1, 0, -1, -1, 1, -1, 1)
		)
		val deltas = Array(ranks) { i ->
			Array(cols) { ii ->
				if (game.getPiece(i, ii).getType() !== PieceType.BLOCKED)
					computeDeltaFor(game, i, ii, NSEW_DIAG_DELTAS, max)
				else
					emptyArray()
			}
		}
		return deltas
	}

	fun computeKnightDeltas(game: Game): Array<Array<Array<IntArray>>> {
		val ALL_KNIGHT_DELTAS =
			arrayOf(intArrayOf(-2, -2, -1, 1, 2, 2, 1, -1),
					intArrayOf(-1, 1, 2, 2, 1, -1, -2, -2))
		val ranks = game.ranks
		val cols = game.columns
		val deltas = Array(ranks) { i ->
			Array(cols) { ii ->
				if (game.getPiece(i, ii).getType() !== PieceType.BLOCKED)
					computeDeltaFor(game, i, ii, ALL_KNIGHT_DELTAS, 1)
				else
					emptyArray()
			}
		}
		return deltas
	}

	private fun computeDeltaFor(game: Game, rank: Int, col: Int, ALL_DELTAS: Array<IntArray>, num: Int): Array<IntArray> {
		val max: Int = ALL_DELTAS[0].size * num
		val d = Array(2) { IntArray(max) }
		var n = 0
		for (i in 0 until ALL_DELTAS[0].size) {
			for (ii in 1..num) {
				val r = rank + ALL_DELTAS[0][i] * ii
				val c = col + ALL_DELTAS[1][i] * ii
				if (!game.isOnBoard(r, c)) break
				d[0][n] = ALL_DELTAS[0][i] * ii
				d[1][n] = ALL_DELTAS[1][i] * ii
				n++
			}
		}
		assert(n > 0)
		var t = d[0]
		d[0] = IntArray(n)
		System.arraycopy(t, 0, d[0], 0, n)
		t = d[1]
		d[1] = IntArray(n)
		System.arraycopy(t, 0, d[1], 0, n)
		return d
	}

	override fun getPlayerColor(side: Int): Color {
		return if (whiteSide == side) Color.WHITE else Color.BLACK
	}

	override fun evaluate(game: Game): Long {
		var value: Long = 0
		return try {
			if (game.isDraw()) return 0.also { value = it.toLong() }.toLong().toLong()
			var side: Int
			when (game.getWinnerNum().also { side = it }) {
				Game.NEAR,
				Game.FAR -> return if (side == game.turn) Long.MAX_VALUE else Long.MIN_VALUE
			}
			for (p in game.getPieces(-1)) {
				val scale = if (p.playerNum == game.turn) 1 else -1
				when (p.getType()) {
					PieceType.EMPTY -> {
					}
					PieceType.PAWN -> value += (110 * scale).toLong()
					PieceType.PAWN_IDLE -> value += (100 * scale).toLong()
					PieceType.PAWN_ENPASSANT -> value += (120 * scale).toLong()
					PieceType.PAWN_TOSWAP -> value += (5000 * scale).toLong()
					PieceType.BISHOP -> value += (300 * scale).toLong()
					PieceType.KNIGHT_R, PieceType.KNIGHT_L -> value += (310 * scale).toLong()
					PieceType.DRAGON_L, PieceType.DRAGON_R, PieceType.ROOK -> value += (500 * scale).toLong()
					PieceType.DRAGON_IDLE_L, PieceType.DRAGON_IDLE_R, PieceType.ROOK_IDLE -> value += (550 * scale).toLong()
					PieceType.QUEEN -> value += (800 * scale).toLong()
					PieceType.CHECKED_KING -> value -= (500 * scale).toLong()
					PieceType.CHECKED_KING_IDLE -> value -= (300 * scale).toLong()
					PieceType.UNCHECKED_KING -> {
						/*
						value -= (100 * scale).toLong()
						if (p.playerNum == game.turn) {
							val pos = move.opponentKingPos
							val krank = pos shr 8
							val kcol = pos and 0xff
							val dist = Math.max(Math.abs(p.rank - krank), Math.abs(p.col - kcol))
							//System.out.println("dist:"+  dist);
							value -= (dist - 2).toLong() // 2 units away is best
						}*/
					}
					PieceType.UNCHECKED_KING_IDLE -> value += (1000 * scale).toLong() // we want avoid moving this piece
					else                                                                  -> throw GException("Unhandled case '" + p.getType() + "'")
				}
			}
			value
		} finally {
			//System.out.println("[" + value + "] " + move);
		}
	}

	override fun reverseMove(game: Game, m: Move) {
		var p: Piece
		when (m.moveType) {
			MoveType.END -> {
			}
			MoveType.CASTLE -> {
				p = game.getPiece(m.castleRookEnd)
				assert(p.getType().idled.canCastleWith)
				game.setPiece(m.castleRookStart, m.playerNum, p.getType().idled)
				game.clearPiece(m.castleRookEnd)
				game.clearPiece(m.end)
				if (m.hasCaptured()) {
					game.setPiece(m.capturedPosition, Game.getOpponent(m.playerNum), m.capturedType!!)
				}
				game.setPiece(m.start, m.playerNum, m.startType!!)
			}
			MoveType.SLIDE, MoveType.JUMP -> {
				game.clearPiece(m.end)
				if (m.hasCaptured()) {
					game.setPiece(m.capturedPosition, Game.getOpponent(m.playerNum), m.capturedType!!)
				}
				game.setPiece(m.start, m.playerNum, m.startType!!)
			}
			MoveType.SWAP -> game.setPiece(m.start, m.playerNum, m.startType!!)
			else                          -> throw GException("Unhandled Case " + m.moveType)
		}
		if (m.hasOpponentKing())
			game.getPiece(m.opponentKingPos).setType(m.opponentKingTypeStart!!)
		if (m.enpassant >= 0) {
			p = game.getPiece(m.enpassant)
			if (p.playerNum == m.playerNum && p.getType() === PieceType.PAWN) p.setType(PieceType.PAWN_ENPASSANT)
		}
		game.turn = m.playerNum
	}

	override val instructions: Table
		get() = Table().addRow("Classic game of Chess")
}