package cc.lib.dungeondice

import cc.lib.game.AGraphics
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

class DungeonRenderer(component: UIComponent) : UIRenderer(component) {
	var dungeon: DDungeon? = null


	override fun draw(g: AGraphics) {
		dungeon?.draw(g)
	}
}