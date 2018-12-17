package cc.game.soc.core;

import cc.game.soc.android.R;

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
	
    CONTINUE(false, 0, R.string.move_type_continue, R.string.move_type_continue_help),

    // use a soldier
    SOLDIER_CARD(true, 50, R.string.move_type_soldier, R.string.move_type_soldier_help),

    // seafarers scenario pirate islands
    WARSHIP_CARD(false, 50, R.string.move_type_warship, R.string.move_type_warship_help),
    
    // Use a monopoly card
    MONOPOLY_CARD(true, 50, R.string.move_type_monopoly, R.string.move_type_monopoly_help),

    // Use a year of plenty card
    YEAR_OF_PLENTY_CARD(true, 0, R.string.move_type_yop, R.string.move_type_yop_help),

    // view trade options
    TRADE(false, 1, R.string.move_type_trade, R.string.move_type_trade_help),

    // Draw a card
    DRAW_DEVELOPMENT(false, 50, R.string.move_type_development, R.string.move_type_development_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.Development.getNiceString(sr));
        }
    },

    // player can move a ship that is not connected to a settlement or is not between 2 other ships and is not adjacent to the pirate
    MOVE_SHIP(true, 5, R.string.move_type_move_ship, R.string.move_type_move_ship_help),

    // use a road building card
    ROAD_BUILDING_CARD(true, 3, R.string.move_type_rb, R.string.move_type_rb_help),

    // Build a ship
    BUILD_SHIP(false, 5, R.string.move_type_build_ship, R.string.move_type_build_ship_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.Ship.getNiceString(sr));
        }
    },

    BUILD_WARSHIP(false, 50, R.string.move_type_build_warship, R.string.move_type_build_warship_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.Warship.getNiceString(sr));
        }
    },
    
    ATTACK_SHIP(false, 0, R.string.move_type_attack_ship, R.string.move_type_attack_ship_help),
    
    // Roll dice and take a chance at converting the fortress to a settlement.  Must have a ship adjacent to the settlement.
    ATTACK_PIRATE_FORTRESS(false, 50, R.string.move_type_attack_fortress, R.string.move_type_attack_fortress_help),
    
    // Build a Road
    BUILD_ROAD(false, 5, R.string.move_type_build_road, R.string.move_type_build_road_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.Road.getNiceString(sr));
        }
    },

    // Repair damaged road due to earthquake
    REPAIR_ROAD(true, 0, R.string.move_type_repair_road, R.string.move_type_repair_road_help_cost) {
        //"Repair Road", "Repair your damaged road for cost of " + BuildableType.Road.getNiceString()),
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.Road.getNiceString(sr));
        }
    },
    
    // Build a Settlement
    BUILD_SETTLEMENT(false, 0, R.string.move_type_build_settlement, R.string.move_type_build_settlement_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.Settlement.getNiceString(sr));
        }
    },

    // Build a City
    BUILD_CITY(false, 0, R.string.move_type_build_city, R.string.move_type_build_city_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.City.getNiceString(sr));
        }
    },

    // user rolls the dice.  For CAK, the user can roll the dice or play an Alchemist card of they have one, otherwise this move is always by itself.
    ROLL_DICE(false, 100, R.string.move_type_roll_dice, R.string.move_type_roll_dice_help),
    ROLL_DICE_NEUTRAL_PLAYER(false, 100, R.string.move_type_roll_dice, R.string.move_type_roll_neutral_player_dice_help),
   
    // used unstead of dice for TAB expansion
    DEAL_EVENT_CARD(false, 100, R.string.move_type_deal_event, R.string.move_type_deal_event_help),
    DEAL_EVENT_CARD_NEUTRAL_PLAYER(false, 100, R.string.move_type_deal_event, R.string.move_type_deal_event_neutral_player_help),
    
    // CAK Moves
    BUILD_CITY_WALL(false, 10, R.string.move_type_build_wall, R.string.move_type_build_wall_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.CityWall.getNiceString(sr));
        }
    },
    
    IMPROVE_CITY_POLITICS(false, 0, R.string.move_type_improve_politics, R.string.move_type_improve_politics_help),
    IMPROVE_CITY_SCIENCE(false, 0, R.string.move_type_improve_science, R.string.move_type_improve_science_help),
    IMPROVE_CITY_TRADE(false, 0, R.string.move_type_improve_trade, R.string.move_type_improve_trade_help),
    
    // Science
    
    // Special progress card can be played instead of roll dice
    ALCHEMIST_CARD(false, 0, R.string.move_type_alchemist, R.string.move_type_alchemist_help),
    INVENTOR_CARD(true, 0, R.string.move_type_inventor, R.string.move_type_inventor_help),
    CRANE_CARD(true, 50, R.string.move_type_crane, R.string.move_type_crane_help),
    IRRIGATION_CARD(true, 0, R.string.move_type_irrigation, R.string.move_type_irrigation_help),
    ENGINEER_CARD(true, 50, R.string.move_type_engineer, R.string.move_type_engineer_help),
    MEDICINE_CARD(true, 50, R.string.move_type_medicine, R.string.move_type_medicine_help),
    SMITH_CARD(true, 50, R.string.move_type_smith, R.string.move_type_smith_help),
    MINING_CARD(true, 50, R.string.move_type_mining, R.string.move_type_mining_help),

    // Politics
    DIPLOMAT_CARD(true, 5, R.string.move_type_diplomat, R.string.move_type_diplomat_help),
    BISHOP_CARD(true, 50, R.string.move_type_bishop, R.string.move_type_bishop_help),
    INTRIGUE_CARD(true, 50, R.string.move_type_intrigue, R.string.move_type_intrigue_help),
    DESERTER_CARD(true, 50, R.string.move_type_deserter, R.string.move_type_deserter_help),
    SABOTEUR_CARD(true, 0, R.string.move_type_saboteur, R.string.move_type_saboteur_help),
    SPY_CARD(true, 0, R.string.move_type_spy, R.string.move_type_spy_help),
    WARLORD_CARD(true, 50, R.string.move_type_warlord, R.string.move_type_warlord_help),
    WEDDING_CARD(true, 0, R.string.move_type_wedding, R.string.move_type_wedding_help),
    
    // Trade
    HARBOR_CARD(true, 0, R.string.move_type_harbor, R.string.move_type_harbor_help),
    MASTER_MERCHANT_CARD(true, 0, R.string.move_type_master_merchant, R.string.move_type_master_merchant_help),
    MERCHANT_CARD(true, 0, R.string.move_type_merchant, R.string.move_type_merchant_help),
    MERCHANT_FLEET_CARD(true, 0, R.string.move_type_merchant_fleet, R.string.move_type_merchant_fleet_help),
    RESOURCE_MONOPOLY_CARD(true, 0, R.string.move_type_resource_monopoly, R.string.move_type_resource_monopoly_help),
    TRADE_MONOPOLY_CARD(true, 0, R.string.move_type_trade_monopoly, R.string.move_type_trade_monopoly_help),

    // Knight actions
    HIRE_KNIGHT(false, 10, R.string.move_type_hire_knight, R.string.move_type_hire_knight_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.Knight.getNiceString(sr));
        }
    },
    ACTIVATE_KNIGHT(false, 50, R.string.move_type_activate_knight, R.string.move_type_activate_knight_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.ActivateKnight.getNiceString(sr));
        }
    },
    PROMOTE_KNIGHT(false, 40, R.string.move_type_promote_knight, R.string.move_type_promote_knight_help_cost) {
        @Override
        public String getHelpText(Rules rules, StringResource sr) {
            return sr.getString(this.helpTextId, BuildableType.PromoteKnight.getNiceString(sr));
        }
    },
    MOVE_KNIGHT(false, 5, R.string.move_type_move_knight, R.string.move_type_move_knight_help),

    KNIGHT_ATTACK_ROAD(false, 50, R.string.move_type_attack_road, R.string.move_type_attack_road_help_die) {
    	public String getHelpText(Rules rules, StringResource sr) {
    		return sr.getString(this.helpTextId, rules.getKnightScoreToDestroyRoad());
    	}
    },
    KNIGHT_ATTACK_STRUCTURE(false, 50, R.string.move_type_attack_structure, 0) {
    	public String getHelpText(Rules rules, StringResource sr) {
    		if (rules.getKnightScoreToDestroySettlement() > 0) {
    			return sr.getString(R.string.move_type_attack_settlement_help_knight_level, rules.getKnightScoreToDestroySettlement());
    		}
    		if (rules.getKnightScoreToDestroyCity() > 0) {
    			return sr.getString(R.string.move_type_attack_city_help_knight_level, rules.getKnightScoreToDestroyCity());
    		}
    		if (rules.getKnightScoreToDestroyWalledCity() > 0) {
    			return sr.getString(R.string.move_type_attack_walled_city_help_knight_level, rules.getKnightScoreToDestroyWalledCity());
    		}
    		if (rules.getKnightScoreToDestroyMetropolis() > 0) {
    			return sr.getString(R.string.move_type_attack_metro_help_knight_level, rules.getKnightScoreToDestroyMetropolis());
    		}
    		return "";
    	}
    },
    ;
    
    final int nameId;
    final int helpTextId;
    final int priority;
    final boolean aiUseOnce; // used by PlayerBot tree generation
    
    MoveType(boolean aiUseOnce, int priority, int nameId, int helpTextId) {
    	this.priority = priority;
    	this.aiUseOnce = aiUseOnce;
    	this.nameId = nameId;
    	this.helpTextId = helpTextId;
    }
    
    public String getName(StringResource sr) {
    	return sr.getString(nameId);
    }
    
    public String getHelpText(Rules rules, StringResource sr) {
    	return sr.getString(helpTextId);
    }

    
}
