package cc.game.soc.core;

public enum DiceEvent {
	AdvanceBarbarianShip(3),
	ScienceCard(1),
	TradeCard(1),
	PoliticsCard(1),
	;
	
	DiceEvent(int occurances) {
		this.occurances = occurances;
	}
	
	final int occurances;
	
	/**
	 * Return thre event for a die num roll.  Valid range is [1-6] inclusive
	 * @param num
	 * @return
	 */
	public static DiceEvent fromDieNum(int num) {
		num -= 1;
		for (DiceEvent ev : values()) {
			if (num >= ev.occurances) {
				num -= ev.occurances;
			} else {
				return ev;
			}
		}
		return null;
	}
}
