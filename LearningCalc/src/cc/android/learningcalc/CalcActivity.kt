package cc.android.learningcalc

import android.content.DialogInterface
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.ScaleAnimation
import android.widget.EditText
import android.widget.TextView
import cc.lib.android.CCActivityBase
import cc.lib.game.Utils
import cc.lib.utils.hasAllOrNull
import java.math.BigInteger

class CalcActivity : CCActivityBase(), View.OnClickListener, OnInitListener {
	private var quiz: Quiz? = null
	private var score = 0
	private var nextQuizScore = QUIZ_SCORE_STEP
	private var num1: BigInteger? = null
	private var num2: BigInteger? = null
	private var op: Op? = null
	private var et: EditText? = null
	private var tvScore: TextView? = null
	private var tts: TextToSpeech? = null
	private var quizGoodET: Spannable? = null
	private var quizGoodSay: String? = null

	private inner class MyHandler : Handler() {
		override fun handleMessage(msg: Message) {
			when (msg.what) {
				TRIGGER_NUMBERS_ANIM -> {
					triggerPulseAnimNumbersAnim()
					sendEmptyMessageDelayed(TRIGGER_NUMBERS_ANIM, (Utils.randRange(8, 16) * 1000).toLong())
				}

				TRIGGER_OPERATIONS_ANIM -> {
					triggerPulseAnimOperationsAnim()
					sendEmptyMessageDelayed(TRIGGER_OPERATIONS_ANIM, (Utils.randRange(8, 16) * 1000).toLong())
				}

				TRIGGER_QUIZ -> {
					when (Utils.rand() % 3) {
						0 -> sayNow("I'm the Quiz! I'm the Quiz and nobody beats me!")
						1 -> sayNow("pop quiz hot shot")
						2 -> sayNow("quiz time")
					}
					generate()
					handler.sendEmptyMessage(TRIGGER_QUIZ_PULSE)
				}

				TRIGGER_QUIZ_PULSE -> {
					if (quiz != null) {
						et!!.setText(quiz!!.spanned) //Html.fromHtml(quiz.getHtml()));
						handler.sendEmptyMessageDelayed(TRIGGER_QUIZ_PULSE, 30)
					}
				}

				TRIGGER_SAY_REPEAT -> {
					sayQ(msg.obj.toString())
					sendMessageDelayed(obtainMessage(TRIGGER_SAY_REPEAT, msg.obj), (5 * 1000).toLong())
				}

				TRIGGER_INACTIVITY -> {
					val sol = Utils.randRange(-100, -10) //Utils.randRange(10, 99*score);
					sayNow("Can you type the number $sol")
					quizGoodSay = "Great job!"
					quiz = Quiz(String.format("%d", sol), sol, 0, sol.toString().length)
					handler.sendEmptyMessage(TRIGGER_QUIZ_PULSE)
				}

				else -> findViewById<View>(msg.what).startAnimation(newPulseAnimation(2000, 1, 800, null))
			}
		}
	}

	private val handler: Handler = MyHandler()
	var buttons = intArrayOf(
		R.id.button1,
		R.id.button2,
		R.id.button3,
		R.id.button_plus,
		R.id.button4,
		R.id.button5,
		R.id.button6,
		R.id.button_minus,
		R.id.button7,
		R.id.button8,
		R.id.button9,
		R.id.button_mult,
		R.id.button_c,
		R.id.button0,
		R.id.button_eq,
		R.id.button_div)

	override fun onCreate(b: Bundle?) {
		super.onCreate(b)
		setContentView(R.layout.calc)
		et = findViewById<View>(R.id.editText1) as EditText
		tvScore = findViewById<View>(R.id.tvScore) as TextView
		for (i in buttons) {
			val button = findViewById<View>(i) as TextView
			button.setOnClickListener(this)
			//button.setText(new AnimatingSpannableString(button));
		}
		reset()
		if (b != null) {
			et!!.setText(b.getString("text", ""))
			clearEquation()
		}
		findViewById<View>(R.id.buttonInfo).setOnClickListener(this)
		tts = TextToSpeech(this, this)
		tts!!.setLanguage(resources.configuration.locale)
	}

	public override fun onResume() {
		super.onResume()
		handler.sendEmptyMessage(TRIGGER_NUMBERS_ANIM)
		handler.sendEmptyMessage(TRIGGER_OPERATIONS_ANIM)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString("text", et!!.text.toString())
	}

	override fun onPause() {
		super.onPause()
		handler.removeCallbacksAndMessages(null)
	}

