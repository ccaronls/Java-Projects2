package cc.lib.yahtzee

/**
 * Rules of yahtzee
 *
 * On a players turn, they roll 5 dice up to 3 times.  On each roll they may choose to keep 1 or more of the dice.  After the 3 rolls
 * they have one or more of:
 *
 * 2 or a kind
 * 3 of a kind
 * 2 pair
 * 4 of a kind
 * 5 of a kind (yahtzee)
 * straight
 * full house
 * ...
 *
 * They must assign
 *
 * @author chriscaron
 */
enum class YahtzeeState {
	READY,  // state of machine after call to reset().  Next call to run advances to rolldice. (Do we really need this?)
	ROLLDICE,  // dice are rolled that are not marked as a keeper
	CHECK_BONUS_YAHTZEE,
	CHOOSE_KEEPERS,  // toggle keepers.  Setting all to KEEP advances state
	CHOOSE_SLOT,  // state advances when onChooseSlot returns non null
	APPLY_BONUS,  // apply bonuses if any and advance to GAME_OVER
	GAME_OVER // once here is where we stay until reset called
}