package cc.lib.zombicide;

public enum ZMoveType {
    DO_NOTHING,
    ORGANNIZE, // equip things, drop things, etc.
    TRADE,
    WALK,
    WALK_DIR,
    MELEE_ATTACK,
    RANGED_ATTACK,
    MAGIC_ATTACK,
    THROW_ITEM, // torch / dragon bile
    RELOAD,
    TOGGLE_DOOR,
    SEARCH,
    CONSUME,
    EQUIP,
    UNEQUIP,
    GIVE,
    TAKE,
    DISPOSE,
    OBJECTIVE,
    // drop or collect items from vault
    DROP_ITEM,
    PICKUP_ITEM,
    MAKE_NOISE,
    SHOVE,
    REROLL,
    KEEP_ROLL,
    ENCHANT,
    BORN_LEADER,
    BLOODLUST_MELEE,
    BLOODLUST_RANGED,
    BLOODLUST_MAGIC
}
