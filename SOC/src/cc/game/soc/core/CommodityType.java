package cc.game.soc.core;

import cc.game.soc.android.R;

public enum CommodityType implements ICardType<DevelopmentArea> {
	Paper(R.string.comm_type_paper, R.string.comm_type_paper_help),
	Cloth(R.string.comm_type_cloth, R.string.comm_type_cloth_help),
	Coin(R.string.comm_type_coin, R.string.comm_type_coin_help);

	final int helpTextId;
	final int nameId;
	DevelopmentArea area;

	CommodityType(int nameId, int helpTextId) {
		this.nameId = nameId;
		this.helpTextId = helpTextId;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.Commodity;
	}

	@Override
	public String getHelpText(Rules rules, StringResource sr) {
		return sr.getString(helpTextId);
	}

	@Override
	public DevelopmentArea getData() {
		return area;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.USABLE;
	}

    @Override
    public String getName(StringResource sr) {
        return sr.getString(nameId);
    }


}
