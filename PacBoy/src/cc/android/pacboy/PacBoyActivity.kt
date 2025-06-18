package cc.android.pacboy

import android.os.Bundle
import android.view.View
import android.widget.TextView
import cc.lib.android.CCActivityBase

class PacBoyActivity : CCActivityBase() {
	private var pbv: PacBoyView? = null
	private var tvScore: TextView? = null
	private var tvHighScore: TextView? = null
	private var highScore = 0
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.pacboy_activity)
		pbv = findViewById<View>(R.id.pacBoyView) as PacBoyView
		tvScore = findViewById<View>(R.id.textViewScore) as TextView
		tvHighScore = findViewById<View>(R.id.textViewHighScore) as TextView
	}

	override fun onResume() {
		super.onResume()
		val width = intent.getIntExtra(INTENT_EXTRA_INT_WIDTH, 10)
		val height = intent.getIntExtra(INTENT_EXTRA_INT_HEIGHT, 10)
		val difficulty = intent.getIntExtra(INTENT_EXTRA_INT_DIFFUCULTY, 0)
		pbv!!.initMaze(width, height, difficulty)
		pbv!!.setPaused(false)
		highScore = prefs.getInt(PREF_HIGH_SCORE_INT, 0)
		tvHighScore!!.text = "" + highScore
		tvScore!!.text = "0"
		startPolling(1)
	}

	override fun onPause() {
		super.onPause()
		pbv!!.setPaused(true)
		val score = pbv!!.score
		val highScore = prefs.getInt(PREF_HIGH_SCORE_INT, 0)
		if (score > highScore) {
			prefs.edit().putInt(PREF_HIGH_SCORE_INT, score).commit()
		}
	}

	override fun onPoll() {
		val score = pbv!!.score
		tvScore!!.text = "" + score
		if (score > highScore) {
			highScore = score
			tvHighScore!!.text = "" + score
		}
		if (pbv!!.difficulty < PacBoyRenderer.DIFFICULTY_NO_CHASE) {
			tvHighScore!!.visibility = View.INVISIBLE
			tvScore!!.visibility = View.INVISIBLE
		} else {
			tvHighScore!!.visibility = View.VISIBLE
			tvScore!!.visibility = View.VISIBLE
		}
	}

	companion object {
		const val INTENT_EXTRA_INT_WIDTH = "width"
		const val INTENT_EXTRA_INT_HEIGHT = "height"
		const val INTENT_EXTRA_INT_DIFFUCULTY = "difficulty"
		private const val PREF_HIGH_SCORE_INT = "HIGH_SCORE"
	}
}
