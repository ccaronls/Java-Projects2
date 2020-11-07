package cc.lib.zombicide;

public enum ZMoveType {
    DO_NOTHING,
    GO_BACK, // goto previous state
    ORGANNIZE, // equip things, drop things, etc.
    TRADE,
    WALK,
    MELEE_ATTACK,
    RANGED_ATTACK,
    OPEN_DOOR,
    SEARCH,
    DROP_DRAGON_BILE,
    IGNITE;
}
