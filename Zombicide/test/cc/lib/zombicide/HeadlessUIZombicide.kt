package cc.lib.zombicide

import cc.lib.game.AGraphics
import cc.lib.math.Vector2D
import cc.lib.zombicide.ui.UIZBoardRenderer
import cc.lib.zombicide.ui.UIZCharacterRenderer
import cc.lib.zombicide.ui.UIZComponent
import cc.lib.zombicide.ui.UIZombicide

class HeadlessComponent : UIZComponent<AGraphics> {
	override fun getWidth(): Int {
		TODO("Not yet implemented")
	}

	override fun getHeight(): Int {
		TODO("Not yet implemented")
	}

	override fun redraw() {
		TODO("Not yet implemented")
	}

	override fun getViewportLocation(): Vector2D {
		TODO("Not yet implemented")
	}

	override fun loadTiles(g: AGraphics, tiles: Array<ZTile>, quest: ZQuest) {
		TODO("Not yet implemented")
	}
}

/**
 * Created by Chris Caron on 3/7/22.
 */
class HeadlessUIZombicide : UIZombicide(
	object : UIZCharacterRenderer(HeadlessComponent()) {
		override fun scrollToTop() {
			TODO("Not yet implemented")
		}

	}, object : UIZBoardRenderer(HeadlessComponent()) {
	}) {
	override val thisUser: ZUser
		get() = TODO("Not yet implemented")

	override fun focusOnMainMenu() {
		TODO("Not yet implemented")
	}

	override fun focusOnBoard() {
		TODO("Not yet implemented")
	}

	override fun undo() {
		TODO("Not yet implemented")
	}
}
