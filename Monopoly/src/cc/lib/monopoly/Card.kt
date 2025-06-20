package cc.lib.monopoly

import cc.lib.reflector.Reflector

class Card(val property: Square=Square.GO) : Reflector<Card>() {
	companion object {
		@JvmStatic
        fun newGetOutOfJailFreeCard(): Card = Card(Square.GOTO_JAIL)

		@JvmStatic
        fun newPropertyCard(property: Square): Card = Card(property)

		init {
			addAllFields(Card::class.java)
		}
	}

	val isGetOutOfJail
		get() = property == Square.GOTO_JAIL

	@JvmField
    var houses = 0
	var isMortgaged = false

	val isSellable: Boolean
		get() = property.canPurchase() && !isMortgaged

	fun canMortgage(): Boolean {
		return property.canPurchase() && !isMortgaged
	}

	fun canUnMortgage(): Boolean {
		return property.canPurchase() && isMortgaged
	}

	override fun toString(): String {
		if (isGetOutOfJail)
			return "Get Out of Jail Free"
		var s = property.name
		if (houses in 1..4) {
			s += "\nhouses X $houses"
		} else if (houses == 5) {
			s += "\nHOTEL"
		}
		s += if (isMortgaged) {
			"""

 	MORTGAGED
	    Buy Back ${"$"}${property.mortgageBuybackPrice}
 	""".trimIndent()
		} else {
			"""

 	Mortgage
 	Value ${"$"}${property.getMortgageValue(houses)}
 	""".trimIndent()
		}
		return s
	}

	override fun hashCode(): Int {
		return property.hashCode()
	}

	override fun equals(obj: Any?): Boolean {
		return (obj as Card?)?.property == property
	}
}