package cc.android.scorekeeper

import android.content.Intent
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import cc.android.scorekeeper.databinding.ChessKeeperBinding
import cc.lib.android.CCActivityBase
import cc.lib.android.LifecycleViewModel
import cc.lib.android.increment
import cc.lib.android.toggle

class ChessKeeperViewModel : LifecycleViewModel() {

	val minutesRemainingLeft = MutableLiveData(0)
	val minutesRemainingRight = MutableLiveData(0)

	val secondsRemainingLeft = MutableLiveData(0)
	val secondsRemainingRight = MutableLiveData(0)

	val running = MutableLiveData(false)
	val leftSideActive = MutableLiveData(false)

	fun setLeftSideActive(active: Boolean) {
		leftSideActive.value = active
	}
}

/**
 * Created by Chris Caron on 9/29/24.
 */
class ChessKeeper : CCActivityBase(), Runnable {

	val viewModel by viewModels<ChessKeeperViewModel>()

	val handler = Handler(Looper.getMainLooper())

	val soundPool: SoundPool by lazy {
		SoundPool(8, AudioManager.STREAM_MUSIC, 0)
	}

	var ding: Int = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		prefs.edit().putString(KEY_START_ACTIVITY, javaClass.simpleName).apply()
		val binding = ChessKeeperBinding.inflate(layoutInflater)
		binding.lifecycleOwner = this
		binding.viewModel = viewModel
		setContentView(binding.root)
		binding.bScore.setOnClickListener {
			startActivity(Intent(this, ChessKeeper::class.java))
			finish()
		}
		binding.bPause.setOnClickListener {
			viewModel.running.toggle()
		}
		viewModel.running.observe(this) {
			handler.removeCallbacks(this)
		}
		viewModel.running.observe(this) {
			handler.removeCallbacks(this)
			setKeepScreenOn(it)
			if (it) {
				handler.post(this)
			}
		}
		ding = soundPool.load(this, R.raw.ding, 1)
	}

	override fun run() {
		if (viewModel.running.value == false)
			return

		val ld = if (viewModel.leftSideActive.value == true) {
			Pair(viewModel.secondsRemainingLeft, viewModel.minutesRemainingLeft)
		} else {
			Pair(viewModel.secondsRemainingRight, viewModel.minutesRemainingRight)
		}

		ld.let { (secs, mins) ->
			if (secs.increment(-1) < 0) {
				if (mins.increment(-1) < 0) {
					timesUp()
				} else {
					secs.postValue(59)
				}
			}
		}
		handler.postDelayed(this, 1000L)
	}

	override fun onPause() {
		viewModel.running.value = false
		save()
		super.onPause()
	}

	override fun onResume() {
		super.onResume()
		restore()
	}

	fun timesUp() {
		viewModel.running.value = false
		soundPool.play(ding, 1f, 1f, 1, 0, 1f)
	}

	fun save() {
		prefs.edit()
			.putInt("lss", viewModel.secondsRemainingLeft.value!!)
			.putInt("lsm", viewModel.minutesRemainingLeft.value!!)
			.putInt("rss", viewModel.secondsRemainingRight.value!!)
			.putInt("rsm", viewModel.minutesRemainingRight.value!!)
			.putBoolean("lsa", viewModel.leftSideActive.value!!).apply()
	}

	fun restore() {
		viewModel.secondsRemainingLeft.value = prefs.getInt("lss", 0)
		viewModel.minutesRemainingLeft.value = prefs.getInt("lsm", 30)
		viewModel.secondsRemainingRight.value = prefs.getInt("rss", 0)
		viewModel.minutesRemainingRight.value = prefs.getInt("rsm", 30)
		viewModel.leftSideActive.value = prefs.getBoolean("lsa", false)
	}
}