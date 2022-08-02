package cc.game.soc.core

import cc.lib.game.IVector2D
import cc.lib.math.Vector2D
import java.io.PrintStream
import java.util.*

abstract class BotNode internal constructor() : Comparable<BotNode> {
	/**
	 *
	 * @return
	 */
	/**
	 *
	 * @param optimal
	 */
	var isOptimal = false
	private var value = 0.0
	private var numProperties = 0
	@JvmField
    var chance = 1.0f
	@JvmField
    var next: BotNode? = null
	@JvmField
    val properties: MutableMap<String, Double> = TreeMap()
	var strategy = MoveType.CONTINUE // TODO
    var children: MutableList<BotNode> = LinkedList()

	/**
	 *
	 * @return
	 */
    @JvmField
    var parent: BotNode? = null

	/**
	 *
	 * @return
	 */
	val isLeaf: Boolean
		get() = children.size == 0

	/**
	 *
	 * @param out
	 */
	fun printTree(out: PrintStream) {
		printTreeR(out, "")
	}

	override fun toString(): String {
		var s = description
		val value = getValue()
		if (data != null && value > -Float.MAX_VALUE) {
			s += " ($value)"
		}
		if (isOptimal) s += " ************* OPTIMAL ***************"
		return s
	}

	private fun printTreeR(out: PrintStream, indent: String) {
		var indent = indent
		out.print(indent)
		out.print(this)
		out.println()
		indent += "+-"
		for (child in children) {
			child.printTreeR(out, indent)
		}
	}

	/**
	 *
	 * @param child
	 * @return
	 */
	fun attach(child: BotNode): BotNode {
		assert(value != Double.NEGATIVE_INFINITY)
		children.add(child)
		child.parent = this
		child.properties.putAll(properties)
		return child
	}

	/**
	 *
	 * @return
	 */
	abstract val data: Any?

	/**
	 *
	 * @return
	 */
	abstract val description: String

	/**
	 *
	 * @param b
	 * @return
	 */
	open fun getBoardPosition(b: Board): IVector2D {
		return Vector2D.ZERO
	}

	/**
	 *
	 * @return
	 */
	fun getValue(): Double {
		if (numProperties != properties.size) {
			value = 0.0
			for ((key, value1) in properties) {
				value += AITuning.getInstance().getScalingFactor(key) * value1
			}
			numProperties = properties.size
		}
		return value
	}

	/**
	 *
	 */
	fun resetCache() {
		numProperties = 0
	}

	override fun compareTo(arg: BotNode): Int {
		val value = getValue()
		val argValue = arg.getValue()

		// descending order
		if (value < argValue) return 1
		return if (value > argValue) -1 else 0
	}

	/**
	 *
	 * @param name
	 * @param value
	 */
	fun addValue(name: String, value: Double) {
		var name = name
		name = name.replace(' ', '_').trim { it <= ' ' }
		if (value != 0.0) {
			properties[name] = value * chance
		} else {
			properties.remove(name)
		}
		numProperties = 0
	}

	/**
	 *
	 * @return
	 */
	val keys: Set<String>
		get() = properties.keys

	/**
	 *
	 * @param key
	 * @return
	 */
	fun getValue(key: String): Double {
		return properties[key]?:0.0
	}

	/**
	 *
	 * @return
	 */
	fun debugDump(): String {
		val buf = StringBuffer()
		for (key in properties.keys) {
			buf.append(String.format("%-30s : %s\n", key, properties[key]))
		}
		buf.append("Value=" + getValue())
		return buf.toString()
	}

	/**
	 *
	 */
	fun clear() {
		properties.clear()
		numProperties = 0
		value = Double.NEGATIVE_INFINITY
	}
}