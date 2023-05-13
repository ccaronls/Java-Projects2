package cc.game.android.risk

import android.util.Log
import cc.lib.game.AAnimation
import cc.lib.game.AGraphics

/**
 * Created by Chris Caron on 9/23/21.
 */
abstract class RiskAnim(durationMSecs: Long) : AAnimation<AGraphics>(durationMSecs) {
	var zOrder = 0
		private set

	override fun onDone() {
		Log.d("RiskAnim", "onDone")
		super.onDone()
	}

	fun setZOrder(order: Int): RiskAnim {
		zOrder = order
		return this
	}
}