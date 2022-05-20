package cc.android.game.robots

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import cc.android.game.robots.databinding.ProbotviewBinding
import cc.lib.android.CCActivityBase
import cc.lib.probot.Command
import cc.lib.probot.CommandType

/**
 * Created by chriscaron on 12/7/17.
 */
class ProbotActivity : CCActivityBase(), View.OnClickListener, OnTouchListener, Runnable {
	/*
	var pv: ProbotView? = null
	var lv: ProbotListView? = null
	var binding.ivArrowForward: View? = null
	var binding.ivArrowRight: View? = null
	var binding.ivArrowLeft: View? = null
	var binding.ivUTurn: View? = null
	var binding.ivArrowJump: View? = null
	var bPlay: View? = null
	var bStop: View? = null
	var bNext: View? = null
	var bPrevious: View? = null
	var tvLevel: TextView? = null
	var tvLevelName: TextView? = null
	var tvForwardCount: TextView? = null
	var tvTurnRightCount: TextView? = null
	var tvTurnLeftCount: TextView? = null
	var tvUTurnCount: TextView? = null
	var tvJumpCount: TextView? = null
	 */
	lateinit var actions: Array<Pair<View, TextView>>
	lateinit var binding: ProbotviewBinding
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ProbotviewBinding.inflate(LayoutInflater.from(this))
		setContentView(binding.root)
		binding.lvProgram.setProbot(binding.probotView.probot)
		binding.ivArrowForward.tag = CommandType.Advance
		binding.ivArrowRight.tag = CommandType.TurnRight
		binding.ivArrowLeft.tag = CommandType.TurnLeft
		binding.ivUTurn.tag = CommandType.UTurn
		binding.ivArrowJump.tag = CommandType.Jump
		binding.tvArrowForwardCount.tag = CommandType.Advance
		binding.tvArrowRightCount.tag = CommandType.TurnRight
		binding.tvArrowLeftCount.tag = CommandType.TurnLeft
		binding.tvUTurnCount.tag = CommandType.UTurn
		binding.tvJumpCount.tag = CommandType.Jump
		actions = arrayOf(
			binding.ivArrowJump to binding.tvJumpCount, 
			binding.ivArrowLeft to binding.tvArrowLeftCount, 
			binding.ivArrowRight to binding.tvArrowRightCount, 
			binding.ivArrowForward to binding.tvArrowForwardCount, 
			binding.ivUTurn to binding.tvUTurnCount
		)
		binding.ivArrowForward.setOnTouchListener(this)
		binding.ivArrowRight.setOnTouchListener(this)
		binding.ivArrowLeft.setOnTouchListener(this)
		binding.ivUTurn.setOnTouchListener(this)
		binding.ivArrowJump.setOnTouchListener(this)
		binding.ibPlay.setOnClickListener(this)
		binding.ibStop.setOnClickListener(this)
		binding.ibPrevious.setOnClickListener(this)
		binding.ibNext.setOnClickListener(this)
		val level = prefs.getInt("Level", 0)
		binding.probotView.maxLevel = prefs.getInt("MaxLevel", 0)
		setLevel(level)
	}

	private fun setLevel(level: Int) {
		binding.probotView.setLevel(level)
		refresh()
	}

	override fun onTouch(v: View, event: MotionEvent): Boolean {
		if (!binding.probotView.probot.isRunning) {
			val type = v.tag as CommandType
			val cmd = Command(type, 0)
			binding.lvProgram.startDrag(v, cmd)
		}
		return true
	}

	override fun run() {
		binding.tvLevel.text = (binding.probotView.probot.levelNum + 1).toString()
		binding.tvLevelName.text = binding.probotView.probot.level.label
		if (binding.probotView.probot.isRunning) {
			binding.ibPrevious.isEnabled = false
			binding.ibNext.isEnabled = false
			binding.ibPlay.visibility = View.GONE
			binding.ibStop.visibility = View.VISIBLE
		} else {
			binding.ibPrevious.isEnabled = binding.probotView.probot.levelNum > 0
			binding.ibNext.isEnabled = BuildConfig.DEBUG || binding.probotView.probot.levelNum < binding.probotView.maxLevel
			binding.ibPlay.visibility = View.VISIBLE
			binding.ibStop.visibility = View.GONE
		}
		for (a in actions) {
			val c = a.first.tag as CommandType
			a.first.visibility = if (binding.probotView.probot.isCommandTypeVisible(c)) View.VISIBLE else View.GONE
			a.second.isEnabled = binding.probotView.probot.getCommandTypeNumAvaialable(c) != 0
		}
		for (tv in actions) {
			val c = tv.second.tag as CommandType
			if (binding.probotView.probot.isCommandTypeVisible(c)) {
				val count = binding.probotView.probot.getCommandTypeNumAvaialable(c)
				if (count < 0) {
					tv.second.visibility = View.GONE
				} else {
					tv.second.visibility = View.VISIBLE
					tv.second.text = count.toString()
				}
			} else {
				tv.second.visibility = View.GONE
			}
		}
	}

	fun refresh() {
		run()
	}

	fun postRefresh() {
		runOnUiThread(this)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.ibStop -> binding.probotView.probot.stop()
			R.id.ibPlay -> if (binding.probotView.probot.size() > 0) {
				object : Thread() {
					override fun run() {
						postRefresh()
						binding.probotView.probot.runProgram()
						postRefresh()
					}
				}.start()
			}
			R.id.ibPrevious -> if (binding.probotView.probot.levelNum > 0) {
				setLevel(binding.probotView.probot.levelNum - 1)
			}
			R.id.ibNext -> if (binding.probotView.probot.levelNum < binding.probotView.maxLevel) {
				setLevel(binding.probotView.probot.levelNum + 1)
			}
		}
		refresh()
	}
}