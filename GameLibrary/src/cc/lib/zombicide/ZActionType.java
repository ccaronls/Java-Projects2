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
    DEFEND,
    BEQUEATH_MOVE;

    boolean oncePerTurn() {
        switch (this) {
            case SEARCH:
            case SHOVE:
            case ENCHANTMENT:
                return true;
        }
        return false;
    }

    int costPerTurn() {
        return 1;
    }

    boolean breaksInvisibility() {
        switch (this) {
            case MELEE:
            case RANGED_BOLTS:
            case RANGED_ARROWS:
            case MAGIC:
            case SHOVE:
            case OPEN_DOOR:
            case CLOSE_DOOR:
            case MAKE_NOISE:
                return true;
        }
        return false;
    }
}
