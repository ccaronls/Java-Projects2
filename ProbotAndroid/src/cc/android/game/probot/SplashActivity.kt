package cc.android.game.probot

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

/**
 * Created by Chris Caron on 8/17/22.
 */
class SplashActivity : Activity() {

	override fun onPostResume() {
		super.onPostResume()
		CoroutineScope(Dispatchers.Main).async {
			delay(3000)
			startActivity(Intent(this@SplashActivity, ProbotActivity::class.java))
		}
	}

}