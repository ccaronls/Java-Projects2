package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.GColor;

@Keep
public enum ZCellType {
    NONE,
    VAULT_DOOR_VIOLET,
    VAULT_DOOR_GOLD,
    OBJECTIVE_RED(GColor.RED, true, false),
    OBJECTIVE_BLUE(GColor.BLUE, true, false),
    OBJECTIVE_GREEN(GColor.GREEN, true, false),
    OBJECTIVE_BLACK(GColor.BLACK, true, false),
    START,
    EXIT,
    WALKER,
    RUNNER,
    FATTY,
    NECROMANCER,
    ABOMINATION;

    ZCellType() {
        this(GColor.YELLOW, false, false);
    }

    ZCellType(GColor color, boolean isObjective, boolean isZombie) {
        this.color = color;
        this.isObjective = isObjective;
        this.isZombie = isZombie;
    }

    public final GColor color;
    public final boolean isObjective;
    public final boolean isZombie;

}