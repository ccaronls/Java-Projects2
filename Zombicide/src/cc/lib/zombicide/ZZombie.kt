package cc.lib.zombicide

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.reflector.Omit
import cc.lib.utils.allMaxOf

open class ZZombie(override val type: ZZombieType = ZZombieType.Walker, val startZone: Int = -1) : ZActor(startZone) {
	companion object {
		init {
			addAllFields(ZZombie::class.java)
		}
	}

	constructor(type: ZZombieType, position: ZActorPosition) : this(type) {
		setPosition(position)
	}

	private var imageIdx = -1

	var destroyed = false
	var frozen = false

	public override val actionsPerTurn: Int
		get() = type.actionsPerTurn

	private val idx: Int
		private get() {
			if (imageIdx < 0 || imageIdx >= type.imageOptions.size) imageIdx =
				Utils.rand() % type.imageOptions.size
			return imageIdx
		}

	override fun name(): String {
        return type.name
    }

    override fun getSpawnQuadrant(): ZCellQuadrant? = when(type) {
        ZZombieType.Abomination,
        ZZombieType.Wolfbomination,
        ZZombieType.BlueTwin,
        ZZombieType.GreenTwin -> ZCellQuadrant.CENTER
        else -> super.getSpawnQuadrant()
    }

    override val scale: Float
        get() = type.scale
    override val imageId: Int
        get() = type.imageOptions[idx]
    override val outlineImageId: Int
        get() = type.imageOutlineOptions[idx]

    override fun getDimension(): GDimension {
        return if (type.imageDims == null) {
            GDimension.EMPTY
        } else type.imageDims[idx]
    }

    override val moveSpeed: Long
        get() = if (type === ZZombieType.Runner) {
	        500
        } else super.moveSpeed

	override val priority: Int
		get() = if (destroyed) -1 else type.ordinal

	fun getDescription(): String {
		return type.description
	}

	override fun isBlockedBy(wallType: ZWallFlag): Boolean = type.isBlockedBy(wallType)

	override suspend fun performAction(action: ZActionType, game: ZGame) {
		when (action) {
			ZActionType.MELEE,
			ZActionType.NOTHING -> actionsLeftThisTurn = 0

			else -> super.performAction(action, game)
		}
	}

	override fun draw(g: AGraphics) {
		if (isAlive || isAnimating) {
			super.draw(g)
            if (actionsLeftThisTurn > 1) {
	            g.pushTextHeight(10f, false)
	            g.color = GColor.TRANSLUSCENT_BLACK
	            val oldWdth = g.setLineWidth(0f)
	            g.drawFilledCircle(getRect().center, .05f)
	            g.color = GColor.WHITE
	            g.drawCircle(getRect().center, 0.05f)
	            g.drawJustifiedString(
		            getRect().center,
		            Justify.CENTER,
		            Justify.CENTER,
		            "$actionsLeftThisTurn"
	            )
	            g.popTextHeight()
	            g.setLineWidth(oldWdth)
            }
        }
    }

	override fun onBeginRound(game: ZGame) {
		super.onBeginRound(game)
		targetZone = null
		if (type == ZZombieType.Ratz) {
			actionsLeftThisTurn = 1 + game.board.getAllZombies().count { it.type == ZZombieType.Ratz }
		}
	}

	@Omit
	private var targetZone: ZZone? = null

	fun getTargetZone(board: ZBoard): ZZone? {
		return targetZone ?: findTargetZone(board)?.also {
			targetZone = it
		}
	}

	override val isAlive: Boolean
		get() = !destroyed

	open fun isEscaping(board: ZBoard): Boolean = false

	protected open fun findTargetZone(board: ZBoard): ZZone? {
		return board.getAllCharacters().filter {
			it.isVisible && board.canSee(occupiedZone, it.occupiedZone)
		}.takeIf {
			it.isNotEmpty()
		}?.map {
			board.getZone(it.occupiedZone)
		}?.allMaxOf {
			it.noiseLevel
		}?.random() ?: board.getMaxNoiseLevelZones().takeIf {
			it.isNotEmpty()
		}?.filter {
			board.isZoneReachable(this, it.zoneIndex)
		}?.randomOrNull()
	}
}