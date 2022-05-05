package cc.lib.zombicide

import cc.lib.game.GRectangle
import cc.lib.utils.Grid
import cc.lib.utils.Reflector

class ZSpawnArea(val cellPos: Grid.Pos=Grid.Pos(), var icon: ZIcon=ZIcon.ARROW, val dir: ZDir=ZDir.NORTH, var isCanSpawnNecromancers: Boolean=false, var isEscapableForNecromancers: Boolean=false, var isCanBeRemovedFromBoard: Boolean=false) : Reflector<ZSpawnArea>() {
    companion object {
        init {
            addAllFields(ZSpawnArea::class.java)
        }
    }

    var rect = GRectangle()

    /**
     * Default behavior for most spawns
     *
     * @param dir
     */
    @JvmOverloads
    constructor(cellPos: Grid.Pos, dir: ZDir) : this(cellPos, ZIcon.SPAWN_RED, dir, false, true, true) {
    }
}