package cc.game.soc.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.*
import java.lang.Runnable

class SOCSplash : Activity() {
	var startTime = System.currentTimeMillis()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		startTime = System.currentTimeMillis()
	}

	override fun onPostResume() {
		super.onPostResume()
		val splashTimeMSecs = if (BuildConfig.DEBUG) 1000L else 5000L
		val timeShown = (System.currentTimeMillis() - startTime).toInt()
		val timeLeft = splashTimeMSecs - timeShown
		val i = Intent(this, SOCActivity::class.java)
		if (timeLeft <= 0) {
			startActivity(i)
			finish()
		}
		CoroutineScope(Dispatchers.IO).launch {
			delay(splashTimeMSecs)
			withContext(Dispatchers.Main) {
				startActivity(i)
				finish()
			}
		}
	}
}