package cc.game.soc.core;

public enum ProgressCardType implements ICardType<DevelopmentArea> {

	// Science
	Alchemist		(DevelopmentArea.Science, "Can assign any values to the dice on your turn", 2),
	Inventor		(DevelopmentArea.Science, MoveType.INVENTOR_CARD, 2),
	Crane			(DevelopmentArea.Science, MoveType.CRANE_CARD, 2),
	Irrigation		(DevelopmentArea.Science, MoveType.IRRIGATION_CARD, 2),
	Engineer		(DevelopmentArea.Science, MoveType.ENGINEER_CARD, 1),
	Medicine		(DevelopmentArea.Science, MoveType.MEDICINE_CARD, 2),
	Smith			(DevelopmentArea.Science, MoveType.SMITH_CARD, 2),
	Mining			(DevelopmentArea.Science, MoveType.MINING_CARD, 2),
	Printer			(DevelopmentArea.Science, "Collect 1 victory point played immediately upon drawing, cannot be taken.", 1),
	RoadBuilding	(DevelopmentArea.Science, MoveType.ROAD_BUILDING_CARD, 1),
	
	// Politics
	Bishop			(DevelopmentArea.Politics, MoveType.BISHOP_CARD, 2),
	Diplomat		(DevelopmentArea.Politics, MoveType.DIPLOMAT_CARD, 2),
	Constitution	(DevelopmentArea.Politics, "Collect 1 victory point played immediately upon drawing, cannot be taken.", 1),
	Intrigue		(DevelopmentArea.Politics, MoveType.INTRIGUE_CARD, 2),
	Deserter		(DevelopmentArea.Politics, MoveType.DESERTER_CARD, 2),
	Saboteur		(DevelopmentArea.Politics, MoveType.SABOTEUR_CARD, 2),
	Spy				(DevelopmentArea.Politics, MoveType.SPY_CARD, 3),
	Warlord			(DevelopmentArea.Politics, MoveType.WARLORD_CARD, 2),
	Wedding			(DevelopmentArea.Politics, MoveType.WEDDING_CARD, 2),
	
	// Trade
	Harbor			(DevelopmentArea.Trade, MoveType.HARBOR_CARD, 2),
	MasterMerchant	(DevelopmentArea.Trade, MoveType.MASTER_MERCHANT_CARD, 2),
	Merchant		(DevelopmentArea.Trade, MoveType.MERCHANT_CARD, 6),
	MerchantFleet	(DevelopmentArea.Trade, MoveType.MERCHANT_FLEET_CARD, 2),
	ResourceMonopoly(DevelopmentArea.Trade, MoveType.RESOURCE_MONOPOLY_CARD, 4),
	TradeMonopoly	(DevelopmentArea.Trade, MoveType.TRADE_MONOPOLY_CARD, 2),
	
	;

	final DevelopmentArea type;
	final int deckOccurances;
	final MoveType moveType;
	final String helpText;

	ProgressCardType(DevelopmentArea type, String helpText, int deckOccurances) { 
		this.type = type;
		this.deckOccurances = deckOccurances;
		this.moveType = null;
		this.helpText = helpText;
	}
	
	ProgressCardType(DevelopmentArea type, MoveType moveType, int deckOccurances) { 
		this.type = type;
		this.deckOccurances = deckOccurances;
		this.moveType = moveType;
		this.helpText = moveType.helpText;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.Progress;
	}

	@Override
	public String helpText() {
		return moveType.helpText;
	}

	@Override
	public DevelopmentArea getData() {
		return type;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.UNUSABLE;
	}

}
