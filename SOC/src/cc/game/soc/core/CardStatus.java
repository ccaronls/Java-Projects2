package cc.game.soc.core;

public enum CardStatus implements ILocalized {

	// order is important here, see Card.compareTo
	
	USABLE("Usable"),   // card is playable
	UNUSABLE("Locked"), // card not usable, for instance if a progress card just picked must wait until next turn
	USED("Used");      // card has been played, for instance Soldier and Special Victory cards

    final String stringId;

    CardStatus(String id) {
        this.stringId = id;
    }

    public String getName() {
        return stringId;
    }

}
