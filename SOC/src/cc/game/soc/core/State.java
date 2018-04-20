package cc.game.soc.core;

import cc.game.soc.android.R;

enum State {
	DEAL_CARDS(false, R.string.state_deal_cards),
	POSITION_SETTLEMENT_CANCEL(true, R.string.state_position_settlement),
	POSITION_SETTLEMENT_NOCANCEL(false, R.string.state_position_settlement),
	POSITION_ROAD_CANCEL(true, R.string.state_position_road),
	POSITION_ROAD_NOCANCEL(false, R.string.state_position_road),
	POSITION_ROAD_OR_SHIP_CANCEL(true, R.string.state_position_road_or_ship),
	POSITION_ROAD_OR_SHIP_NOCANCEL(false, R.string.state_position_road_or_ship),
	POSITION_CITY_CANCEL(true, R.string.state_position_city),
	POSITION_CITY_NOCANCEL(false, R.string.state_position_city),
	CHOOSE_CITY_FOR_WALL(true, R.string.state_choose_city_for_wall),
	POSITION_SHIP_CANCEL(true, R.string.state_position_ship),
	// Player is position a moved ship.  Only one allowed per turn
	POSITION_SHIP_AND_LOCK_CANCEL(true, R.string.state_choose_ship_to_move),
	UPGRADE_SHIP_CANCEL(true, R.string.state_upgrade_ship),
	POSITION_SHIP_NOCANCEL(false, R.string.state_position_ship),
	CHOOSE_SHIP_TO_MOVE(true, R.string.state_choose_ship_to_move),
	NEXT_PLAYER(false, 0),
	PREV_PLAYER(false, 0),
	START_ROUND(false, R.string.state_start_round),
	SETUP_GIVEUP_CARDS(false, R.string.state_setup_giveup_cards),
	GIVE_UP_CARD(false, R.string.state_give_up_cards),
	INIT_PLAYER_TURN(false, 0),
	PLAYER_TURN_NOCANCEL(false, R.string.state_player_turn),
	POSITION_ROBBER_OR_PIRATE_CANCEL(true, R.string.state_position_robber_or_pirate),
	POSITION_ROBBER_OR_PIRATE_NOCANCEL(false, R.string.state_position_robber_or_pirate),
	POSITION_ROBBER_CANCEL(true, R.string.state_position_robber),
    POSITION_ROBBER_NOCANCEL(false, R.string.state_position_robber),
	POSITION_PIRATE_CANCEL(true, R.string.state_position_pirate),
    POSITION_PIRATE_NOCANCEL(false, R.string.state_position_pirate),
    DRAW_RESOURCE_OR_COMMODITY_CANCEL(true, R.string.state_draw_resource_or_commodity),
    DRAW_RESOURCE_OR_COMMODITY_NOCANCEL(false, R.string.state_draw_resource_or_commodity),
	DRAW_RESOURCE_CANCEL(true, R.string.state_draw_resource),
	DRAW_RESOURCE_NOCANCEL(false, R.string.state_draw_resource),
	CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM(false, R.string.state_choose_opponent_to_take_resource),
	CHOOSE_OPPONENT_FOR_GIFT_CARD(false, R.string.state_choose_opponent_for_gift),
	CHOOSE_RESOURCE_MONOPOLY(true, R.string.state_choose_resource_monopoly),
	SHOW_TRADE_OPTIONS(true, R.string.state_show_trade_options),
	SET_PLAYER(false, 0),
	SET_VERTEX_TYPE(false, 0),
	CHOOSE_KNIGHT_TO_ACTIVATE(true, R.string.state_choose_knight_to_activate),
	POSITION_NEW_KNIGHT_CANCEL(true, R.string.state_position_new_knight),
	POSITION_KNIGHT_CANCEL(true, R.string.state_position_knight),
	POSITION_KNIGHT_NOCANCEL(false, R.string.state_position_deserter_knight),
	POSITION_DISPLACED_KNIGHT(false, R.string.state_position_displaced_knight), // player's knight has been displaced
	CHOOSE_KNIGHT_TO_MOVE(true, R.string.state_choose_knight_to_move),
	CHOOSE_KNIGHT_TO_PROMOTE(true, R.string.state_choose_knight_to_move),
	CHOOSE_PROGRESS_CARD_TYPE(false, R.string.state_choose_progress_card_type), // player chooses from one Science, Trade or Politics
	CHOOSE_METROPOLIS(false, R.string.state_choose_metro),
	CHOOSE_CITY_IMPROVEMENT(true, R.string.state_choose_city_improvement),
	CHOOSE_KNIGHT_TO_DESERT(false, R.string.state_choose_knight_for_desertion),
	CHOOSE_PLAYER_FOR_DESERTION(true, R.string.state_choose_player_for_desertion),
	CHOOSE_DIPLOMAT_ROUTE(true, R.string.state_choose_diplomat_route),
	CHOOSE_HARBOR_RESOURCE(true, R.string.state_choose_harbor_resource),
	CHOOSE_HARBOR_PLAYER(true, R.string.state_choose_harbor_player),
	EXCHANGE_CARD(false, R.string.state_choose_card_for_exchange),
	CHOOSE_OPPONENT_KNIGHT_TO_DISPLACE(true, R.string.state_choose_opponent_knight_for_displace), // Intrigue
	CHOOSE_TILE_INVENTOR(true, R.string.state_choose_tile_inventor),
	CHOOSE_PLAYER_MASTER_MERCHANT(true, R.string.state_choose_master_merchant_player),
	TAKE_CARD_FROM_OPPONENT(false, R.string.state_choose_opponent_to_take_card),
	POSITION_MERCHANT(true, R.string.state_position_merchant),
	CHOOSE_RESOURCE_FLEET(true, R.string.state_choose_resource_fleet), // Merchant Fleet
	CHOOSE_PLAYER_TO_SPY_ON(true, R.string.state_choose_player_to_spy), // Spy
	CHOOSE_TRADE_MONOPOLY(true, R.string.state_choose_trade_monopoly),
	CHOOSE_GIFT_CARD(false, R.string.state_choose_card_to_gift),
    PROCESS_DICE(false, 0), // transition state
    PROCESS_PIRATE_ATTACK(false, 0), // transition state
    CHOOSE_PIRATE_FORTRESS_TO_ATTACK(true, R.string.state_choose_pirate_fortress_to_attack),
    CHOOSE_ROAD_TO_ATTACK(true, R.string.state_choose_road_to_attack),
    ROLL_DICE_ATTACK_ROAD(false, R.string.state_roll_dice_attack_road),
    CHOOSE_STRUCTURE_TO_ATTACK(true, R.string.state_choose_structure_to_attack),
    ROLL_DICE_ATTACK_STRUCTURE(false, R.string.state_roll_dice_attack_structure),
    CHOOSE_SHIP_TO_ATTACK(true, R.string.state_choose_ship_to_attack),
    ROLL_DICE_ATTACK_SHIP(false, R.string.state_roll_dice_attack_ship),
    CLEAR_FORCED_SETTLEMENTS(false, 0), // transition state
	;
	
	State(boolean canCancel, int helpTextId) {
	    this.canCancel = canCancel;
	    this.helpTextId = helpTextId;
	}
	
	final boolean canCancel;
    private final int helpTextId;


    public final String getHelpText(StringResource sr) {
        if (helpTextId == 0)
            return "";
        return sr.getString(helpTextId);
    }
}
