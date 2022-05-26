package cc.applets.kaiser

import cc.game.kaiser.core.Bid
import cc.game.kaiser.core.Card
import cc.game.kaiser.core.Kaiser

class SwingPlayerUser : SwingPlayer {
	lateinit var applet: KaiserApplet

	constructor() {
	}

	constructor(name: String, applet: KaiserApplet) : super(name) {
		this.applet = applet
	}

	override fun isCardsShowing(): Boolean {
		return true
	}

	override fun onProcessRound(k: Kaiser) {
	}

	override fun playTrick(kaiser: Kaiser, options: Array<Card>): Card? {
		return applet.pickCard(options)
	}

	override fun makeBid(kaiser: Kaiser, options: Array<Bid>): Bid? {
		return applet.pickBid(options)
	}
}