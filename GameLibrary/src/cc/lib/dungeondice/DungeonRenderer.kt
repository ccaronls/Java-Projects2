package cc.lib.dungeondice

import cc.lib.game.APGraphics
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

class DungeonRenderer(component: UIComponent) : UIRenderer(component) {
	var dungeon: DDungeon? = null
	override fun draw(g: APGraphics, px: Int, py: Int) {
		dungeon?.draw(g)
	}
}