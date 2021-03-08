package cc.lib.zombicide;

import cc.lib.annotation.Keep;

@Keep
public enum ZWallFlag {
    NONE,
    WALL,
    CLOSED,
    OPEN,
    LOCKED;

    boolean isOpen() {
        switch(this) {
        case OPEN:
        case NONE:
            return true;
        default:
            return false;
        }
    }
}
