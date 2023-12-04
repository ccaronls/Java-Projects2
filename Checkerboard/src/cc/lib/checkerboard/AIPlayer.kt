package cc.lib.checkerboard

import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.reflector.Omit
import cc.lib.utils.GException
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Writer
import java.util.*

open class AIPlayer : Player {
	var maxSearchDepth = 2

	companion object {
		@JvmField
        var algorithm = Algorithm.miniMaxAB
		@JvmField
        var lastSearchResult: Move? = null
		@JvmField
        var movePathNodeToFront = true
		@JvmField
        var randomizeDuplicates = true
		var prevStats: AIStats? = null
		@JvmField
        var stats = AIStats()
		private val log = LoggerFactory.getLogger(AIPlayer::class.java)
		private var kill = false
		@JvmStatic
        @Throws(IOException::class)
		fun dumpTree(out: Writer, root: Move?) {
			dumpTreeR(out, root, "", null)
		}

		@Throws(IOException::class)
		private fun dumpTreeR(out: Writer, root: Move?, indent: String, parent: Move?) {
			if (root == null) return
			out.write(indent + root.getXmlStartTag(parent))
			var endTag = root.getXmlEndTag(parent)
			//if (parent != null && parent.path == root)
			//  out.write("== PATH ==");
			if (root.moveType != null) out.write(root.toString())
			//out.write(INDENT_LEVELS[indent] + INDENT_LEVELS[0] + "<move>[" + root.bestValue + "] " + root + "</move>\n");
			if (root.children != null) {
				out.write("\n")
				endTag = indent + endTag
				root.children?.forEach { child ->
					dumpTreeR(out, child, "$indent  ", root)
				}
			}
			out.write("""
	$endTag

	""".trimIndent())
		}

		fun evaluate(game: Game, actualDepth: Int): Long {
			val startTime = System.currentTimeMillis()
			var value = game.getRules().evaluate(game)
			game.mostRecentMove?.let { move ->
				move.startType?.takeIf { Math.abs(value) < Long.MAX_VALUE }?.let {
					stats.pieceTypeCount[it.ordinal]++
					stats.pieceTypeValue[it.ordinal] += value.toDouble()
				}
			}
			if (value < 0) {
				value += actualDepth.toLong() // shorter paths that lead to the same value are scored higher.
			} else if (value > 0) {
				value -= actualDepth.toLong()
			}
			if (randomizeDuplicates && value != 0L && value > Long.MIN_VALUE / 2 && value < Long.MAX_VALUE / 2) {
				// add randomness to boards with same value
				value *= 100
				if (value < 0) {
					value -= Utils.randRange(99, 0).toLong()
				} else if (value > 0) {
					value += Utils.randRange(0, 99).toLong()
				}
			}
			stats.evalTimeTotalMSecs += System.currentTimeMillis() - startTime
			++stats.evalCount
			/*
        if (++evalCount % 200 == 0) {
            System.out.print('.');
            if (evalCount % (200*50) == 0)
                System.out.println();
        }*/return value
		}

		/*
    function minimax(node, depth, maximizingPlayer) is
    if depth = 0 or node is a terminal node then
        return the heuristic value of node
    if maximizingPlayer then
        value := −∞
        for each child of node do
            value := max(value, minimax(child, depth − 1, FALSE))
        return value
    else (* minimizing player *)
        value := +∞
        for each child of node do
            value := min(value, minimax(child, depth − 1, TRUE))
        return value
     */
		fun miniMaxR(game: Game, root: Move?, maximizePlayer: Boolean, depth: Int, actualDepth: Int): Long {
			if (root == null || root.playerNum < 0) throw GException()
			if (kill) return 0
			var winner: Int
			when (game.getWinnerNum().also { winner = it }) {
				Game.NEAR, Game.FAR -> return if (root.playerNum == winner) Long.MAX_VALUE - actualDepth else Long.MIN_VALUE + actualDepth
			}
			if (game.isDraw()) return 0
			if (depth <= 0) {
				return evaluate(game, actualDepth)
			}
			root.children = ArrayList(game.getMoves())
			return if (maximizePlayer) {
				var value = Long.MIN_VALUE
				var path: Move? = null
				root.maximize = 1
				root.children?.forEach { m -> 
					game.executeMove(m)
					val sameTurn = m.playerNum == game.turn // if turn has not changed
					val v = miniMaxR(game, m, sameTurn, if (sameTurn) depth else depth - 1, actualDepth + 1)
					m.bestValue = v
					if (v > value) {
						path = m
						value = v
					}
					game.undo()
				}
				root.path = path
				value
			} else { /* minimizing */
				var value = Long.MAX_VALUE
				var path: Move? = null
				root.maximize = -1
				root.children?.forEach { m -> 
					game.executeMove(m)
					val sameTurn = m.playerNum == game.turn
					val v = miniMaxR(game, m, !sameTurn, if (!sameTurn) depth else depth - 1, actualDepth + 1)
					//v += 100*actualDepth;
					m.bestValue = v
					if (v < value) {
						path = m
						value = v
					}
					game.undo()
				}
				root.path = path
				value
			}
		}

		var SORT_ASCENDING = Comparator { m0: Move, m1: Move -> Integer.compare(m0.compareValue, m1.compareValue) }
		var SORT_DESCENDING = Comparator { m0: Move, m1: Move -> Integer.compare(m1.compareValue, m0.compareValue) }
		var SORT_DESCENDING2 = Comparator<Move> { m0, m1 ->
			val d0 = m0.compareValue.toDouble() // + prevStats.pieceTypeValue[m0.getStartType().ordinal()];
			val d1 = m1.compareValue.toDouble() // + prevStats.pieceTypeValue[m1.getStartType().ordinal()];
			java.lang.Double.compare(d1, d0)
		} //(Move m0, Move m1) -> Integer.compare(m1.getCompareValue(), m0.getCompareValue());

		fun miniMaxABR(game: Game, root: Move?, maximizePlayer: Boolean, depth: Int, actualDepth: Int, alpha: Long, beta: Long): Long {
			var alpha = alpha
			var beta = beta
			if (root == null || root.playerNum < 0) throw GException()
			var winner: Int
			when (game.getWinnerNum().also { winner = it }) {
				Game.NEAR, Game.FAR -> {
					val v = if (root.playerNum == winner) Long.MAX_VALUE - actualDepth else Long.MIN_VALUE + actualDepth
					// TODO: Investigate why need to return negative here
					return if (maximizePlayer) -v else v
				}
			}
			if (game.isDraw()) return 0
			if (kill || depth <= 0 || actualDepth > 30) {
				return evaluate(game, actualDepth)
			}
			root.children = ArrayList(game.getMoves())
			if (maximizePlayer) {
				Collections.sort(root.children, SORT_DESCENDING)
			} else {
				Collections.sort(root.children, SORT_ASCENDING)
			}
			var value = if (maximizePlayer) Long.MIN_VALUE else Long.MAX_VALUE
			var path: Move? = null
			root.maximize = if (maximizePlayer) 1 else -1
			root.children?.forEach { m->
				m.parent = root

				//            String gameBeforeMove = game.toString();
				try {
					game.executeMove(m)
				} catch (e: Exception) {
					e.printStackTrace()
					throw MoveException("Execute Move", m)
				}
				val sameTurn = m.playerNum == game.turn // if turn has not changed
				var v: Long
				v = if (maximizePlayer) {
					miniMaxABR(game, m, sameTurn, if (sameTurn) depth else depth - 1, actualDepth + 1, alpha, beta)
				} else {
					miniMaxABR(game, m, !sameTurn, if (!sameTurn) depth else depth - 1, actualDepth + 1, alpha, beta)
				}
				try {
					game.undo()
				} catch (e: Exception) {
					e.printStackTrace()
					throw MoveException("Undo Move", m)
				}
				m.bestValue = v
				if (maximizePlayer) {
					if (v > value) {
						path = m
						value = v
					}
					alpha = Math.max(alpha, value)
					if (alpha > beta) {
						stats.prunes++
						return@forEach
					}
				} else {
					if (v < value) {
						path = m
						value = v
					}
					beta = Math.min(beta, value)
					if (beta < alpha) {
						stats.prunes++
						return@forEach
					}
				}
			}
			root.path = path
			return value
		}

		/*
    function negamax(node, depth, color) is
    if depth = 0 or node is a terminal node then
        return color × the heuristic value of node
    value := −∞
    for each child of node do
        value := max(value, −negamax(child, depth − 1, −color))
    return value
(* Initial call for Player A's root node *)
negamax(rootNode, depth, 1)
(* Initial call for Player B's root node *)
negamax(rootNode, depth, −1)
     */
		private fun negamaxR(game: Game, root: Move?, color: Int, depth: Int, actualDepth: Int): Long {
			if (color == 0) throw GException()
			if (root == null || root.playerNum < 0) throw GException()
			if (kill) return 0
			var winner: Int
			when (game.getWinnerNum().also { winner = it }) {
				Game.NEAR, Game.FAR -> return (if (root.playerNum == winner) Long.MAX_VALUE - actualDepth else Long.MIN_VALUE + actualDepth) * color
			}
			if (game.isDraw()) return 0
			if (depth <= 0) {
				return evaluate(game, actualDepth) * color
			}
			root.children = ArrayList(game.getMoves())
			var value = Long.MIN_VALUE
			var path: Move? = null
			root.children?.forEach { m ->
				game.executeMove(m)
				val sameTurn = game.turn == m.playerNum
				var v: Long
				v = if (sameTurn) {
					negamaxR(game, m, color, depth, actualDepth + 1)
				} else {
					-negamaxR(game, m, -color, depth - 1, actualDepth + 1)
				}
				m.bestValue = v
				m.maximize = color
				game.undo()
				if (v > value) {
					path = m
					value = v
				}
			}
			root.path = path
			return value
		}

		@JvmStatic
        fun cancel() {
			kill = true
			Thread.yield()
		}

		init {
			addAllFields(AIPlayer::class.java)
		}
	}

