package cc.game.soc.core;

public enum DevelopmentArea {
	Science		(MoveType.IMPROVE_CITY_SCIENCE, 	CommodityType.Paper,	VertexType.METROPOLIS_SCIENCE,  Player.VertexChoice.SCIENCE_METROPOLIS,
			"", "Abbey", "Library", "Aqueduct", "Theatre", "University"),
	Trade		(MoveType.IMPROVE_CITY_TRADE, 		CommodityType.Cloth,	VertexType.METROPOLIS_TRADE,	Player.VertexChoice.TRADE_METROPOLIS,
			"", "Market", "Trading House","Merchant Guild", "Bank", "Bazaar"),
	Politics	(MoveType.IMPROVE_CITY_POLITICS,	CommodityType.Coin,		VertexType.METROPOLIS_POLITICS,	Player.VertexChoice.POLITICS_METROPOLIS, 
			"", "Town Hall", "Church", "Fortress", "Cathedral", "Castle");
	
	/*
	 * Special abilities
	 * Science, when achieve aqueduct, then on a die roll, if the player receives no resources, then they get to pick one
	 * Trade, when achieve merchant guild then get 2:1 trading on resources and commodities
	 * Politics, when achieve fortress then get to promote knights to level 3
	 */
	
	
	public final static int MAX_CITY_IMPROVEMENT = 5;
	public final static int MIN_METROPOLIS_IMPROVEMENT = 4;
	public final static int CITY_IMPROVEMENT_FOR_SPECIAL_ABILITY = 3;
	
	DevelopmentArea(MoveType move, CommodityType commodity, VertexType vertexType, Player.VertexChoice choice, String ... levelName) {
		this.move = move;
		this.commodity = commodity;
		this.vertexType = vertexType;
		this.choice = choice;
		this.levelName = levelName;
		commodity.area = this;
	}
	
	public final CommodityType commodity;
	public final MoveType move;
	public final VertexType vertexType;
	public final Player.VertexChoice choice;
	public final String [] levelName;
}
