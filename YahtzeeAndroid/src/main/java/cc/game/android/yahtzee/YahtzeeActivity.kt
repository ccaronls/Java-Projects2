package cc.game.android.yahtzee

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import cc.game.yahtzee.core.YahtzeeRules
import cc.game.yahtzee.core.YahtzeeSlot
import cc.game.yahtzee.core.YahtzeeState
import cc.lib.game.Utils
import java.util.*

class YahtzeeActivity() : Activity(), View.OnClickListener {
	private val DICE_COUNT = 5

	private val imageViewDie = arrayOfNulls<ImageView>(DICE_COUNT)
	private val textViewKeepDie = arrayOfNulls<TextView>(DICE_COUNT)
	private var textViewRollsValue: TextView? = null
	private var textViewYahtzeesValue: TextView? = null
	private var textViewUpperPointsValue: TextView? = null
	private var textViewTotalPointsValue: TextView? = null
	private var textViewBonusPointsValue: TextView? = null
	private var textViewTopScoreValue: TextView? = null
	private var listViewYahtzeeSlots: ListView? = null
	private var buttonRollDice: Button? = null

	// should these really be in other files?
	private lateinit var slotAdapter: YahtzeeSlotAdapter
	private lateinit var runner: YahtzeeRunner

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		runner = YahtzeeRunner(this)
		setContentView(R.layout.yahtzeeactivity)
		imageViewDie[0] = findViewById<View>(R.id.imageViewDie1) as ImageView
		imageViewDie[1] = findViewById<View>(R.id.imageViewDie2) as ImageView
		imageViewDie[2] = findViewById<View>(R.id.imageViewDie3) as ImageView
		imageViewDie[3] = findViewById<View>(R.id.imageViewDie4) as ImageView
		imageViewDie[4] = findViewById<View>(R.id.imageViewDie5) as ImageView
		textViewKeepDie[0] = findViewById<View>(R.id.textViewKeepDie1) as TextView
		textViewKeepDie[1] = findViewById<View>(R.id.textViewKeepDie2) as TextView
		textViewKeepDie[2] = findViewById<View>(R.id.textViewKeepDie3) as TextView
		textViewKeepDie[3] = findViewById<View>(R.id.textViewKeepDie4) as TextView
		textViewKeepDie[4] = findViewById<View>(R.id.textViewKeepDie5) as TextView
		imageViewDie[0]!!.setOnClickListener(this)
		imageViewDie[1]!!.setOnClickListener(this)
		imageViewDie[2]!!.setOnClickListener(this)
		imageViewDie[3]!!.setOnClickListener(this)
		imageViewDie[4]!!.setOnClickListener(this)
		textViewRollsValue = findViewById<View>(R.id.TextViewRollsValue) as TextView
		textViewYahtzeesValue = findViewById<View>(R.id.textViewYahtzeesValue) as TextView
		textViewUpperPointsValue = findViewById<View>(R.id.textViewUpperPointsValue) as TextView
		textViewBonusPointsValue = findViewById<View>(R.id.textViewBonusPointValue) as TextView
		textViewTotalPointsValue = findViewById<View>(R.id.textViewTotalPointsValue) as TextView
		textViewTopScoreValue = findViewById<View>(R.id.textViewTopScoreValue) as TextView
		listViewYahtzeeSlots = findViewById<View>(R.id.listViewYahtzeeSlots) as ListView
		slotAdapter = YahtzeeSlotAdapter(this, runner)
		listViewYahtzeeSlots!!.adapter = slotAdapter
		buttonRollDice = findViewById<View>(R.id.buttonRollDice) as Button
		buttonRollDice!!.setOnClickListener(this)
	}

	private val diceImageIds = intArrayOf(
		R.drawable.dice1,
		R.drawable.dice2,
		R.drawable.dice3,
		R.drawable.dice4,
		R.drawable.dice5,
		R.drawable.dice6
	)

	fun setDieImage(view: ImageView?, dieNum: Int) {
		if ((view != null) && (dieNum > 0) && (dieNum <= diceImageIds.size)) {
			view.setImageResource(diceImageIds[dieNum - 1])
		}
	}

	var keepers = BooleanArray(DICE_COUNT)
	var slotChoice: YahtzeeSlot? = null
	fun refresh() {
		runOnUiThread {
			slotAdapter.notifyDataSetChanged()
			when (runner.state) {
				YahtzeeState.CHOOSE_SLOT -> {
					buttonRollDice!!.text = "Choose Slot"
					buttonRollDice!!.isEnabled = false
				}
				YahtzeeState.GAME_OVER   -> {
					buttonRollDice!!.text = "Play Again"
					buttonRollDice!!.isEnabled = true
				}
				else                     -> {
					buttonRollDice!!.text = "Roll Dice"
					buttonRollDice!!.isEnabled = true
				}
			}
			val dice = runner.diceRoll
			for (i in dice.indices) {
				setDieImage(imageViewDie[i], dice[i])
				textViewKeepDie.get(i)!!.visibility = if (keepers.get(i)) View.VISIBLE else View.INVISIBLE
			}
			textViewRollsValue!!.text = "${runner.rollCount} of ${runner.getRules().numRollsPerRound}"
			textViewYahtzeesValue!!.text = "${runner.numYahtzees}"
			textViewUpperPointsValue!!.text = "${runner.upperPoints}"
			textViewBonusPointsValue!!.text = "${runner.bonusPoints}"
			textViewTotalPointsValue!!.text = "${runner.totalPoints}"
			textViewTopScoreValue!!.text = "${runner.topScore}"
		}
	}

	fun showGameOver() {
		refresh()
		runner.lock.acquireAndBlock()
	}

	// called from other runner thread
	fun showChooseSlot(slots: List<YahtzeeSlot>): YahtzeeSlot? {
		slotChoice = null
		refresh()
		runner.lock.acquireAndBlock()
		return slotChoice
	}

	private fun sleep() {
		runner.lock.acquireAndBlock()
	}

	private fun wake() {
		runner.lock.release()
	}

	private var rollEm = false

	// called from other runner thread
	fun showChooseKeepers(keepers: BooleanArray): Boolean {
		rollEm = false
		this.keepers = keepers
		refresh()
		sleep()
		return rollEm
	}

	fun chooseSlot(slot: YahtzeeSlot?) {
		slotChoice = slot
		slotAdapter.notifyDataSetChanged()
		Arrays.fill(keepers, false)
	}

	override fun onResume() {
		super.onResume()
		runner.startThread()
	}

	override fun onPause() {
		super.onPause()
		runner.stopThread()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menu.add("New Game")
		menu.add("About")
		menu.add("Rules")
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if ((item.title == "New Game")) {
			runner.reset()
			wake()
		} else if ((item.title == "Rules")) {
			val builder = AlertDialog.Builder(this)
			val items = arrayOf("Normal", "Alternate")
			val checkedItem = if (runner.getRules().isEnableAlternateVersion) 1 else 0
			builder.setTitle("Choose game type")
				.setSingleChoiceItems(items, checkedItem, object : DialogInterface.OnClickListener {
					override fun onClick(dialog: DialogInterface, which: Int) {
						val rules = YahtzeeRules()
						rules.isEnableAlternateVersion = if (which == 0) false else true
						runner.reset(rules)
						dialog.dismiss()
						wake()
					}
				}).setNegativeButton("Cancel", null)
				.show()
		}
		return super.onOptionsItemSelected(item)
	}

	private var numDiceRolling = 0

	internal inner class DieRoller(view: ImageView?) : Runnable {
		var delay: Int
		var view: ImageView?
		override fun run() {
			delay += Utils.rand() % 30 + 10
			if (delay < 500) {
				buttonRollDice!!.isEnabled = false
				view!!.setImageResource(diceImageIds[Utils.rand() % diceImageIds.size])
				view!!.postDelayed(this, delay.toLong())
			} else {
				numDiceRolling--
				if (numDiceRolling <= 0) {
					rollEm = true
					buttonRollDice!!.isEnabled = false
					wake()
				}
			}
		}

		init {
			delay = Utils.rand() % 10 + 10
			this.view = view
			view!!.postDelayed(this, delay.toLong())
			numDiceRolling++
		}
	}

	fun rollTheDice() {
		refresh()
		numDiceRolling = 0
		val keepers = runner.keepers
		for (i in keepers.indices) {
			if (!keepers[i]) {
				DieRoller(imageViewDie[i])
			}
		}
		if (numDiceRolling <= 0) {
			wake()
		} else {
			sleep()
		}
	}

	override fun onClick(v: View) {
		if (v.tag != null && (v.tag is YahtzeeSlot)) {
			chooseSlot(v.tag as YahtzeeSlot)
		} else if (v.id == R.id.buttonRollDice) {
			if (runner.state == YahtzeeState.GAME_OVER) {
				runner.reset()
			} else {
				rollEm = true
			}
		} else {
			if (numDiceRolling <= 0) {
				for (i in imageViewDie.indices) {
					if (v.id == imageViewDie[i]!!.id) {
						keepers[i] = !keepers[i]
						break
					}
				}
			}
		}
		wake()
	}
}