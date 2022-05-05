package cc.lib.zombicide

import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.Utils
import cc.lib.utils.Grid
import cc.lib.utils.Reflector

class ZDoor (val cellPosStart: Grid.Pos, val cellPosEnd: Grid.Pos, val moveDirection: ZDir, val lockedColor: GColor) : Reflector<ZDoor>() {
    companion object {
        init {
            addAllFields(ZDoor::class.java)
        }
    }

    constructor() : this(Grid.Pos(), Grid.Pos(), ZDir.NORTH, GColor.BLACK)
    constructor(start: Grid.Pos, dir:ZDir, color:GColor):this(start, dir.getAdjacent(start)!!, dir, color)

    var isJammed = false
        private set

    fun isLocked(b: ZBoard): Boolean {
        return b.getDoor(this) === ZWallFlag.LOCKED
    }

    fun isClosed(board: ZBoard): Boolean {
        return !board.getCell(cellPosStart).getWallFlag(moveDirection).opened
    }

    fun getRect(board: ZBoard): GRectangle {
        return board.getCell(cellPosStart).getWallRect(moveDirection)
    }

    @JvmOverloads
    fun toggle(board: ZBoard, jammed: Boolean = false) {
        val otherSide = otherSide
        when (board.getDoor(this)) {
            ZWallFlag.OPEN -> board.setDoor(this, ZWallFlag.CLOSED)
            ZWallFlag.LOCKED, ZWallFlag.CLOSED -> board.setDoor(this, ZWallFlag.OPEN)
        }
        otherSide.isJammed = jammed
        isJammed = otherSide.isJammed
    }

    val otherSide: ZDoor
        get() = ZDoor(cellPosEnd, cellPosStart, moveDirection.opposite, lockedColor)

    fun canBeClosed(c: ZCharacter): Boolean {
        when (moveDirection) {
            ZDir.DESCEND, ZDir.ASCEND -> return true
        }
        for (sk in c.getAvailableSkills()) {
            if (sk.canCloseDoors()) return true
        }
        return false
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        if (!super.equals(o)) return false
        val zDoor = o as ZDoor
        return Utils.equals(cellPosStart, zDoor.cellPosStart) &&
                moveDirection === zDoor.moveDirection
    }

    override fun hashCode(): Int {
        return Utils.hashCode(cellPosStart, moveDirection)
    }

    init {
        when (moveDirection) {
            ZDir.ASCEND, ZDir.DESCEND -> isJammed = false
            else                      -> isJammed = true
        }
    }
}