	@Omit
	private var startTime: Long = 0

	@Omit
	private val moveList = LinkedList<Move>()

	enum class Algorithm {
		minimax,
		miniMaxAB,
		negamax,
		negamaxAB
	}

	constructor() {}
	constructor(maxSearchDepth: Int) {
		this.maxSearchDepth = maxSearchDepth
	}

	val isThinking: Boolean
		get() = startTime > 0
	val thinkingTimeSecs: Int
		get() = Math.max(0, System.currentTimeMillis() - startTime).toInt() / 1000

	fun forceRebuildMovesList(game: Game) {
		moveList.clear()
		buildMovesList(game)
	}

	internal class MoveException(msg: String?, val move: Move) : GException(msg)

	fun buildMovesList(_game: Game) {
		if (moveList.size > 0 && moveList.first.playerNum == _game.turn) return
		prevStats = stats
		stats = AIStats()
		kill = false
		moveList.clear()
		val game = Game()
		game.copyFrom(_game)

		//log.debug("perform minimax search on game\n" + game.getInfoString());
		// minmax search moves
		startTime = System.currentTimeMillis()
		lastSearchResult = Move(MoveType.END, game.turn)
		var root = lastSearchResult
		try {
			when (algorithm) {
				Algorithm.minimax -> root!!.bestValue = miniMaxR(game, root, true, maxSearchDepth, 0)
				Algorithm.miniMaxAB -> root!!.bestValue = miniMaxABR(game, root, true, maxSearchDepth, 0, Long.MIN_VALUE, Long.MAX_VALUE)
				Algorithm.negamax ->                     //                root.bestValue = negamaxR(game, root, -1, maxSearchDepth, 0);
					root!!.bestValue = negamaxR(game, root, if (game.turn != 0) 1 else -1, maxSearchDepth, 0)
				Algorithm.negamaxAB -> root!!.bestValue = negamaxABR(game, root, 1, maxSearchDepth, 0, Long.MIN_VALUE, Long.MAX_VALUE)
			}
		} catch (e: MoveException) {
			e.printStackTrace()
			log.error(e.message + " moves: " + e.move.pathString)
		} catch (e: Throwable) {
			e.printStackTrace()
			if (false) {
				val fname = algorithm.name + "_error.xml"
				val file = File(fname)
				try {
					FileWriter(file).use { out ->
						dumpTree(out, root)
						log.error(e)
						log.error("Write decision tree state to file '" + file.canonicalPath + "'")
					}
				} catch (e2: IOException) {
					e2.printStackTrace()
				}
			}
			log.error("Game state at error:%s\n", game.toString())
			game.trySaveToFile(File("game_" + algorithm.name + "_error.txt"))
		}
		val evalTimeMS: Float = (System.currentTimeMillis() - startTime).toFloat()
		startTime = 0
		log.debug(algorithm.toString() + " ran in %3.2f seconds with best value of %s", evalTimeMS / 1000, root!!.bestValue)
		log.debug("Time spent in eval %3.4f seconds", stats.evalTimeTotalMSecs.toFloat() / 1000)
		var m = root.path
		while (m != null) {
			// move the 'path' node to front so that it appears first in the xml
			if (movePathNodeToFront && root!!.children != null) {
				if (root.children?.remove(m) == false) throw GException()
				root.children?.add(0, m)
			}
			moveList.add(m)
			root = m
			m = m.path
		}
		onMoveListGenerated(moveList)
		printMovesListToLog()
	}


