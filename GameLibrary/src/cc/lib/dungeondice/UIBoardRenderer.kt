package cc.lib.dungeondice

import cc.lib.game.APGraphics
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

class UIBoardRenderer internal constructor(component: UIComponent) : UIRenderer(component) {
	var board: DBoard? = null
	override fun draw(g: APGraphics, px: Int, py: Int) {
		board?.drawCells(g, 1f)
	}
}