package cc.game.soc.core;

public enum SpecialVictoryType implements ICardType {

	DefenderOfCatan("Awarded when a player single-handedly defends against Barbarians."),
	Tradesman("Given to the player who controls the Merchant."), 
	Constitution("When this prgress card is picked it is emmediately played and cannot be taken."),
	Printer("When this prgress card is picked it is emmediately played and cannot be taken."),
	;

	SpecialVictoryType(String description) {
		this.description = description;
	}
	
	public final String description;
	
	@Override
	public CardType getCardType() {
		return CardType.SpecialVictory;
	}
	
	
	
}