	// Negamax with alpha - beta pruning simple
	/*
    function negamax(node, depth, α, β, color) is
    if depth = 0 or node is a terminal node then
        return color × the heuristic value of node

    childNodes := generateMoves(node)
    childNodes := orderMoves(childNodes)
    value := −∞
    foreach child in childNodes do
        value := max(value, −negamax(child, depth − 1, −β, −α, −color))
        α := max(α, value)
        if α ≥ β then
            break (* cut-off *)
    return value
(* Initial call for Player A's root node *)
negamax(rootNode, depth, −∞, +∞, 1)
     */
	fun negamaxABR(game: Game, root: Move?, color: Int, depth: Int, actualDepth: Int, alpha: Long, beta: Long): Long {
		var alpha = alpha
		if (color == 0) throw GException()
		if (root == null || root.playerNum < 0) throw GException()
		if (kill) return 0
		var winner: Int
		when (game.getWinnerNum().also { winner = it }) {
			Game.NEAR, Game.FAR -> return (if (root.playerNum == winner) Long.MAX_VALUE - actualDepth else Long.MIN_VALUE + actualDepth) * color
		}
		if (game.isDraw()) return 0
		if (depth <= 0) {
			return evaluate(game, actualDepth) * color
		}
		root.children = ArrayList(game.getMoves())
		Collections.sort(root.children, SORT_DESCENDING2)
		var value = Long.MIN_VALUE
		var path: Move? = null
		root.maximize = color
		root.children?.forEach { child ->
			game.executeMove(child)
			val sameTurn = root.playerNum == child.playerNum
			var v: Long
			v = if (sameTurn) {
				negamaxABR(game, child, color, depth, actualDepth + 1, alpha, beta)
			} else {
				-negamaxABR(game, child, color * -1, depth - 1, actualDepth + 1, -beta, -alpha)
			}
			//if (sameTurn)
			//    v *= -1;
			child.bestValue = v
			//child.maximize = color;
			if (v >= value) {
				path = child
				value = v
			}
			game.undo()
			alpha = Math.max(alpha, value)
			if (alpha > beta)
				return@forEach
		}
		root.path = path
		return value
	}

