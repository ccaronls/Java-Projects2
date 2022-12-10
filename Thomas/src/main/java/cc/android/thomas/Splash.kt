package cc.android.thomas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import cc.android.thomas.databinding.SplashBinding

class Splash : Activity(), Runnable {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
	    with (SplashBinding.inflate(layoutInflater)) {
	    	setContentView(root)
		    screen.setOnClickListener { run() }
	    }
        handler.postDelayed(this, if (BuildConfig.DEBUG) 2000 else 5000.toLong())
    }

    var handler = Handler()

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(this)
    }

    override fun run() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}