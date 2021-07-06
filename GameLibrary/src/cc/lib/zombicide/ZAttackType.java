package cc.lib.zombicide;

import cc.lib.game.Utils;

public enum ZAttackType {
    NORMAL,
    FIRE,
    ELECTROCUTION,
    DISINTEGRATION,
    BLADE,
    CRUSH,
    RANGED_ARROWS,
    RANGED_BOLTS,
    RANGED_THROW,
    EARTHQUAKE,
    MENTAL_STRIKE,
    DRAGON_FIRE;

    public boolean needsReload() {
        return this == RANGED_BOLTS;
    }

    public ZActionType getActionType() {
        switch (this) {
            case RANGED_ARROWS:
                return ZActionType.ARROWS;
            case RANGED_BOLTS:
                return ZActionType.BOLTS;
            case BLADE:
            case CRUSH:
                return ZActionType.MELEE;
            case EARTHQUAKE:
            case MENTAL_STRIKE:
                return ZActionType.MAGIC;
        }
        Utils.unhandledCase(this);
        return null;
    }
}
