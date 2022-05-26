package cc.game.kaiser.core

/**
 * Defines the rank of a card (Ace, King, Queen, Jack, etc.)
 * @author ccaron
 */
enum class Rank(val rankString: String) {
	THREE("3 "),
	FIVE("5 "),
	SEVEN("7 "),
	EIGHT("8 "),
	NINE("9 "),
	TEN("10"),
	JACK("J "),
	QUEEN("Q "),
	KING("K "),
	ACE("A ");

}