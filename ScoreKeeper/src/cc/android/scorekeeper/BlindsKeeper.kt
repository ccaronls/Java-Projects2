package cc.android.scorekeeper

import android.content.Intent
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.NumberPicker
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import cc.android.scorekeeper.databinding.BlindsKeeperBinding
import cc.android.scorekeeper.databinding.BlindsSettingsBinding
import cc.lib.android.CCActivityBase
import cc.lib.android.LifecycleViewModel
import cc.lib.android.TransformedLiveData
import cc.lib.android.combine
import cc.lib.android.increment

class BlindsKeeperViewModel : LifecycleViewModel() {
	val timerMax = MutableLiveData(0)
	val timerProgress = MutableLiveData(0)
	val running = MutableLiveData(false)
	val bigBlind = MutableLiveData(20)
	val timeLeft = combine(timerProgress, timerMax) { progress, max ->
		val timeLeftSecs = progress!!.coerceAtLeast(0)
		val hours = timeLeftSecs / 60
		val mins = timeLeftSecs % 60
		String.format("%02d:%02d", hours, mins)
	}
	val blindsText = TransformedLiveData(bigBlind) {
		String.format("$%.2f / $%.2f", it.toFloat() / 100, it.toFloat() / 200)
	}
}

const val KEY_BLINDS = "blinds"
const val KEY_PERIODS = "periods"
const val KEY_TIMER = "timer"
const val KEY_BIG_BLIND = "bigBlind"
const val KEY_BEHAVIOR = "behavior"

const val BEHAVIOR_DOUBLE = 0
const val BEHAVIOR_ADDITIVE = 1

/**
 * Created by Chris Caron on 9/3/24.
 */
class BlindsKeeper : CCActivityBase(), Runnable {

	val viewModel by viewModels<BlindsKeeperViewModel>()

	val blindValues = intArrayOf(50, 100, 150, 200, 250, 300, 350, 400, 450, 500)
	val periodValues = intArrayOf(10, 15, 20, 30, 45, 60, 75, 90, 120)

	val handler = Handler(Looper.getMainLooper())

	val defaultBlind = blindValues[0]

	val soundPool: SoundPool by lazy {
		SoundPool(8, AudioManager.STREAM_MUSIC, 0)
	}

	var ding: Int = 0

	override fun run() {
		if (viewModel.running.value == true) {
			viewModel.timerProgress.increment(-1)
			handler.postDelayed(this, 1000L)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		prefs.edit().putString(KEY_START_ACTIVITY, javaClass.simpleName).apply()
		val binding = BlindsKeeperBinding.inflate(layoutInflater)
		binding.lifecycleOwner = this
		binding.viewModel = viewModel
		setContentView(binding.root)
		binding.bSettings.setOnClickListener {
			openSettings()
		}
		binding.bReset.setOnClickListener {
			reset()
		}
		binding.bChess.setOnClickListener {
			startActivity(Intent(this, ChessKeeper::class.java))
			finish()
		}
		if (BuildConfig.DEBUG) {
			binding.tvTimer.setOnClickListener {
				viewModel.timerProgress.value = (viewModel.timerProgress.value ?: 0) / 2
			}
		}
		viewModel.running.observe(this) {
			if (it == true) {
				setKeepScreenOn(true)
				if (viewModel.timerProgress.value == 0)
					viewModel.timerProgress.value = viewModel.timerMax.value
				handler.post(this)
			} else {
				setKeepScreenOn(false)
			}
		}
		viewModel.timerProgress.observe(this) {
			prefs.edit().putInt(KEY_TIMER, it ?: 0).apply()
			if (it == 0) {
				viewModel.bigBlind.value = increaseBlinds(viewModel.bigBlind.value ?: defaultBlind)
				prefs.edit().putInt(KEY_BIG_BLIND, viewModel.bigBlind.value ?: defaultBlind).apply()
				viewModel.running.value = false
				viewModel.timerProgress.value = viewModel.timerMax.value
				soundPool.play(ding, 1f, 1f, 1, 0, 1f)
			}
		}
		ding = soundPool.load(this, R.raw.ding, 1)
		restore()
	}

	fun increaseBlinds(bigBlind: Int): Int {
		return when (prefs.getInt(KEY_BEHAVIOR, 0)) {
			BEHAVIOR_DOUBLE -> bigBlind * 2
			BEHAVIOR_ADDITIVE -> bigBlind + prefs.getInt(KEY_BLINDS, defaultBlind)
			else -> throw IllegalArgumentException()
		}
	}

	override fun onPause() {
		super.onPause()
		viewModel.running.value = false
	}

	fun openSettings() {
		val settings = BlindsSettingsBinding.inflate(layoutInflater)
		val edit = prefs.edit()
		settings.npBlinds.init(blindValues, prefs.getInt(KEY_BLINDS, 0), NumberPicker.Formatter {
			String.format("%.2f / %.2f", it.toFloat() / 100, it.toFloat() / 200)
		}, NumberPicker.OnValueChangeListener { picker, oldVal, newVal ->
			edit.putInt(KEY_BLINDS, newVal)
		})
		settings.npPeriod.init(periodValues, prefs.getInt(KEY_PERIODS, 0), NumberPicker.Formatter {
			val hours = it / 60
			val mins = it % 60
			String.format("%d:%02d", hours, mins)
		}, NumberPicker.OnValueChangeListener { picker, oldVal, newVal ->
			edit.putInt(KEY_PERIODS, newVal)
		})
		val rbMap = mapOf(
			BEHAVIOR_ADDITIVE to R.id.rbAdditive,
			BEHAVIOR_DOUBLE to R.id.rbDouble
		)

		val rbMapInverse = rbMap.map {
			it.value to it.key
		}.toMap()

		settings.rgBlindsBehavior.check(rbMap[prefs.getInt(KEY_BEHAVIOR, BEHAVIOR_DOUBLE)] ?: 0)
		settings.rgBlindsBehavior.setOnCheckedChangeListener { _, checkedId ->
			edit.putInt(KEY_BEHAVIOR, rbMapInverse[checkedId] ?: 0)
		}

		newDialogBuilder()
			.setView(settings.root)
			.setPositiveButton(R.string.popup_button_ok) { _, _ -> edit.commit() }
			.setNegativeButton(R.string.popup_button_cancel, null)
			.show()
	}


	fun reset() {
		viewModel.timerMax.value = periodValues[prefs.getInt(KEY_PERIODS, 0)] * 60
		viewModel.bigBlind.value = blindValues[prefs.getInt(KEY_BLINDS, 0)]
		viewModel.running.value = false
		viewModel.timerProgress.value = viewModel.timerMax.value
	}


	fun restore() {
		viewModel.timerMax.value = periodValues[prefs.getInt(KEY_PERIODS, 0)] * 60
		viewModel.bigBlind.value = prefs.getInt(KEY_BIG_BLIND, 50)
		viewModel.running.value = false
		viewModel.timerProgress.value = prefs.getInt(KEY_TIMER, 0)
	}

}