package cc.game.soc.core;

enum State {
//	INIT_GAME(false),
	DEAL_CARDS(false, "Dealing cards..."),
	POSITION_SETTLEMENT_CANCEL(true, "Place a Settlement on the board."),
	POSITION_SETTLEMENT_NOCANCEL(false, "Place a Settlement on the board."),
	POSITION_ROAD_CANCEL(true, "Place a road on the board."),
	POSITION_ROAD_NOCANCEL(false, "Place a road on the board."),
	POSITION_ROAD_OR_SHIP_CANCEL(true, "Choose to place a road or ship."),
	POSITION_ROAD_OR_SHIP_NOCANCEL(false, "Choose to place a road or ship."),
	POSITION_CITY_CANCEL(true, "Upgrade one of your setlements to a City."),
	POSITION_CITY_NOCANCEL(false, "Place a city on the board."),
	CHOOSE_CITY_FOR_WALL(true, "Build a wall around one of your cites to protect against barbarian attack."),
	POSITION_SHIP_CANCEL(true, "Place a ship on the board."),
	UPGRADE_SHIP_CANCEL(true, "Choose a ship to upgrade to warship"),
	POSITION_SHIP_NOCANCEL(false, "Place a ship on the board."),
	CHOOSE_SHIP_TO_MOVE(true, "Choose from an open ended ship to reposition."),
	NEXT_PLAYER(false, ""),
	PREV_PLAYER(false, ""),
	START_ROUND(false, "Round starting."),
	SETUP_GIVEUP_CARDS(false, "Computing each players cards to give up"),
	GIVE_UP_CARD(false, "Discard one of your cards back to the deck."),
	INIT_PLAYER_TURN(false, ""),
	PLAYER_TURN_NOCANCEL(false, "Choose from one of your move options or Continue to allow next player to take a turn."),
	POSITION_ROBBER_OR_PIRATE_CANCEL(true, "Pick a hexagon for the Robber or Pirate."),
	POSITION_ROBBER_OR_PIRATE_NOCANCEL(false, "Pick a hexagon for the Robber or Pirate."),
	POSITION_ROBBER_CANCEL(true, "Pick a land hexagon for the Robber."),
    POSITION_ROBBER_NOCANCEL(false, "Pick a land hexagon for the Robber."),
	POSITION_PIRATE_CANCEL(true, "Pick a water hexagon for the Pirate."),
    POSITION_PIRATE_NOCANCEL(false, "Pick a water hexagon for the Pirate."),
    DRAW_RESOURCE_OR_COMMODITY_CANCEL(true, "Pick a Resouce or Commodity Card to take into your hand."),
    DRAW_RESOURCE_OR_COMMODITY_NOCANCEL(false, "Pick a Resouce or Commodity Card to take into your hand."),
	DRAW_RESOURCE_CANCEL(true, "Pick a Resource Card to take into your hand."),
	DRAW_RESOURCE_NOCANCEL(false, "Pick a Resource Card to take into your hand."),
	CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM(false, "Pick an opponent to take a card from."),
	CHOOSE_OPPONENT_FOR_GIFT_CARD(false, "Pick an opponent to give a resource of your choice"),
	CHOOSE_RESOURCE_MONOPOLY(true, "Choose a Resource to monopolize (collect all from all opponents hands.)"),
	SHOW_TRADE_OPTIONS(true, "Display your trade options."), 
//	TRADE_COMPLETED(false),
	SET_PLAYER(false, ""),
	CHOOSE_KNIGHT_TO_ACTIVATE(true, "Choose form one of your knight to Activate.  This makes it possible to defend against Barbarians or reposition."),
	POSITION_KNIGHT_CANCEL(true, "Place your knight on the board.  Active knights can displace an opponents knight or chase the Robber."),
	POSITION_KNIGHT_NOCANCEL(false, "Place the deserted knight on the board."),
	POSITION_DISPLACED_KNIGHT(false, "Place your displaced knight on the board."), // player's knight has been displaced
	CHOOSE_KNIGHT_TO_MOVE(true, "Pick an Active knight to move."),
	CHOOSE_KNIGHT_TO_PROMOTE(true, "Choose from one of your knights to Promote."),
	CHOOSE_PROGRESS_CARD_TYPE(false, "Choose from one of the Development Areas to pick a Progress Card."), // player chooses from one Science, Trade or Politics
	CHOOSE_METROPOLIS(false, "Choose from one of your Cities to upgrade to a Metropolis.  Metropolis cannot be pilledged by Barbarians."),
	CHOOSE_CITY_IMPROVEMENT(true, "Choose from one of the City Improvment options."),
	CHOOSE_KNIGHT_TO_DESERT(false, "Choose from one of your one knights to desert into your opponents ranks."),
	CHOOSE_PLAYER_FOR_DESERTION(true, "Choose from which player."),
	CHOOSE_DIPLOMAT_ROUTE(true, "Pick a road.  If you pick an opponents road it will be removed from the board.  If you pick one of your one roads you will be allowed to reposition it."),
	CHOOSE_HARBOR_RESOURCE(true, "Pick a resource for Harbor Trade."),
	CHOOSE_HARBOR_PLAYER(true, "Choose a player with whom to force trade"),
	EXCHANGE_CARD(false, "Pick a card form your hand to Exchange."),
	CHOOSE_OPPONENT_KNIGHT_TO_DISPLACE(true, "Choose an opponents knight to displace."), // Intrigue
	CHOOSE_TILE_INVENTOR(true, "Pick 2 tiles to switch their die numbers."),
	CHOOSE_PLAYER_MASTER_MERCHANT(true, "Choose from one of your opponents.  You can take any 2 resource or commodity cards form thier hand if they have it."),
	TAKE_CARD_FROM_OPPONENT(false, "Choose from one of your opponents cards to take into your hand."),
	POSITION_MERCHANT(true, "Place the merchant on the board.  Whatever resource the Merchant is on, you will be allowed a 2:1 trade for the Resource until the Merchant is moved again"),
	CHOOSE_RESOURCE_FLEET(true, "Choose any Resource to use for a 2:1 trade for the duration of your turn."), // Merchant Fleet
	CHOOSE_PLAYER_TO_SPY_ON(true, "Choose an Opponent to Spy on.  You can pick any one of their Progress cards into your hand."), // Spy
	CHOOSE_OPPONENT_CARD(false, "Choose a Progress card to take into your hand."), // Spy
	CHOOSE_TRADE_MONOPOLY(true, "Choose a Commodity to Monopolize.  All playres with that commodity in their hand will forfeit into your own."),
	CHOOSE_GIFT_CARD(false, "Choose a card to give to the happy couple on their Wedding day."),
    PROCESS_DICE(false, ""), // transition state
    PROCESS_PIRATE_ATTACK(false, ""), // transition state 
    CHOOSE_PIRATE_FORTRESS_TO_ATTACK(true, "Choose a pirate fortress to attack"),	

	;
	
	private State(boolean canCancel, String helpText) {
	    this.canCancel = canCancel;
	    this.helpText = helpText;
	}
	
	final boolean canCancel;
	final String helpText;
}
