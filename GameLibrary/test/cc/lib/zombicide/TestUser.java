package cc.lib.zombicide;

import java.util.List;

import cc.lib.game.Utils;

class TestUser extends ZUser {

    TestUser(ZPlayerName...names) {
        for (ZPlayerName nm : names) {
            addCharacter(nm);
        }
    }

    @Override
    public ZPlayerName chooseCharacter(List<ZPlayerName> options) {
        return Utils.randItem(options);
    }

    @Override
    public ZMove chooseMove(ZPlayerName cur, List<ZMove> options) {
        return Utils.randItem(options);
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
    public ZEquipment chooseEquipment(ZPlayerName cur, List<ZEquipment> equipOptions) {
        return Utils.randItem(equipOptions);
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
    public ZDoor chooseDoorToToggle(ZPlayerName cur, List<ZDoor> doors) {
        return Utils.randItem(doors);
    }

    @Override
    public ZWeapon chooseWeaponSlot(ZPlayerName c, List<ZWeapon> weapons) {
        return Utils.randItem(weapons);
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
    public ZEquipment chooseItemToPickup(ZPlayerName cur, List<ZEquipment> list) {
        return Utils.randItem(list);
    }

    @Override
    public ZEquipment chooseItemToDrop(ZPlayerName cur, List<ZEquipment> list) {
        return Utils.randItem(list);
    }

    @Override
    public ZItem chooseItemToThrow(ZPlayerName cur, List<ZItem> slots) {
        return Utils.randItem(slots);
    }

    @Override
    public Integer chooseZoneToThrowItem(ZPlayerName cur, ZItem toThrow, List<Integer> zones) {
        return Utils.randItem(zones);
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
}
