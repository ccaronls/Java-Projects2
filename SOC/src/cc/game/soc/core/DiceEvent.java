package cc.game.soc.core;

public enum DiceEvent {
	AdvanceBarbarianShip,
	ScienceCard,
	TradeCard,
	PoliticsCard,
	;
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
}
