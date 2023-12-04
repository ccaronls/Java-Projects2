package cc.lib.zombicide

import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.reflector.Reflector
import cc.lib.zombicide.ZCellQuadrant.Companion.valuesForRender

class ZCell internal constructor(private val x: Float, private val y: Float) : Reflector<ZCell>(), IRectangle {
    companion object {
        const val ENV_OUTDOORS = 0
        const val ENV_BUILDING = 1
        const val ENV_VAULT = 2
        const val ENV_TOWER = 4

        init {
            addAllFields(ZCell::class.java)
            require(ZCellType.values().size < 32) // Bit flag can only handle 32 values
        }
    }

    private val walls = arrayOf(ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.WALL, ZWallFlag.WALL)
    @JvmField
    var environment = ENV_OUTDOORS // 0 == outdoors, 1 == building, 2 == vault
    @JvmField
    var zoneIndex = 0
    @JvmField
    var vaultId = 0
    private var cellFlag = 0
    @JvmField
    var discovered = false
    @JvmField
    var scale = 1f
	private val occupied = arrayOfNulls<String?>(ZCellQuadrant.values().size)

	@JvmField
	var spawns = arrayOfNulls<ZSpawnArea>(2)
    @JvmField
    var numSpawns = 0

    constructor() : this(-1f, -1f) {}

    override fun X(): Float {
        return x
    }

    override fun Y(): Float {
        return y
    }

    override fun getWidth(): Float {
        return 1f
    }

    override fun getHeight(): Float {
        return 1f
    }

    fun isCellType(vararg types: ZCellType): Boolean {
        for (t in types) {
            if (1 shl t.ordinal and cellFlag != 0) return true
        }
        return false
    }

    val vaultType: ZCellType
        get() {
            if (isCellType(ZCellType.VAULT_DOOR_GOLD)) return ZCellType.VAULT_DOOR_GOLD
            return if (isCellType(ZCellType.VAULT_DOOR_VIOLET)) ZCellType.VAULT_DOOR_VIOLET else ZCellType.NONE
        }

    val isVault: Boolean
        get() = environment == ENV_VAULT

    val isCellTypeEmpty: Boolean
        get() = cellFlag == 0

    fun setCellType(type: ZCellType, enabled: Boolean) {
        cellFlag = if (enabled) {
            cellFlag or (1 shl type.ordinal)
        } else {
	        cellFlag and (1 shl type.ordinal).inv()
        }
    }

	fun clearCellTypes(vararg types: ZCellType) {
		for (t in types) {
			cellFlag = cellFlag and (1 shl t.ordinal).inv()
		}
	}

	fun getOccupant(board: ZBoard, quadrant: ZCellQuadrant): ZActor? {
		return board.getActor(occupied[quadrant.ordinal])
	}

	fun setQuadrant(actor: ZActor?, quadrant: ZCellQuadrant) {
		occupied[quadrant.ordinal] = actor?.getId()
	}

	fun getOccupants(board: ZBoard): Iterable<ZActor> {
		return valuesForRender().mapNotNull { board.getActor(occupied[it.ordinal]) }
	}

	val numOccupants: Int
		get() = occupied.filterNotNull().size

	fun findLowestPriorityOccupant(board: ZBoard): ZCellQuadrant {
		var min = ZCharacter.PRIORITY
		var best: ZCellQuadrant? = null
		for (q in ZCellQuadrant.values()) {
			with(board.getActor(occupied[q.ordinal])) {
				if (this == null)
					return q
				if (priority < min || best == null) {
					min = priority
					best = q
				}
			}
        }
        return best!!
    }

    val isInside: Boolean
        get() = environment == ENV_BUILDING

    fun getWallFlag(dir: ZDir): ZWallFlag {
        return walls[dir.ordinal]
    }

    fun setWallFlag(dir: ZDir, flag: ZWallFlag) {
        walls[dir.ordinal] = flag
    }

    fun getWallRect(dir: ZDir): GRectangle {
        when (dir) {
            ZDir.NORTH -> return GRectangle(topLeft, topRight)
            ZDir.SOUTH -> return GRectangle(bottomLeft, bottomRight)
            ZDir.EAST -> return GRectangle(topRight, bottomRight)
	        ZDir.WEST -> return GRectangle(topLeft, bottomLeft)
	        else -> Unit
        }
        return GRectangle(this).scaledBy(.5f)
    }

    fun getQuadrant(quadrant: ZCellQuadrant): GRectangle {
        when (quadrant) {
            ZCellQuadrant.UPPERLEFT -> return GRectangle(topLeft, center)
            ZCellQuadrant.LOWERRIGHT -> return GRectangle(center, bottomRight)
            ZCellQuadrant.UPPERRIGHT -> return GRectangle(center, topRight)
            ZCellQuadrant.LOWERLEFT -> return GRectangle(center, bottomLeft)
            ZCellQuadrant.CENTER -> return GRectangle(X() + width / 4, Y() + height / 4, width / 2, height / 2)
            ZCellQuadrant.TOP -> return GRectangle(X() + width / 4, Y(), width / 2, height / 2)
            ZCellQuadrant.LEFT -> return GRectangle(X(), Y() + height / 4, width / 2, height / 2)
            ZCellQuadrant.RIGHT -> return GRectangle(X() + width / 2, Y() + height / 4, width / 2, height / 2)
            ZCellQuadrant.BOTTOM -> return GRectangle(X() + width / 4, Y() + height / 2, width / 2, height / 2)
        }
    }

    val isFull: Boolean
        get() {
            for (a in occupied) if (a == null) return false
            return true
        }
    val spawnAreas: List<ZSpawnArea>
        get() = spawns.toList().filterNotNull()

    fun removeSpawn(dir: ZDir) {
        require(numSpawns > 0)
        if (spawns[0]!!.dir === dir) {
            spawns[0] = spawns[1]
	        numSpawns --
        } else {
	        require(numSpawns > 1)
	        require(spawns[1]!!.dir === dir)
            spawns[--numSpawns] = null
        }
    }

}