	override fun onDestroy() {
		super.onDestroy()
		if (tts != null) {
			tts!!.shutdown()
			tts = null
		}
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		val audio = getSystemService(AUDIO_SERVICE) as AudioManager ?: return false
		val type = AudioManager.STREAM_MUSIC
		var vol = audio.getStreamVolume(type)
		val max = audio.getStreamMaxVolume(type)
		val step = max / 10
		when (keyCode) {
			KeyEvent.KEYCODE_VOLUME_UP -> {
				vol = Math.min(max, vol + step)
				audio.setStreamVolume(type, vol, AudioManager.FLAG_SHOW_UI)
			}

			KeyEvent.KEYCODE_VOLUME_DOWN -> {
				vol = Math.max(0, vol - step)
				audio.setStreamVolume(type, vol, AudioManager.FLAG_SHOW_UI)
			}

			KeyEvent.KEYCODE_VOLUME_MUTE -> audio.setStreamVolume(type, 0, AudioManager.FLAG_SHOW_UI)
			else -> return super.onKeyDown(keyCode, event)
		}
		sayNow("Hello")
		return true // we handled the event
	}

	fun sayNow(txt: String?) {
		if (tts != null && txt != null) {
			tts!!.stop()
			tts!!.speak(txt, TextToSpeech.QUEUE_ADD, null)
		}
	}

	fun sayQ(txt: String?) {
		if (tts != null && txt != null) {
			tts!!.speak(txt, TextToSpeech.QUEUE_ADD, null)
		}
	}

	fun triggerPulseAnimNumbersAnim() {
		handler.sendEmptyMessageDelayed(R.id.button1, 200)
		handler.sendEmptyMessageDelayed(R.id.button2, 200)
		handler.sendEmptyMessageDelayed(R.id.button3, 200)
		handler.sendEmptyMessageDelayed(R.id.button4, 500)
		handler.sendEmptyMessageDelayed(R.id.button5, 500)
		handler.sendEmptyMessageDelayed(R.id.button6, 500)
		handler.sendEmptyMessageDelayed(R.id.button7, 800)
		handler.sendEmptyMessageDelayed(R.id.button8, 800)
		handler.sendEmptyMessageDelayed(R.id.button9, 800)
		handler.sendEmptyMessageDelayed(R.id.button0, 1100)
	}

	fun triggerPulseAnimOperationsAnim() {
		handler.sendEmptyMessageDelayed(R.id.button_plus, 2500)
		handler.sendEmptyMessageDelayed(R.id.button_minus, 2500)
		handler.sendEmptyMessageDelayed(R.id.button_mult, 2500)
		handler.sendEmptyMessageDelayed(R.id.button_div, 2500)
	}

	fun endQuiz() {
		quiz = null
		handler.removeCallbacksAndMessages(null)
		handler.sendEmptyMessageDelayed(TRIGGER_INACTIVITY, (20 * 1000).toLong())
		et!!.setText("")
	}

	override fun onClick(v: View) {
		if (v.id == R.id.buttonInfo) {
			showInfoDialog()
			return
		}
		if (v is TextView) {
			val tv = v
			tv.text = AnimatingSpannableString(tv, 1000, 1, true)
		}
		if (quiz != null) {
			when (quiz!!.test(v.id)) {
				Quiz.TestResult.KEEP_GOING -> sayNow("Keep going")
				Quiz.TestResult.TRY_AGAIN -> sayNow("Try again")
				Quiz.TestResult.GOOD -> {
					et!!.setText(quizGoodET)
					sayNow(quizGoodSay)
					incrementScore(1)
					nextQuizScore += QUIZ_SCORE_STEP
					endQuiz()
				}

				Quiz.TestResult.WRONG -> {
					sayNow("incorrect")
					incrementScore(-3)
					endQuiz()
				}
			}
			return
		}
		when (v.id) {
			R.id.button1 -> tryAddNumber(1)
			R.id.button2 -> tryAddNumber(2)
			R.id.button3 -> tryAddNumber(3)
			R.id.button_plus -> trySetOp(Op.ADD)
			R.id.button4 -> tryAddNumber(4)
			R.id.button5 -> tryAddNumber(5)
			R.id.button6 -> tryAddNumber(6)
			R.id.button_minus -> if (num1 != null) trySetOp(Op.SUB) else if (et!!.text.toString().trim { it <= ' ' }.isEmpty()) {
				sayNow("negative")
				et!!.setText("-")
			}

			R.id.button7 -> tryAddNumber(7)
			R.id.button8 -> tryAddNumber(8)
			R.id.button9 -> tryAddNumber(9)
			R.id.button_mult -> trySetOp(Op.MULT)
			R.id.button_c -> {
				sayNow("clear")
				reset()
				clearEquation()
			}

			R.id.button0 -> tryAddNumber(0)
			R.id.button_eq -> Triple(op, num1, num2).hasAllOrNull()?.let { (op, n1, n2) ->
				trySolve(op, n1, n2)
			}

			R.id.button_div -> trySetOp(Op.DIV)
		}
	}

