package cc.android.pacboy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Toast
import cc.lib.android.CCActivityBase

class HomeActivity : CCActivityBase(), View.OnClickListener {
	//private GoogleApiClient mGAPI;
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.home_activity)
		findViewById<View>(R.id.buttonSmallEasy).setOnClickListener(this)
		findViewById<View>(R.id.buttonSmallHard).setOnClickListener(this)
		findViewById<View>(R.id.buttonSmallVeryHard).setOnClickListener(this)
		findViewById<View>(R.id.buttonMedEasy).setOnClickListener(this)
		findViewById<View>(R.id.buttonMedHard).setOnClickListener(this)
		findViewById<View>(R.id.buttonMedVeryHard).setOnClickListener(this)
		findViewById<View>(R.id.buttonLargeEasy).setOnClickListener(this)
		findViewById<View>(R.id.buttonLargeHard).setOnClickListener(this)
		findViewById<View>(R.id.buttonLargeVeryHard).setOnClickListener(this)
		findViewById<View>(R.id.buttonHugeEasy).setOnClickListener(this)
		findViewById<View>(R.id.buttonHugeHard).setOnClickListener(this)
		findViewById<View>(R.id.buttonHugeVeryHard).setOnClickListener(this)
		findViewById<View>(R.id.buttonViewLeaderboards).setOnClickListener(this)
		/*
		mGAPI = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Games.API)
				.addScope(Games.SCOPE_GAMES)
				.build();*/findViewById<View>(R.id.buttonViewLeaderboards).isEnabled = false
	}

	public override fun onStart() {
		super.onStart()
		//mGAPI.connect();
	}

	public override fun onStop() {
		super.onStop()
		//mGAPI.disconnect();
	}

	override fun onResume() {
		super.onResume()
	}

	override fun onPause() {
		super.onPause()
	}

	override fun onPoll() {
		super.onPoll()
	}

	override fun onClick(v: View) {
		val i = Intent(this, PacBoyActivity::class.java)
		when (v.id) {
			R.id.buttonSmallEasy -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 6)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 4)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0)
			}

			R.id.buttonSmallHard -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 6)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 4)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 10)
			}

			R.id.buttonSmallVeryHard -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 6)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 4)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 20)
			}

			R.id.buttonMedEasy -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 10)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 6)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0)
			}

			R.id.buttonMedHard -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 10)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 6)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 10)
			}

			R.id.buttonMedVeryHard -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 10)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 6)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 20)
			}

			R.id.buttonLargeEasy -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 16)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 10)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0)
			}

			R.id.buttonLargeHard -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 16)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 10)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 10)
			}

			R.id.buttonLargeVeryHard -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 16)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 10)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 20)
			}

			R.id.buttonHugeEasy -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 24)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 16)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0)
			}

			R.id.buttonHugeHard -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 24)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 16)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 10)
			}

			R.id.buttonHugeVeryHard -> {
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 24)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 16)
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 20)
			}
		}
		startActivity(i)
	}

	private val MSG_SIGNIN_FAILED = 33
	private val MSG_SIGNIN_SUCCESS = 34
	private val MSG_SCORE_SUBMIT_RESULT = 35
	private val handler: Handler = object : Handler() {
		override fun handleMessage(msg: Message) {
			val a: Activity = this@HomeActivity
			when (msg.what) {
				MSG_SIGNIN_FAILED -> {
					Toast.makeText(a, "Failed to Sign in to Google Play", Toast.LENGTH_SHORT).show()
					findViewById<View>(R.id.buttonViewLeaderboards).isEnabled = false
				}

				MSG_SIGNIN_SUCCESS -> {
					Toast.makeText(a, "Signed in to Google Play", Toast.LENGTH_SHORT).show()
					findViewById<View>(R.id.buttonViewLeaderboards).isEnabled = true
				}

				MSG_SCORE_SUBMIT_RESULT -> Toast.makeText(a, msg.obj.toString(), Toast.LENGTH_LONG).show()
				else -> super.handleMessage(msg)
			}
		}
	}
}