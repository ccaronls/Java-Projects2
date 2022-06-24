package cc.game.android.yahtzee

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableArrayList
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.game.android.yahtzee.databinding.YahtzeeactivityBinding
import cc.lib.android.CCActivityBase
import cc.lib.android.LayoutFactory
import cc.lib.utils.Lock
import cc.lib.utils.random
import cc.lib.yahtzee.YahtzeeRules
import cc.lib.yahtzee.YahtzeeSlot

const val DICE_COUNT = 5

@BindingAdapter("dieImage")
fun ImageView.setDieImage(id: Int) {
	setImageResource(arrayOf(
		R.drawable.dice1,
		R.drawable.dice2,
		R.drawable.dice3,
		R.drawable.dice4,
		R.drawable.dice5,
		R.drawable.dice6)[id.coerceIn(1..6)-1])
}

class YahtzeeViewModel : ViewModel() {

	val adapter = MutableLiveData<BaseAdapter>(null)
	val buttonRollDiceText = MutableLiveData("")
	val buttonRollDiceEnabled = MutableLiveData(false)
	val listViewClickable = MutableLiveData(false)
	val dice = ObservableArrayList<Int>().also {
		it.addAll(IntArray(DICE_COUNT) { 0 }.toList())
	}
	val keepers = ObservableArrayList<Boolean>().also {
		//MutableLiveData(BooleanArray(DICE_COUNT) { false })
		it.addAll(BooleanArray(DICE_COUNT) { false }.toList())
	}

	val rollsValue = MutableLiveData("")
	val yahtzeesValue = MutableLiveData("")
	val upperPointsValue = MutableLiveData("")
	val bonusPointsValue = MutableLiveData("")
	val totalPointsValue = MutableLiveData("")
	val topScoreValue = MutableLiveData("")
	val rollingDice = MutableLiveData(false)

	fun toggleKeeper(idx: Int) {
		keepers[idx] = keepers[idx].not()
	}

}

class YahtzeeActivity : CCActivityBase() {

	private lateinit var slotAdapter: YahtzeeSlotAdapter
	private lateinit var runner: YahtzeeRunner
	private lateinit var binding: YahtzeeactivityBinding
	private lateinit var viewModel: YahtzeeViewModel

	override fun getLayoutFactory(): LayoutFactory {
		return LayoutFactory(this, R.layout.yahtzeeactivity, YahtzeeViewModel::class.java)
	}

	override fun onLayoutCreated(binding: ViewDataBinding, viewModel: ViewModel?) {
		this.binding = binding as YahtzeeactivityBinding
		this.viewModel = viewModel as YahtzeeViewModel
		binding.viewModel = viewModel
	}


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)
		runner = YahtzeeRunner(this)
		slotAdapter = YahtzeeSlotAdapter(this, runner)
		viewModel.adapter.postValue(slotAdapter)
		binding.listViewYahtzeeSlots.setOnItemClickListener { parent, view, position, id ->
			viewModel.keepers.fill(false)
			runner.result = view.tag
		}
	}

	fun refresh() {
		viewModel.dice.clear()
		viewModel.dice.addAll(runner.diceRoll.toList())
		viewModel.rollsValue.postValue("${runner.rollCount} of ${runner.rules.numRollsPerRound}")
		viewModel.yahtzeesValue.postValue("${runner.numYahtzees}")
		viewModel.upperPointsValue.postValue("${runner.upperPoints}")
		viewModel.bonusPointsValue.postValue("${runner.bonusPoints}")
		viewModel.totalPointsValue.postValue("${runner.totalPoints}")
		viewModel.topScoreValue.postValue("${runner.topScore}")
	}

	fun showGameOver() {
		refresh()
		slotAdapter.postNotifyDataSetChanged()
		viewModel.listViewClickable.postValue(false)
		viewModel.buttonRollDiceText.postValue("Play Again")
		viewModel.buttonRollDiceEnabled.postValue(true)
		binding.buttonRollDice.setOnClickListener {
			runner.result = true
		}
	}

	// called from other runner thread
	fun showChooseSlot(slots: List<YahtzeeSlot>) {
		refresh()
		slotAdapter.postNotifyDataSetChanged()
		viewModel.listViewClickable.postValue(true)
		viewModel.buttonRollDiceText.postValue("Choose Slot")
		viewModel.buttonRollDiceEnabled.postValue(false)
	}

	// called from other runner thread
	fun showChooseKeepers(keepers: BooleanArray) {
		refresh()
		slotAdapter.postNotifyDataSetChanged()
		viewModel.listViewClickable.postValue(false)
		viewModel.buttonRollDiceText.postValue("Choose Keepers")
		viewModel.buttonRollDiceEnabled.postValue(false)
		binding.root.postDelayed( {
			viewModel.buttonRollDiceText.postValue("Roll Dice")
			viewModel.buttonRollDiceEnabled.postValue(true)
			binding.buttonRollDice.setOnClickListener {
				viewModel.keepers.forEachIndexed { index, b ->
					keepers[index] = b
				}
				runner.result = true
			}
		}, 2000L)
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
			refresh()
			runner.result = null
		} else if ((item.title == "Rules")) {
			val builder = AlertDialog.Builder(this)
			val items = arrayOf("Normal", "Alternate")
			val checkedItem = if (runner.rules.isEnableAlternateVersion) 1 else 0
			builder.setTitle("Choose game type")
				.setSingleChoiceItems(items, checkedItem, object : DialogInterface.OnClickListener {
					override fun onClick(dialog: DialogInterface, which: Int) {
						val rules = YahtzeeRules()
						rules.isEnableAlternateVersion = if (which == 0) false else true
						runner.reset(rules)
						dialog.dismiss()
						runner.result = null
					}
				}).setNegativeButton("Cancel", null)
				.show()
		}
		return super.onOptionsItemSelected(item)
	}

	fun rollTheDice() {
		refresh()
		slotAdapter.postNotifyDataSetChanged()
		viewModel.rollingDice.postValue(true)
		val animLock = Lock()
		binding.root.postDelayed(DieRoller(animLock), 1)
		animLock.acquireAndBlock()
	}

	internal inner class DieRoller(val animLock: Lock) : Runnable {
		var delay: Long
		override fun run() {
			delay += random(30) + 10
			if (delay < 500) {
				viewModel.buttonRollDiceEnabled.postValue(false)
				val dice = viewModel.dice.toTypedArray()
				viewModel.dice.clear()
				viewModel.dice.addAll(IntArray(DICE_COUNT) {
					if (viewModel.keepers[it])
						dice[it]
					else
						random(1..6)
					}.toList()
				)
				binding.root.postDelayed(this, delay)
			} else {
				viewModel.rollingDice.postValue(false)
				viewModel.buttonRollDiceEnabled.postValue(false)
				animLock.release()
			}
		}

		init {
			delay = random(10).toLong() + 10
			binding.root.postDelayed(this, delay)
		}
	}
}