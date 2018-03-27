package cc.game.soc.core;

import cc.game.soc.android.R;

public enum DiceEvent {
	AdvanceBarbarianShip(R.string.dice_event_advance_barbarian),
	ScienceCard(R.string.dice_event_science_card),
	TradeCard(R.string.dice_event_trade_card),
	PoliticsCard(R.string.dice_event_politics_card),
	;

	final int stringId;

	DiceEvent(int stringId) {
        this.stringId = stringId;
    }

	/**
	 * Return thre event for a die num roll.  Valid range is [1-6] inclusive
	 * @param num
	 * @return
	 */
	public static DiceEvent fromDieNum(int num) {
		switch (num) {
			case 1: case 2: case 3:
				return DiceEvent.AdvanceBarbarianShip;
			case 4:
				return DiceEvent.PoliticsCard;
			case 5:
				return DiceEvent.ScienceCard;
			case 6:
				return DiceEvent.TradeCard;
		}
		return null;
	}

	public final String getName(StringResource sr) {
        return sr.getString(stringId);
    }
}
