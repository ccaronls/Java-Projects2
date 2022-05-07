package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.utils.prettify

@Keep
enum class ZActionType {
    NOTHING,
    MOVE,
    SEARCH,
    OPEN_DOOR,
    CLOSE_DOOR,
    BARRICADE_DOOR {
        override fun costPerTurn(): Int {
            return 3
        }
    },
    MELEE,
    RANGED,
    MAGIC,
    THROW_ITEM,
    ENCHANTMENT,
    ACTIVATE,
    INVENTORY,
    CONSUME,
    OBJECTIVE,
    RELOAD,
    DROP_ITEM,
    PICKUP_ITEM,
    MAKE_NOISE,
    SHOVE,
    DEFEND,
    BEQUEATH_MOVE;

    fun oncePerTurn(): Boolean {
        when (this) {
            SEARCH, SHOVE, ENCHANTMENT -> return true
        }
        return false
    }

    open fun costPerTurn(): Int {
        return 1
    }

    fun breaksInvisibility(): Boolean {
        when (this) {
            MELEE, RANGED, MAGIC, SHOVE, OPEN_DOOR, CLOSE_DOOR, BARRICADE_DOOR, MAKE_NOISE -> return true
        }
        return false
    }

    val isRanged: Boolean
        get() = this === RANGED
    val isMagic: Boolean
        get() = this === MAGIC
    val isProjectile: Boolean
        get() {
            when (this) {
                RANGED, MAGIC, THROW_ITEM -> return true
            }
            return false
        }
    val isMelee: Boolean
        get() = this === MELEE
    val isMovement: Boolean
        get() = this === MOVE
    val label: String
        get() = prettify(name)
}