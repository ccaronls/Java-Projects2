package cc.game.soc.core;

public enum BarbarianAttackDevelopmentCardType implements ICardType<Void> {

	KnightHood(14, "Knight Hood", "Place 1 knight on one of the unoccupied paths of the castle tile."),
	BlackKnight(4, "Black Knight", "Place a knight on an open path of your choice."),
	Intrigue(4, "Intrigue", "Remove a barbarian from a hex of your choice and add to your prisoners.  If there are no more barbarians, then draw a new card."),
	Treason(4, "Treason", "Obtain 2 Gold and remove 2 barbarians from 2 different tiles (or from supply) and place on 2 other unconquered tiles.")
	
	;
	
	public final int occurance;
	public final String nameId;
	public final String helpTextId;
	
	BarbarianAttackDevelopmentCardType(int occurance, String nameId, String helpTextId) {
		this.occurance = occurance;
		this.nameId = nameId;
		this.helpTextId = helpTextId;
	}

	@Override
	public CardType getCardType() {
		return CardType.BarbarianAttackDevelopment;
	}

    @Override
	public String getHelpText(Rules rules) {
		return helpTextId;
	}

	@Override
	public Void getData() {
		return null;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.USED;
	}

    @Override
    public String getName() {
        return nameId;
    }


}
