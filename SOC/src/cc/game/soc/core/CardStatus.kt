package cc.game.soc.core

enum class CardStatus(  // card has been played, for instance Soldier and Special Victory cards
	val stringId: String) : ILocalized {
	// order is important here, see Card.compareTo
	USABLE("Usable"),  // card is playable
	UNUSABLE("Locked"),  // card not usable, for instance if a progress card just picked must wait until next turn
	USED("Used");

	override fun getNameId(): String {
		return stringId
	}
}