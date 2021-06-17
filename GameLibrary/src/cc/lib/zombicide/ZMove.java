package cc.lib.zombicide;

import java.util.Arrays;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.ui.IButton;

public class ZMove implements IButton {

    public final ZMoveType type;
    public final Integer integer;
    public final ZCharacter character;
    public final ZEquipment equipment;
    public final ZEquipSlot fromSlot;
    public final ZEquipSlot toSlot;
    public final List list;
    public final ZDir dir;
    public final ZSkill skill;

    public ZMove(ZMove copy, Object singleListElement) {
        this(copy.type, copy.integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, Arrays.asList(singleListElement), copy.dir, copy.skill);
    }

    public ZMove(ZMove copy, Object singleListElement, int integer) {
        this(copy.type, integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, Arrays.asList(singleListElement), copy.dir, copy.skill);
    }

    public ZMove(ZMove copy, Object singleListElement, ZCharacter character) {
        this(copy.type, copy.integer, character, copy.equipment, copy.fromSlot, copy.toSlot, Arrays.asList(singleListElement), copy.dir, copy.skill);
    }

    public ZMove(ZMove copy, Object singleListElement, ZEquipment equipment) {
        this(copy.type, copy.integer, copy.character, equipment, copy.fromSlot, copy.toSlot, Arrays.asList(singleListElement), copy.dir, copy.skill);
    }

    private ZMove(ZMoveType type) {
        this(type, (Integer)null);
    }

    private ZMove(ZMoveType type, ZDir dir) {
        this(type, null, null, null, null, null, null, dir, null);
    }

    private ZMove(ZMoveType type, Integer num) {
        this(type, num, null, null, null, null, null, null, null);
    }

    private ZMove(ZMoveType type, List list) {
        this(type, null, null, null, null, null, list, null, null);
    }

    private ZMove(ZMoveType type, List list, ZSkill skill) {
        this(type, null, null, null, null, null, list, null, skill);
    }

    private ZMove(ZMoveType type, ZEquipment equip, ZEquipSlot fromSlot) {
        this(type, null, null, equip, fromSlot, null, null, null, null);
    }

    private ZMove(ZMoveType type, int targetIndex, ZCharacter character, ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot, List list) {
        this(type, targetIndex, character, equip, fromSlot, toSlot, list, null, null);
    }

    private ZMove(ZMoveType type, Integer integer, ZCharacter character, ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot, List list, ZDir dir, ZSkill skill) {
        this.type = type;
        this.integer = integer;
        this.character = character;
        this.equipment = equip;
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
        this.list  = list;
        this.dir = dir;
        this.skill = skill;
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
                ", dir=" + dir +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null)
            return false;
        if (o instanceof ZMoveType) {
            if (type == (ZMoveType)o)
                return true;
        }
        if (!(o instanceof ZMove))
            return false;
        ZMove zMove = (ZMove) o;
        return type == zMove.type;
    }

    @Override
    public String getTooltipText() {
        if (equipment != null) {
            return equipment.getTooltipText();
        }
        if (skill != null) {
            return skill.getTooltipText();
        }
        if (this.character != null) {
            return character.getTooltipText();
        }
        return type.getTooltipText();
    }

    @Override
    public String getLabel() {
        String label = Utils.toPrettyString(type.name());
        if (equipment != null)
            label += " " + equipment.getLabel();
        if (fromSlot != null) {
            label += " from " + fromSlot.getLabel();
        }
        if (toSlot != null)
            label += " to " + toSlot.getLabel();

        return label;
    }

    public static ZMove newDoNothing() {
        return new ZMove(ZMoveType.DO_NOTHING);
    }

    public static ZMove newEndTurn() {
        return new ZMove(ZMoveType.END_TURN);
    }

    public static ZMove newWalkMove(List<Integer> zones) {
        return new ZMove(ZMoveType.WALK, zones);
    }

    public static ZMove newUseLeftHand() {
        return new ZMove(ZMoveType.USE_LEFT_HAND);
    }

    public static ZMove newUseRightHand() {
        return new ZMove(ZMoveType.USE_RIGHT_HAND);
    }

    static ZMove newToggleDoor(List<ZDoor> doors) {
        return new ZMove(ZMoveType.OPERATE_DOOR, doors);
    }

    public static ZMove newSearchMove(int zoneIndex) {
        return new ZMove(ZMoveType.SEARCH, zoneIndex);
    }

    public static ZMove newMeleeAttackMove(List<ZWeapon> weapons) {
        return new ZMove(ZMoveType.MELEE_ATTACK, weapons);
    }

    public static ZMove newRangedAttackMove(List<ZWeapon> weapons) {
        return new ZMove(ZMoveType.RANGED_ATTACK, weapons);
    }

    public static ZMove newMagicAttackMove(List<ZWeapon> weapons) {
        return new ZMove(ZMoveType.MAGIC_ATTACK, weapons);
    }

    public static ZMove newThrowItemMove(List<ZItem> slots) {
        return new ZMove(ZMoveType.THROW_ITEM, slots);
    }

    public static ZMove newInventoryMove() {
        return new ZMove(ZMoveType.INVENTORY);
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

    public static ZMove newKeepMove(ZEquipment equip) {
        return new ZMove(ZMoveType.KEEP, equip, null);
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
        return new ZMove(ZMoveType.TAKE_OBJECTIVE, zone);
    }

    public static ZMove newReloadMove(ZWeapon slot) {
        return new ZMove(ZMoveType.RELOAD, slot, null);
    }

    public static ZMove newPickupItemMove(List<ZEquipment> takables) {
        return new ZMove(ZMoveType.PICKUP_ITEM, takables);
    }

    public static ZMove newDropItemMove(List<ZEquipment> items) {
        return new ZMove(ZMoveType.DROP_ITEM, items);
    }

    public static ZMove newWalkDirMove(ZDir dir) {
        return new ZMove(ZMoveType.WALK_DIR, dir);
    }

    public static ZMove newSwitchActiveCharacter() {
        return new ZMove(ZMoveType.SWITCH_ACTIVE_CHARACTER);
    }

    public static ZMove newMakeNoiseMove(int occupiedZone) {
        return new ZMove(ZMoveType.MAKE_NOISE, occupiedZone);
    }

    public static ZMove newShoveMove(List<Integer> toZones) {
        return new ZMove(ZMoveType.SHOVE, toZones);
    }

    public static ZMove newReRollMove() {
        return new ZMove(ZMoveType.REROLL);
    }

    public static ZMove newKeepRollMove() {
        return new ZMove(ZMoveType.KEEP_ROLL);
    }

    public static ZMove newEnchantMove(List<ZSpell> spells) {
        return new ZMove(ZMoveType.ENCHANT, spells);
    }

    public static ZMove newBornLeaderMove(List<ZCharacter> options) {
        return new ZMove(ZMoveType.BORN_LEADER, options);
    }

    public static ZMove newBloodlustMeleeMove(List<Integer> zones, ZSkill skill) {
        return new ZMove(ZMoveType.BLOODLUST_MELEE, zones, skill);
    }

    public static ZMove newBloodlustRangedMove(List<Integer> zones, ZSkill skill) {
        return new ZMove(ZMoveType.BLOODLUST_RANGED, zones, skill);
    }

    public static ZMove newBloodlustMagicMove(List<Integer> zones, ZSkill skill) {
        return new ZMove(ZMoveType.BLOODLUST_MAGIC, zones, skill);
    }

    public static ZMove newDisposeEquipmentMove(ZEquipment e) {
        return new ZMove(ZMoveType.DISPOSE, e, null);
    }

}
