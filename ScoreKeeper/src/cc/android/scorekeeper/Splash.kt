package cc.android.scorekeeper

import android.content.Intent
import android.os.Bundle
import cc.lib.android.CCActivityBase
import cc.lib.utils.launchIn
import cc.lib.utils.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

const val KEY_START_ACTIVITY = "startActivity"

class Splash : CCActivityBase() {
	var startTime = System.currentTimeMillis()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		startTime = System.currentTimeMillis()
	}

	override fun onPostResume() {
		super.onPostResume()
		val splashTimeMSecs = test(BuildConfig.DEBUG, 1000, 5000)
		val timeShown = (System.currentTimeMillis() - startTime).toInt()
		val timeLeft = splashTimeMSecs - timeShown
		val launchActivity = "$packageName." + prefs.getString(KEY_START_ACTIVITY, "ScoreKeeper")
		val clazz = Splash::class.java.classLoader.loadClass(launchActivity)
		val i = Intent(this, clazz)
		if (timeLeft <= 0) {
			startActivity(i)
			finish()
		} else {
			launchIn {
				delay(timeLeft.toLong())
				if (isActive) {
					startActivity(i)
					finish()
				}
			}
		}
	}
}
