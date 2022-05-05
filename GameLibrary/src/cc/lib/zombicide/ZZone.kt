package cc.lib.zombicide

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IShape
import cc.lib.game.Utils
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.GException
import cc.lib.utils.Grid
import cc.lib.utils.Reflector
import cc.lib.utils.rotate
import java.util.*

/**
 * Zones are sets of adjacent cells that comprise rooms or streets separated by doors and walls
 */
class ZZone(val zoneIndex: Int=-1) : Reflector<ZZone>(), IShape {
    companion object {
        init {
            addAllFields(ZZone::class.java)
        }
    }

    @JvmField
    val cells: MutableList<Grid.Pos> = ArrayList()
    @JvmField
    val doors: MutableList<ZDoor> = ArrayList()
    var type = ZZoneType.OUTDOORS
    var noiseLevel = 0
    var isDragonBile = false
    var isObjective = false
    private var nextCell = 0

    fun canSpawn(): Boolean {
        return type === ZZoneType.BUILDING
    }

    override fun getCenter(): MutableVector2D {
        if (cells.size == 0) return MutableVector2D(Vector2D.ZERO)
        val v = MutableVector2D()
        for (p in cells) {
            v.addEq(.5f + p.column, .5f + p.row)
        }
        v.scaleEq(1f / cells.size)
        return v
    }

    /**
     *
     * @return
     */
    val rectangle: GRectangle
        get() {
            val rect = GRectangle()
            for (p in cells) {
                rect.addEq(p.column.toFloat(), p.row.toFloat(), 1f, 1f)
            }
            return rect
        }

    override fun drawFilled(g: AGraphics) {
        for (p in cells) {
            g.drawFilledRect(p.column.toFloat(), p.row.toFloat(), 1f, 1f)
        }
    }

    override fun drawOutlined(g: AGraphics) {
        for (p in cells) {
            g.drawRect(p.column.toFloat(), p.row.toFloat(), 1f, 1f)
        }
    }

    val isSearchable: Boolean
        get() = type === ZZoneType.BUILDING

    fun getCells(): Iterable<Grid.Pos> {
        return cells
    }

    override fun contains(x: Float, y: Float): Boolean {
        for (pos in cells) {
            if (Utils.isPointInsideRect(x, y, pos.column.toFloat(), pos.row.toFloat(), 1f, 1f)) return true
        }
        return false
    }

    fun addNoise(amt: Int) {
        noiseLevel += amt
    }

    val nextCellAndIncrement: Int
        get() {
            val next = nextCell
            nextCell = nextCell.rotate(cells.size)
            return next
        }

    fun checkSanity() {
        if (cells.size > 1) {
            for (i in 0 until cells.size - 1) {
                for (ii in i + 1 until cells.size) {
                    if (cells[i].isAdjacentTo(cells[ii])) {
                        return  // zone is sane
                    }
                }
            }
            throw GException("Zone $zoneIndex is INSANE!! Not all positions are adjacent:$cells")
        }
    }
}