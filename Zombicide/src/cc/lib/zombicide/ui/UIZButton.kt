package cc.lib.zombicide.ui

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.ui.IButton
import cc.lib.utils.Table

interface UIZButton : IButton {
	fun onClick() {
		UIZombicide.instance.setResult(this)
	}

	fun getRect() : GRectangle

	fun getInfo(g: AGraphics?, width: Int, height: Int): Table? {
		return null
	}

	/**
	 * Higher numbers have priority when buttons overlapp
	 *
	 * @return
	 */
	override fun getZOrder(): Int {
		return 0
	}
}