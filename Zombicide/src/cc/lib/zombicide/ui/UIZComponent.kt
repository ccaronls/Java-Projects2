package cc.lib.zombicide.ui

import cc.lib.game.AGraphics
import cc.lib.ui.UIComponent
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZTile

interface UIZComponent<in T : AGraphics> : UIComponent {
	fun loadTiles(g: T, tiles: Array<ZTile>, quest: ZQuest)
}