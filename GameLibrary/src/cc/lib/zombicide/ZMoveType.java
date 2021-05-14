package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;

@Keep
public enum ZMoveType {
    DO_NOTHING,
    INVENTORY, // equip things, drop things, etc.
    TRADE,
    WALK,
    WALK_DIR,
    SWITCH_ACTIVE_CHARACTER,
    USE_LEFT_HAND,
    USE_RIGHT_HAND,
    MELEE_ATTACK,
    RANGED_ATTACK,
    MAGIC_ATTACK,
    THROW_ITEM, // torch / dragon bile
    RELOAD,
    OPERATE_DOOR,
    SEARCH,
    CONSUME,
    EQUIP,
    UNEQUIP,
    GIVE,
    TAKE,
    DISPOSE,
    TAKE_OBJECTIVE,
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
    BLOODLUST_MAGIC;

    public ZActionType getActionType(ZWeapon slot) {
        switch (this) {
            case MAGIC_ATTACK:
                return ZActionType.MAGIC;
            case RANGED_ATTACK:
                if (slot.type.usesArrows)
                    return ZActionType.RANGED_ARROWS;
                else if (slot.type.usesBolts)
                    return ZActionType.RANGED_BOLTS;
                break;
            case MELEE_ATTACK:
                return ZActionType.MELEE;
        }
        Utils.assertTrue(false);
        return null;
    }
}
