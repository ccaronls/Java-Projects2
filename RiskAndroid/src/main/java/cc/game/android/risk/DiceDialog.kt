package cc.game.android.risk

import android.app.Dialog
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import cc.game.android.risk.databinding.DiceDialogBinding
import cc.lib.risk.Army
import cc.lib.utils.Lock
import cc.lib.utils.prettify

/**
 * Created by Chris Caron on 9/15/21.
 */
internal class DiceDialog(val context: RiskActivity, val attacker: Army, val defender: Army, val attackingDice: IntArray, val defendingDice: IntArray, val result: BooleanArray) : Runnable {
	private val lock = Lock()
	private lateinit var dialog: Dialog
	private lateinit var text: Array<TextView>
	
	override fun run() {
		val binding = DiceDialogBinding.inflate(LayoutInflater.from(context))
		binding.tvAttacker.text = prettify(attacker)
		binding.tvDefender.text = prettify(defender)
		val red = arrayOf(
			binding.red1,
			binding.red2,
			binding.red3,
		)
		val white = arrayOf(
			binding.white1,
			binding.white2
		)
		for (i in red.indices) {
			if (i >= attackingDice.size) {
				red[i].visibility = View.INVISIBLE
			} else {
				red[i].rollDice(attackingDice[i], lock)
			}
		}
		for (i in white.indices) {
			if (i >= defendingDice.size)
				white[i].visibility = View.INVISIBLE
			else
				white[i].rollDice(defendingDice[i], lock)
		}
		text = arrayOf(
			binding.text1,
			binding.text2)
		dialog = context.newDialogBuilder().setView(binding.root)
			.setNegativeButton("Pause") { _, _ ->
				dialog.dismiss()
				context.stopGameThread()
			}.show()
		dialog.setCanceledOnTouchOutside(true)
		dialog.setOnDismissListener { lock.releaseAll() }
	}

	private fun showResult() {
		for (i in result.indices) {
			text[i].setText(if (result[i]) R.string.arrow_left else R.string.arrow_right)
		}
	}

	init {
		context.runOnUiThread(this)
		Thread.sleep(1000)
		lock.block()
		context.runOnUiThread { showResult() }
		if (dialog.isShowing) {
			lock.acquireAndBlock(6000)
			context.runOnUiThread { dialog.dismiss() }
		}
	}
}