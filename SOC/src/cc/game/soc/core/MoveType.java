package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 *
 */
public enum MoveType {
	
	// These are ordered in a way such that the AI will evaluate them in a manner to reduce the size of the search tree
    // stop making moves
    CONTINUE("Continue", "End your turn."),

    // use a soldier
    SOLDIER_CARD("Soldier", "Move the robber or Pirate to cell of players choice"),

    // seafarers scenario pirate islands
    WARSHIP_CARD("Warship", "Turn one of your ships into a warship"),
    
    // Use a monopoly card
    MONOPOLY_CARD("Monopoly", "Choose a Resource and all opponents give you that resource from their hand."),

    // Use a year of plenty card
    YEAR_OF_PLENTY_CARD("Year of Plenty", "Choose 2 Resources to add to your hand"),

    // view trade options
    TRADE("Trade", "View your trade options"),

    // Draw a card
    DRAW_DEVELOPMENT("Development", "Draw random Development Card for cost of " + BuildableType.Development.getNiceString()),

    // player can move a ship that is not connected to a settlement or is not between 2 other ships and is not adjacent to the pirate
    MOVE_SHIP("Move Ship", "Reposition an opne ship"),

    // use a road building card
    ROAD_BUILDING_CARD("Road Building", "Build two Routes for free"),

    // Build a ship
    BUILD_SHIP("Build Ship", "Position a new ship for cost of " + BuildableType.Ship.getNiceString()),

    BUILD_WARSHIP("Build Warship", "Upgrade an existing ship to a war ship for cost of " + BuildableType.Warship.getNiceString()),
    
    // Roll dice and take a chance at converting the fortress to a settlement.  Must have a ship adjacent to the settlement.
    ATTACK_PIRATE_FORTRESS("Attack Fortress", "Roll the dice and take a chance at converting the fortress to a new settlement.  Three wins converts the settlement.  A loss makes you lose the 2 ships adjacent to the fortress.  A win costs you 1 ship."),
    
    // Build a Road
    BUILD_ROAD("Build Road", "Position a new road for cost of " + BuildableType.Road.getNiceString()),

    // Repair damaged road due to earthquake
    REPAIR_ROAD("Repair Road", "Repair your damaged road for cost of " + BuildableType.Road.getNiceString()),
    
    // Build a Settlement
    BUILD_SETTLEMENT("Build Settlement", "Position a new Settlement for cost of " + BuildableType.Settlement.getNiceString()),

    // Build a City
    BUILD_CITY("Build City", "Convert a settlement to a City for cost of " + BuildableType.City.getNiceString()),

    // user rolls the dice.  For CAK, the user can roll the dice or play an Alchemist card of they have one, otherwise this move is always by itself.
    ROLL_DICE("Roll dice", ""),
   
    // used unstead of dice for TAB expansion
    DEAL_EVENT_CARD("Deal Event", "Turn over the next event card"),
    
    // CAK Moves
    BUILD_CITY_WALL("Build Wall", "Build a wall around one of your cities for cost of 2 Brick.  Wall be destroyed when barbarians win an attack but city is reserved."), // Available when the user has necessary resources and a city without a wall
    
    IMPROVE_CITY_POLITICS("Improve Politics", "Exchange Coin to improve city Politics.  Upgrade to Cathedral coverts to Metropolis if not taken."),
    IMPROVE_CITY_SCIENCE("Improve Science", "Exchange Paper to improve city science.  Upgrade to Theatre converts to a Metropolis if not taken."),
    IMPROVE_CITY_TRADE("Improve Trade", "Exchange Cloth to improve city trade.Upgrade to Bank converts to a Metropolis if not taken."),
    
    // Science
    
    // Special progress card can be played instead of roll dice
    ALCHEMIST_CARD("Alchemist", "Playable Prior to die roll.  Control outcome of 2 production dice"),
    INVENTOR_CARD("Inventor", "Switch 2 tile number tokens that have values: 3,4,5,9,10,11"),
    CRANE_CARD("Crane", "Build a city improvement for 1 commodity card less than normal"),
    IRRIGATION_CARD("Irrigation", "Collect 2 wheat cards for each structure next to a wheat tile"),
    ENGINEER_CARD("Engineer", "Build 1 city wall for free"),
    MEDICINE_CARD("Medicine", "Upgrade settlement to city for 2 ore and 1 wheat"),
    SMITH_CARD("Smith", "Promote 2 knights for free"),
    MINING_CARD("Mining", "Collect 2 ore card for each structure adjacent to a ore tile"),
    //PRINTER_CARD("Printer", ProgressCardType.Printer.helpText),

    // Politics
    DIPLOMAT_CARD("Diplomat", "Move one of your own open roads or remove one of your opponents open roads"),
    //CONSTITUTION_CARD("Constitution", ProgressCardType.Constitution.helpText),
    BISHOP_CARD("Bishop", "Move robber or pirate to cell of players choice"),
    INTRIGUE_CARD("Intrigue", "Displace opponent knight that is on one of your roads"),
    DESERTER_CARD("Deserter", "Remove an opponent knight to be replaced with one of your own of equal strength"),
    SABOTEUR_CARD("Saboteur", "Players with equal or higher victory points must discard half (rounded down) of their inhand cards"),
    SPY_CARD("Spy", "View and choose any one of a single opponents progress cards except victory cards"),
    WARLORD_CARD("Warlord", "Activate all your knights for free"),
    WEDDING_CARD("Wedding", "All players with more points give you any 2 commoditty or resource cards of their choice"),
    
    // Trade
    HARBOR_CARD("Harbor", "Force each player to exchange a commodity card of their choice for a resource card.  If they have no commodity cards, then the trade is voided."),
    MASTER_MERCHANT_CARD("Master Merchant", "View and then take any 2 resource or commodity cards from another players hand"),
    MERCHANT_CARD("Merchant", "Place the merchant on a land tile to recieve the 2:1 trade bonus for that tile type for as long as the merchant is on the tile"),
    MERCHANT_FLEET_CARD("Merchant Fleet", "Choose one resource or commodity to get a 2:1 trade bonus for that turn"),
    RESOURCE_MONOPOLY_CARD("Resource Monopoly", "All players give you 2 resources of your choice if they have it"),
    TRADE_MONOPOLY_CARD("Trade Monopoly", "All players give you 1 commodity of your choice if they have it"),

    // Knight actions
    HIRE_KNIGHT("Hire Knight", "Position new basic inactive knight for cost of " + BuildableType.Knight.getNiceString()),
    ACTIVATE_KNIGHT("Activate Knight", "Activate one of your knights for cost of " + BuildableType.ActivateKnight.getNiceString()),
    PROMOTE_KNIGHT("Promote Knight", "Promote one of your knight for cost " + BuildableType.PromoteKnight.getNiceString()),
    MOVE_KNIGHT("Move Knight", "Move one of your active knights.  Position next to robber to reposition the robber.  Can displace another players knight of lesser rank."),

    ;
    
    public final String niceText;
    public final String helpText;
    
    MoveType(String niceText, String helpText) {
    	this.niceText = niceText;
    	this.helpText = helpText;
    }
}