	fun tryAddNumber(num: Int) {
		var num = num
		if (op == null) {
			if (clearOnNumber) {
				num1 = BigInteger.valueOf(0)
				clearOnNumber = false
			}
			if (et!!.text.toString().startsWith("-") && et!!.text.toString().length == 1) num = -num
			num1 = addNumber(num, num1)
		} else {
			num2 = addNumber(num, num2)
		}
		resetEquation()
	}

	fun addNumber(num: Int, target: BigInteger?): BigInteger? {
		var target = target
		if (target == null) {
			target = BigInteger.valueOf(num.toLong())
		} else {
			target = target.multiply(BigInteger.TEN)
			target = target.add(BigInteger.valueOf(num.toLong()))
		}
		sayNow(target.toString())
		return target
	}

	fun trySetOp(op: Op) {
		sayNow(op.tts())
		if (num1 != null) {
			this.op = op
			resetEquation()
		}
	}

	fun clearEquation() {
		var line = et!!.text.toString()
		val endl = line.indexOf('\n')
		if (endl > 0) {
			line = line.substring(endl)
			et!!.setText(line)
		} else {
			et!!.setText("")
		}
	}

	fun resetEquation() {
		var text = et!!.text.toString()
		val endl = text.indexOf('\n')
		text = if (endl >= 0) {
			text.substring(endl)
		} else {
			""
		}
		var line = ""
		if (num1 != null) {
			line += num1.toString()
		}
		if (op != null) {
			line += " "
			line += op!!.sign(this)
		}
		if (num2 != null) {
			line += " "
			line += num2.toString()
		}
		if (line.length > 0) {
			et!!.setText(line + text)
		}
	}

	var clearOnNumber = false
	fun trySolve(op: Op, n1: BigInteger, n2: BigInteger) {
		val n = op.solve(n1, n2)
		if (n != null) {
			sayNow("equals $n")
			incrementScore(op.ordinal + 1)
			reset()
			append(n.toString())
			num1 = n
		} else {
			reset()
			append("NAN")
			sayNow("Not a number")
		}
		clearOnNumber = true
	}

	fun append(s: String?) {
		if (s == null || s.trim { it <= ' ' }.length == 0) return
		val line = et!!.text.toString()
		et!!.setText("""
	$s
	$line
	""".trimIndent())
	}

	fun reset() {
		num2 = null
		num1 = num2
		op = null
	}

	val greetings: String
		get() = prefs.getString("GREETINGS", null) ?: getString(R.string.default_greeting)

	override fun onInit(status: Int) {
		when (status) {
			TextToSpeech.SUCCESS -> sayNow(greetings)
			else -> Log.e("TTS", "Failed to init.  status=$status")
		}
	}

	fun newPulseAnimation(delay: Long, repeat: Int, duration: Long, listener: Animation.AnimationListener?): Animation {
		val anim = ScaleAnimation(1f, 1.1f, 1f, 1.1f)
		anim.duration = duration
		anim.interpolator = LinearInterpolator()
		anim.repeatCount = repeat
		anim.repeatMode = Animation.REVERSE
		anim.startTime = AnimationUtils.currentAnimationTimeMillis() + delay
		anim.setAnimationListener(listener)
		return anim
	}

