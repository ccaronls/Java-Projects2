package cc.game.soc.core;

import cc.game.soc.android.R;

public enum ProgressCardType implements ICardType<DevelopmentArea> {

	// Science
	Alchemist		(DevelopmentArea.Science, MoveType.ALCHEMIST_CARD, 2),
	Inventor		(DevelopmentArea.Science, MoveType.INVENTOR_CARD, 2),
	Crane			(DevelopmentArea.Science, MoveType.CRANE_CARD, 2),
	Irrigation		(DevelopmentArea.Science, MoveType.IRRIGATION_CARD, 2),
	Engineer		(DevelopmentArea.Science, MoveType.ENGINEER_CARD, 1),
	Medicine		(DevelopmentArea.Science, MoveType.MEDICINE_CARD, 2),
	Smith			(DevelopmentArea.Science, MoveType.SMITH_CARD, 2),
	Mining			(DevelopmentArea.Science, MoveType.MINING_CARD, 2),
	Printer			(DevelopmentArea.Science, R.string.special_victory_printer, 1),
	RoadBuilding	(DevelopmentArea.Science, MoveType.ROAD_BUILDING_CARD, 1),
	
	// Politics
	Bishop			(DevelopmentArea.Politics, MoveType.BISHOP_CARD, 2),
	Diplomat		(DevelopmentArea.Politics, MoveType.DIPLOMAT_CARD, 2),
	Constitution	(DevelopmentArea.Politics, R.string.special_victory_constitution, 1),
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

	final int nameId;
	final DevelopmentArea type;
	final int deckOccurances;
	final MoveType moveType;

    ProgressCardType(DevelopmentArea type, int nameId, int deckOccurances) {
        this.nameId = nameId;
        this.type = type;
        this.deckOccurances = deckOccurances;
        this.moveType = null;
    }

    ProgressCardType(DevelopmentArea type, MoveType moveType, int deckOccurances) {
	    this.nameId = moveType.nameId;
		this.type = type;
		this.deckOccurances = deckOccurances;
		this.moveType = moveType;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.Progress;
	}

	@Override
	public String getHelpText(Rules rules, StringResource sr) {
		return moveType.getHelpText(rules, sr);
	}

	@Override
	public DevelopmentArea getData() {
		return type;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.UNUSABLE;
	}

    @Override
    public String getName(StringResource sr) {
        return sr.getString(nameId);
    }

    public boolean isEnabled(Rules rules) {
		switch (this) {
			case Alchemist:
				return !rules.isEnableEventCards();
			case Bishop:
				return rules.isEnableRobber();
			case Constitution:
				break;
			case Crane:
				break;
			case Deserter:
				break;
			case Diplomat:
				break;
			case Engineer:
				break;
			case Harbor:
				break;
			case Intrigue:
				break;
			case Inventor:
				return !rules.isEnableEventCards();
			case Irrigation:
				break;
			case MasterMerchant:
				break;
			case Medicine:
				break;
			case Merchant:
				break;
			case MerchantFleet:
				break;
			case Mining:
				break;
			case Printer:
				break;
			case ResourceMonopoly:
				break;
			case RoadBuilding:
				break;
			case Saboteur:
				break;
			case Smith:
				break;
			case Spy:
				break;
			case TradeMonopoly:
				break;
			case Warlord:
				break;
			case Wedding:
				break;
		}
		return true;
	}
}
