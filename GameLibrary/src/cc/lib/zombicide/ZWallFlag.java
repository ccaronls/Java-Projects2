package cc.lib.zombicide;

public enum ZWallFlag {
    NONE, WALL, CLOSED, OPEN, LOCKED0, LOCKED1, LOCKED2;

    boolean isOpen() {
        switch (this) {
            case OPEN:
            case NONE:
                return true;
        }
        return false;
    }
}
