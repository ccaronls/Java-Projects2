package cc.game.android.yahtzee

import cc.lib.yahtzee.Yahtzee
import cc.lib.yahtzee.YahtzeeSlot
import cc.lib.utils.Lock
import java.io.File

internal class YahtzeeRunner(private val activity: YahtzeeActivity) : Yahtzee() {
	private val SAVE_FILE: File = File(activity.filesDir, "yahtzee.save")
	private val RULES_FILE: File = File(activity.filesDir, "yahtzeerules.sav")

	private val lock = Lock()
	var result: Any? = null
		set(value) {
			field = value
			lock.release()
		}

	override fun onChooseKeepers(keeprs: BooleanArray): Boolean {
		activity.showChooseKeepers(keeprs)
		lock.acquireAndBlock()
		return when (result) {
			null -> false
			is Boolean -> result as Boolean
			else -> false
		}
	}

	override fun onChooseSlotAssignment(choices: List<YahtzeeSlot>): YahtzeeSlot? {
		activity.showChooseSlot(choices)
		lock.acquireAndBlock()
		return when (result) {
			is YahtzeeSlot -> result as YahtzeeSlot
			else -> null
		}
	}

	override fun onRollingDice() {
		activity.rollTheDice()
	}

	override fun onBonusYahtzee(bonusScore: Int) {
		// TODO Auto-generated method stub
		super.onBonusYahtzee(bonusScore)
	}

	override fun onGameOver() {
		activity.showGameOver()
	}

	override fun onError(msg: String) {
		// TODO Auto-generated method stub
		super.onError(msg)
	}

	internal enum class RunState {
		STOPPED,
		STARTED,
		STOPPING
	}

	private var runState = RunState.STOPPED
	private val thread = Runnable {
		try {
			while (runState == RunState.STARTED) {
				saveToFile(SAVE_FILE)
				runGame()
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		runState = RunState.STOPPED
	}

	fun startThread() {
		synchronized(this) {
			if (runState == RunState.STOPPED) {
				runState = RunState.STARTED
				Thread(thread).start()
			}
		}
	}

	fun stopThread() {
		synchronized(this) {
			runState = RunState.STOPPING
			lock.releaseAll()
		}
	}

	val isRunning: Boolean
		get() = runState == RunState.STARTED

	init {
		if (RULES_FILE.exists()) {
			try {
				rules.loadFromFile(RULES_FILE)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		if (SAVE_FILE.exists()) {
			try {
				loadFromFile(SAVE_FILE)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}
}