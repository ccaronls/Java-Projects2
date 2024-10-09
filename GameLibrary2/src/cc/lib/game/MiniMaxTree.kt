package cc.lib.game

import cc.lib.logger.LoggerFactory
import cc.lib.utils.randRange
import java.util.LinkedList

/**
 * Created by chriscaron on 10/7/17.
 */
abstract class MiniMaxTree<M : IMove> {
	private var maxSearchDepth = 4
	private var startTime: Long = 0
	private val moveList = LinkedList<IMove>()

	enum class Algorithm {
		minimax,
		miniMaxAB,
		negamax,
		negamaxAB
	}

	constructor()
	constructor(maxSearchDepth: Int) {
		this.maxSearchDepth = maxSearchDepth
	}

	val isThinking: Boolean
		get() = startTime > 0
	val thinkingTimeSecs: Int
		get() = Math.max(0, System.currentTimeMillis() - startTime).toInt() / 1000

	fun forceRebuildMovesList(game: IGame<M>) {
		moveList.clear()
		buildMovesList(game)
	}

	/*
    static void dumpTree(Writer out, Move root) throws IOException {
        dumpTreeR(out, root, "", null);
    }

    private static void dumpTreeR(Writer out, Move root, String indent, Move parent) throws IOException {

        if (root == null)
            return;
        out.write(indent + root.getXmlStartTag(parent));
        String endTag = root.getXmlEndTag(parent);
        //if (parent != null && parent.path == root)
        //  out.write("== PATH ==");
        if (root.getMoveType() != null)
            out.write(root.toString());
        //out.write(INDENT_LEVELS[indent] + INDENT_LEVELS[0] + "<move>[" + root.bestValue + "] " + root + "</move>\n");
        if (root.children != null) {
            out.write("\n");
            endTag = indent + endTag;
            for (Move child : root.children) {
                dumpTreeR(out, child, indent + "  ", root);
            }
        }
        out.write(endTag+"\n");
    }
*/
	fun buildMovesList(game: IGame<M>) {
		if (moveList.size > 0 && moveList.first.playerNum === game.turn) return
		evalCount = 0
		evalTimeTotalMSecs = 0
		kill = false
		moveList.clear()

		//log.debug("perform minimax search on game\n" + game.getInfoString());
		// minmax search moves
		startTime = System.currentTimeMillis()
		var bestValue: Long = 0
		lastSearchResult = makeEmptyMove(game)
		var root = lastSearchResult
		bestValue = when (algorithm) {
			Algorithm.minimax -> miniMaxR(game, root as M, true, maxSearchDepth, 0)
			Algorithm.miniMaxAB -> miniMaxABR(
				game,
				root as M,
				true,
				maxSearchDepth,
				0,
				Long.MIN_VALUE,
				Long.MAX_VALUE
			)

			Algorithm.negamax -> //                root.bestValue = negamaxR(game, root, -1, maxSearchDepth, 0);
				negamaxR(game, root as M, if (game.turn != 0) 1 else -1, maxSearchDepth, 0)

			Algorithm.negamaxAB -> negamaxABR(
				game,
				root as M,
				1,
				maxSearchDepth,
				0,
				Long.MIN_VALUE,
				Long.MAX_VALUE
			)
		}
		val evalTimeMS = (System.currentTimeMillis() - startTime).toInt().toFloat()
		startTime = 0
		log.debug(algorithm.toString() + " ran in %3.2f seconds with best value of %s", evalTimeMS / 1000, bestValue)
		log.debug("Time spent in eval %3.4f seconds", evalTimeTotalMSecs.toFloat() / 1000)
		var m: IMove? = pathMap[root] ?: throw AssertionError("Invalid Path generated")
		while (m != null) {
			// move the 'path' node to front so that it appears first in the xml
			/*
            if (movePathNodeToFront && root.children != null) {
                if (!root.children.remove(m))
                    throw new AssertionError();
                root.children.add(0, m);
            }*/
			moveList.add(m)
			root = m
			m = pathMap[m]
		}
		onMoveListGenerated(moveList)
		//printMovesListToLog();
	}

