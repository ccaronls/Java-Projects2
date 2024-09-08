package cc.lib.zombicide

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.utils.Grid
import cc.lib.zombicide.ui.UIZButton

data class ZSpawnArea(
	val cellPos: Grid.Pos = Grid.Pos(),
	var icon: ZIcon = ZIcon.SPAWN_RED,
	val dir: ZDir = ZDir.NORTH,
	var isCanSpawnNecromancers: Boolean = false,
	var isEscapableForNecromancers: Boolean = false,
	var isCanBeRemovedFromBoard: Boolean = false,
	var isCanBeDestroyedByCatapult: Boolean = false
) : UIZButton() {

	private var rect: GRectangle = GRectangle()

	companion object {
		init {
			addAllFields(ZSpawnArea::class.java)
		}
	}

	override fun equals(other: Any?): Boolean {
		if (other !is ZSpawnArea)
			return false
		if (other === this)
			return true
		return cellPos == other.cellPos && dir == other.dir
	}

	override fun getRect(): IRectangle = rect

	fun setRect(rect: IRectangle) {
		this.rect = GRectangle(rect)
	}

	fun draw(g: AGraphics, r: IRectangle = rect) {
		g.drawImage(icon.imageIds[dir.ordinal], r)
	}
}