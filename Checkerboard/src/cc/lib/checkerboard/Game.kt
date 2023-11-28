package cc.lib.checkerboard

import cc.lib.game.IGame
import cc.lib.logger.LoggerFactory
import cc.lib.utils.GException
import cc.lib.utils.Reflector
import java.util.*

open class Game : Reflector<Game>(), IGame<Move> {
	companion object {
		val log = LoggerFactory.getLogger(Game::class.java)
		var counter = 0
		private const val DEBUG = false
		const val NEAR = 0
		const val FAR = 1
		const val NOP = -1

		/**
		 *
		 * @param player
		 * @return
		 */
		fun getOpponent(player: Int): Int {
			if (player == NEAR) return FAR
			if (player == FAR) return NEAR
			throw GException()
		}

		init {
			addAllFields(Game::class.java)
		}
	}

	enum class GameState(
		/**
		 *
		 * @return
		 */
		val isGameOver: Boolean, val playerNum: Int) {
		PLAYING(false, -1),
		NEAR_WINS(true, NEAR),
		FAR_WINS(true, FAR),
		DRAW(true, -1);
	}

	private val players = arrayOf(Player(), Player())

	// transform input of Array<Pair<Int, Array<PieceType>>> -> Array<Array<Piece>>
	var board: Array<Array<Piece>> = arrayOf()

	val ranks
		get() = board.size
	val columns
		get() = if (board.size > 0) board[0].size else 0
	override var turn = 0
	private var selectedPos = -1
	var movesCache: MutableList<Move>? = null

	private val undoStack = Stack<State>()
	private lateinit var rules: Rules
	private var gameState = GameState.PLAYING
	private val numPieces: IntArray = intArrayOf(0,0)

	fun setRules(rules: Rules) {
		this.rules = rules
		board = rules.init(this)
	}

	fun getRules() : Rules = rules

	fun clear() {
		for (r in 0 until ranks) {
			for (c in 0 until columns) {
				board[r][c] = Piece(r, c, -1, PieceType.EMPTY)
			}
		}
		gameState = GameState.PLAYING
		movesCache = null
		selectedPos = -1
		numPieces.fill(0)
		undoStack.clear()
	}

	/**
	 *
	 * @param position
	 * @param p
	 */
	fun setPlayer(position: Int, p: Player) {
		players[position] = p
		p.color = rules.getPlayerColor(position)
		p.playerNum = position
	}

	fun setPlayers(near: Player, far: Player) {
		for (p in arrayOf(Pair(NEAR, near), Pair(FAR, far))) {
			players[p.first] = p.second
			players[p.first].color = rules.getPlayerColor(p.first)
			p.second.playerNum = p.first
		}
	}

	/**
	 *
	 * @return
	 */
    val opponent: Int
		get() = getOpponent(turn)

	/**
	 *
	 * @return
	 */
	val selectedPiece: Piece?
		get() = if (selectedPos < 0) null else getPiece(selectedPos)

	/**
	 * Called after players have been assigned
	 */
	open fun newGame() {
		undoStack.clear()
		selectedPos = -1
		movesCache = null
		clear()
		board = rules.init(this)
		var num = 0
		for (p in players) {
			p.playerNum = num++
			p.color = rules.getPlayerColor(p.playerNum)
			p.newGame()
		}
		countPieces()
		refreshGameState()
	}

	fun countPieces() {
		numPieces[0] = 0
		numPieces[1] = 0
		for (i in 0 until ranks) {
			for (ii in 0 until columns) {
				if (!isOnBoard(i, ii)) continue
				val p = getPiece(i, ii)
				if (p.playerNum >= 0) {
					numPieces[p.playerNum]++
				}
			}
		}
	}

