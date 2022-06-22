package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 *
 */
public enum MoveType implements ILocalized {

	// The priority value is for sorting of moves for the PlayerBot.  Here is the priorities from highest to lowest:
	// Trades 
	// Progress Cards / Development
	// Add/Move routes
	// Structures
	// Attacking opponents/pirate fortress
	// Alchemist
	// roll die/event
	
    CONTINUE(false, 0, "End Turn", "End your turn and allow next player to play."),

    // use a soldier
    SOLDIER_CARD(true, 50, "Soldier", "Move the robber or Pirate to cell of players choice"),

    // seafarers scenario pirate islands
    WARSHIP_CARD(false, 50, "Warship", "Turn one of your ships into a warship"),
    
    // Use a monopoly card
    MONOPOLY_CARD(true, 50, "Monopoly", "Choose a Resource and all opponents give you that resource from their hand"),

    // Use a year of plenty card
    YEAR_OF_PLENTY_CARD(true, 0, "Year of Plenty", "Choose 2 Resources to add to your hand"),

    // view trade options
    TRADE(false, 1, "Trade", "View your trade options"),

    // Draw a card
    DRAW_DEVELOPMENT(false, 50, "Development", "Draw random Development Card for cost of %s") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.Development.getNiceString());
        }
    },

    // player can move a ship that is not connected to a settlement or is not between 2 other ships and is not adjacent to the pirate
    MOVE_SHIP(true, 5, "Move Ship", "Reposition an open ended ship"),

    // use a road building card
    ROAD_BUILDING_CARD(true, 3, "Road Building", "Build two Routes for free"),

    // Build a ship
    BUILD_SHIP(false, 5, "Build Ship", "Position a new ship for cost of %s") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.Ship.getNiceString());
        }
    },

    BUILD_WARSHIP(false, 50, "Build Warship", "Upgrade an existing ship to a war ship for cost of %s. Warships can chase away the pirate and engage other players ships.") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.Warship.getNiceString());
        }
    },
    
    ATTACK_SHIP(false, 0, "Attack a ship", "Your warship can attack an opponent's ship. Roll a die. If 1,2,3 then opponent wins and you lose your warship.  On a 4,5,6 you win and opponent ship becomes yours.  The midpoint of the die is shifted based on the difference in number of cities possessed be each player."),
    
    // Roll dice and take a chance at converting the fortress to a settlement.  Must have a ship adjacent to the settlement.
    ATTACK_PIRATE_FORTRESS(false, 50, "Attack Fortress", "Roll the dice and take a chance at converting the fortress to a new settlement. Three wins converts the settlement. A loss makes you lose the 2 ships adjacent to the fortress. A win costs you 1 ship"),
    
    // Build a Road
    BUILD_ROAD(false, 5, "Build Road", "Position a new road for cost of %s") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.Road.getNiceString());
        }
    },

    // Repair damaged road due to earthquake
    REPAIR_ROAD(true, 0, "Repair Road", "Repair your damaged road for cost of %s") {
        //"Repair Road", "Repair your damaged road for cost of " + BuildableType.Road.getNiceString()),
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.Road.getNiceString());
        }
    },
    
    // Build a Settlement
    BUILD_SETTLEMENT(false, 0, "Build Settlement", "Position a new Settlement for cost of %s") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.Settlement.getNiceString());
        }
    },

    // Build a City
    BUILD_CITY(false, 0, "Build City", "Convert a settlement to a City for cost of %s") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.City.getNiceString());
        }
    },

    // user rolls the dice.  For CAK, the user can roll the dice or play an Alchemist card of they have one, otherwise this move is always by itself.
    ROLL_DICE(false, 100, "Roll dice", "Determines resource distribution. Rolling a seven causes the Robber sequence when enabled."),
    ROLL_DICE_NEUTRAL_PLAYER(false, 100, "Roll dice", "Roll for the neutral player. Dice outcome will not be same as first roll."),
   
    // used unstead of dice for TAB expansion
    DEAL_EVENT_CARD(false, 100, "Deal Event", "Turn over the next event card"),
    DEAL_EVENT_CARD_NEUTRAL_PLAYER(false, 100, "Deal Event", "Turn over the next event card for the neutral player"),
    
    // CAK Moves
    BUILD_CITY_WALL(false, 10, "Build Wall", "Build a wall around one of your cities for cost of %s. Wall be destroyed when barbarians win an attack but city is reserved") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.CityWall.getNiceString());
        }
    },
    
    IMPROVE_CITY_POLITICS(false, 0, "Improve Politics", "Exchange Coin to improve city Politics. Upgrade to Cathedral coverts to Metropolis if not taken"),
    IMPROVE_CITY_SCIENCE(false, 0, "Improve Science", "Exchange Paper to improve city science. Upgrade to Theatre converts to a Metropolis if not taken"),
    IMPROVE_CITY_TRADE(false, 0, "Improve Trade", "Exchange Cloth to improve city trade.Upgrade to Bank converts to a Metropolis if not taken>"),
    
    // Science
    
    // Special progress card can be played instead of roll dice
    ALCHEMIST_CARD(false, 0, "Alchemist", "Playable Prior to die roll. Control outcome of 2 production dice"),
    INVENTOR_CARD(true, 0, "Inventor", "Switch 2 tile number tokens that have values: 3,4,5,9,10,11"),
    CRANE_CARD(true, 50, "Crane", "Build a city improvement for 1 commodity card less than normal"),
    IRRIGATION_CARD(true, 0, "Irrigation", "Collect 2 wheat cards for each structure next to a wheat tile>"),
    ENGINEER_CARD(true, 50, "Engineer", "Build 1 city wall for free"),
    MEDICINE_CARD(true, 50, "Medicine", "Upgrade settlement to city for 2 ore and 1 wheat"),
    SMITH_CARD(true, 50, "Smith", "Promote 2 knights for free"),
    MINING_CARD(true, 50, "Mining", "Collect 2 ore card for each structure adjacent to a ore tile"),

    // Politics
    DIPLOMAT_CARD(true, 5, "Diplomat", "Move one of your own open roads or remove one of your opponents open roads"),
    BISHOP_CARD(true, 50, "Bishop", "Move robber or pirate to cell of players choice"),
    INTRIGUE_CARD(true, 50, "Intrigue", "Displace opponent knight that is on one of your roads"),
    DESERTER_CARD(true, 50, "Deserter", "Choose an opponent to remove one of their knights, then place one of equal strength."),
    SABOTEUR_CARD(true, 0, "Saboteur", "Players with equal or higher victory points must discard half (rounded down) of their in hand cards"),
    SPY_CARD(true, 0, "Spy", "View and choose any one of a single opponents progress cards except victory cards"),
    WARLORD_CARD(true, 50, "Warlord", "Activate all your knights for free"),
    WEDDING_CARD(true, 0, "Wedding", "All players with more points give you any 2 commodity or resource cards of their choice"),
    
    // Trade
    HARBOR_CARD(true, 0, "Harbor", "Force each player to exchange a commodity card of their choice for a resource card. If they have no commodity cards, then the trade is voided"),
    MASTER_MERCHANT_CARD(true, 0, "Master Merchant", "View and then take any 2 resource or commodity cards from another players hand who has more points"),
    MERCHANT_CARD(true, 0, "Merchant", "Place the merchant on a land tile to receive the 2:1 trade bonus for that tile type for as long as the merchant is on the tile"),
    MERCHANT_FLEET_CARD(true, 0, "Merchant Fleet", "Choose one resource or commodity to get a 2:1 trade bonus for that turn"),
    RESOURCE_MONOPOLY_CARD(true, 0, "Resource Monopoly", "All players give you 2 resources of your choice if they have it"),
    TRADE_MONOPOLY_CARD(true, 0, "Trade Monopoly", "All players give you 1 commodity of your choice if they have it"),

    // Knight actions
    HIRE_KNIGHT(false, 10, "Hire Knight", "Position new basic inactive knight for cost of %s") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.Knight.getNiceString());
        }
    },
    ACTIVATE_KNIGHT(false, 50, "Activate Knight", "Activate one of your knights for cost of %s") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.ActivateKnight.getNiceString());
        }
    },
    PROMOTE_KNIGHT(false, 40, "Promote Knight", "Promote one of your knight for cost %s") {
        @Override
        public String getHelpText(Rules rules) {
            return String.format(this.helpTextId, BuildableType.PromoteKnight.getNiceString());
        }
    },
    MOVE_KNIGHT(false, 5, "Move Knight", "Move one of your active knights. Position next to robber to reposition the robber. Can displace another players knight of lesser rank"),

    KNIGHT_ATTACK_ROAD(false, 50, "Attack Road", "Pick a road adjacent to the knight and roll a die. If the die roll+knight level is greater than or equal to %d then the road is removed from the board") {
    	public String getHelpText(Rules rules) {
    		return String.format(this.helpTextId, rules.getKnightScoreToDestroyRoad());
    	}
    },
    KNIGHT_ATTACK_STRUCTURE(false, 50, "Attack Structure", "") {
    	public String getHelpText(Rules rules) {
    		if (rules.getKnightScoreToDestroySettlement() > 0) {
    			return String.format("If knight level+die roll is greater than or equal to %d, then settlement destroyed", rules.getKnightScoreToDestroySettlement());
    		}
    		if (rules.getKnightScoreToDestroyCity() > 0) {
    			return String.format("If knight level+die roll is greater than or equal to %d, then city reduced to settlement", rules.getKnightScoreToDestroyCity());
    		}
    		if (rules.getKnightScoreToDestroyWalledCity() > 0) {
    			return String.format("If knight level+die roll is greater than or equal to %d, then wall removed", rules.getKnightScoreToDestroyWalledCity());
    		}
    		if (rules.getKnightScoreToDestroyMetropolis() > 0) {
    			return String.format("If knight level+die roll is greater than or equal to %d, then metropolis reduced to city", rules.getKnightScoreToDestroyMetropolis());
    		}
    		return "";
    	}
    },
    ;
    
    final String nameId;
    final String helpTextId;
    final int priority;
    final boolean aiUseOnce; // used by PlayerBot tree generation
    
    MoveType(boolean aiUseOnce, int priority, String nameId, String helpTextId) {
    	this.priority = priority;
    	this.aiUseOnce = aiUseOnce;
    	this.nameId = nameId;
    	this.helpTextId = helpTextId;
    }
    
    public String getName() {
    	return String.format(nameId);
    }
    
    public String getHelpText(Rules rules) {
    	return String.format(helpTextId);
    }

    
}
