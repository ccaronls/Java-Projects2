package cc.android.game.robots

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.android.game.robots.databinding.ProbotviewBinding
import cc.lib.android.CCActivityBase
import cc.lib.android.LayoutFactory
import cc.lib.probot.*
import cc.lib.utils.Reflector
import kotlin.math.max

class ProbotViewModel : ViewModel() {

	val advanceVisible = MutableLiveData(false)
	val advanceCount  = MutableLiveData(0)

	val leftVisible = MutableLiveData(false)
	val leftCount  = MutableLiveData(0)

	val rightVisible = MutableLiveData(false)
	val rightCount  = MutableLiveData(0)

	val uturnVisible = MutableLiveData(false)
	val uturnCount  = MutableLiveData(0)

	val jumpVisible = MutableLiveData(false)
	val jumpCount  = MutableLiveData(0)

	val levelName = MutableLiveData("???")
	val running = MutableLiveData(false)
	val maxLevel = MutableLiveData(0)
	val level = MutableLiveData(0)
}


/**
 * Created by chriscaron on 12/7/17.
 */
class ProbotActivity : CCActivityBase() {

	lateinit var binding: ProbotviewBinding
	lateinit var viewModel: ProbotViewModel
	lateinit var adapter: ProbotAdapter

	private val levels: List<Level> by lazy {
		assets.open("levels.txt").use {
			Reflector.deserializeFromInputStream(it)
		}
	}

	@JvmField
	var probot: Probot = object : Probot() {
		override fun onCommand(line: Int) {
			Log.d("ProbotView", "onCommand: $line")
			adapter.setProgramLineNum(line)
			when (get(line).type) {
				CommandType.LoopStart,
				CommandType.LoopEnd -> Thread.sleep(500)
			}
		}

		override fun onFailed() {
			adapter.markFailed()
			binding.probotView.postInvalidate()
		}

		override fun onDotsLeftUneaten() {
			for (guy in getGuys())
				binding.probotView.startFailedAnim(guy)
			lock.acquireAndBlock()
		}

		override fun onAdvanced(guy: Guy) {
			super.onAdvanced(guy)
			binding.probotView.startAdvanceAnim(guy)
			lock.acquireAndBlock()
		}

		override fun onAdvanceFailed(guy: Guy) {
			binding.probotView.startFailedAnim(guy)
			lock.acquireAndBlock()
		}

		override fun onJumped(guy: Guy) {
			binding.probotView.startJumpAnim(guy)
			lock.acquireAndBlock()
		}

		override fun onTurned(guy: Guy, dir: Int) {
			binding.probotView.startTurnAnim(guy, dir)
			lock.acquireAndBlock()
		}

		override fun onLazered(guy: Guy, type: Int) {
			binding.probotView.startLazeredAnim(guy, type == 0)
			lock.acquireAndBlock()
		}

		override fun onSuccess() {
			for (guy in getGuys())
				binding.probotView.startSuccessAnim(guy)
			lock.acquireAndBlock()
			nextLevel()
		}

		override fun stop() {
			super.stop()
			adapter.setProgramLineNum(-1)
			binding.probotView.stopAnimations()
		}

		override fun onChanged() {
			super.onChanged()
			adapter.postRefresh()
			refresh()
		}
	}

	fun nextLevel() {
		setLevel(probot.levelNum + 1)
	}

	override fun getLayoutFactory(): LayoutFactory {
		return LayoutFactory(this, R.layout.probotview, ProbotViewModel::class.java)
	}

	override fun onLayoutCreated(binding: ViewDataBinding, viewModel: ViewModel) {
		this.binding = binding as ProbotviewBinding
		this.viewModel = viewModel as ProbotViewModel
		binding.viewModel = viewModel
		binding.activity = this
		setContentView(binding.root)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding.probotView.probot = probot
		adapter = ProbotAdapter(probot, binding.listView)
		adapter.addDraggable(binding.ivArrowForward, Command(CommandType.Advance, 0))
		adapter.addDraggable(binding.ivArrowRight, Command(CommandType.TurnRight, 0))
		adapter.addDraggable(binding.ivArrowLeft, Command(CommandType.TurnLeft, 0))
		adapter.addDraggable(binding.ivUTurn, Command(CommandType.UTurn, 0))
		adapter.addDraggable(binding.ivArrowJump, Command(CommandType.Jump, 0))
		setLevel(prefs.getInt("Level", 0))
	}

	private fun setLevel(level: Int) {
		var level = level
		val maxLevel = max(level, prefs.getInt("MaxLevel", 0))
		val showInfo = level == maxLevel
		//max(viewModel.maxLevel.value?:0, level)
		viewModel.maxLevel.postValue(maxLevel)
		if (level >= levels.size)
			level = 0
		prefs.edit()
			.putInt("Level", level)
			.putInt("MaxLevel", maxLevel)
			.apply()
		viewModel.maxLevel.postValue(maxLevel)
		viewModel.level.postValue(level)
		probot.setLevel(level, levels[level].deepCopy())
		probot.start()
		adapter.setProgramLineNum(-1)
		refresh()
		binding.probotView.postInvalidate()
		if (showInfo)
			newDialogBuilder()
				.setTitle(levels[level].label)
				.setMessage(levels[level].info)
				.setNegativeButton(R.string.popup_button_ok, null)
				.show()
	}

	fun refresh() {
		viewModel.levelName.postValue(probot.level.label)
		viewModel.running.postValue(probot.isRunning)

		viewModel.advanceVisible.postValue(probot.isCommandTypeVisible(CommandType.Advance))
		viewModel.advanceCount.postValue(probot.getCommandTypeNumAvaialable(CommandType.Advance))

		viewModel.leftVisible.postValue(probot.isCommandTypeVisible(CommandType.TurnLeft))
		viewModel.leftCount.postValue(probot.getCommandTypeNumAvaialable(CommandType.TurnLeft))

		viewModel.rightVisible.postValue(probot.isCommandTypeVisible(CommandType.TurnRight))
		viewModel.rightCount.postValue(probot.getCommandTypeNumAvaialable(CommandType.TurnRight))

		viewModel.jumpVisible.postValue(probot.isCommandTypeVisible(CommandType.Jump))
		viewModel.jumpCount.postValue(probot.getCommandTypeNumAvaialable(CommandType.Jump))

		viewModel.uturnVisible.postValue(probot.isCommandTypeVisible(CommandType.UTurn))
		viewModel.uturnCount.postValue(probot.getCommandTypeNumAvaialable(CommandType.UTurn))
	}

	fun onPlayClicked(view: View) {
		if (probot.size > 0) {
			object : Thread() {
				override fun run() {
					refresh()
					probot.runProgram()
					refresh()
				}
			}.start()
		}
	}

	fun onStopClicked(view: View) {
		probot.stop()
		refresh()
	}

	fun onPrevClicked(view: View) {
		if (probot.levelNum > 0) {
			setLevel(probot.levelNum - 1)
			refresh()
		}
	}

	fun onNextClicked(view: View) {
		if (probot.levelNum < viewModel.maxLevel.value?:0) {
			setLevel(probot.levelNum + 1)
			refresh()
		}
	}
}