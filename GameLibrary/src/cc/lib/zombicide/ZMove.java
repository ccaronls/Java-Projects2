package cc.lib.zombicide;

import java.util.List;

import cc.lib.game.Utils;
import cc.lib.ui.IButton;

public class ZMove implements IButton {

    public final ZMoveType type;
    public final int integer;
    public final ZCharacter character;
    public final ZEquipment equipment;
    public final ZEquipSlot fromSlot;
    public final ZEquipSlot toSlot;
    public final List list;
    public final ZDir dir;

    private ZMove(ZMoveType type) {
        this(type, 0);
    }

    private ZMove(ZMoveType type, ZDir dir) {
        this(type, 0, null, null, null, null, null, dir);
    }

    private ZMove(ZMoveType type, int num) {
        this(type, num, null, null, null, null, null, null);
    }

    private ZMove(ZMoveType type, List list) {
        this(type, 0, null, null, null, null, list, null);
    }

    private ZMove(ZMoveType type, ZEquipment equip, ZEquipSlot fromSlot) {
        this(type, 0, null, equip, fromSlot, null, null, null);
    }

    private ZMove(ZMoveType type, int targetIndex, ZCharacter character, ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot, List list) {
        this(type, targetIndex, character, equip, fromSlot, toSlot, list, null);
    }

    private ZMove(ZMoveType type, int targetIndex, ZCharacter character, ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot, List list, ZDir dir) {
        this.type = type;
        this.integer = targetIndex;
        this.character = character;
        this.equipment = equip;
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
        this.list  = list;
        this.dir = dir;
    }

    @Override
    public String toString() {
        return "ZMove{" +
                "type=" + type +
                ", integer=" + integer +
                ", equipment=" + equipment +
                ", fromSlot=" + fromSlot +
                ", toSlot=" + toSlot +
                ", list=" + list +
                '}';
    }

    @Override
    public String getTooltipText() {
        if (equipment != null) {
            if (toSlot != null) {
                return String.format("%s\nfrom: %s\nto: %s", equipment, fromSlot, toSlot);
            } else if (fromSlot != null) {
                return String.format("%s\nto: %s", equipment, toSlot);
            } else {
                return equipment.getTooltipText();
            }
        }
        return null;
    }

    @Override
    public String getLabel() {
        return Utils.toPrettyString(type.name());
    }

    public static ZMove newDoNothing() {
        return new ZMove(ZMoveType.DO_NOTHING);
    }

    public static ZMove newWalkMove(List<Integer> zones) {
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
        return new ZMove(ZMoveType.MAGIC_ATTACK, weapons);
    }

    public static ZMove newThrowItemMove(List<ZEquipSlot> slots) {
        return new ZMove(ZMoveType.THROW_ITEM, slots);
    }

    public static ZMove newOrganizeMove() {
        return new ZMove(ZMoveType.ORGANNIZE);
    }

    public static ZMove newTradeMove(List<ZCharacter> tradeOptions) {
        return new ZMove(ZMoveType.TRADE, tradeOptions);
    }

    public static ZMove newConsumeMove(ZEquipment equip, ZEquipSlot slot) {
        return new ZMove(ZMoveType.CONSUME, equip, slot);
    }

    public static ZMove newEquipMove(ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot) {
        return new ZMove(ZMoveType.EQUIP, 0, null, equip, fromSlot, toSlot, null);
    }

    public static ZMove newUnequipMove(ZEquipment equip, ZEquipSlot slot) {
        return new ZMove(ZMoveType.UNEQUIP, equip, slot);
    }

    public static ZMove newDisposeMove(ZEquipment equip, ZEquipSlot slot) {
        return new ZMove(ZMoveType.DISPOSE, equip, slot);
    }

    public static ZMove newGiveMove(ZCharacter taker, ZEquipment toGive) {
        return new ZMove(ZMoveType.GIVE, 0, taker, toGive, null, null, null);
    }

    public static ZMove newTakeMove(ZCharacter giver, ZEquipment toTake) {
        return new ZMove(ZMoveType.TAKE, 0, giver, toTake, null, null, null);
    }

    public static ZMove newObjectiveMove(int zone) {
        return new ZMove(ZMoveType.OBJECTIVE, zone);
    }

    public static ZMove newReloadMove(ZEquipSlot slot) {
        return new ZMove(ZMoveType.RELOAD, null, slot);
    }

    public static ZMove newPickupItemMove(List<ZEquipment> takables) {
        return new ZMove(ZMoveType.PICKUP_ITEM, takables);
    }

    public static ZMove newDropItemMove(List<ZEquipment> items) {
        return new ZMove(ZMoveType.DROP_ITEM, items);
    }

    public static Object newWalkDirMove(ZDir dir) {
        return new ZMove(ZMoveType.WALK_DIR, dir);
    }

}
