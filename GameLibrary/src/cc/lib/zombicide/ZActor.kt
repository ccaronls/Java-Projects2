package cc.lib.zombicide

import cc.lib.game.*
import cc.lib.math.Vector2D
import cc.lib.utils.Grid
import cc.lib.utils.Reflector
import cc.lib.zombicide.ui.UIZButton

abstract class ZActor<E : Enum<E>> internal constructor(var occupiedZone: Int=-1) : Reflector<ZActor<E>>(), UIZButton, IRectangle, IVector2D, IInterpolator<Vector2D> {
    companion object {
        init {
            addAllFields(ZActor::class.java)
        }
    }

    var priorZone:Int = -1
        get() = if (field < 0) occupiedZone else priorZone

    lateinit var occupiedCell: Grid.Pos
    lateinit var occupiedQuadrant: ZCellQuadrant
    @JvmField
    var actionsLeftThisTurn = 0
    private var rect = GRectangle()

    @JvmField
    @Omit
    var animation: ZActorAnimation? = null
    fun getRect(b: ZBoard): GRectangle {
        return b.getCell(occupiedCell)
                .getQuadrant(occupiedQuadrant)
                .fit(dimension)
                .scaledBy(scale * b.getCell(occupiedCell).scale, Justify.CENTER, Justify.BOTTOM).also { rect = it }
    }

    override fun getRect(): GRectangle {
        return animation?.rect?:rect
    }

    open fun onBeginRound() {
        actionsLeftThisTurn = actionsPerTurn
    }

    open fun getSpawnQuadrant(): ZCellQuadrant? = null

    protected abstract val actionsPerTurn: Int
    abstract fun name(): String
    open fun performAction(action: ZActionType, game: ZGame): Boolean {
        if (isAlive) {
            actionsLeftThisTurn -= action.costPerTurn()
            Utils.assertTrue(actionsLeftThisTurn >= 0)
        }
        return false
    }

    fun addExtraAction() {
        actionsLeftThisTurn++
    }

    abstract fun drawInfo(g: AGraphics, game: ZGame, width: Float, height: Float): GDimension?

    open val noise: Int
        get() = 0
    abstract val type: E
    open val scale: Float
        get() = 1f
    abstract val imageId: Int
    abstract val outlineImageId: Int
    abstract override fun getDimension(): GDimension
    open val isInvisible: Boolean
        get() = false

    fun addAnimation(anim: ZActorAnimation) {
        if (animation == null || animation!!.isDone) {
            animation = anim
        } else {
            animation!!.add(anim)
        }
    }

    open val moveSpeed: Long
        get() = 1000
    val isAnimating: Boolean
        get() = animation?.isDone?:false

    fun getAnimation(): ZAnimation? {
        return animation
    }

    fun drawOrAnimate(g: AGraphics) {
        if (animation != null && !animation!!.isDone) {
            if (!animation!!.hidesActor()) draw(g)
            if (!animation!!.isStarted) animation!!.start<AAnimation<AGraphics>>()
            animation!!.update(g)
        } else {
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
        return rect.center.X()
    }

    override fun getY(): Float {
        return rect.center.Y()
    }

    override fun getAtPosition(position: Float): Vector2D {
        return rect.center
    }

    val position: ZActorPosition
        get() = ZActorPosition(occupiedCell, occupiedQuadrant)
    open val isNoisy: Boolean
        get() = false
}