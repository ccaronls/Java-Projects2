package cc.game.soc.core;

import cc.game.soc.android.R;

public enum BarbarianAttackDevelopmentCardType implements ICardType<Void> {

	KnightHood(14, R.string.barb_atk_type_knighthood, R.string.barb_atk_type_knighthood_help),
	BlackKnight(4, R.string.barb_atk_type_black_knight, R.string.barb_atk_type_black_knight_help),
	Intrigue(4, R.string.barb_atk_type_intrigue, R.string.barb_atk_type_intrigue_help),
	Treason(4, R.string.barb_atk_type_treason, R.string.barb_atk_type_treason_help)
	
	;
	
	public final int occurance;
	public final int nameId;
	public final int helpTextId;
	
	private BarbarianAttackDevelopmentCardType(int occurance, int nameId, int helpTextId) {
		this.occurance = occurance;
		this.nameId = nameId;
		this.helpTextId = helpTextId;
	}

	@Override
	public CardType getCardType() {
		return CardType.BarbarianAttackDevelopment;
	}

    @Override
	public String getHelpText(Rules rules, StringResource sr) {
		return sr.getString(helpTextId);
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
    public String getName(StringResource sr) {
        return sr.getString(nameId);
    }


}
