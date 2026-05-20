package cc.game.zombicide

import cc.game.zombicide.ui.UIZButton
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.reflector.Omit
import cc.lib.reflector.dirty
import cc.lib.timer.GlobalTimer
import cc.lib.utils.Grid
import java.util.LinkedList

abstract class ZActor<T : Enum<T>> internal constructor(_occupiedZone: Int) : UIZButton() {
	companion object {
		init {
			addAllFields(ZActor::class.java)
		}
	}

	abstract val type: T
	var priorZone: Int by dirty(-1)
	var occupiedZone by dirty(_occupiedZone)
	var occupiedCell by dirty(Grid.Pos(-1, -1))
	var occupiedQuadrant by dirty(ZCellQuadrant.CENTER)

	fun isOccupying(): Boolean = occupiedCell.row >= 0
	var actionsLeftThisTurn by dirty(0)

	private var rect by dirty(GRectangle())

	@Omit
	val animations = LinkedList<ZActorAnimation>()

	fun stopAnimating() {
		animations.clear()
	}

	protected fun makeId(): String {
		return (type.name + Utils.genRandomString(8) + (GlobalTimer.currentTimeMillis() % 1000))
	}

	abstract fun actionToCross(wallType: ZWallFlag): ZActionType

	fun getRect(b: ZBoard): GRectangle {
		return b.getCell(occupiedCell)
			.getQuadrant(occupiedQuadrant)
			.fit(dimension)
			.scaledBy(scale * b.getCell(occupiedCell).scale, Justify.CENTER, Justify.BOTTOM)
	}

	fun updateRect(b: ZBoard) {
		rect = getRect(b)
		require(!rect.isNan)
	}

	override fun getRect(): IRectangle {
		return animations.firstOrNull()?.rect ?: rect
	}

	open fun onBeginRound(game: ZGame) {
		actionsLeftThisTurn = actionsPerTurn
	}

	open fun getSpawnQuadrant(board: ZBoard): ZCellQuadrant? = null

    protected abstract val actionsPerTurn: Int
    abstract fun name(): String
	open suspend fun performAction(action: ZActionType, game: ZGame) {
		if (isAlive) {
			require(actionsLeftThisTurn + action.costPerTurn >= 0)
			actionsLeftThisTurn -= action.costPerTurn
		}
	}

	fun addExtraAction() {
		actionsLeftThisTurn++
	}

	open val noise: Int
		get() = 0
	abstract val imageId: Int
	abstract val outlineImageId: Int

	abstract val id: String

	open val scale: Float
		get() = 1f
	open val isInvisible: Boolean
		get() = false

	fun addAnimation(anim: ZAnimation) {
		animations.add(anim as ZActorAnimation)
	}

	open val moveSpeed: Long
		get() = 1000
	val isAnimating: Boolean
		get() = animations.isNotEmpty()

	fun drawOrAnimate(g: AGraphics) {
		while (animations.firstOrNull()?.isDone == true) {
			animations.first.rect?.let {
			    rect = it
		    }
		    animations.pop()
	    }
	    animations.firstOrNull()?.let {
		    if (!it.hidesActor())
			    draw(g)
		    if (!it.isStarted)
			    it.start()
		    it.update(g)
	    } ?: run {
		    draw(g)
	    }
    }

    open fun draw(g: AGraphics) {
        if (isInvisible) {
            g.setTransparencyFilter(.5f)
        }
        g.drawImage(imageId, getRect())
        g.removeFilter()
    }

    open val priority: Int
        get() = if (isAlive) 0 else -1

    override fun getLabel(): String {
        return name()
    }

    override fun getTooltipText(): String? {
        return null
    }

    fun clearActions() {
        actionsLeftThisTurn = 0
    }

    open val isAlive: Boolean
        get() = true

	val position: ZActorPosition
		get() = ZActorPosition(occupiedCell, occupiedQuadrant, occupiedZone)
	open val isNoisy: Boolean
		get() = false

	open val isRendered: Boolean
		get() = isAlive || isAnimating

	open val isSiegeEngine: Boolean
		get() = false

	fun setPosition(position: ZActorPosition) {
		occupiedQuadrant = position.quadrant
		occupiedCell = position.pos
		occupiedZone = position.zone
	}

	override fun toString(): String {
		return "${getLabel()} zone:$occupiedZone"
	}

	open fun getMoveOptions(name: ZPlayerName, game: ZGame): List<ZMove> = emptyList()

	open fun hasSkill(skill: ZSkill): Boolean = false
}