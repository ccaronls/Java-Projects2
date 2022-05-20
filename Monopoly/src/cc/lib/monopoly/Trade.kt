package cc.lib.monopoly

class Trade(val card: Card, val price: Int, val trader: Player) {
	override fun toString(): String {
		return "${card.property!!.name} $$ $price"
	}
}