package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GColor

@Keep
enum class ZCellType(@JvmField val color: GColor = GColor.YELLOW, @JvmField val isObjective: Boolean = false, @JvmField val isZombie: Boolean = false) {
    NONE,
    VAULT_DOOR_VIOLET,
    VAULT_DOOR_GOLD,
    OBJECTIVE_RED(GColor.RED, true, false),
    OBJECTIVE_BLUE(GColor.BLUE, true, false),
    OBJECTIVE_GREEN(GColor.GREEN, true, false),
    OBJECTIVE_BLACK(GColor.BLACK, true, false),
    START,
    EXIT,
    WALKER,
    RUNNER,
    FATTY,
    NECROMANCER,
    ABOMINATION;
}