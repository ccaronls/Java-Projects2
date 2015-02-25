package cc.game.soc.core;

public enum DevelopmentArea {
	Science		(MoveType.IMPROVE_CITY_SCIENCE, 	CommodityType.Paper,	VertexType.METROPOLIS_SCIENCE,  Player.VertexChoice.SCIENCE_METROPOLIS),
	Trade		(MoveType.IMPROVE_CITY_TRADE, 		CommodityType.Cloth,	VertexType.METROPOLIS_TRADE,	Player.VertexChoice.TRADE_METROPOLIS),
	Politics	(MoveType.IMPROVE_CITY_POLITICS, 	CommodityType.Coin,		VertexType.METROPOLIS_POLITICS,	Player.VertexChoice.POLITICS_METROPOLIS);
	
	DevelopmentArea(MoveType move, CommodityType commodity, VertexType vertexType, Player.VertexChoice choice) {
		this.move = move;
		this.commodity = commodity;
		this.vertexType = vertexType;
		this.choice = choice;
	}
	
	public final CommodityType commodity;
	public final MoveType move;
	public final VertexType vertexType;
	public final Player.VertexChoice choice;
}
