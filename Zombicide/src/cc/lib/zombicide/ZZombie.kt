package cc.lib.zombicide

import cc.lib.game.*
import cc.lib.utils.Table

class ZZombie(override val type: ZZombieType = ZZombieType.Walker, val startZone: Int = -1) : ZActor<ZZombieType>(startZone) {
    companion object {
        init {
            addAllFields(ZZombie::class.java)
        }
    }

    private var imageIdx = -1
    @JvmField
    var destroyed = false
    public override val actionsPerTurn: Int
        get() = type.actionsPerTurn
    private val idx: Int
        private get() {
            if (imageIdx < 0 || imageIdx >= type.imageOptions.size) imageIdx = Utils.rand() % type.imageOptions.size
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

    override fun drawInfo(g: AGraphics, game: ZGame, width: Float, height: Float): GDimension {
        val info = Table(label).setNoBorder()
        info.addRow("Min Hits", type.minDamageToDestroy)
        info.addRow("Actions", type.actionsPerTurn)
        info.addRow("Experience", type.expProvided)
        info.addRow("Ignores Armor", type.ignoresArmor)
        info.addRow("Ranged Priority", type.rangedPriority)
        val outer = Table().setNoBorder()
        outer.addRow(info, type.description)
        return outer.draw(g) //g.drawString(outer.toString(), 0, 0);
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

    override fun performAction(action: ZActionType, game: ZGame) {
        if (action === ZActionType.MELEE) {
            actionsLeftThisTurn = 0
        } else {
	        super.performAction(action, game)
        }
    }

    override fun draw(g: AGraphics) {
        if (isAlive || isAnimating) {
            super.draw(g)
            if (actionsLeftThisTurn > 1) {
                g.color = GColor.WHITE
                val oldHgt = g.setTextHeight(10f)
                g.drawJustifiedString(getRect().centerBottom, Justify.CENTER, Justify.BOTTOM, java.lang.String.valueOf(actionsLeftThisTurn))
                g.textHeight = oldHgt
            }
        }
    }

    override val isAlive: Boolean
        get() = !destroyed

    init {
        onBeginRound()
    }
}