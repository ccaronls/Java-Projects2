package cc.game.soc.core;

import cc.game.soc.android.R;

public enum CardStatus {

	// order is important here, see Card.compareTo
	
	USABLE(R.string.card_status_usable),   // card is playable
	UNUSABLE(R.string.card_status_unusable), // card not usable, for instance if a progress card just picked must wait until next turn
	USED(R.string.card_status_used);      // card has been played, for instance Soldier and Special Victory cards

    final int stringId;

    CardStatus(int id) {
        this.stringId = id;
    }

    public String getName(StringResource sr) {
        return sr.getString(stringId);
    }

}
