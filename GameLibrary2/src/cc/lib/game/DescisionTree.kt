package cc.lib.game

import cc.lib.logger.LoggerFactory
import java.io.IOException
import java.io.Writer
import java.util.Collections

/**
 * Created by chriscaron on 10/5/17.
 */
class DescisionTree internal constructor(val move: IMove?, var value: Long, private val maxTreeScale: Int) :
	Comparable<DescisionTree> {
	var parent: DescisionTree? = null
		private set
	private val children = ArrayList<DescisionTree>()
	var sorted = true
	private var path: DescisionTree? = null
	private var startPlayerNum = -1
	var meta = "" // anything extra to display to the user

	constructor(startPlayerNum: Int) : this(null, 0, 0) {
		this.startPlayerNum = startPlayerNum
	}

	override operator fun compareTo(o: DescisionTree): Int {
		if (maxTreeScale == 0) throw AssertionError()
		if (o.value < value) return -maxTreeScale
		return if (o.value == value) 0 else maxTreeScale
	}

	/**
	 * Called AFTER the value of child is set so that we can add
	 * @param child
	 */
	fun addChild(child: DescisionTree) {
		// TODO: Use binary insert not linear
		if (child.parent != null) throw AssertionError("Child already has a parent")
		children.add(child)
		if (children.size > 1) sorted = false
		child.parent = this
	}

	fun clearChildren() {
		children.clear()
		sorted = true
	}

	fun getChildren(max: Int): Iterable<DescisionTree> {
		if (!sorted) {
			Collections.sort(children)
			sorted = true
		}
		if (max > 0) {
			while (children.size > max) {
				children.removeAt(children.size - 1)
			}
		}
		return children
	}

	val startTag: String
		get() {
			if (parent == null) return "<root startPlayer=\"$startPlayerNum\">"
			return if (parent!!.path === this) "<path>" else "<move>"
		}
	val endTag: String
		get() {
			if (parent == null) return "</root>"
			return if (parent!!.path === this) "</path>" else "</move>"
		}

	fun appendMeta(s: String, vararg args: Any?) {
		if (meta.length > 0 && !s.startsWith("\n")) meta += "\n"
		meta += String.format(s, *args)
	}

	val numChildren: Int
		get() = children.size
	val root: DescisionTree
		/**
		 * Return the root fo this tree (non-recursive)
		 * @return
		 */
		get() {
			var root: DescisionTree = this
			while (root.parent != null)
				root = root.parent!!
			return root
		}

	fun dumpTreeXML(out: Writer) {
		try {
			dumpTree(out, this, "")
		} catch (e: Exception) {
		}
	}

	fun searchMiniMaxPath(): List<*> {
		if (startPlayerNum < 0) throw AssertionError()
		miniMax(this, true, startPlayerNum)
		val moves = mutableListOf<IMove>()
		var dt = path
		var min = Long.MAX_VALUE
		var max = Long.MIN_VALUE
		while (dt != null) {
			if (dt.move!!.playerNum == startPlayerNum) max = Math.max(max, dt.value) else min = min.coerceAtMost(dt.value)
			moves.add(dt.move!!)
			dt = dt.path
		}
		log.debug("MiniMax Vector [$max,$min]")
		return moves
	}

	companion object {
		private val log = LoggerFactory.getLogger(DescisionTree::class.java)

		@Throws(IOException::class)
		private fun dumpTree(out: Writer, root: DescisionTree?, indent: String) {
			if (root == null) return
			out.write(
				indent + root.startTag + (if (root.parent == null) "" else "[" + root.value + "] ") + root.meta.replace(
					'\n',
					','
				)
			)
			//log.info("%s%s", indent, root.getMeta().replace('\n', ','));
			var endTag = root.endTag
			if (root.numChildren > 0) {
				out.write("\n")
				endTag = indent + endTag
				for (t in root.getChildren(0)) {
					dumpTree(out, t, "$indent  ")
				}
			}
			out.write(endTag + "\n")
		}

		private fun miniMax(root: DescisionTree, maximizingPlayer: Boolean, playerNum: Int): DescisionTree {
			if (root.numChildren == 0) return root
			return if (maximizingPlayer) {
				var max: DescisionTree? = null
				for (child in root.getChildren(0)) {
					val dt = miniMax(child, child.move!!.playerNum == playerNum, child.move.playerNum)
					if (max == null || dt!!.value > max.value) {
						max = child
					}
				}
				root.path = max
				root.value = max!!.value
				max
			} else {
				var min: DescisionTree? = null
				for (child in root.getChildren(0)) {
					val dt = miniMax(child, child.move!!.playerNum != playerNum, child.move.playerNum)
					if (min == null || dt!!.value < min.value) {
						min = child
					}
				}
				root.path = min
				root.value = min!!.value
				min
			}
		} /*
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
		/*
    public DescisionTree findDominantChild() {
        DescisionTree [] result = new DescisionTree [] { this };
        long [] miniMax = { Long.MIN_VALUE, Long.MAX_VALUE };
        findDominantChildR(this, miniMax, result, true);
        result[0].dominant = true;
        return result[0];
    }

    private static long findDominantChildR(DescisionTree root, long [] highest, DescisionTree [] result, boolean maximizingPlayer) {
        if (root.getNumChildren() == 0) {
            return root.getValue();
        } else {
            if (maximizingPlayer) {
                for (DescisionTree child : root.getChildren(0)) {

                }
            }
            if (root.getValue() > highest[0]) {
                highest[0] = root.getValue();
                result[0] = root;
            }
        }
    }*/
	}
}