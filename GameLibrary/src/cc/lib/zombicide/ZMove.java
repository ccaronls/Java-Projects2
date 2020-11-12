package cc.lib.zombicide;

import java.util.List;

public class ZMove {

    public final ZMoveType type;
    public final int integer;
    public final ZEquipment equipment;
    public final ZEquipSlot fromSlot;
    public final ZEquipSlot toSlot;
    public final List list;

    private ZMove(ZMoveType type) {
        this(type, 0);
    }

    private ZMove(ZMoveType type, int num) {
        this(type, num, null, null, null, null);
    }

    private ZMove(ZMoveType type, List list) {
        this(type, 0, null, null, null, list);
    }

    private ZMove(ZMoveType type, ZEquipment equip, ZEquipSlot fromSlot) {
        this(type, 0, equip, fromSlot, null, null);
    }

    private ZMove(ZMoveType type, int targetIndex, ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot, List list) {
        this.type = type;
        this.integer = targetIndex;
        this.equipment = equip;
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
        this.list  = list;
    }

    static ZMove newDoNothing() {
        return new ZMove(ZMoveType.DO_NOTHING);
    }

    static ZMove newWalkMove(List<Integer> zones) {
        return new ZMove(ZMoveType.WALK, zones);
    }

    static ZMove newToggleDoor(List<ZDoor> doors) {
        return new ZMove(ZMoveType.TOGGLE_DOOR, doors);
    }

    public static ZMove newSearchMove(int zoneIndex) {
        return new ZMove(ZMoveType.SEARCH, zoneIndex);
    }

    public static ZMove newMeleeAttackMove(List<ZEquipSlot> weapons) {
        return new ZMove(ZMoveType.MELEE_ATTACK, weapons);
    }

    public static ZMove newRangedAttackMove(List<ZEquipSlot> weapons) {
        return new ZMove(ZMoveType.RANGED_ATTACK, weapons);
    }

    public static ZMove newMagicAttackMove(List<ZEquipSlot> weapons) {
        return new ZMove(ZMoveType.RANGED_ATTACK, weapons);
    }

    public static ZMove newOrganizeMove() {
        return new ZMove(ZMoveType.ORGANNIZE);
    }

    public static ZMove newTradeMove(List<ZCharacter> tradeOptions) {
        return new ZMove(ZMoveType.TRADE);
    }

    public static ZMove newConsumeMove(ZEquipment equip, ZEquipSlot slot) {
        return new ZMove(ZMoveType.CONSUME, equip, slot);
    }

    public static ZMove newEquipMove(ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot) {
        return new ZMove(ZMoveType.EQUIP, 0, equip, fromSlot, toSlot, null);
    }

    public static ZMove newUnequipMove(ZEquipment equip, ZEquipSlot slot) {
        return new ZMove(ZMoveType.UNEQUIP, equip, slot);
    }

    public static ZMove newDisposeMove(ZEquipment equip, ZEquipSlot slot) {
        return new ZMove(ZMoveType.DISPOSE, equip, slot);
    }

}