	/**
	 * Called repeatedly after newGame
	 */
	fun runGame() {
		when (gameState) {
			GameState.DRAW -> {
				onGameOver(null)
				return
			}
			GameState.FAR_WINS -> {
				onGameOver(players[FAR])
				return
			}
			GameState.NEAR_WINS -> {
				onGameOver(players[NEAR])
				return
			}
			else -> Unit
		}
		val moves = getMoves()
		if (moves.isEmpty()) {
			onGameOver(winner)
			return
		}
		if (selectedPos < 0) {
			val movable = movablePieces
			if (movable.size == 1) {
				selectedPos = movable[0].position
			}
		}
		if (selectedPos < 0) {
			val p = players[turn].choosePieceToMove(this, movablePieces)
			if (p != null) {
				selectedPos = p.position
				onPieceSelected(p)
				return  // we dont want to block twice in one 'runGame'
			}
		} else {
			val m = players[turn].chooseMoveForPiece(this, getMovesforPiece(selectedPos))
			if (m != null) {
				onMoveChosen(m)
				executeMove(m)
			}
			selectedPos = -1
		}
	}

	/**
	 *
	 * @param pos
	 * @return
	 */
	fun getPiece(pos: Int): Piece {
		return board[pos shr 8][pos and 0xff]
	}

	/**
	 *
	 * @param rank
	 * @param col
	 * @return
	 */
	fun getPiece(rank: Int, col: Int): Piece {
		//if (!isOnBoard(rank, col)) {
		//    throw new GException("Not on board [" + rank + "," + col + "]");
		// }
		return board[rank][col]
	}

	private fun validatePiceCounts() {
		val numPieces = IntArray(2)
		for (i in 0 until ranks) {
			for (ii in 0 until columns) {
				if (!isOnBoard(i, ii)) continue
				val p = getPiece(i, ii)
				if (p.playerNum >= 0) {
					numPieces[p.playerNum]++
				}
			}
		}
		if (this.numPieces[0] != numPieces[0]) {
			log.error("Piece count wrong")
		}
		if (this.numPieces[1] != numPieces[1]) {
			log.error("Piece count wrong")
		}
	}

	fun getPieces(playerNum: Int): Iterable<Piece> {
		if (numPieces[0] == 0 || numPieces[1] == 0)
			countPieces()
		else if (DEBUG) {
			validatePiceCounts()
		}

		//log.debug("Piece count for player %d = %d", 0, numPieces[0]);
		//log.debug("Piece count for player %d = %d", 1, numPieces[1]);
		return Iterable {
			var num = 0
			var pNum = playerNum
			var startRank = 0
			var advDir = 1
			if (pNum == NEAR) {
				num = numPieces[NEAR]
				pNum = FAR
				startRank = getStartRank(playerNum)
				advDir = getAdvanceDir(playerNum)
			} else if (playerNum == FAR) {
				num = numPieces[FAR]
				pNum = NEAR
				startRank = getStartRank(playerNum)
				advDir = getAdvanceDir(playerNum)
			} else {
				num = numPieces[NEAR] + numPieces[FAR]
			}
			PieceIterator(pNum, startRank, advDir, num)
		}
	}

	internal inner class PieceIterator(notPlayerNum: Int, startRank: Int, advDir: Int, maxPieces: Int) : Iterator<Piece> {
		var rank = 0
		var col = 0
		var advDir: Int
		var notPlayerNum: Int
		var ranks: Int
		var num: Int
		var next: Piece? = null
		override fun hasNext(): Boolean {
			if (num <= 0) return false
			next = null
			val end = if (advDir > 0) ranks else -1
			while (rank != end) {
				while (col < columns) {
					val p = board[rank][col].playerNum
					if (p >= 0 && p != notPlayerNum) {
						next = board[rank][col++]
						num--
						return true
					}
					col++
				}
				col = 0
				rank += advDir
			}
			return false
		}

		override fun next(): Piece {
			return next!!
		}

		init {
			ranks = this@Game.ranks
			rank = startRank
			this.notPlayerNum = notPlayerNum
			this.advDir = advDir
			num = maxPieces
		}
	}

	fun isGameOver() : Boolean = gameState.isGameOver

	/**
	 *
	 * @param rank
	 * @param col
	 * @return
	 */
	fun isOnBoard(rank: Int, col: Int): Boolean {
		return if (rank < 0 || col < 0 || rank >= ranks || col >= columns) false else board[rank][col].getType() !== PieceType.BLOCKED
	}

