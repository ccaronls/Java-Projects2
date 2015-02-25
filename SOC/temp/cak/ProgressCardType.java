package cc.game.soc.core.cak;

public enum ProgressCardType {

	// Science
	Alchemist(DevelopmentArea.Science, "Playable Prior to die roll.  Control outcome of 2 production dice"),
	Inventor(DevelopmentArea.Science, "switch 2 tile number tokens that value values: 3,4,5,9,10,11"),
	Crane(DevelopmentArea.Science, "build a city improvement for 1 commodity card less than normal"),
	Irrigation(DevelopmentArea.Science, "collect 2 wheat cards for each structure next to a wheat tile"),
	Engineer(DevelopmentArea.Science, "build 1 city wall for free"),
	Medicine(DevelopmentArea.Science, "upgrade settlement to city for 2 ore and 1 wheat"),
	Smith(DevelopmentArea.Science, "promote 2 knights for free"),
	Mining(DevelopmentArea.Science, "collect 2 ore card for each structure adjacent to a ore tile"),
	Printer(DevelopmentArea.Science, "collect 1 victory point played immediately upon drawing, cannot be taken."),
	RoadBuilding(DevelopmentArea.Science, "build 2 roads as normal"),
	
	// Politics
	Bishop(DevelopmentArea.Politics, "plays like the soldier card"),
	Diplomat(DevelopmentArea.Politics, "move one of your own open roads or remove one of your opponents open roads"),
	Constitution(DevelopmentArea.Politics, "collect 1 victory point played immediately upon drawing, cannot be taken"),
	Intrigue(DevelopmentArea.Politics, "displace opponent knight that is on one of your roads"),
	Deserter(DevelopmentArea.Politics, "remove an opponent knight to be replace with one of your own of equal strength"),
	Saboteur(DevelopmentArea.Politics, "players with equal or higher victory points must discard half of their inhand cards"),
	Spy(DevelopmentArea.Politics, "choose from any of an opponents progress cards"),
	Warlord(DevelopmentArea.Politics, "activate all your knights for free"),
	Wedding(DevelopmentArea.Politics, "all players with more victory points give you any 2 cards of their choice"),
	
	// Trade
	Harbor(DevelopmentArea.Trade, "force a player to exchange a commodity card of their choice for a resource card.  If they have no commodity cards, then the trade is voided."),
	MasterMerchant(DevelopmentArea.Trade, "view and then take any 2 resource or commodity cards from another players hand"),
	Merchant(DevelopmentArea.Trade, "place the merchant on a land tile to recieve the 2:1 trade bonus for that tile type for as long as the merchant is on the tile"),
	MerchantFleet(DevelopmentArea.Trade, "choose one resource or commodity to get a 2:1 trade bonus for that turn"),
	ResourceMonopoly(DevelopmentArea.Trade, "all players give you 2 resources of your choice if they have it"),
	TradeMonopoly(DevelopmentArea.Trade, "all players give you 1 commodity of your choice if they have it"),
	
	;
	
	ProgressCardType(DevelopmentArea type, String description) { // TODO: Add deck occurances
		this.type = type;
		this.description = description;
	}
	
	final DevelopmentArea type;
	final String description;
}
