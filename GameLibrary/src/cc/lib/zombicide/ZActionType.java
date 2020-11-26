package cc.lib.zombicide;

public enum ZActionType {
    DO_NOTHING,
    MOVE,
    SEARCH,
    OPEN_DOOR,
    CLOSE_DOOR,
    MELEE,
    RANGED_BOLTS,
    RANGED_ARROWS,
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
    MAKE_NOISE,
    SHOVE,
    DEFEND;

    boolean oncePerTurn() {
        switch (this) {
            case SEARCH:
            case SHOVE:
                return true;
        }
        return false;
    }

    int costPerTurn() {
        return 1;
    }
}
