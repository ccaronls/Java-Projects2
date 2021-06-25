package cc.lib.zombicide;

import cc.lib.annotation.Keep;

@Keep
public enum ZWallFlag {
    NONE(true, true),
    WALL(false, false),
    CLOSED(false, false),
    OPEN(true, true),
    LOCKED(false, false),
    RAMPART(false, true);

    ZWallFlag(boolean open, boolean openForProjectile) {
        this.opened  = open;
        this.openForProjectile = openForProjectile;
    }

    public final boolean opened;
    public final boolean openForProjectile;

}
