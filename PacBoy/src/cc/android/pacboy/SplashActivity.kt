package cc.android.pacboy

import android.content.Intent
import android.os.Bundle
import cc.lib.android.CCActivityBase

class SplashActivity : CCActivityBase(), Runnable {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val v = PacBoyView(this)
		v.setOnClickListener { v ->
			v.removeCallbacks(this@SplashActivity)
			v.post(this@SplashActivity)
		}
		v.initIntro()
		setContentView(v)
		v.postDelayed(this, (if (BuildConfig.DEBUG) 9000 else 9000).toLong())
	}

	override fun run() {
		startActivity(Intent(this, HomeActivity::class.java))
		finish()
	}
}
