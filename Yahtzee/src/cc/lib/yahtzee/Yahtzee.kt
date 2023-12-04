package cc.lib.yahtzee

import cc.lib.game.Utils
import cc.lib.reflector.Reflector
import cc.lib.utils.random
import java.util.*

/**
 * Rules of Yahtzee here:
 * http://en.wikipedia.org/wiki/Yahtzee
 *
 * Like all my simple games, this acts like a (black box) state machine.  Apps call runGame repeatedly and respond to
 * callbacks.  Can be run synchronously or asynchronously.
 *
 * There are 2 callbacks that app must over ride: chooseKeepers and chooseSlot.
 * These correspond to the distinct actions a user can make during game play.
 *
 * There is no player types because Yahtzee is a solitaire game.  Multi-player forms are simply multiple solitaire instances.
 *
 * @author chriscaron
 */
abstract class Yahtzee : Reflector<Yahtzee>() {
	companion object {
		init {
			addAllFields(Yahtzee::class.java)
		}
	}

	var state = YahtzeeState.READY
		private set
	val rules = YahtzeeRules()
	var rollCount = 0
		private set
	private var dice = IntArray(5) { 0 }
	private var keepers = BooleanArray(5) { false }
	var numYahtzees = 0
		private set
	var upperPoints = 0
		private set
	var totalPoints = 0
		private set
	var topScore = 0
		private set
	private val slots = ArrayList<YahtzeeSlot>()
	private val scoring: MutableMap<YahtzeeSlot, Int> = HashMap()

	@JvmOverloads
	fun reset(rules: YahtzeeRules = this.rules) {
		this.rules.copyFrom(rules)
		dice = IntArray(rules.numDicePerRoll)
		keepers = BooleanArray(rules.numDicePerRoll)
		keepers.fill(false)
		slots.clear()
		slots.addAll(YahtzeeSlot.values().filter {
			when (it) {
				YahtzeeSlot.ONE_PAIR,
				YahtzeeSlot.TWO_PAIR -> rules.isEnableAlternateVersion
				else -> true
			}
		})
		scoring.clear()
		rollCount = 0
		numYahtzees = 0
		totalPoints = 0
		upperPoints = 0
		//		bonusPoints = 0;
		state = YahtzeeState.READY
	}

	@Synchronized
	fun runGame() {
		when (state) {
			YahtzeeState.READY               -> state = YahtzeeState.ROLLDICE
			YahtzeeState.ROLLDICE            -> {
				onRollingDice()
				rollDice(dice)
				rollCount++
				state = if (rollCount == rules.numRollsPerRound) YahtzeeState.CHOOSE_SLOT else YahtzeeState.CHECK_BONUS_YAHTZEE
			}
			YahtzeeState.CHECK_BONUS_YAHTZEE -> {
				if (isYahtzee(*dice) && numYahtzees > 0) {
					onBonusYahtzee(rules.yahtzeeBonusScore)
				}
				state = YahtzeeState.CHOOSE_KEEPERS
			}
			YahtzeeState.CHOOSE_KEEPERS      -> if (onChooseKeepers(keepers)) {
				val allKept = keepers.count { !it } == 0
				state = if (allKept) YahtzeeState.CHOOSE_SLOT else YahtzeeState.ROLLDICE
			}
			YahtzeeState.CHOOSE_SLOT         -> {
				val unusedSlots = unusedSlots
				if (unusedSlots.size == 1) {
					// just choose the last slot automatically
					applySlot(unusedSlots[0])
					state = YahtzeeState.APPLY_BONUS
				} else {
					val slot = onChooseSlotAssignment(unusedSlots)
					if (slot != null) {
						if (unusedSlots.contains(slot)) {
							applySlot(slot)
							keepers.fill(false)
							rollCount = 0
							state = YahtzeeState.ROLLDICE
						} else {
							onError("Invalid choice '$slot' not one of: $slots")
						}
					}
				}
			}
			YahtzeeState.APPLY_BONUS         -> {
				totalPoints += bonusPoints
				state = YahtzeeState.GAME_OVER
				topScore = Math.max(topScore, totalPoints)
			}
			YahtzeeState.GAME_OVER           -> onGameOver()
		}
	}

