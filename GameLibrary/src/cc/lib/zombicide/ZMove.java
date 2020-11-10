package cc.lib.zombicide;

public class ZMove {

    public final ZMoveType type;
    public final int integer;
    public final ZWeapon weapon;
    public final ZCharacter tradeWith;
    public final ZEquipment equip;

    private ZMove(ZMoveType type, int targetIndex) {
        this(type, targetIndex, null, null, null);
    }
    private ZMove(ZMoveType type, int targetIndex, ZWeapon weapon, ZCharacter tradeWith, ZEquipment equip) {
        this.type = type;
        this.integer = targetIndex;
        this.weapon = weapon;
        this.tradeWith = tradeWith;
        this.equip = equip;
    }

    static ZMove newDoNothing() {
        return new ZMove(ZMoveType.DO_NOTHING, 0);
    }

    static ZMove newWalkMove(int targetIndex) {
        return new ZMove(ZMoveType.WALK, targetIndex);
    }

    static ZMove newOpenDoor(int targetIndex) {
        return new ZMove(ZMoveType.OPEN_DOOR, targetIndex);
    }

    public static ZMove newSearchMove(int zoneIndex) {
        return new ZMove(ZMoveType.SEARCH, zoneIndex);
    }

    public static ZMove newMeleeAttackMove(int zombieIndex, ZWeapon attackWeapon) {
        return new ZMove(ZMoveType.MELEE_ATTACK, zombieIndex, attackWeapon, null, null);
    }

    public static ZMove newRangedAttackMove(int zombieIndex, ZWeapon attackWeapon) {
        return new ZMove(ZMoveType.RANGED_ATTACK, zombieIndex, attackWeapon, null, null);
    }

    public static ZMove newGoBack(ZState prevState) {
        return new ZMove(ZMoveType.GO_BACK, prevState.ordinal());
    }

    public static ZMove newOrganizeMove() {
        return new ZMove(ZMoveType.ORGANNIZE, 0);
    }

    public static ZMove newTradeMove(ZCharacter c) {
        return new ZMove(ZMoveType.TRADE, 0, null, c,null);
    }

    public static ZMove newReloadMove(ZWeapon w) {
        return new ZMove(ZMoveType.RELOAD, 0, w, null, null);
    }

    public static ZMove newEquipMove(ZEquipment equip) {
        return new ZMove(ZMoveType.EQUIP, 0, null, null, equip);
    }

    public static ZMove newUnequipMove(ZEquipment equip) {
        return new ZMove(ZMoveType.UNEQUIP, 0, null, null, equip);
    }

    public static ZMove newDisposeMove(ZEquipment equip) {
        return new ZMove(ZMoveType.DISPOSE, 0, null, null, equip);
    }

    public static ZMove newConsumeMove(ZEquipment equip) {
        return new ZMove(ZMoveType.CONSUME, 0, null, null, equip);
    }
}
