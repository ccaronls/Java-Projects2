package cc.game.golf.swing

import cc.game.golf.core.Card
import cc.game.golf.core.DrawType
import cc.game.golf.core.Golf
import cc.game.golf.core.Player

class SwingPlayerUser : Player {
	lateinit var g: GolfSwing

	constructor() : super()
	constructor(name: String) : super(name)
	constructor(name: String, g: GolfSwing) : super(name) {
		this.g = g
	}

	override suspend fun turnOverCard(golf: Golf, row: Int): Int? {
		return g.pickCard(getRow(row))
		//        if (card >= 0)
		//g.addTurnOverCardAnimation(this, row, card);
//        return card;
	}

	override suspend fun chooseDrawPile(golf: Golf): DrawType? {
		val options: MutableList<Card?> = ArrayList()
		options.add(golf.topOfDeck)
		options.add(golf.topOfDiscardPile)
		val p = g.pickCard(options.filterNotNull())
		if (p == 0) {
			return DrawType.DTStack
		}
		return if (p == 1) DrawType.DTDiscardPile else DrawType.DTWaiting
	}

	override suspend fun chooseDiscardOrPlay(golf: Golf, drawCard: Card): Card? {
		var swapped: Card? = null
		val all: MutableList<Card> = ArrayList(this.numCardsDealt + 1)
		all.addAll(allCards)
		all.add(drawCard)
		//g.setExtraCard(drawCard);
		val p = g.pickCard(all)
		//g.setExtraCard(null);
		if (p >= 0) {
			swapped = all[p]
			//onChooseCardToSwap(swapped);
		}
		return swapped
	}

	override suspend fun chooseCardToSwap(golf: Golf, drawCard: Card): Card? {
		var swapped: Card? = null
		val all: MutableList<Card> = ArrayList(this.numCardsDealt)
		all.addAll(allCards)
		val p = g.pickCard(all)
		if (p >= 0) {
			swapped = all[p]
		}
		return swapped
	}

	val message: String
		get() = "I am not a robot!"

	fun setGolfSwing(g: GolfSwing) {
		this.g = g
	}
}