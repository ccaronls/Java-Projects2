package cc.game.soc.core

/**
 * Event cards are a variation that replace the dice.
 *
 * @author chriscaron
 */
enum class EventCardType(val _nameId: String, val helpTextId: String, cakEventDie: Int, vararg val production: Int) : ICardType<Void> {
	RobberAttack("Robber\nAttack", "Current player places the robber and all players discard half of your cards (round down).  Same as rolling a '7'", 7, 7, 7, 7, 7),
	Epidemic("Epidemic", "Each player receives 1 resource for a city (no commodity)", 6, 8),
	Earthquake("Earthquake", "Each player has one of their roads damaged", 6),
	GoodNeighbor("Good\nNeighbor", "Each player give the player to their left 1 resource or commodity of the givers choice", 6),
	Tournament("Tournament", "The player(s) with most (active) knights take 1 resource card", 5),
	TradeAdvantage("Trade\nAdvantage", "The single player with longest road can take 1 resource card from another player", 5),
	CalmSea("Calm\nSea", "Player with the most harbors can take 1 resource card of their choice", 9, 12),
	RobberFlees("Robber\nFlees", "Robber returns to the desert", 4, 4),
	NeighborlyAssistance("Neighborly\nAssistance", "Player with most victory points gives a player of their choice a resource card of the givers choice if they have it.", 10, 11),
	Conflict("Conflict", "The single player with largest army (active knights) take a random resource or commodity card from another player", 3),
	PlentifulYear("Plentiful\nYear", "Each player takes 1 resource of their choice from supply", 2),
	NoEvent("No Event", "The settlers labor and Catan prospers.", 3, 4, 5, 5, 6, 6, 6, 8, 8, 8, 8, 9, 9, 9, 10, 10, 11);

	override val cardType: CardType
		get() = CardType.Event

	override fun getHelpText(rules: Rules): String? {
		return helpTextId
	}

	override fun getData(): Void? = null

	override fun defaultStatus(): CardStatus {
		return CardStatus.USED
	}

	override fun getNameId(): String = _nameId
}