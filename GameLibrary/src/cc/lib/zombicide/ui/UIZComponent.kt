package cc.lib.zombicide.ui

import cc.lib.game.AGraphics
import cc.lib.ui.UIComponent
import cc.lib.zombicide.ZTile

interface UIZComponent<T : AGraphics> : UIComponent {
	fun loadTiles(g: T, tiles: Array<ZTile>)
}