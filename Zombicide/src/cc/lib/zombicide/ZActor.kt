package cc.lib.zombicide

import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GDimension
import cc.lib.game.GRectangle
import cc.lib.game.IInterpolator
import cc.lib.game.IRectangle
import cc.lib.game.IVector2D
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.math.Vector2D
import cc.lib.reflector.Omit
import cc.lib.utils.Grid
import cc.lib.zombicide.ui.UIZButton
import java.util.LinkedList

abstract class ZActor internal constructor(var occupiedZone: Int) : UIZButton(),
	IVector2D, IInterpolator<Vector2D> {
	companion object {
		init {
			addAllFields(ZActor::class.java)
		}
	}

	var priorZone: Int = -1
		get() = if (field < 0) occupiedZone else field

	lateinit var occupiedCell: Grid.Pos
	lateinit var occupiedQuadrant: ZCellQuadrant

	fun isOccupying(): Boolean = ::occupiedQuadrant.isInitialized
	var actionsLeftThisTurn = 0
	private var rect = GRectangle()

	@Omit
	val animations = LinkedList<ZActorAnimation>()

	open val drawPathsOnHighlight = false

	private var id: String? = null

	fun stopAnimating() {
		animations.clear()
	}

	fun getId(): String = id ?: makeId()

	protected open fun makeId(): String {
		return (type.name + Utils.genRandomString(8) + (System.currentTimeMillis() % 1000)).also {
			id = it
		}
	}

	abstract fun isBlockedBy(wallType: ZWallFlag): Boolean

	fun getRect(b: ZBoard): GRectangle {
		return b.getCell(occupiedCell)
			.getQuadrant(occupiedQuadrant)
			.fit(dimension)
			.scaledBy(scale * b.getCell(occupiedCell).scale, Justify.CENTER, Justify.BOTTOM)
	}

	fun updateRect(b: ZBoard) {
		rect = getRect(b)
	}

	override fun getRect(): IRectangle {
		return animations.firstOrNull()?.rect ?: rect
	}

	open fun onBeginRound(game: ZGame) {
		actionsLeftThisTurn = actionsPerTurn
	}

    open fun getSpawnQuadrant(): ZCellQuadrant? = null

    protected abstract val actionsPerTurn: Int
    abstract fun name(): String
    open fun performAction(action: ZActionType, game: ZGame) {
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
	abstract val type: Enum<*>
	open val scale: Float
		get() = 1f
	abstract val imageId: Int
	abstract val outlineImageId: Int
	abstract override fun getDimension(): GDimension
	open val isInvisible: Boolean
		get() = false

	fun addAnimation(anim: ZActorAnimation) {
		animations.add(anim)
	}

	open val moveSpeed: Long
		get() = 1000
	val isAnimating: Boolean
		get() = animations.isNotEmpty()

    fun drawOrAnimate(g: AGraphics) {
	    while (animations.size > 0 && animations.first.isDone) {
		    animations.first.rect?.let {
			    rect = it
		    }
		    animations.pop()
	    }
	    animations.firstOrNull()?.let {
		    if (!it.hidesActor())
			    draw(g)
		    if (!it.isStarted)
			    it.start<AAnimation<AGraphics>>()
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

    override fun getX(): Float {
	    return center.X()
    }

    override fun getY(): Float {
	    return center.Y()
    }

    override fun getAtPosition(position: Float): Vector2D {
	    return center
    }

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