	// package access for JUnit test to override as necessary
	open fun rollDice(dice: IntArray) {
		dice.forEachIndexed { index, i -> if (!keepers[index]) dice[index] = random(1..rules.numDiceSides) }
	}

	val unusedSlots: List<YahtzeeSlot>
		get() {
			val unused = ArrayList<YahtzeeSlot>()
			for (s in slots) {
				if (!scoring.containsKey(s))
					unused.add(s)
			}
			return unused
		}

	private fun applySlot(slot: YahtzeeSlot) {
		val score = slot.getScore(rules, *dice)
		if (isYahtzee(*dice)) {
			if (slot == YahtzeeSlot.YAHZEE || numYahtzees > 0) numYahtzees++
		}
		scoring[slot] = score
		//slots.remove(slot);
		totalPoints += score
		if (slot.isUpper) {
			upperPoints += score
		}
	}

	fun isYahtzee(vararg roll: Int): Boolean {
		return YahtzeeSlot.YAHZEE.getScore(rules, *roll) > 0
	}

	val diceRoll: IntArray
		get() = Utils.copyOf(dice)

	fun getKeepers(): BooleanArray {
		return Utils.copyOf(keepers)
	}

	val allSlots: List<YahtzeeSlot>
		get() = Collections.unmodifiableList(slots)

	fun isSlotUsed(slot: YahtzeeSlot): Boolean {
		return scoring.containsKey(slot)
	}

	fun getSlotScore(slot: YahtzeeSlot): Int {
		return scoring[slot]!!
	}

	// look for bonus in upper
	val bonusPoints: Int
		get() {
			var bonusPoints = 0
			// look for bonus in upper
			if (upperPoints >= rules.upperScoreForBonus) {
				bonusPoints += rules.upperBonusPoints
			}
			if (numYahtzees > 1) {
				bonusPoints += rules.yahtzeeBonusScore * (numYahtzees - 1)
			}
			return bonusPoints
		}

	/**
	 * Called after user has performed their first and second rolls.  The on rules.getNumRollsPerRound() roll automatically advances state to onAssignToSlot.
	 * roll is an array copy so changes to it are ignored.  keeprs dictates which of the die to keep.  If all are choosen then state advances.
	 * @param keeprs
	 * @return true to indicate the roll was handled and to advance the state on the next call to runGame()
	 */
	protected abstract fun onChooseKeepers(keeprs: BooleanArray): Boolean

	/**
	 * Make an assignment into one of the slot choices.  Returning and an out of range value
	 * results in no advancement in state.
	 *
	 * Default behavior chooses slot with highest value
	 *
	 * @param choices
	 * @return
	 */
	protected abstract fun onChooseSlotAssignment(choices: List<YahtzeeSlot>): YahtzeeSlot?

	/**
	 * Called whenever dice are rolled
	 */
	protected open fun onRollingDice() {}

	/**
	 * Called when a bonus yahtzee is achieved.
	 * Base method just prints to stdout
	 * @param bonusScore
	 */
	protected open fun onBonusYahtzee(bonusScore: Int) {
		println("Bonus Yahtzee! +$bonusScore points")
	}

	/**
	 * Called at end of game if there is bonus due to upper slots exceding bonus range
	 * Base method just prints to stdout
	 * @param bonusScore
	 */
	@Deprecated("")
	protected fun onUpperSlotBonus(bonusScore: Int) {
		println("Bonus Upper slots excede " + rules.upperScoreForBonus + "! " + bonusScore + " points")
	}

	/**
	 * Called when the game is over.  Default behavior is to reset the game.
	 */
	protected open fun onGameOver() {
		println("Game Over")
		reset()
	}

	/**
	 * Called when an error has occured
	 * Default prints to stderr
	 *
	 * @param msg
	 */
	protected open fun onError(msg: String) {
		System.err.println(msg)
	}

	init {
		reset()
	}
}