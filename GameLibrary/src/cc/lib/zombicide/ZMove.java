package cc.lib.zombicide;

import java.util.Arrays;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.ui.IButton;
import cc.lib.utils.Reflector;

public class ZMove extends Reflector<ZMove> implements IButton {

    static {
        addAllFields(ZMove.class);
    }

    public final ZMoveType type;
    public final Integer integer;
    public final ZPlayerName character;
    public final ZEquipment equipment;
    public final ZEquipSlot fromSlot;
    public final ZEquipSlot toSlot;
    public final List list;
    public final ZDir dir;
    public final ZSkill skill;

    public ZMove() {
        this(null);
    }

    public ZMove(ZMove copy, Object singleListElement) {
        this(copy.type, copy.integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, Arrays.asList(singleListElement), copy.dir, copy.skill);
    }

    public ZMove(ZMove copy, Object singleListElement, int integer) {
        this(copy.type, integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, Arrays.asList(singleListElement), copy.dir, copy.skill);
    }

    public ZMove(ZMove copy, Object singleListElement, ZPlayerName character) {
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

    private ZMove(ZMoveType type, int targetIndex, ZPlayerName character, ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot, List list) {
        this(type, targetIndex, character, equip, fromSlot, toSlot, list, null, null);
    }

    private ZMove(ZMoveType type, Integer integer, ZPlayerName character, ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot, List list, ZDir dir, ZSkill skill) {
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
        if (!(o instanceof ZMove))
            return false;
        ZMove zMove = (ZMove) o;
        return type == zMove.type
                && dir == zMove.dir
                && Utils.isEquals(equipment, zMove.equipment)
                && Utils.isEquals(integer, zMove.integer)
                && character == zMove.character
                && fromSlot == zMove.fromSlot
                && toSlot == zMove.toSlot
                && skill == zMove.skill;
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
        if (toSlot == null && fromSlot != null) {
            label += " from " + fromSlot.getLabel();
        }
        if (toSlot != null)
            label += " to " + toSlot.getLabel();

        return label;
    }

    static ZMove newEndTurn() {
        return new ZMove(ZMoveType.END_TURN);
    }

    static ZMove newWalkMove(List<Integer> zones) {
        return new ZMove(ZMoveType.WALK, zones);
    }

    static ZMove newJumpMove(List<Integer> zones) {
        return new ZMove(ZMoveType.JUMP, zones);
    }

    static ZMove newChargeMove(List<Integer> zones) {
        return new ZMove(ZMoveType.CHARGE, zones);
    }

    static ZMove newUseLeftHand() {
        return new ZMove(ZMoveType.USE_LEFT_HAND);
    }

    static ZMove newUseRightHand() {
        return new ZMove(ZMoveType.USE_RIGHT_HAND);
    }

    static ZMove newToggleDoor(List<ZDoor> doors) {
        return new ZMove(ZMoveType.OPERATE_DOOR, doors);
    }

    static ZMove newSearchMove(int zoneIndex) {
        return new ZMove(ZMoveType.SEARCH, zoneIndex);
    }

    static ZMove newMeleeAttackMove(List<ZWeapon> weapons) {
        return new ZMove(ZMoveType.MELEE_ATTACK, weapons);
    }

    static ZMove newRangedAttackMove(List<ZWeapon> weapons) {
        return new ZMove(ZMoveType.RANGED_ATTACK, weapons);
    }

    static ZMove newMagicAttackMove(List<ZWeapon> weapons) {
        return new ZMove(ZMoveType.MAGIC_ATTACK, weapons);
    }

    static ZMove newThrowEquipmentMove(List<ZEquipment> slots) {
        return new ZMove(ZMoveType.THROW_ITEM, slots);
    }

    static ZMove newInventoryMove() {
        return new ZMove(ZMoveType.INVENTORY);
    }

    static ZMove newTradeMove(List<ZPlayerName> tradeOptions) {
        return new ZMove(ZMoveType.TRADE, tradeOptions);
    }

    static ZMove newConsumeMove(ZEquipment equip, ZEquipSlot slot) {
        return new ZMove(ZMoveType.CONSUME, equip, slot);
    }

    static ZMove newEquipMove(ZEquipment equip, ZEquipSlot fromSlot, ZEquipSlot toSlot) {
        return new ZMove(ZMoveType.EQUIP, 0, null, equip, fromSlot, toSlot, null);
    }

    static ZMove newKeepMove(ZEquipment equip) {
        return new ZMove(ZMoveType.KEEP, equip, null);
    }

    static ZMove newUnequipMove(ZEquipment equip, ZEquipSlot slot) {
        return new ZMove(ZMoveType.UNEQUIP, equip, slot);
    }

    static ZMove newDisposeMove(ZEquipment equip, ZEquipSlot slot) {
        return new ZMove(ZMoveType.DISPOSE, equip, slot);
    }

    static ZMove newGiveMove(ZPlayerName taker, ZEquipment toGive) {
        return new ZMove(ZMoveType.GIVE, 0, taker, toGive, null, null, null);
    }

    static ZMove newTakeMove(ZPlayerName giver, ZEquipment toTake) {
        return new ZMove(ZMoveType.TAKE, 0, giver, toTake, null, null, null);
    }

    public static ZMove newObjectiveMove(int zone) {
        return new ZMove(ZMoveType.TAKE_OBJECTIVE, zone);
    }

    static ZMove newReloadMove(ZWeapon slot) {
        return new ZMove(ZMoveType.RELOAD, slot, null);
    }

    static ZMove newPickupItemMove(List<ZEquipment> takables) {
        return new ZMove(ZMoveType.PICKUP_ITEM, takables);
    }

    static ZMove newDropItemMove(List<ZEquipment> items) {
        return new ZMove(ZMoveType.DROP_ITEM, items);
    }

    static ZMove newWalkDirMove(ZDir dir) {
        return new ZMove(ZMoveType.WALK_DIR, dir);
    }

    static ZMove newSwitchActiveCharacter() {
        return new ZMove(ZMoveType.SWITCH_ACTIVE_CHARACTER);
    }

    static ZMove newMakeNoiseMove(int occupiedZone) {
        return new ZMove(ZMoveType.MAKE_NOISE, occupiedZone);
    }

    static ZMove newShoveMove(List<Integer> toZones) {
        return new ZMove(ZMoveType.SHOVE, toZones);
    }

    static ZMove newReRollMove() {
        return new ZMove(ZMoveType.REROLL);
    }

    static ZMove newKeepRollMove() {
        return new ZMove(ZMoveType.KEEP_ROLL);
    }

    static ZMove newEnchantMove(List<ZSpell> spells) {
        return new ZMove(ZMoveType.ENCHANT, spells);
    }

    static ZMove newBornLeaderMove(List<ZPlayerName> options) {
        return new ZMove(ZMoveType.BORN_LEADER, options);
    }

    static ZMove newBloodlustMeleeMove(List<Integer> zones, ZSkill skill) {
        return new ZMove(ZMoveType.BLOODLUST_MELEE, zones, skill);
    }

    static ZMove newBloodlustRangedMove(List<Integer> zones, ZSkill skill) {
        return new ZMove(ZMoveType.BLOODLUST_RANGED, zones, skill);
    }

    static ZMove newBloodlustMagicMove(List<Integer> zones, ZSkill skill) {
        return new ZMove(ZMoveType.BLOODLUST_MAGIC, zones, skill);
    }

    static ZMove newDisposeEquipmentMove(ZEquipment e) {
        return new ZMove(ZMoveType.DISPOSE, e, null);
    }

    static ZMove newIgniteMove(List<Integer> ignitableZones) {
        return new ZMove(ZMoveType.IGNITE, ignitableZones);
    }
}
