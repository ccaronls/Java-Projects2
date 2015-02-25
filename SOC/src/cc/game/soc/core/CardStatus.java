package cc.game.soc.core;

public enum CardStatus {

	// order is important here, see Card.compareTo
	
	USABLE,   // card is playable
	UNUSABLE, // card not usable, for instance if a progress card just picked must wait until next turn
	USED      // card has been played, for instance Soldier and Special Victory cards
	
}
