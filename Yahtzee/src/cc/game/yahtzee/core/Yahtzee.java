package cc.game.yahtzee.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

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
 *
 */
public abstract class Yahtzee extends Reflector<Yahtzee> {

	static {
		addAllFields(Yahtzee.class);
	}
	
	private YahtzeeState state = YahtzeeState.READY;
	private YahtzeeRules rules = new YahtzeeRules();
	
	private int rolls = 0;

	private int [] dice;
	private boolean [] keepers;
	
	private int numYahtzees = 0;
	private int upperPoints = 0;
	private int totalPoints = 0;
	private int topScore    = 0;

	private final ArrayList<YahtzeeSlot> slots = new ArrayList<YahtzeeSlot>();
	private final Map<YahtzeeSlot, Integer> scoring = new HashMap<YahtzeeSlot, Integer>();
	
	public Yahtzee() {
		reset();
	}
	
	public final void reset() {
		reset(rules);
	}
	
	public final void reset(YahtzeeRules rules) {
		this.rules = rules;
		dice = new int[rules.getNumDicePerRoll()];
		keepers = new boolean[rules.getNumDicePerRoll()];
		Arrays.fill(keepers,  false);
		slots.clear();
		for (YahtzeeSlot slot : YahtzeeSlot.values()) {
			switch (slot) {
			case ONE_PAIR:
			case TWO_PAIR:
				if (!rules.isEnableAlternateVersion())
					break;
				// else fallthrough
			default:
				slots.add(slot);
			}
		}
		scoring.clear();
		rolls = 0;
		numYahtzees = 0;
		totalPoints = 0;
		upperPoints = 0;
//		bonusPoints = 0;
		state = YahtzeeState.READY;
	}
	
	public final synchronized void runGame() {
		
		switch (state) {
		case READY:
			state = YahtzeeState.ROLLDICE; 
			break;
		case ROLLDICE: {
			onRollingDice();
			rollDice(dice);
			rolls ++;
			if (rolls == rules.getNumRollsPerRound())
				state = YahtzeeState.CHOOSE_SLOT;
			else
				state = YahtzeeState.CHECK_BONUS_YAHTZEE;
			break;
		}
		case CHECK_BONUS_YAHTZEE:
			if (isYahtzee(dice) && numYahtzees > 0) {
				onBonusYahtzee(rules.getYahtzeeBonusScore());
			}
			state = YahtzeeState.CHOOSE_KEEPERS;
			break;
		
		case CHOOSE_KEEPERS:
			if (onChooseKeepers(keepers)) {
				boolean allKept = true;
				for (boolean b : keepers) {
					if (!b)
						allKept = false;
				}
				if (allKept)
					state = YahtzeeState.CHOOSE_SLOT;
				else
					state = YahtzeeState.ROLLDICE;
			}
			break;
		case CHOOSE_SLOT: {
			List<YahtzeeSlot> unusedSlots = getUnusedSlots();
			if (unusedSlots.size() == 1) {
				// just choose the last slot automatically
				applySlot(unusedSlots.get(0));
				state = YahtzeeState.APPLY_BONUS;
			} else {
				YahtzeeSlot slot = onChooseSlotAssignment(unusedSlots);
				if (slot != null) {
					if (unusedSlots.contains(slot)) {
						applySlot(slot);
						Arrays.fill(keepers, false);
						rolls = 0;
						state = YahtzeeState.ROLLDICE;
					} else {
						onError("Invalid choice '" + slot + "' not one of: " + slots);
					}
				}
			}			
			break;
		}
		case APPLY_BONUS:
			totalPoints += getBonusPoints();
			state = YahtzeeState.GAME_OVER;
			topScore = Math.max(topScore, totalPoints);
			break;
		case GAME_OVER:
			onGameOver();
			break;
		}
	}

	// package access for JUnit test to override as necessary
	void rollDice(int [] dice) {
		for (int i=0; i<dice.length; i++) {
			if (!keepers[i])
				dice[i] = (Utils.rand() % rules.getNumDiceSides()) + 1;
		}			
	}
	
