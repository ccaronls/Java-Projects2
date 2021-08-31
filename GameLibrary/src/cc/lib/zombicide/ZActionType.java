package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;

@Keep
public enum ZActionType {
    NOTHING,
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

    public boolean oncePerTurn() {
        switch (this) {
            case SEARCH:
            case SHOVE:
            case ENCHANTMENT:
                return true;
        }
        return false;
    }

    public int costPerTurn() {
        return 1;
    }

    public boolean breaksInvisibility() {
        switch (this) {
            case MELEE:
            case RANGED:
            case MAGIC:
            case SHOVE:
            case OPEN_DOOR:
            case CLOSE_DOOR:
            case MAKE_NOISE:
                return true;
        }
        return false;
    }

    public boolean isRanged() {
        switch (this) {
            case RANGED:
                return true;
        }
        return false;
    }

    public boolean isMagic() {
        switch (this) {
            case MAGIC:
                return true;
        }
        return false;
    }

    public boolean isProjectile() {
        switch (this) {
            case RANGED:
            case MAGIC:
            case THROW_ITEM:
                return true;
        }
        return false;
    }

    public boolean isMelee() {
        return this == MELEE;
    }

    public boolean isMovement() {
        return this == MOVE;
    }

    public String getLabel() {
        return Utils.toPrettyString(name());
    }
}