	// TODO: negimax with alpha - beta pruning version 2
	/*
    function negamax(node, depth, α, β, color) is
    alphaOrig := α

    (* Transposition Table Lookup; node is the lookup key for ttEntry *)
    ttEntry := transpositionTableLookup(node)
    if ttEntry is valid and ttEntry.depth ≥ depth then
        if ttEntry.flag = EXACT then
            return ttEntry.value
        else if ttEntry.flag = LOWERBOUND then
            α := max(α, ttEntry.value)
        else if ttEntry.flag = UPPERBOUND then
            β := min(β, ttEntry.value)

        if α ≥ β then
            return ttEntry.value

    if depth = 0 or node is a terminal node then
        return color × the heuristic value of node

    childNodes := generateMoves(node)
    childNodes := orderMoves(childNodes)
    value := −∞
    for each child in childNodes do
        value := max(value, −negamax(child, depth − 1, −β, −α, −color))
        α := max(α, value)
        if α ≥ β then
            break

    (* Transposition Table Store; node is the lookup key for ttEntry *)
    ttEntry.value := value
    if value ≤ alphaOrig then
        ttEntry.flag := UPPERBOUND
    else if value ≥ β then
        ttEntry.flag := LOWERBOUND
    else
        ttEntry.flag := EXACT
    ttEntry.depth := depth
    transpositionTableStore(node, ttEntry)

    return value
(* Initial call for Player A's root node *)
negamax(rootNode, depth, −∞, +∞, 1)
     */
	fun printMovesListToLog() {
		val str = StringBuffer()
		var curTurn = -1 //game.getTurn();
		for (m in moveList) {
			if (str.length > 0) str.append("->")
			if (m.playerNum != curTurn) {
				str.append(m)
				curTurn = m.playerNum
			} else {
				str.append(m.moveType)
				if (m.hasEnd()) str.append(" ").append(Move.toStr(m.end))
				if (m.hasCaptured()) str.append(" cap:").append(Move.toStr(m.capturedPosition))
				if (m.endType != null && m.endType !== m.startType) str.append(" becomes:").append(m.endType)
			}
		}
		log.debug("Moves: $str")
	}

	override fun choosePieceToMove(game: Game, pieces: List<Piece>): Piece? {
		buildMovesList(game)
		if (moveList.size == 0) throw GException("Empty move list")
		return game.getPiece(moveList.first.start)
	}

	override fun chooseMoveForPiece(game: Game, moves: List<Move>): Move? {
		buildMovesList(game)
		return moveList.removeFirst()
	}

	protected fun onMoveListGenerated(moveList: List<Move>?) {
		log.debug(stats.toString())
	}
}