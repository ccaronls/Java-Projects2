package cc.lib.zombicide;

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
