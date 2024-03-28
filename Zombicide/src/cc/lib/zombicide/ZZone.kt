package cc.lib.zombicide

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.game.Utils
import cc.lib.math.MutableVector2D
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import cc.lib.utils.GException
import cc.lib.utils.Grid
import cc.lib.utils.rotate

/**
 * Zones are sets of adjacent cells that comprise rooms or streets separated by doors and walls
 */
class ZZone(val zoneIndex: Int = -1) : Reflector<ZZone>(), IRectangle {
	companion object {
		init {
			addAllFields(ZZone::class.java)
		}
	}

	val cells: MutableList<Grid.Pos> = ArrayList()
	val doors: MutableList<ZDoor> = ArrayList()
	var type = ZZoneType.UNSET
	var noiseLevel = 0
	var isDragonBile = false
	var isObjective = false
	private var nextCell = 0

	val isBuilding: Boolean
		get() = type == ZZoneType.BUILDING

	val isVault: Boolean
		get() = type == ZZoneType.VAULT

	val isOutside: Boolean
		get() = type == ZZoneType.OUTDOORS

	@Omit
	var pickable = false

	fun canSpawn(): Boolean {
		return type === ZZoneType.BUILDING
	}

	override fun getCenter(): MutableVector2D = rectangle.center

	override fun getArea(): Float = rectangle.area

	/**
	 *
	 * @return
	 */
	@delegate:Omit
	val rectangle by lazy {
		GRectangle().also {
			for (p in cells) {
				it.addEq(p.column.toFloat(), p.row.toFloat(), 1f, 1f)
			}
		}
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

	override fun getWidth(): Float = rectangle.w

	override fun getHeight(): Float = rectangle.h
	override fun X(): Float = rectangle.x

	override fun Y(): Float = rectangle.y
}