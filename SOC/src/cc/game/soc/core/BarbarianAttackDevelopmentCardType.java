package cc.game.soc.core;

public enum BarbarianAttackDevelopmentCardType implements ICardType<Void> {

	KnightHood(14, "Knight Hood", "Place 1 knight on one of the unoccupied paths of the castle tile."),
	BlackKnight(4, "Black Knight", "Place a knight on an open path of your choice."),
	Intrigue(4, "Intregue", "Remove a barbarian form a hex of your choice and add to your prisoners.  If there are no more barbarians, then draw a new card."),
	Treason(4, "Treason", "Obtain 2 Gold and remove 2 barbarians from 2 different tiles (or from supply) and place on 2 other unconquered tiles.")
	
	;
	
	public final int occurance;
	public final String niceText;
	public final String helpText;
	
	private BarbarianAttackDevelopmentCardType(int occurance, String niceText, String helpText) {
		this.occurance = occurance;
		this.niceText = niceText;
		this.helpText = helpText;
	}

	@Override
	public CardType getCardType() {
		return CardType.BarbarianAttackDevelopment;
	}

	@Override
	public String helpText(Rules rules) {
		return this.helpText;
	}

	@Override
	public Void getData() {
		return null;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.USED;
	}
	
}