	protected abstract fun onMoveListGenerated(moveList: LinkedList<IMove>)
	protected abstract fun makeEmptyMove(game: IGame<M>): M
	private val pathMap: MutableMap<IMove, IMove?> = HashMap()
	fun setPath(parent: IMove, child: IMove?) {
		pathMap[parent] = child
	}

	protected fun setMoveValue(move: IMove, value: Long) {}
	fun evaluate(game: IGame<M>, move: M, actualDepth: Int): Long {
		val startTime = System.currentTimeMillis()
		var value = evaluate(game, move)
		if (value > Long.MIN_VALUE) {
			value -= actualDepth.toLong() // shorter paths that lead to the same value are scored higher.
		}
		if (randomizeDuplicates && value != 0L && value > Long.MIN_VALUE / 1000 && value < Long.MAX_VALUE / 1000) {
			// add randomness to boards with same value
			value *= 100
			if (value < 0) {
				value += randRange(-99, 0).toLong()
			} else if (value > 0) {
				value += randRange(0, 99).toLong()
			}
		}
		evalTimeTotalMSecs += System.currentTimeMillis() - startTime
		++evalCount
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
	fun miniMaxR(game: IGame<M>, root: M, maximizePlayer: Boolean, depth: Int, actualDepth: Int): Long {
		if (root.playerNum < 0) throw AssertionError()
		if (kill) return 0
		val winner = game.getWinnerNum()
		if (winner >= 0) {
			return if (root.playerNum === winner) Long.MAX_VALUE - actualDepth else Long.MIN_VALUE + actualDepth
		}
		if (game.isDraw()) return 0
		if (depth <= 0) {
			return evaluate(game, root, actualDepth)
		}
		val moves: List<M> = ArrayList(game.getMoves())
		return if (maximizePlayer) {
			var value = Long.MIN_VALUE
			var path: IMove? = null
			//root.maximize = 1;
			for (m in moves) {
				game.executeMove(m)
				val sameTurn = m!!.playerNum === game.turn // if turn has not changed
				val v = miniMaxR(game, m, sameTurn, if (sameTurn) depth else depth - 1, actualDepth + 1)
				setMoveValue(m, v)
				if (v > value) {
					path = m
					value = v
				}
				game.undo()
			}
			setPath(root, path)
			//root.path = path;
			value
		} else { /* minimizing */
			var value = Long.MAX_VALUE
			var path: IMove? = null
			//root.maximize = -1;
			for (m in moves) {
				game.executeMove(m)
				val sameTurn = m!!.playerNum === game.turn
				val v = miniMaxR(game, m, !sameTurn, if (!sameTurn) depth else depth - 1, actualDepth + 1)
				//v += 100*actualDepth;
				setMoveValue(m, v)
				if (v < value) {
					path = m
					value = v
				}
				game.undo()
			}
			setPath(root, path)
			value
		}
	}

	fun miniMaxABR(
		game: IGame<M>,
		root: M,
		maximizePlayer: Boolean,
		depth: Int,
		actualDepth: Int,
		alpha: Long,
		beta: Long
	): Long {
		var alpha = alpha
		var beta = beta
		if (root == null || root.playerNum < 0) throw AssertionError()
		if (kill) return 0
		val winner = game.getWinnerNum()
		if (winner >= 0) {
			return if (root.playerNum === winner) Long.MAX_VALUE - actualDepth else Long.MIN_VALUE + actualDepth
		}
		if (game.isDraw()) return 0
		if (depth <= 0) {
			return evaluate(game, root, actualDepth)
		}
		val moves: List<M> = ArrayList(game.getMoves())
		var value = if (maximizePlayer) Long.MIN_VALUE else Long.MAX_VALUE
		var path: IMove? = null
		//root.maximize = maximizePlayer ? 1 : -1;
		for (m in moves) {
			game.executeMove(m)
			val sameTurn = m!!.playerNum === game.turn // if turn has not changed
			var v: Long
			v = if (maximizePlayer) {
				miniMaxABR(game, m, sameTurn, if (sameTurn) depth else depth - 1, actualDepth + 1, alpha, beta)
			} else {
				miniMaxABR(game, m, !sameTurn, if (!sameTurn) depth else depth - 1, actualDepth + 1, alpha, beta)
			}
			game.undo()
			setMoveValue(m, v)
			if (maximizePlayer) {
				if (v > value) {
					path = m
					value = v
				}
				alpha = Math.max(alpha, value)
				if (alpha > beta) break
			} else {
				if (v < value) {
					path = m
					value = v
				}
				beta = Math.min(beta, value)
				if (beta < alpha) break
			}
		}
		setPath(root, path)
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
	private fun negamaxR(game: IGame<M>, root: M, color: Int, depth: Int, actualDepth: Int): Long {
		if (color == 0) throw AssertionError()
		if (root == null || root.playerNum < 0) throw AssertionError()
		if (kill) return 0
		val winner = game.getWinnerNum()
		if (winner >= 0) {
			return (if (root.playerNum === winner) Long.MAX_VALUE - actualDepth else Long.MIN_VALUE + actualDepth) * color
		}
		if (game.isDraw()) return 0
		if (depth <= 0) {
			return evaluate(game, root, actualDepth) * color
		}
		val moves: List<M> = ArrayList(game.getMoves())
		var value = Long.MIN_VALUE
		var path: IMove? = null
		for (m in moves) {
			game.executeMove(m)
			val sameTurn = game.turn == m!!.playerNum
			var v: Long
			v = if (sameTurn) {
				negamaxR(game, m, color, depth, actualDepth + 1)
			} else {
				-negamaxR(game, m, -color, depth - 1, actualDepth + 1)
			}
			setMoveValue(m, v)
			//m.maximize = color;
			game.undo()
			if (v > value) {
				path = m
				value = v
			}
		}
		setPath(root, path)
		return value
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
	fun negamaxABR(game: IGame<M>, root: M, color: Int, depth: Int, actualDepth: Int, alpha: Long, beta: Long): Long {
		var alpha = alpha
		if (color == 0) throw AssertionError()
		if (root == null || root.playerNum < 0) throw AssertionError()
		if (kill) return 0
		val winner = game.getWinnerNum()
		if (winner >= 0) {
			return (if (root.playerNum === winner) Long.MAX_VALUE - actualDepth else Long.MIN_VALUE + actualDepth) * color
		}
		if (game.isDraw()) return 0
		if (depth <= 0) {
			return evaluate(game, root, actualDepth) * color
		}
		val moves: List<M> = game.getMoves().sorted()
		var value = Long.MIN_VALUE
		var path: IMove? = null
		//root.maximize = color;
		for (child in moves) {
			game.executeMove(child)
			val sameTurn = root.playerNum === child!!.playerNum
			var v: Long
			v = if (sameTurn) {
				negamaxABR(game, child, color, depth, actualDepth + 1, alpha, beta)
			} else {
				-negamaxABR(game, child, color * -1, depth - 1, actualDepth + 1, -beta, -alpha)
			}
			//if (sameTurn)
			//    v *= -1;
			//child.bestValue = v;
			//child.maximize = color;
			if (v >= value) {
				path = child
				value = v
			}
			game.undo()
			alpha = Math.max(alpha, value)
			if (alpha > beta) break
		}
		setPath(root, path)
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
	/**
	 *
	 */
	fun cancel() {
		kill = true
		Thread.yield()
	}

	/**
	 * Implement a valuation method with the following properties
	 *
	 * If eval(player A) = x then eval(player B) = -x
	 * If moves re zero then value is automatically (+/-) INF
	 *
	 * @param game
	 * @param move
	 * @return
	 */
	protected abstract fun evaluate(game: IGame<*>?, move: IMove?): Long

	/**
	 * Callback for this event. base method does nothing.
	 * @param node
	 */
	protected open fun onNewNode(game: IGame<*>?, node: DescisionTree?) {}
	protected abstract fun getZeroMovesValue(game: IGame<*>?): Long

	companion object {
		val log = LoggerFactory.getLogger(MiniMaxTree::class.java)
		private var kill = false
		var lastSearchResult: IMove? = null
		var evalCount = 0
		var evalTimeTotalMSecs: Long = 0
		var algorithm = Algorithm.minimax
		var randomizeDuplicates = true
	}
}
