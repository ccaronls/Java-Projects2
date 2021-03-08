package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.GColor;

@Keep
public enum ZCellType {
    NONE,
    VAULT_DOOR_VIOLET,
    VAULT_DOOR_GOLD,
    OBJECTIVE_RED,
    OBJECTIVE_BLUE,
    OBJECTIVE_GREEN,
    OBJECTIVE_BLACK,
    SPAWN_NORTH,
    SPAWN_SOUTH,
    SPAWN_EAST,
    SPAWN_WEST,
    START,
    EXIT,
    WALKER,
    RUNNER,
    FATTY,
    NECRO,
    ABOMINATION;

    public GColor getColor() {
        switch (this) {
            case OBJECTIVE_BLACK:
                return GColor.BLACK;
            case OBJECTIVE_BLUE:
                return GColor.BLUE;
            case OBJECTIVE_GREEN:
                return GColor.GREEN;
            case OBJECTIVE_RED:
                return GColor.RED;
        }
        return GColor.YELLOW;
    }
}