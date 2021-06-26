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
    public void showMessage(String s) {
        System.out.println(s);
    }

    @Override
    public ZCharacter chooseCharacter(List<ZCharacter> options) {
        return Utils.randItem(options);
    }

    @Override
    public ZMove chooseMove(ZGame game, ZCharacter cur, List<ZMove> options) {
        return Utils.randItem(options);
    }

    @Override
    public ZSkill chooseNewSkill(ZGame game, ZCharacter character, List<ZSkill> skillOptions) {
        return Utils.randItem(skillOptions);
    }

    @Override
    public ZEquipSlot chooseSlotToOrganize(ZGame game, ZCharacter cur, List<ZEquipSlot> slots) {
        return Utils.randItem(slots);
    }

    @Override
    public ZEquipment chooseEquipment(ZGame game, ZCharacter cur, List<ZEquipment> equipOptions) {
        return Utils.randItem(equipOptions);
    }

    @Override
    public ZEquipSlot chooseSlotForEquip(ZGame game, ZCharacter cur, List<ZEquipSlot> equipableSlots) {
        return Utils.randItem(equipableSlots);
    }

    @Override
    public Integer chooseZoneToWalk(ZGame game, ZCharacter cur, List<Integer> zones) {
        return Utils.randItem(zones);
    }

    @Override
    public ZDoor chooseDoorToToggle(ZGame game, ZCharacter cur, List<ZDoor> doors) {
        return Utils.randItem(doors);
    }

    @Override
    public ZWeapon chooseWeaponSlot(ZGame game, ZCharacter c, List<ZWeapon> weapons) {
        return Utils.randItem(weapons);
    }

    @Override
    public ZCharacter chooseTradeCharacter(ZGame game, ZCharacter c, List<ZCharacter> list) {
        return Utils.randItem(list);
    }

    @Override
    public Integer chooseZoneForAttack(ZGame game, ZCharacter c, List<Integer> zones) {
        return Utils.randItem(zones);
    }

    @Override
    public ZEquipment chooseItemToPickup(ZGame game, ZCharacter cur, List<ZEquipment> list) {
        return Utils.randItem(list);
    }

    @Override
    public ZEquipment chooseItemToDrop(ZGame game, ZCharacter cur, List<ZEquipment> list) {
        return Utils.randItem(list);
    }

    @Override
    public ZItem chooseItemToThrow(ZGame game, ZCharacter cur, List<ZItem> slots) {
        return Utils.randItem(slots);
    }

    @Override
    public Integer chooseZoneToThrowItem(ZGame game, ZCharacter cur, ZItem toThrow, List<Integer> zones) {
        return Utils.randItem(zones);
    }

    @Override
    public Integer chooseZoneToShove(ZGame game, ZCharacter cur, List<Integer> list) {
        return Utils.randItem(list);
    }

    @Override
    public ZSpell chooseSpell(ZGame game, ZCharacter cur, List<ZSpell> spells) {
        return Utils.randItem(spells);
    }

    @Override
    public ZCharacter chooseCharacterForSpell(ZGame game, ZCharacter cur, ZSpell spell, List<ZCharacter> targets) {
        return Utils.randItem(targets);
    }

    @Override
    public ZCharacter chooseCharacterToBequeathMove(ZGame game, ZCharacter cur, List<ZCharacter> list) {
        return Utils.randItem(list);
    }

    @Override
    public Integer chooseZoneForBloodlust(ZGame zGame, ZCharacter cur, List<Integer> list) {
        return Utils.randItem(list);
    }

    @Override
    public Integer chooseZoneToRemoveSpawn(ZGame game, ZCharacter cur, List<Integer> list) {
        return Utils.randItem(list);
    }
}