	private val movablePieces: List<Piece>
		private get() {
			val pieces: MutableList<Piece> = ArrayList()
			for (p in getPieces(turn)) {
				if (p.numMoves > 0) pieces.add(p)
			}
			assert(pieces.size > 0)
			return pieces
		}

	private fun getMovesforPiece(pos: Int): List<Move> {
		val pm: MutableList<Move> = ArrayList()
		for (m in getMovesInternal()) {
			if (m.start == pos) {
				pm.add(m)
			}
		}
		return pm
	}

	/**
	 *
	 * @param side
	 * @return
	 */
	fun getPlayer(side: Int): Player? {
		return players[side]
	}

	/**
	 *
	 * @return
	 */
	val currentPlayer: Player?
		get() = players[turn]

	/**
	 *
	 * @return
	 */
	val opponentPlayer: Player?
		get() = players[opponent]

	/**
	 *
	 * @param p
	 */
	protected open fun onPieceSelected(p: Piece?) {}

	/**
	 *
	 * @param m
	 */
	protected open fun onMoveChosen(m: Move?) {}

	/**
	 * @param winner draw game if null
	 */
	protected open fun onGameOver(winner: Player?) {}
	@Synchronized
	override fun executeMove(move: Move) {
		if (move.playerNum != turn) throw GException("Invalid move to execute")
		val state = State(getMovesInternal().indexOf(move) // <-- TODO: Make this faster
			, movesCache!!)
		assert(undoStack.size < 1024)

		//
		movesCache = null
		clearPieceMoves()
		if (DEBUG) println("""
	COUNTER:${counter++}
	GAME BEFORE MOVE: $move
	$this
	""".trimIndent())
		rules.executeMove(this, move)
		undoStack.push(state)
		movesCache?.let {
			countPieceMoves()
		}
		var gameAfter: String? = null
		if (DEBUG) {
			gameAfter = toString()
			println("GAME AFTER:\n$gameAfter")
		}
		refreshGameState()
		if (DEBUG) {
			val gameAfter2 = toString()
			println("GAME STATE AFTER REFRESH:\n$gameAfter2")
			if (gameAfter != gameAfter2) throw GException("Logic Error: Game State changed after refresh")
		}
	}

	fun refreshGameState() {
		gameState = if (rules.isDraw(this)) {
			GameState.DRAW
		} else when (rules.getWinner(this)) {
			NEAR -> GameState.NEAR_WINS
			FAR -> GameState.FAR_WINS
			else -> GameState.PLAYING
		}
	}

	/**
	 *
	 * @return
	 */
	fun canUndo(): Boolean {
		return undoStack.size > 0
	}

	private fun clearPieceMoves() {
		for (rank in 0 until ranks) {
			for (col in 0 until columns) {
				board[rank][col].numMoves = 0
			}
		}
	}

	fun countPieceMoves(): Int {
		var totalMoves = 0
		movesCache?.let { moves ->
			clearPieceMoves()
			for (m in moves) {
				if (m.start >= 0) {
					getPiece(m.start).numMoves++
					totalMoves++
				}
			}
		}
		return totalMoves
	}

	@Synchronized
	override fun undo(): Move {
		if (undoStack.size > 0) {
			val state = undoStack.pop()
			val m = state.move
			movesCache = state.moves.toMutableList()
			rules.reverseMove(this, m)
			selectedPos = -1
			//turn = moves.get(0).getPlayerNum();
			countPieceMoves()
			gameState = GameState.PLAYING
			return m
		}
		throw GException("Cannot undo")
	}

	override fun getMoves(): List<Move> {
		if (movesCache == null) {
			movesCache = rules.computeMoves(this).toMutableList()
			countPieceMoves()
		}
		return movesCache!!
	}

	fun getMovesInternal() : MutableList<Move> {
		movesCache?.let {
			return it
		}
		return ArrayList<Move>().also {
			movesCache = it
		}
	}

