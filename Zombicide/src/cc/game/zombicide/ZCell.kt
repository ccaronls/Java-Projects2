package cc.game.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.reflector.Alternate
import cc.lib.reflector.DirtyArray
import cc.lib.reflector.DirtyList
import cc.lib.reflector.DirtyReflector
import cc.lib.reflector.Omit
import cc.lib.reflector.dirty
import cc.lib.reflector.dirtyArrayOf
import cc.lib.utils.padEndToFit

@Keep
enum class ZEnvironmentType(val color: GColor, val code: String) {
	NOTHING(GColor.TRANSPARENT, ""),
	OUTDOORS(GColor.LIGHT_GRAY, "O"),
	BUILDING(GColor.DARK_GRAY, "B"),
	VAULT(GColor.BROWN, "V"),
	TOWER(GColor.LIGHT_GRAY, "T"),
	WATER(GColor.SKY_BLUE, "W"),
	HOARD(GColor.ORANGE, "H"), // cell marked as a place where players cannot travel, accumulates hoard and can be targeted by catapult
	;

	fun getVaultDirection(): ZDir = when (this) {
		VAULT -> ZDir.ASCEND
		else -> ZDir.DESCEND
	}
}


class ZCell internal constructor(private val x: Float, private val y: Float) : DirtyReflector<ZCell>(),
                                                                               IRectangle {
	companion object {
		init {
			addAllFields(ZCell::class.java)
			require(ZCellType.values().size < 32) // Bit flag can only handle 32 values
		}
	}

	private val walls = dirtyArrayOf(
		ZWallFlag.NONE,
		ZWallFlag.NONE,
		ZWallFlag.NONE,
		ZWallFlag.NONE,
		ZWallFlag.WALL,
		ZWallFlag.WALL
	)
	var environment by dirty(ZEnvironmentType.OUTDOORS) // 0 == outdoors, 1 == building, 2 == vault
	var zoneIndex = 0
	var vaultId = 0
	private var cellFlag by dirty(0)
	var discovered by dirty(false)
	var scale = 1f
	private val occupied = DirtyArray<String?>(ZCellQuadrant.values().size)

	@Alternate("spawns")
	private val _spawns = DirtyList<ZSpawnArea>()
	val spawns: List<ZSpawnArea>
		get() = _spawns

	constructor() : this(-1f, -1f) {}

	override val width = 1f
	override val height = 1f
	override val left = x
	override val top: Float = y

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
	    get() = environment == ZEnvironmentType.VAULT

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

	fun getOccupant(board: ZBoard, quadrant: ZCellQuadrant): ZActor<*>? {
		return board.getActor(occupied[quadrant.ordinal])
	}

	fun setQuadrant(actor: ZActor<*>?, quadrant: ZCellQuadrant) {
		occupied[quadrant.ordinal] = actor?.id
	}

	fun getOccupants(board: ZBoard): Iterable<ZActor<*>> {
		return ZCellQuadrant.valuesForRender().mapNotNull { board.getActor(occupied[it.ordinal]) }
	}

	val numOccupants: Int
		get() = occupied.filterNotNull().size

	fun findLowestPriorityOccupant(board: ZBoard): ZCellQuadrant {
		var min = ZCharacter.PRIORITY
		var best: ZCellQuadrant? = null
		for (q in ZCellQuadrant.valuesForInsert()) {
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
	    get() = environment == ZEnvironmentType.BUILDING

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
		    ZCellQuadrant.UPPER_LEFT -> return GRectangle(topLeft, center)
		    ZCellQuadrant.LOWER_RIGHT -> return GRectangle(center, bottomRight)
		    ZCellQuadrant.UPPER_RIGHT -> return GRectangle(center, topRight)
		    ZCellQuadrant.LOWER_LEFT -> return GRectangle(center, bottomLeft)
		    ZCellQuadrant.CENTER -> return GRectangle(x + width / 4, y + height / 4, width / 2, height / 2)
		    ZCellQuadrant.TOP -> return GRectangle(x + width / 4, y, width / 2, height / 2)
		    ZCellQuadrant.LEFT -> return GRectangle(x, y + height / 4, width / 2, height / 2)
		    ZCellQuadrant.RIGHT -> return GRectangle(x + width / 2, y + height / 4, width / 2, height / 2)
		    ZCellQuadrant.BOTTOM -> return GRectangle(x + width / 4, y + height / 2, width / 2, height / 2)

		    ZCellQuadrant.UPPER_LEFT2 -> return GRectangle(topLeft, center).moveBy(.1f, .1f)
		    ZCellQuadrant.LOWER_RIGHT2 -> return GRectangle(center, bottomRight).moveBy(-.1f, -.1f)
		    ZCellQuadrant.UPPER_RIGHT2 -> return GRectangle(center, topRight).moveBy(-.1f, .1f)
		    ZCellQuadrant.LOWER_LEFT2 -> return GRectangle(center, bottomLeft).moveBy(.1f, -.1f)
		    ZCellQuadrant.CENTER2 -> return GRectangle(x + width / 4, y + height / 4, width / 2, height / 2)
		    ZCellQuadrant.TOP2 -> return GRectangle(x + width / 4, y, width / 2, height / 2).moveBy(.0f, .1f)
		    ZCellQuadrant.LEFT2 -> return GRectangle(x, y + height / 4, width / 2, height / 2).moveBy(.1f, 0f)
		    ZCellQuadrant.RIGHT2 -> return GRectangle(x + width / 2, y + height / 4, width / 2, height / 2).moveBy(-.1f, 0f)
		    ZCellQuadrant.BOTTOM2 -> return GRectangle(x + width / 4, y + height / 2, width / 2, height / 2)
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
		require(_spawns.size > 0)
		_spawns.removeIf { it.dir == dir }
	}

	fun addSpawn(spawn: ZSpawnArea) {
		require(spawns.size < 2)
		require(spawns.none { it.dir == spawn.dir })
		_spawns.add(spawn)
	}

	@delegate:Omit
	val codes by lazy {
		fun String.appendSpawnCount(): String {
			if (spawns.isEmpty())
				return this
			return "${this}S${spawns.size}"
		}
		ZCellType.values().filter { isCellType(it) }.map { it.code }.joinToString("").appendSpawnCount().padEndToFit(8)
	}
}