package cc.lib.zombicide

import cc.lib.game.*
import cc.lib.math.Vector2D
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import cc.lib.utils.Grid
import cc.lib.zombicide.ui.UIZButton

abstract class ZActor internal constructor(var occupiedZone: Int = -1) : Reflector<ZActor>(), UIZButton, IRectangle, IVector2D, IInterpolator<Vector2D> {
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

	@Omit
	private var rect = GRectangle()

	@Omit
	var animation: ZActorAnimation? = null

	private var id: String? = type?.let {
		makeId()
	}

	fun getId(): String = id ?: makeId()

	protected open fun makeId(): String {
		return (type.name + Utils.genRandomString(8) + (System.currentTimeMillis() % 1000)).also {
			id = it
		}
	}

	fun getRect(b: ZBoard): GRectangle {
		return b.getCell(occupiedCell)
			.getQuadrant(occupiedQuadrant)
			.fit(dimension)
			.scaledBy(scale * b.getCell(occupiedCell).scale, Justify.CENTER, Justify.BOTTOM).also { rect = it }
	}

	override fun getRect(): GRectangle {
		return animation?.rect ?: rect
    }

    open fun onBeginRound() {
        actionsLeftThisTurn = actionsPerTurn
    }

    open fun getSpawnQuadrant(): ZCellQuadrant? = null

    protected abstract val actionsPerTurn: Int
    abstract fun name(): String
    open fun performAction(action: ZActionType, game: ZGame) {
        if (isAlive) {
            actionsLeftThisTurn -= action.costPerTurn()
            require(actionsLeftThisTurn >= 0)
        }
    }

    fun addExtraAction() {
        actionsLeftThisTurn++
    }

    abstract fun drawInfo(g: AGraphics, game: ZGame, width: Float, height: Float): IDimension?

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

	@Omit
	var pickable = false

	fun addAnimation(anim: ZActorAnimation) {
		animation?.takeIf { !it.isDone }?.also {
			it.add(anim)
		} ?: run {
			animation = anim.start()
		}
	}

	open val moveSpeed: Long
        get() = 1000
    val isAnimating: Boolean
		get() = animation?.isDone?.not()?:false

    fun getAnimation(): ZAnimation? {
        return animation
    }

    fun drawOrAnimate(g: AGraphics) {
	    animation?.takeIf { !it.isDone }?.also {
		    if (!it.hidesActor())
		    	draw(g)
		    if (!it.isStarted)
		    	it.start<AAnimation<AGraphics>>()
		    it.update(g)
	    }?:run {
		    animation?.rect?.let {
			    rect = it
		    }
		    animation = null
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

    override fun X(): Float {
        return rect.x
    }

    override fun Y(): Float {
        return rect.y
    }

    override fun getWidth(): Float {
        return rect.w
    }

    override fun getHeight(): Float {
        return rect.h
    }

    override fun getX(): Float {
        return getRect().center.X()
    }

    override fun getY(): Float {
        return getRect().center.Y()
    }

    override fun getAtPosition(position: Float): Vector2D {
        return rect.center
    }

	val position: ZActorPosition
		get() = ZActorPosition(occupiedCell, occupiedQuadrant, occupiedZone)
	open val isNoisy: Boolean
		get() = false

	open val isVisible: Boolean
		get() = isAlive || isAnimating

	fun setPosition(position: ZActorPosition) {
		occupiedQuadrant = position.quadrant
		occupiedCell = position.pos
		occupiedZone = position.zone
	}

	override fun toString(): String {
		return "$label zone:$occupiedZone"
	}

}