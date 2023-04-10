package cc.lib.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZWallFlag(open val opened: Boolean, open val openForProjectile: Boolean) {
    NONE(true, true),
    WALL(false, false),
    CLOSED(false, false),
    OPEN(true, true),
    LOCKED(false, false),
    RAMPART(false, true);
}