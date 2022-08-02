package cc.game.soc.core

import cc.lib.utils.Reflector

/**
 *
 * @author Chris Caron
 */
class Trade : Reflector<Trade> {
	companion object {
		init {
			addAllFields(Trade::class.java)
		}
	}

	private val type: Card
	var amount = 0
		private set

	constructor() : this(ResourceType.Brick, 0)

	override fun toString(): String {
		return "$type X $amount"
	}

	constructor(card: Card, amount: Int) {
		type = card
		this.amount = amount
	}

	constructor(type: ICardType<*>, amount: Int) {
		this.type = Card(type, CardStatus.USABLE)
		this.amount = amount
	}

	fun getType(): ICardType<*> {
		return type.cardType.dereferenceOrdinal(type.typeOrdinal)
	}
}