package cc.game.zombicide.ui

import cc.game.zombicide.ZQuest
import cc.game.zombicide.ZTile
import cc.lib.game.AGraphics
import cc.lib.ui.UIComponent

interface UIZComponent<in T : AGraphics> : UIComponent {
	fun loadTiles(g: T, tiles: Array<ZTile>, quest: ZQuest)
}