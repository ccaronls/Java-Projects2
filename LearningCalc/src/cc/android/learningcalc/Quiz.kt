package cc.android.learningcalc

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import cc.lib.game.Utils

class Quiz {
	private val buttons = CharArray(10)
	private var numButtons = 0
	private var numTypes = 0
	private var question: Spannable? = null
	private var qmarkStart = 0
	private var qmarkEnd = 0
	private var answerStart = 0
	private val start = System.currentTimeMillis()
	private var attemptsLeft = 3

	internal constructor()
	internal constructor(question: String, solution: Int, spanStart: Int, spanLen: Int) : this(question, solution) {
		answerStart = spanStart
		qmarkStart = answerStart
		qmarkEnd = qmarkStart + spanLen
	}

	internal constructor(question: String, solution: Int) {
		var solution = solution
		var isNeg = false
		if (solution < 0) {
			solution = -solution
			isNeg = true
		}
		while (solution > 0) {
			Utils.pushFirst(Character.forDigit(solution % 10, 10), buttons)
			numButtons++
			solution = solution / 10
		}
		if (isNeg) {
			Utils.pushFirst('-', buttons)
			numButtons++
		}
		if (numButtons == 0) {
			buttons[numButtons++] = 0.toChar()
		}
		this.question = SpannableString(question)
		while (qmarkStart < question.length) {
			if (question[qmarkStart] == '?') {
				break
			}
			qmarkStart++
		}
		qmarkEnd = qmarkStart + numButtons
		answerStart = qmarkStart
	}

	@get:Synchronized
	val spanned: Spannable?
		get() {
			var dt = System.currentTimeMillis() - start
			val pulseSpeed: Long = 3000
			var scale = 0f
			while (dt > pulseSpeed) {
				dt -= pulseSpeed
			}
			scale = if (dt < pulseSpeed / 2) {
				// forward
				(dt * 2).toFloat() / pulseSpeed
			} else {
				// reverse
				1.0f - ((dt - pulseSpeed / 2) * 2).toFloat() / pulseSpeed
			}

			/*
		int red = 255;
		int grn = (int)Utils.clamp(scale * 0 + (1f-scale)*255, 0, 255);
		int blu = (int)Utils.clamp(scale * 0 + (1f-scale)*255, 0, 255);
		
		int c = Color.argb(255, red, grn, blu);
		*/
			//String s = String.format(html, Integer.toHexString(c));
			//System.out.println("scale = " + scale + " HTML=" + s);
			//return s;
			val c = Color.argb(Math.round(255 * scale), 255, 0, 0)
			question!!.setSpan(ForegroundColorSpan(c), qmarkStart, qmarkEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
			return question
		}

	fun getCharFromButton(buttonId: Int): Char {
		for (i in buttonMap.indices) {
			if (buttonId == buttonMap[i][0]) return buttonMap[i][1].toChar()
		}
		return Char(0)
	}

	enum class TestResult {
		GOOD,
		WRONG,
		KEEP_GOING,
		TRY_AGAIN
	}

	@Synchronized
	fun test(buttonId: Int): TestResult {
		val button = buttons[numTypes]
		if (button == getCharFromButton(buttonId)) {
			numTypes++
			if (numTypes == numButtons) {
				return TestResult.GOOD
			}
			//html = html.replaceFirst("[\\?]", String.valueOf(button));
			qmarkStart++
			question!!.setSpan(ForegroundColorSpan(Color.GREEN), answerStart, qmarkStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
			return TestResult.KEEP_GOING // more
		} else if (--attemptsLeft > 0) {
			return TestResult.TRY_AGAIN
		}
		return TestResult.WRONG // fail
	}

	companion object {
		val buttonMap =
			arrayOf(intArrayOf(R.id.button_minus, '-'.code), intArrayOf(R.id.button0, '0'.code), intArrayOf(R.id.button1, '1'.code), intArrayOf(R.id.button2, '2'.code), intArrayOf(R.id.button3, '3'.code), intArrayOf(R.id.button4, '4'.code), intArrayOf(R.id.button5, '5'.code), intArrayOf(R.id.button6, '6'.code), intArrayOf(R.id.button7, '7'.code), intArrayOf(R.id.button8, '8'.code), intArrayOf(R.id.button9, '9'.code))
	}
}