	public final List<YahtzeeSlot> getUnusedSlots() {
		ArrayList<YahtzeeSlot> unused = new ArrayList<YahtzeeSlot>();
		for (YahtzeeSlot s : slots) {
			if (!scoring.containsKey(s))
				unused.add(s);
		}
		return unused;
	}
	
	private void applySlot(YahtzeeSlot slot) {
		int score = slot.getScore(rules, dice);
		if (isYahtzee(dice)) {
			if (slot == YahtzeeSlot.YAHZEE || numYahtzees > 0)
				numYahtzees ++;
		}
		scoring.put(slot, score);
		//slots.remove(slot);
		totalPoints += score;
		if (slot.isUpper) {
			upperPoints += score;
		}
	}

	public final boolean isYahtzee(int ... roll) {
		return YahtzeeSlot.YAHZEE.getScore(rules, roll) > 0;
	}
	
	public final int [] getDiceRoll() {
		return Utils.copyOf(dice);
	}
	
	public final boolean [] getKeepers() {
		return Utils.copyOf(keepers);
	}
	
	public final List<YahtzeeSlot> getAllSlots() {
		return Collections.unmodifiableList(this.slots);
	}
	
	public final YahtzeeRules getRules() {
		return this.rules.deepCopy();
	}
	
	public final boolean isSlotUsed(YahtzeeSlot slot) {
		return this.scoring.containsKey(slot);
	}
	
	public final int getSlotScore(YahtzeeSlot slot) {
		return this.scoring.get(slot);
	}
	
	public final int getNumYahtzees() {
		return this.numYahtzees;
	}
	public final int getUpperPoints() {
		return this.upperPoints;
	}
	public final int getBonusPoints() {
		int bonusPoints = 0;
		// look for bonus in upper
		if (upperPoints >= rules.getUpperScoreForBonus()) {
			bonusPoints += rules.getUpperBonusPoints();
		}
		if (numYahtzees > 1) {
			bonusPoints += rules.getYahtzeeBonusScore() * (numYahtzees-1);
		}
		return bonusPoints;
	}
	public final int getTotalPoints() {
		return this.totalPoints;
	}
	public final YahtzeeState getState() {
		return this.state;
	}
	public final int getTopScore() {
		return this.topScore;
	}
	public final int getRollCount() {
		return this.rolls;
	}

	/**
	 * Called after user has performed their first and second rolls.  The on rules.getNumRollsPerRound() roll automatically advances state to onAssignToSlot.
	 * roll is an array copy so changes to it are ignored.  keeprs dictates which of the die to keep.  If all are choosen then state advances.
	 * @param dice
	 * @param keeprs
	 * @return true to indicate the roll was handled and to advance the state on the next call to runGame()
	 */
	protected  abstract boolean onChooseKeepers(boolean [] keeprs);
	
	/**
	 * Make an assignment into one of the slot choices.  Returning and an out of range value
	 * results in no advancement in state.
	 * 
	 * Default behavior chooses slot with highest value
	 * 
	 * @param dice2
	 * @param choices
	 * @return
	 */
	protected abstract YahtzeeSlot onChooseSlotAssignment(List<YahtzeeSlot> choices);

	/**
	 * Called whenever dice are rolled
	 */
	protected void onRollingDice() {}
	
	/**
	 * Called when a bonus yahtzee is achieved.  
	 * Base method just prints to stdout
	 * @param bonusScore
	 */
	protected void onBonusYahtzee(int bonusScore) {
		System.out.println("Bonus Yahtzee! +" + bonusScore + " points");
	}

	/**
	 * Called at end of game if there is bonus due to upper slots exceding bonus range
	 * Base method just prints to stdout
	 * @param bonusScore
	 */
	@Deprecated
	protected void onUpperSlotBonus(int bonusScore) {
		System.out.println("Bonus Upper slots excede " + rules.getUpperScoreForBonus() + "! " + bonusScore + " points");
	}
	
	/**
	 * Called when the game is over.  Default behavior is to reset the game.
	 */
	protected void onGameOver() {
		System.out.println("Game Over");
		reset();
	}
	
	/**
	 * Called when an error has occured
	 * Default prints to stderr
	 *
	 * @param msg
	 */
	protected void onError(String msg) {
		System.err.println(msg);
	}
}
