package cc.lib.utils

import junit.framework.TestCase

/**
 * Created by chriscaron on 3/13/18.
 */
class TestUtils : TestCase() {
	internal open inner class X {
		var s: String? = null
	}

	private var x: X = object : X() {}
	fun testClassnames() {
		printClassnames(x.javaClass)
		println()
		printClassnames(x.javaClass.superclass)
	}

	private fun printClassnames(c: Class<*>) {
		println(c.toString())
		println(c.toGenericString())
		println(c.name)
		println(c.simpleName)
		println(c.typeName)
	}

	fun quoteMe(s: String): String {
		return "\"" + s + "\""
	}

	fun testPrettyString() {
		println(quoteMe(toPrettyString(";asihfva.kjvnakwhv")))
		println(quoteMe(toPrettyString("12324 hgjt $90")))
		println(quoteMe(toPrettyString("THIS_IS_A_TYPICAL_EXAMPLE")))
		println(quoteMe(toPrettyString("the quick br0wn fox jumped over the lazy brown dog")))
		println(quoteMe(toPrettyString("PLAYER1")))
		println(quoteMe(toPrettyString("00 001HELLO100 This is 10101010 test 0001")))
	}

	fun testWrapString() {
		val str = arrayOf<String>(
			"\nhello\n\ngoodbye\n",
			"",
			"a",
			"a".repeat(100),
			"the quick brown fox jumped over the lazy brown dog"
		)
		for (s in str) {
			val wrapped: String = s.wrap(10).joinToString("\n")
			println("$s->\n'$wrapped'")
		}
	}

	fun distSqPointLine(
		point_x: Double,
		point_y: Double,
		x0: Double,
		y0: Double,
		x1: Double,
		y1: Double
	): Double {
		// get the normal (N) to the line
		var nx = -(y1 - y0)
		var ny = x1 - x0
		if (Math.abs(nx) == 0.0 && Math.abs(ny) == 0.0) {
			val dx = point_x - x0
			val dy = point_y - y0
			return dx * dx + dy * dy
		}
		// normalize n
		val mag = Math.sqrt(nx * nx + ny * ny)
		nx /= mag
		ny /= mag

		// get the vector (L) from point to line
		val lx = point_x - x0
		val ly = point_y - y0

		// compute N dot N
		//double ndotn = (nx * nx + ny * ny);
		// compute N dot L
		val ndotl = nx * lx + ny * ly
		// get magnitude squared of vector of L projected onto N
		val px = nx * ndotl // / ndotn;
		val py = ny * ndotl // / ndotn;
		return Math.sqrt(px * px + py * py)
	}

	fun testTable() {
		val txt: Array<String> =
			"ThisIsThePartThatIsGettingTestedToMakeSureThereIsAHyphen The quick brown fox jumped over the lazy brown dog and then tried to eat a sandwich before going over to his friends house for some tea".wrap(
				20
			)

		// Try a table with 7 entries
		val t = Table()
		t.addRow(Table().addRow(Table().addColumnNoHeader(txt), Table().addColumnNoHeader(txt), Table().addColumnNoHeader(txt)))
		t.addRow(Table().addRow(Table().addColumnNoHeader(txt), Table().addColumnNoHeader(txt)))

		//t.addRow(new Table().addColumnNoHeader(txt), new Table().addColumnNoHeader(txt));
		println(t.toString(10))
	}

}
