package cc.lib.deckbattle;


/**
 * Example Card script loaded at runtime:
 *
 * Card Events:
 * onDealt
 * onPlayed
 * onDestroyed
 *
 * Player Events:
 *
 *
 *
 * Card Types:
 * Monster
 * Magic
 *
 * Objects:
 * Player, Hand, Deck,
 *
 * Card {
 *     cost=2
 *     attack=5
 *     health=3
 *     name="Some name"
 *     onPlayedFromHand={
 *         for (Card c in opponent.hand) {
 *             c.cost += 2
 *         }
 *
 *         for (Card c in opponent.deck) {
 *             c.cost -= 1
 *         }
 *
 *         for (Card c in player.hand) {
 *             c.cost -= 2
 *         }
 *
 *         for (Card c in player.deck) {
 *             c.cost += 1
 *         }
 *     }
 * }
 *
 */
public class Card {
}
