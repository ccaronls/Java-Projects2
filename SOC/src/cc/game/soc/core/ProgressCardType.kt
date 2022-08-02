package cc.game.soc.core

enum class ProgressCardType : ICardType<DevelopmentArea> {
	// Science
	Alchemist(DevelopmentArea.Science, MoveType.ALCHEMIST_CARD, 2),
	Inventor(DevelopmentArea.Science, MoveType.INVENTOR_CARD, 2),
	Crane(DevelopmentArea.Science, MoveType.CRANE_CARD, 2),
	Irrigation(DevelopmentArea.Science, MoveType.IRRIGATION_CARD, 2),
	Engineer(DevelopmentArea.Science, MoveType.ENGINEER_CARD, 1),
	Medicine(DevelopmentArea.Science, MoveType.MEDICINE_CARD, 2),
	Smith(DevelopmentArea.Science, MoveType.SMITH_CARD, 2),
	Mining(DevelopmentArea.Science, MoveType.MINING_CARD, 2),
	Printer(DevelopmentArea.Science, "Printer", 1),
	RoadBuilding(DevelopmentArea.Science, MoveType.ROAD_BUILDING_CARD, 1),  // Politics
	Bishop(DevelopmentArea.Politics, MoveType.BISHOP_CARD, 2),
	Diplomat(DevelopmentArea.Politics, MoveType.DIPLOMAT_CARD, 2),
	Constitution(DevelopmentArea.Politics, "Constitution", 1),
	Intrigue(DevelopmentArea.Politics, MoveType.INTRIGUE_CARD, 2),
	Deserter(DevelopmentArea.Politics, MoveType.DESERTER_CARD, 2),
	Saboteur(DevelopmentArea.Politics, MoveType.SABOTEUR_CARD, 2),
	Spy(DevelopmentArea.Politics, MoveType.SPY_CARD, 3),
	Warlord(DevelopmentArea.Politics, MoveType.WARLORD_CARD, 2),
	Wedding(DevelopmentArea.Politics, MoveType.WEDDING_CARD, 2),  // Trade
	Harbor(DevelopmentArea.Trade, MoveType.HARBOR_CARD, 2),
	MasterMerchant(DevelopmentArea.Trade, MoveType.MASTER_MERCHANT_CARD, 2),
	Merchant(DevelopmentArea.Trade, MoveType.MERCHANT_CARD, 6),
	MerchantFleet(DevelopmentArea.Trade, MoveType.MERCHANT_FLEET_CARD, 2),
	ResourceMonopoly(DevelopmentArea.Trade, MoveType.RESOURCE_MONOPOLY_CARD, 4),
	TradeMonopoly(DevelopmentArea.Trade, MoveType.TRADE_MONOPOLY_CARD, 2);

	override fun getNameId(): String = nameId
	@JvmField
	val nameId: String
	@JvmField
    val deckOccurances: Int
	@JvmField
    val moveType: MoveType?
    val type: DevelopmentArea

	constructor(type: DevelopmentArea, nameId: String, deckOccurances: Int) {
		this.nameId = nameId
		this.deckOccurances = deckOccurances
		moveType = null
		this.type = type
	}

	constructor(type: DevelopmentArea, moveType: MoveType, deckOccurances: Int) {
		nameId = moveType.getNameId()
		this.deckOccurances = deckOccurances
		this.moveType = moveType
		this.type = type
	}

	override fun getData(): DevelopmentArea = type

	override val cardType: CardType
		get() = CardType.Progress

	override fun getHelpText(rules: Rules): String? {
		return moveType!!.getHelpText(rules)
	}

	override fun defaultStatus(): CardStatus {
		return CardStatus.UNUSABLE
	}

	fun isEnabled(rules: Rules): Boolean {
		when (this) {
			Alchemist -> return !rules.isEnableEventCards
			Bishop -> return rules.isEnableRobber
			Constitution -> {
			}
			Crane -> {
			}
			Deserter -> {
			}
			Diplomat -> {
			}
			Engineer -> {
			}
			Harbor -> {
			}
			Intrigue -> {
			}
			Inventor -> return !rules.isEnableEventCards
			Irrigation -> {
			}
			MasterMerchant -> {
			}
			Medicine -> {
			}
			Merchant -> {
			}
			MerchantFleet -> {
			}
			Mining -> {
			}
			Printer -> {
			}
			ResourceMonopoly -> {
			}
			RoadBuilding -> {
			}
			Saboteur -> {
			}
			Smith -> {
			}
			Spy -> {
			}
			TradeMonopoly -> {
			}
			Warlord -> {
			}
			Wedding -> {
			}
		}
		return true
	}
}