package cc.game.golf.swing

import cc.game.golf.core.Card
import cc.game.golf.core.Rules
import cc.game.golf.core.State

internal interface IGolfGame {
	/**
	 * Get card representing top of deck
	 * @return
	 */
	val topOfDeck: Card?

	/**
	 * Get card representing top of discard pile.  Can be null.
	 * @return
	 */
	val topOfDiscardPile: Card?
	val currentPlayer: Int

	/**
	 * Get current knocker or -1
	 * @return
	 */
	val knocker: Int

	/**
	 * Get number of players
	 * @return
	 */
	val numPlayers: Int

	/**
	 * Get the player who is facing the front
	 * @return
	 */
	val frontPlayer: Int

	/**
	 * Get the rules.
	 * @return
	 */
	val rules: Rules

	/**
	 * Get a players cards
	 * @param player
	 * @return
	 */
	fun getPlayerCards(player: Int): Array<Array<Card?>>

	/**
	 * Get the players name
	 * @param player
	 * @return
	 */
	fun getPlayerName(player: Int): String

	/**
	 * Get a players showing points
	 * @param player
	 * @return
	 */
	fun getHandPoints(player: Int): Int

	/**
	 * Get number of rounds played
	 * @return
	 */
	val numRounds: Int

	/**
	 * Get the dealer.
	 * @return
	 */
	val dealer: Int

	/**
	 * Get the winner or -1
	 * @return
	 */
	val winner: Int

	/**
	 * Get a players total game points.
	 * @param player
	 * @return
	 */
	fun getPlayerPoints(player: Int): Int

	/**
	 * Get the deck
	 * @return
	 */
	val deck: List<Card>

	/**
	 * Get the current game state
	 * @return
	 */
	val state: State
	fun getPlayerCard(player: Int, row: Int, col: Int): Card?
	fun canResume(): Boolean
	fun updateRules()
	fun quit()
	val isRunning: Boolean

	@Throws(Exception::class)
	fun resume()
	fun startNewGame()
}
