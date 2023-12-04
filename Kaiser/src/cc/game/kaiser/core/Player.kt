package cc.game.kaiser.core

import cc.lib.reflector.Reflector

/**
 * base class for a KaiserPlayer
 * @author ccaron
 */
abstract class Player protected constructor(name: String) : Reflector<Player>() {
	companion object {
		const val HAND_SIZE = 8
		const val MAX_PLAYER_TRICKS = 8

		init {
			addAllFields(Player::class.java)
		}
	}
	/**
	 *
	 * @return
	 */
	/**
	 *
	 * @param nm
	 */
	var name: String

	/**
	 *
	 * @return
	 */
	var team = 0
	@JvmField
    var mHand = Hand()

	/**
	 *
	 * @return
	 */
    @JvmField
    var playerNum = -1

	/**
	 *
	 * @return
	 *
	 * public Player getTeammate() {
	 * if (this.mTeam.getPlayerA() == this)
	 * return mTeam.getPlayerB();
	 * return mTeam.getPlayerA();
	 * }
	 *
	 * / **
	 *
	 * @return
	 */
	val tricks = mutableListOf<Hand>()

	/**
	 * Callback when a new game has started. Default impl does nothing.
	 *
	 * @param k
	 */
	fun onNewGame(k: Kaiser) {}

	/**
	 * handle when a new round is started. kaiser state is NEW_ROUND Default
	 * impl does nothing.
	 *
	 * @param k
	 */
	fun onNewRound(k: Kaiser) {}

	/**
	 * handle when this player wins a trick. kaiser state is PROCESS_TRICK
	 * Default impl does nothing.
	 *
	 * @param k
	 */
	fun onWinsTrick(k: Kaiser, trick: Hand?) {}

	/**
	 * handle for every card this player is dealt. kaiser state is DEAL Default
	 * impl does nothing.
	 *
	 * @param c
	 */
	fun onDealtCard(k: Kaiser, c: Card?) {}

	/**
	 * handle for end of trick processing. kaiser state is PROCESS_TRICK Default
	 * impl does nothing.
	 *
	 * @param kaiser
	 * @param trick
	 * @param reciever
	 * @param pointsInTrick
	 */
	fun onProcessTrick(kaiser: Kaiser, trick: Hand?, reciever: Player,
	                   pointsInTrick: Int) {
	}

	/**
	 * handle for end of round processing. kaiser state is PROCESS_ROUND Default
	 * impl does nothing.
	 *
	 * @param k
	 */
	open fun onProcessRound(k: Kaiser) {}

	/**
	 * Returns a value from the options array. returning null will result in no
	 * advance in state. This is to support integrations that do not want this
	 * method to ever block.
	 *
	 * @param kaiser
	 * @param options
	 * @param numOptions
	 * @return
	 */
	abstract fun playTrick(kaiser: Kaiser, options: Array<Card>): Card?

	/**
	 * Returning NULL will result in no advancement of state. This is to support
	 * integrations that do not want this method to ever block.
	 *
	 * @param kaiser
	 * @param numOptions
	 * @return
	 */
	abstract fun makeBid(kaiser: Kaiser, options: Array<Bid>): Bid?

	/**
	 *
	 * @return
	 */
	val numCards: Int
		get() = mHand.size

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getCard(index: Int): Card {
		return mHand.get(index)
	}

	/**
	 * Return a copy if this hand to avoid invalidating the game.
	 * @return
	 */
	val hand: Hand
		get() = mHand.deepCopy()

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getTrick(index: Int): Hand {
		return tricks[index]
	}

	/**
	 *
	 * @param name
	 */
	init {
		this.name = name
	}
}