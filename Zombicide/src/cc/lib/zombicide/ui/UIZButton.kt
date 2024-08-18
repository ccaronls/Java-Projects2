package cc.lib.zombicide.ui

import cc.lib.game.AGraphics
import cc.lib.game.IRectangle
import cc.lib.ui.IButton
import cc.lib.utils.Table

interface UIZButton : IButton, IRectangle {
	fun onClick() {
		UIZombicide.instance.setResult(this)
	}

	fun getInfo(g: AGraphics, width: Int, height: Int): Table? {
		return null
	}

	override fun getLeft() = getRect().getLeft()

	override fun getTop() = getRect().getTop()

	override fun getWidth() = getRect().width

	override fun getHeight() = getRect().height
}