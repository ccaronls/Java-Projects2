package cc.applets.kaiser

import cc.game.kaiser.ai.PlayerBot

open class SwingPlayer : PlayerBot {
	constructor() {}
	constructor(name: String) : super(name) {}

	open fun isCardsShowing(): Boolean = false
}