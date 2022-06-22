package cc.game.soc.core;

public enum DiceEvent implements ILocalized {
	AdvanceBarbarianShip("Advance Barbarian Ship"),
	ScienceCard("Science Card"),
	TradeCard("Trade Card"),
	PoliticsCard("Politics Card"),
	;

	final String stringId;

	DiceEvent(String stringId) {
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

	public final String getName() {
        return stringId;
    }
}
