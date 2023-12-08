package cc.lib.kreflector

import java.io.BufferedReader
import java.io.EOFException
import java.io.IOException
import java.io.Reader

class RBufferedReader internal constructor(reader: Reader) : BufferedReader(reader) {
	private var markedLineNum = 0
	var lineNum = 0
	var depth = 0

	@Throws(IOException::class)
	override fun readLine(): String? {
		lineNum++
		return try {
			var line = super.readLine()
			if (line == null) {
				if (depth > 0) throw EOFException()
				return null
			}
			line = line.trim { it <= ' ' }
			if (line.endsWith("{")) {
				depth++
				return line.substring(0, line.length - 1).trim { it <= ' ' }
			}
			if (line.endsWith("}")) {
				depth--
				return null
			}
			line
		} catch (e: IOException) {
			throw IOException("Error on line: " + lineNum + " " + e.message, e)
		}
	}

	@Throws(IOException::class)
	override fun mark(readAheadLimit: Int) {
		super.mark(readAheadLimit)
		markedLineNum = lineNum
	}

	@Throws(IOException::class)
	override fun reset() {
		super.reset()
		lineNum = markedLineNum
	}

	@Throws(IOException::class)
	fun peekLine(): String {
		return try {
			mark(1024)
			super.readLine()
		} finally {
			reset()
		}
	}

	fun readLineOrThrowEOF(): String {
		return readLineOrEOF() ?: throw EOFException()
	}

	@Throws(IOException::class)
	fun readLineOrEOF(): String? {
		while (true) {
			var line = readLine() ?: return null
			line = line.trim { it <= ' ' }
			if (line.isEmpty() || line.startsWith("#")) continue
			return line
		}
	}

	@Throws(IOException::class)
	fun readLineAndClosedParen(): String {
		val value = readLineOrEOF()
		val line = readLineOrEOF()
		if (line != null) throw IOException("Expected closing paren } but found: $line")
		return value ?: throw IOException("Non-Empty value expected")
	}
}