	fun hasNoMoreMoves(): Boolean {
		return movesCache?.let {
			it.isEmpty()
		}?:true
	}

	fun nextTurn() {
		turn = (turn + 1) % players.size
		movesCache = null
	}

	/**
	 *
	 * @param playerNum NEAR, FAR or -1 for DRAWGAME
	 */
	fun forfeit(playerNum: Int) {
		gameState = when (playerNum) {
			NEAR -> GameState.NEAR_WINS
			FAR -> GameState.FAR_WINS
			else -> GameState.DRAW
		}
	}

	/**
	 * This is the rank closest to the given side
	 *
	 * @param side
	 * @return
	 */
	fun getStartRank(side: Int): Int {
		when (side) {
			FAR -> return 0
			NEAR -> return ranks - 1
		}
		throw GException("Logic Error: not a valid side: $side")
	}

	/**
	 * Get how many units of advancement the rank is for a side
	 *
	 * @param side
	 * @param rank
	 * @return
	 */
	fun getAdvancementFromStart(side: Int, rank: Int): Int {
		return getStartRank(side) + rank * getAdvanceDir(side)
	}

	/**
	 * This is the rank increment based on side
	 * @param side
	 * @return
	 */
	fun getAdvanceDir(side: Int): Int {
		when (side) {
			FAR -> return 1
			NEAR -> return -1
		}
		throw GException("Logic Error: not a valid side: $side")
	}

	fun movePiece(m: Move): Piece {
		var p = getPiece(m.start)
		assert(p.playerNum == turn) //, "Logic Error: Not player " + p.getPlayerNum() + "'s turn: " + turn + "'s turn. For Piece:\n" + p);
		assert(p.getType().flag != 0) //, "Logic Error: Moving an empty piece. For Move:\n" + m);
		copyPiece(m.start, m.end)
		clearPiece(m.start)
		p = getPiece(m.end)
		p.setType(m.endType!!)
		return p
	}

	@Synchronized
	fun clearPiece(rank: Int, col: Int) {
		numPieces[board[rank][col].playerNum]--
		board[rank][col].clear()
	}

	@Synchronized
	fun clearPiece(pos: Int) {
		clearPiece(pos shr 8, pos and 0xff)
	}

	@Synchronized
	fun setPiece(rank: Int, col: Int, playerNum: Int, pt: PieceType): Piece {
		val p = board[rank][col]
		p.setType(pt)
		numPieces[playerNum]++
		val pnum = p.playerNum
		if (pnum >= 0) {
			numPieces[pnum]--
		}
		p.playerNum = playerNum
		return p
	}

	@Synchronized
	fun setPiece(pos: Int, pc: Piece) {
		val rank = pos shr 8
		val col = pos and 0xff
		val pnum = board[rank][col].playerNum
		if (pnum >= 0) throw GException("Cannot assign to non-empty square")
		board[rank][col].copyFrom(pc)
		numPieces[pc.playerNum]++
	}

	fun setPiece(pos: Int, playerNum: Int, p: PieceType): Piece {
		return setPiece(pos shr 8, pos and 0xff, playerNum, p)
	}

	@Synchronized
	fun copyPiece(from: Piece, to: Piece) {
		to.copyFrom(from)
		numPieces[to.playerNum]++
	}

	fun copyPiece(fromPos: Int, toPos: Int) {
		val from = getPiece(fromPos)
		val to = getPiece(toPos)
		copyPiece(from, to)
	}

	val winner: Player?
		get() {
			when (gameState) {
				GameState.PLAYING -> {
				}
				GameState.NEAR_WINS -> return players[NEAR]
				GameState.FAR_WINS -> return players[FAR]
				GameState.DRAW -> {
				}
			}
			return null
		}

	override fun getWinnerNum(): Int {
		return gameState.playerNum
	}

	/**
	 *
	 * @return
	 */
	override fun isDraw(): Boolean {
		return gameState == GameState.DRAW
	}

