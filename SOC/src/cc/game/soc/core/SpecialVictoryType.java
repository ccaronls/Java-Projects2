package cc.game.soc.core;

public enum SpecialVictoryType implements ICardType {

	DefenderOfCatan,
	Tradesman, 
	Constitution,
	Printer
	;

	@Override
	public CardType getCardType() {
		return CardType.SpecialVictory;
	}
	
	
	
}
