package cc.lib.zombicide;

import cc.lib.game.GColor;

public enum ZCellType {
    NONE,
    VAULT_DOOR,
    OBJECTIVE_RED,
    OBJECTIVE_BLUE,
    OBJECTIVE_GREEN,
    OBJECTIVE_BLACK,
    SPAWN,
    START,
    EXIT,
    WALKER,
    RUNNER,
    FATTY,
    NECRO,
    ABOMINATION;

    GColor getColor() {
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