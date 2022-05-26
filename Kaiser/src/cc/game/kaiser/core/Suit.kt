package cc.game.kaiser.core

enum class Suit(val suitString: String, val suitChar: Char) {
	HEARTS("Hearts", '\u2665'),
	DIAMONDS("Diamonds", '\u2666'),
	CLUBS("Clubs", '\u2663'),
	SPADES("Spades", '\u2660'),
	NOTRUMP("NOTRUMP", ' ');
}