	/**
	 * Returns history of moves with most recent move (last move) in the first list position.
	 * In other words, iterating over the history in order will rewind the game.
	 * @return
	 */
	val moveHistory: List<Move>
		get() {
			val history: MutableList<Move> = ArrayList()
			for (st in undoStack) {
				history.add(st.move)
			}
			history.reverse()
			return history
		}

	/**
	 *
	 * @return
	 */
	val moveHistoryCount: Int
		get() = undoStack.size

	/**
	 *
	 * @return
	 */
	val mostRecentMove: Move?
		get() = if (undoStack.size > 0) undoStack.peek().move else null

	fun getTurnStr(turn: Int): String {
		if (turn == NEAR) return "NEAR"
		return if (turn == FAR) "FAR" else "UNKNOWN($turn)"
	}

	fun getCapturedPieces(playerNum: Int): List<PieceType> {
		return undoStack.filter { it.move.hasCaptured() && it.move.playerNum == playerNum }.map {
			it.move.capturedType!!
		}
	}

	override fun toString(): String {
		val s = StringBuffer(gameState.name)
		var turn = turn
		when (gameState) {
			GameState.PLAYING -> {
			}
			GameState.NEAR_WINS -> turn = NEAR
			GameState.FAR_WINS -> turn = FAR
			GameState.DRAW -> {
			}
		}
		s.append("(").append(turn).append(") ").append(getTurnStr(turn))
		var ii = 0
		while (ii < 2) {
			val pl = getPlayer(turn)
			s.append("\n")
			if (pl != null) s.append(pl.color) else s.append("<INVESTIGATE: NULL COLOR>")
			s.append("(").append(turn).append(")")
			val captured = getCapturedPieces(turn)
			if (captured.size > 0) {
				s.append(" cap:")
				val counts = IntArray(PieceType.values().size)
				for (pt in captured) {
					counts[pt.displayType.ordinal]++
				}
				for (i in counts.indices) {
					if (counts[i] == 0) continue
					if (counts[i] == 1) s.append(PieceType.values()[i].abbrev).append(" ") else s.append(PieceType.values()[i].abbrev).append(" X ").append(counts[i]).append(" ")
				}
			}
			ii++
			turn = getOpponent(turn)
		}
		s.append("\n   ")
		for (c in 0 until columns) {
			s.append(String.format("%3d ", c))
		}
		s.append("\n   ")
		for (r in 0 until ranks) {
			for (c in 0 until columns) {
				s.append("+---")
			}
			s.append(String.format("+\n%2d ", r))
			for (c in 0 until columns) {
				s.append("|")
				val p = getPiece(r, c)
				if (p.playerNum < 0) {
					if (p.getType() !== PieceType.EMPTY) {
						s.append(" X ")
					} else {
						s.append("   ")
					}
				} else {
					val pl = getPlayer(p.playerNum)
					if (pl == null) {
						s.append(p.playerNum)
					} else {
						s.append(pl.color.name[0])
					}
					if (p.isCaptured) {
						s.append(p.getType().abbrev[0].toString() + "*")
					} else if (p.stackSize > 1) {
						s.append(p.getType().abbrev[0].toString() + java.lang.String.valueOf(p.stackSize))
					} else {
						s.append(p.getType().abbrev)
					}
				}
			}
			s.append("|\n   ")
		}
		for (c in 0 until columns) {
			s.append("+---")
		}
		s.append("+\n")
		return s.toString()
	}

	fun isBoardsEqual(other: Game): Boolean {
		if (turn != other.turn) return false
		if (columns != other.columns || ranks != other.ranks) return false
		for (r in 0 until other.ranks) {
			for (c in 0 until other.columns) {
				var p0: Piece?
				var p1: Piece?
				if (!other.getPiece(r, c).also { p0 = it }.equals(other.getPiece(r, c).also { p1 = it })) {
					//System.out.println("Piece at position " + r + "," + c + " differ: " + p0 + "\n" + p1);
					return false
				}
			}
		}
		return true
	}

	fun getPreviousMove(playerNum: Int): Move? {
		for (i in undoStack.indices.reversed()) {
			val m = undoStack[i].move
			if (m.playerNum == playerNum) return m
		}
		return null
	}
}