package cc.lib.zombicide;

import cc.lib.annotation.Keep;

@Keep
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
}
