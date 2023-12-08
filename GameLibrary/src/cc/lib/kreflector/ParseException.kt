package cc.lib.kreflector

import java.io.IOException

class ParseException : IOException {
	val lineNum: Int

	internal constructor(lineNum: Int, msg: String?) : super(msg) {
		this.lineNum = lineNum
	}

	internal constructor(lineNum: Int, e: Exception?) : super(e) {
		this.lineNum = lineNum
	}

	override val message: String
		get() = "Line (" + lineNum + ") " + super.message
}