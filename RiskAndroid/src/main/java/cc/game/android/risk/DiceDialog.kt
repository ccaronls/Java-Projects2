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
internal class DiceDialog(val context: RiskActivity, val attacker: Army, val defender: Army, val attackingDice: IntArray, val defendingDice: IntArray, val result: BooleanArray) : Runnable, DialogInterface.OnDismissListener, DialogInterface.OnClickListener {
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
			.setNegativeButton("Pause", this).show()
		dialog.setOnDismissListener(this)
	}

	override fun onDismiss(dialog: DialogInterface) {
		lock.releaseAll()
	}

	override fun onClick(dialog: DialogInterface, which: Int) {
		context.stopGameThread()
	}

	fun dismiss() {
		dialog.dismiss()
	}

	private fun showResult() {
		for (i in result.indices) {
			text[i].setText(if (result[i]) R.string.arrow_left else R.string.arrow_right)
		}
	}

	private fun init() {
		context.runOnUiThread(this)
		lock.block(2000)
		if (!dialog.isShowing)
			return
		lock.block()
		if (!dialog.isShowing)
			return
		context.runOnUiThread { showResult() }
		if (dialog.isShowing) {
			dialog.setCanceledOnTouchOutside(true)
			lock.block(6000)
		}
		context.runOnUiThread { dialog.dismiss() }
	}
	
	init {
		init()
	}
}