package cc.lib.kreflector

import java.io.OutputStream
import java.io.PrintWriter
import java.io.Writer

class RPrintWriter : PrintWriter {
	val numbered: Boolean
	var lineNum = 0
	var indented = false

	@JvmOverloads
	constructor(out: Writer?, numbered: Boolean = false) : super(out, true) {
		this.numbered = numbered
	}

	@JvmOverloads
	constructor(out: OutputStream?, numbered: Boolean = false) : super(out, true) {
		this.numbered = numbered
	}

	private var currentIndent = 0
	fun push() {
		println(" {")
		if (currentIndent < indents.size - 1) currentIndent++
	}

	fun pop() {
		require(currentIndent > 0)
		if (currentIndent > 0) currentIndent--
		println("}")
	}

	override fun write(s: String) {
		if (!indented) {
			if (numbered) super.write(String.format("%-5d:", lineNum++))
			super.write(indents[currentIndent])
			indented = true
		}
		super.write(s)
	}

	override fun println() {
		super.println()
		indented = false
	}

	fun p(o: Any): RPrintWriter {
		write(o.toString())
		return this
	}

	companion object {
		var indents: Array<String?>

		init {
			indents = arrayOfNulls(32)
			var indent = ""
			for (i in indents.indices) {
				indents[i] = indent
				indent += "   "
			}
		}
	}
}