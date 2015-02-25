package cc.game.soc.core.cak;

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
}