	fun generateBasic(n1: Int, n2: Int, op: Op) {
		var s = op.solve(n1, n2)
		var neg = ""
		if (s < 0) {
			neg = "-"
			s = -s
		}
		sayQ(String.format("%s %s %s equals what", n1, op.tts(), n2))
		quiz =
			Quiz(String.format("QUIZ TIME\n%d %s %d = %s%s", n1, op.sign(this), n2, neg, Utils.getRepeatingChars('?', s.toString().length)), s)
		quizGoodSay = String.format("%s %s %s equals %s. Great job.", n1, op.sign(this), n2, op.solve(n1, n2))
		quizGoodET = SpannableString(String.format("%s %s %s = %s", n1, op.sign(this), n2, s)).apply {
			setSpan(ForegroundColorSpan(Color.GREEN), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
		}
	}

	fun generate() {
		if (score < 20) {
			generateBasic(Utils.randRange(1, 3), Utils.randRange(1, 3), Op.ADD)
		} else if (score < 30) {
			// (2-5) + (2-5) = ?
			generateBasic(Utils.randRange(2, 5), Utils.randRange(2, 5), Op.ADD)
		} else if (score < 40) {
			// (2-4) - (1-3) = ?
			generateBasic(Utils.randRange(3, 12), Utils.randRange(5, 20), Op.ADD)
		} else if (score < 50) {
			// (3-6) - (2-6) = ?
			generateBasic(Utils.randRange(3, 5), Utils.randRange(1, 3), Op.SUB)
		} else if (score < 60) {
			// (2-8) +/- (2-8) = ?
			generateBasic(Utils.randRange(5, 10), Utils.randRange(3, 5), Op.SUB)
		} else if (score < 70) {
			// (1-3) * (1-3) = ?
			generateBasic(Utils.randRange(10, 20), Utils.randRange(5, 9), Op.SUB)
		} else if (score < 80) {
			// (2-5) * (2-5) = ?
			generateBasic(Utils.randRange(5, 20), Utils.randRange(5, 20), if (Utils.flipCoin()) Op.SUB else Op.ADD)
		} else if (score < 90) {
			generateBasic(Utils.randRange(10, 100), Utils.randRange(10, 100), if (Utils.flipCoin()) Op.SUB else Op.ADD)
			// ? + (1-3) = (3-5)
		} else if (score < 100) {
			generateBasic(Utils.randRange(1, 100), Utils.randRange(1, 100), if (Utils.flipCoin()) Op.SUB else Op.ADD)
			// (1-3) + ? = (3-5)
		} else if (score < 110) {
			// (3-5) - ? = (1-5)
		} else if (score < 120) {
			generateBasic(Utils.randRange(2, 5), Utils.randRange(2, 5), Op.MULT)
			// (1-3) ? (1-3) = (1-9)
		} else if (score < 130) {
			// (3-5) / (1-3) = ?
		} else if (score < 140) {
			// (1-9)? + (1-9) = (2-18)
		} else if (score < 150) {
			// ?(0-9) + (1-9) = (1-108)
		} else if (score < 160) {
			// ?(0-9) ? (1-9) = (1-108)
		} else {
			generateBasic(Utils.randRange(0, 1000), Utils.randRange(0, 1000), Utils.randItem(Op.entries.toTypedArray()))
		}
		et!!.setText(quiz!!.spanned)
	}

	private fun incrementScore(amt: Int) {
		//TextView tv = ((TextView)findViewById(R.id.tvScore)).setText("" + score);
		score = Math.max(0, amt.let { score += it; score })
		if (score >= nextQuizScore) {
			handler.sendEmptyMessageDelayed(TRIGGER_QUIZ, 1000)
		} else if (score < nextQuizScore - QUIZ_SCORE_STEP) {
			nextQuizScore -= QUIZ_SCORE_STEP
		}
		if (score > 0) {
			tvScore!!.text = String.format("%d (%d)", score, nextQuizScore)
		} else {
			tvScore!!.text = ""
		}
	}

	var isQuizEnabled: Boolean
		get() = prefs.getBoolean("QUIZ_ENABLED", true)
		set(enabled) {
			prefs.edit().putBoolean("QUIZ_ENABLED", enabled).apply()
		}

	fun showInfoDialog() {
		newDialogBuilder().setItems(R.array.options_dialog) { dialog, which ->
			when (which) {
				0 -> newDialogBuilder().setMessage(getString(R.string.about_msg, appVersionFromManifest))
					.setNeutralButton(R.string.cancel, null).show()

				1 -> {
					val v = View.inflate(this@CalcActivity, R.layout.edit_greetings_view, null)
					val et = v.findViewById<View>(R.id.edit_greetings) as EditText
					et.setText(prefs.getString("GREETINGS", ""), TextView.BufferType.NORMAL)
					et.selectAll()
					newDialogBuilder().setView(v)
						.setNegativeButton(R.string.cancel, null)
						.setPositiveButton(R.string.apply) { dialog, which ->
							val text = et.text.toString()
							if (text.isEmpty()) {
								prefs.edit().remove("GREETINGS").apply()
							} else {
								prefs.edit().putString("GREETINGS", text).apply()
							}
						}.show()
				}

				2 -> newDialogBuilder().setMessage(R.string.quiz_dialog_msg)
					.setNegativeButton(R.string.disable, object : DialogInterface.OnClickListener {
						override fun onClick(dialog: DialogInterface, which: Int) {
							isQuizEnabled = false
						}
					}).setPositiveButton(R.string.enable, object : DialogInterface.OnClickListener {
						override fun onClick(dialog: DialogInterface, which: Int) {
							isQuizEnabled = true
						}
					}).show()
			}
		}.show()
	}

	companion object {
		const val TRIGGER_NUMBERS_ANIM = -100
		const val TRIGGER_OPERATIONS_ANIM = -101
		const val TRIGGER_QUIZ = -102
		const val TRIGGER_QUIZ_PULSE = -103
		const val TRIGGER_SAY_REPEAT = -104
		const val TRIGGER_INACTIVITY = -105
		var QUIZ_SCORE_STEP = 10
	}
}
