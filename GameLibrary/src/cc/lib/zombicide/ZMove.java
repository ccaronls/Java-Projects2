package cc.lib.zombicide;

public class ZMove {

    public final ZMoveType type;
    public final int targetIndex;
    public final ZWeapon attackWeapon;
    public final ZCharacter tradeWith;

    private ZMove(ZMoveType type, int targetIndex) {
        this(type, targetIndex, null, null);
    }
    private ZMove(ZMoveType type, int targetIndex, ZWeapon attackWeapon, ZCharacter tradeWith) {
        this.type = type;
        this.targetIndex = targetIndex;
        this.attackWeapon = attackWeapon;
        this.tradeWith = tradeWith;
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
        return new ZMove(ZMoveType.MELEE_ATTACK, zombieIndex, attackWeapon, null);
    }

    public static ZMove newRangedAttackMove(int zombieIndex, ZWeapon attackWeapon) {
        return new ZMove(ZMoveType.RANGED_ATTACK, zombieIndex, attackWeapon, null);
    }

    public static ZMove newGoBack(ZGame.State prevState) {
        return new ZMove(ZMoveType.GO_BACK, prevState.ordinal());
    }

    public static ZMove newOrganizeMove() {
        return new ZMove(ZMoveType.ORGANNIZE, 0);
    }

    public static ZMove newTradeMove(ZCharacter c) {
        return new ZMove(ZMoveType.TRADE, 0, null, c);
    }
}
