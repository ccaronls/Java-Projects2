package cc.game.soc.core

class EventCard(type: EventCardType=EventCardType.NoEvent, val production: Int=-1) : Card(CardType.Event, type.ordinal, CardStatus.UNUSABLE) {
	companion object {
		init {
			addAllFields(EventCard::class.java)
		}
	}

	val type: EventCardType
		get() = EventCardType.values()[typeOrdinal]
}