package cc.game.soc.core;

import cc.game.soc.android.R;

public enum DevelopmentArea implements ILocalized {
	Science		(R.string.dev_area_science, MoveType.IMPROVE_CITY_SCIENCE, 	CommodityType.Paper,	VertexType.METROPOLIS_SCIENCE,  Player.VertexChoice.SCIENCE_METROPOLIS,
            R.string.dev_area_empty, R.string.dev_area_science_abbey, R.string.dev_area_science_library, R.string.dev_area_science_aqueduct, R.string.dev_area_science_theatre, R.string.dev_area_science_university),
	Trade		(R.string.dev_area_trade, MoveType.IMPROVE_CITY_TRADE, 		CommodityType.Cloth,	VertexType.METROPOLIS_TRADE,	Player.VertexChoice.TRADE_METROPOLIS,
			R.string.dev_area_empty, R.string.dev_area_trade_market, R.string.dev_area_trade_trading_house, R.string.dev_area_trade_merchant_guild, R.string.dev_area_trade_bank, R.string.dev_area_trade_bazaar),
	Politics	(R.string.dev_area_politics, MoveType.IMPROVE_CITY_POLITICS,	CommodityType.Coin,		VertexType.METROPOLIS_POLITICS,	Player.VertexChoice.POLITICS_METROPOLIS,
			R.string.dev_area_empty, R.string.dev_area_politics_town_hall, R.string.dev_area_politics_church, R.string.dev_area_politics_cathedral, R.string.dev_area_politics_castle);
	
	/*
	 * Special abilities
	 * Science, when achieve aqueduct, then on a die roll, if the player receives no resources, then they get to pick one
	 * Trade, when achieve merchant guild then get 2:1 trading on resources and commodities
	 * Politics, when achieve fortress then get to promote knights to level 3
	 */
	
	
	public final static int MAX_CITY_IMPROVEMENT = 5;
	public final static int MIN_METROPOLIS_IMPROVEMENT = 4;
	public final static int CITY_IMPROVEMENT_FOR_SPECIAL_ABILITY = 3;
	
	DevelopmentArea(int stringId, MoveType move, CommodityType commodity, VertexType vertexType, Player.VertexChoice choice, int ... levelNameId) {
	    this.stringId= stringId;
		this.move = move;
		this.commodity = commodity;
		this.vertexType = vertexType;
		this.choice = choice;
		this.levelNameId = levelNameId;
		commodity.area = this;
	}

	final int stringId;
	public final CommodityType commodity;
	public final MoveType move;
	public final VertexType vertexType;
	public final Player.VertexChoice choice;
	public final int [] levelNameId;

	public final String getName(StringResource sr) {
        return sr.getString(stringId);
    }

    public final String getLevelName(int level, StringResource sr) {
        return sr.getString(levelNameId[level]);
    }
}
