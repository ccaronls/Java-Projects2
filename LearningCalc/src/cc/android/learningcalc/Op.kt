package cc.android.learningcalc

import android.content.Context
import java.math.BigInteger

enum class Op {
	ADD,
	SUB,
	MULT,
	DIV;

	fun solve(a: Int, b: Int): Int {
		return when (this) {
			ADD -> a + b
			DIV -> a / b
			MULT -> a * b
			SUB -> a - b
		}
		return 0
	}

	fun solve(a: BigInteger, b: BigInteger): BigInteger? {
		try {
			return when (this) {
				ADD -> a.add(b)
				DIV -> a.divide(b)
				MULT -> a.multiply(b)
				SUB -> a.subtract(b)
			}
		} catch (e: ArithmeticException) {
			e.printStackTrace()
		}
		return null
	}

	fun sign(c: Context): String {
		return when (this) {
			ADD -> "+"
			DIV -> c.getString(R.string.div_symbol)
			MULT -> "X"
			SUB -> "-"
		}
		return "?"
	}

	fun tts(): String {
		return when (this) {
			ADD -> "plus"
			DIV -> "divided by"
			MULT -> "times"
			SUB -> "minus"
		}
		return "um"
	}
}