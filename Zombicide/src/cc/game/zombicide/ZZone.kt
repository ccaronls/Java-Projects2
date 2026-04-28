package cc.game.zombicide

import cc.game.zombicide.ui.UIZButton
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.game.IVector2D
import cc.lib.game.Tiles
import cc.lib.math.Vector2D
import cc.lib.reflector.DirtyDelegate
import cc.lib.reflector.Omit
import cc.lib.utils.GException
import cc.lib.utils.Grid.Pos
import cc.lib.utils.rotate

/**
 * Zones are sets of adjacent cells that comprise rooms or streets separated by doors and walls
 */
class ZZone(val zoneIndex: Int = -1, var type: ZEnvironmentType = ZEnvironmentType.NOTHING) : UIZButton() {
	companion object {
		init {
			addAllFields(ZZone::class.java)
		}
	}

	val cells: List<Pos> = ArrayList()
	val doors: List<ZDoor> = ArrayList()
	var noiseLevel by DirtyDelegate(0)
	var isDragonBile by DirtyDelegate(false)
	var isObjective by DirtyDelegate(false)
	private var nextCell = 0

	val isBuilding: Boolean
		get() = type == ZEnvironmentType.BUILDING

	val isVault: Boolean
		get() = type == ZEnvironmentType.VAULT

	val isOutside: Boolean
		get() = type == ZEnvironmentType.OUTDOORS

	fun canSpawn(): Boolean {
		return type === ZEnvironmentType.BUILDING
	}

	fun addDoorIfNeeded(board: ZBoard, door: ZDoor) {
		if (door !in doors)
			(doors as MutableList<ZDoor>).add(door)
		door.setRect(board)
	}

	fun addCell(pos: Pos) {
		(cells as MutableList<Pos>).add(pos)
	}

    val isSearchable: Boolean
	    get() = type === ZEnvironmentType.BUILDING

	fun getCells(): Iterable<Pos> {
		return cells
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

	override val resultObject = zoneIndex

	fun Pos.toRectangle(): GRectangle = GRectangle(column.toFloat(), row.toFloat(), 1f, 1f)

	override fun getRect(): IRectangle = tiles.enclosingRect()

	fun getRandomPointInside(factor: Float = 1f): IVector2D {
		return cells.map {
			it.toRectangle()
		}.randomOrNull()?.scaledBy(factor)?.randomPointInside ?: Vector2D.ZERO
	}

	@delegate:Omit
	private val tiles by lazy {
		Tiles(cells.map { it.toRectangle() })
	}

	override fun drawOutlined(g: AGraphics) {
		tiles.drawOutlined(g)
	}

	override fun contains(px: Number, py: Number): Boolean {
		return tiles.contains(px, py)
	}

	override val center: IVector2D
		get() = tiles.center

	fun getEscapeSpawnArea(board: ZBoard): ZSpawnArea? {
		return cells.map {
			board.getCell(it).spawnAreas
		}.flatten().firstOrNull { it.isEscapableForNecromancers }
	}
}