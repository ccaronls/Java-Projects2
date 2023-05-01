package cc.lib.zombicide

import cc.lib.game.GRectangle
import cc.lib.utils.Grid
import cc.lib.utils.Reflector

data class ZSpawnArea(val cellPos: Grid.Pos=Grid.Pos(), var icon: ZIcon=ZIcon.SPAWN_RED, val dir: ZDir=ZDir.NORTH, var isCanSpawnNecromancers: Boolean=false, var isEscapableForNecromancers: Boolean=false, var isCanBeRemovedFromBoard: Boolean=false) : Reflector<ZSpawnArea>() {
    companion object {
        init {
            addAllFields(ZSpawnArea::class.java)
        }
    }

    var rect = GRectangle()
}