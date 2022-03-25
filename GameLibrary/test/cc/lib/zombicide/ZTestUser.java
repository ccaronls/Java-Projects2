package cc.lib.zombicide;

import java.util.List;

import cc.lib.game.Utils;

public class ZTestUser extends ZUser {

    public ZTestUser(ZPlayerName...names) {
        for (ZPlayerName nm : names) {
            addCharacter(nm);
        }
    }

    @Override
    public ZPlayerName chooseCharacter(List<ZPlayerName> options) {
        return Utils.randItem(options);
    }

    @Override
    public Integer chooseMove(ZPlayerName cur, List<ZMove> options) {
        return Utils.rand() % options.size();
    }

    @Override
    public ZSkill chooseNewSkill(ZPlayerName character, List<ZSkill> skillOptions) {
        return Utils.randItem(skillOptions);
    }

    @Override
    public ZEquipSlot chooseSlotToOrganize(ZPlayerName cur, List<ZEquipSlot> slots) {
        return Utils.randItem(slots);
    }

    @Override
    public Integer chooseEquipment(ZPlayerName cur, List<ZEquipment> equipOptions) {
        return Utils.rand() % equipOptions.size();
    }

    @Override
    public ZEquipSlot chooseSlotForEquip(ZPlayerName cur, List<ZEquipSlot> equipableSlots) {
        return Utils.randItem(equipableSlots);
    }

    @Override
    public Integer chooseZoneToWalk(ZPlayerName cur, List<Integer> zones) {
        return Utils.randItem(zones);
    }

    @Override
    public Integer chooseDoorToToggle(ZPlayerName cur, List<ZDoor> doors) {
        return Utils.rand() % doors.size();
    }

    @Override
    public Integer chooseWeaponSlot(ZPlayerName c, List<ZWeapon> weapons) {
        return Utils.rand() % weapons.size();
    }

    @Override
    public ZPlayerName chooseTradeCharacter(ZPlayerName c, List<ZPlayerName> list) {
        return Utils.randItem(list);
    }

    @Override
    public Integer chooseZoneForAttack(ZPlayerName c, List<Integer> zones) {
        return Utils.randItem(zones);
    }

    @Override
    public Integer chooseItemToPickup(ZPlayerName cur, List<ZEquipment> list) {
        return Utils.rand() % list.size();
    }

    @Override
    public Integer chooseItemToDrop(ZPlayerName cur, List<ZEquipment> list) {
        return Utils.rand() % list.size();
    }

    @Override
    public Integer chooseZoneToShove(ZPlayerName cur, List<Integer> list) {
        return Utils.randItem(list);
    }

    @Override
    public ZSpell chooseSpell(ZPlayerName cur, List<ZSpell> spells) {
        return Utils.randItem(spells);
    }

    @Override
    public ZPlayerName chooseCharacterForSpell(ZPlayerName cur, ZSpell spell, List<ZPlayerName> targets) {
        return Utils.randItem(targets);
    }

    @Override
    public ZPlayerName chooseCharacterToBequeathMove(ZPlayerName cur, List<ZPlayerName> list) {
        return Utils.randItem(list);
    }

    @Override
    public Integer chooseZoneForBloodlust(ZPlayerName cur, List<Integer> list) {
        return Utils.randItem(list);
    }

    @Override
    public Integer chooseSpawnAreaToRemove(ZPlayerName cur, List<ZSpawnArea> list) {
        int idx = Utils.rand() % list.size();
        return idx;
    }

    @Override
    public Integer chooseEquipmentToThrow(ZPlayerName cur, List<ZEquipment> slots) {
        return Utils.rand() % slots.size();
    }

    @Override
    public Integer chooseZoneToThrowEquipment(ZPlayerName cur, ZEquipment toThrow, List<Integer> zones) {
        return Utils.randItem(zones);
    }

    @Override
    public Integer chooseZoneToIgnite(ZPlayerName playerName, List<Integer> ignitableZones) {
        return Utils.randItem(ignitableZones);
    }

    @Override
    public ZEquipmentClass chooseEquipmentClass(ZPlayerName playerName, List<ZEquipmentClass> classes) {
        return Utils.randItem(classes);
    }

    @Override
    public ZEquipmentType chooseStartingEquipment(ZPlayerName playerName, List<ZEquipmentType> list) {
        return list.get(0);
    }
}
