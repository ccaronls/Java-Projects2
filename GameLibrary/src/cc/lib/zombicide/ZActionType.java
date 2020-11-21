package cc.lib.zombicide;

public enum ZActionType {
    DO_NOTHING,
    MOVE,
    SEARCH,
    OPEN_DOOR,
    CLOSE_DOOR,
    MELEE,
    RANGED,
    MAGIC,
    THROW_ITEM,
    ENCHANTMENT,
    ACTIVATE,
    ORGANIZE,
    CONSUME,
    OBJECTIVE,
    RELOAD,
    DROP_ITEM,
    PICKUP_ITEM,
    MAKE_NOISE;

    boolean oncePerTurn() {
        switch (this) {
            case SEARCH:
                return true;
        }
        return false;
    }

    int costPerTurn() {
        return 1;
    }
}
