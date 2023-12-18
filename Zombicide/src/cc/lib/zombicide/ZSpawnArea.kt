package cc.lib.zombicide

import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import cc.lib.utils.Grid

data class ZSpawnArea(val cellPos: Grid.Pos = Grid.Pos(), var icon: ZIcon = ZIcon.SPAWN_RED, val dir: ZDir = ZDir.NORTH, var isCanSpawnNecromancers: Boolean = false, var isEscapableForNecromancers: Boolean = false, var isCanBeRemovedFromBoard: Boolean = false) : Reflector<ZSpawnArea>(), IRectangle {
	companion object {
		init {
			addAllFields(ZSpawnArea::class.java)
		}
	}

	var rect = GRectangle()

	@Omit
	var pickable = false

	override fun getWidth(): Float = rect.width

	override fun getHeight(): Float = rect.height

	override fun X(): Float = rect.x
	override fun Y(): Float = rect.y
}