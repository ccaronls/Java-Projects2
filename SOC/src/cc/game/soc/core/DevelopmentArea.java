package cc.game.soc.core;

public enum DevelopmentArea {
	Science		(MoveType.IMPROVE_CITY_SCIENCE, 	CommodityType.Paper,	VertexType.METROPOLIS_SCIENCE,  Player.VertexChoice.SCIENCE_METROPOLIS,  "", "Abbey", 		"Library", 		"Aqueduct", 		"Theatre"),
	Trade		(MoveType.IMPROVE_CITY_TRADE, 		CommodityType.Cloth,	VertexType.METROPOLIS_TRADE,	Player.VertexChoice.TRADE_METROPOLIS,    "", "Market", 		"Trading House","Merchant Guild", 	"Bank"),
	Politics	(MoveType.IMPROVE_CITY_POLITICS, 	CommodityType.Coin,		VertexType.METROPOLIS_POLITICS,	Player.VertexChoice.POLITICS_METROPOLIS, "", "Town Hall", 	"Church", 		"Fortress", 		"Cathedral");
	
	DevelopmentArea(MoveType move, CommodityType commodity, VertexType vertexType, Player.VertexChoice choice, String ... levelName) {
		this.move = move;
		this.commodity = commodity;
		this.vertexType = vertexType;
		this.choice = choice;
		this.levelName = levelName;
	}
	
	public final CommodityType commodity;
	public final MoveType move;
	public final VertexType vertexType;
	public final Player.VertexChoice choice;
	public final String [] levelName;
}
