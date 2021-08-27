package cc.lib.zombicide.ui;

import java.util.List;

import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipSlot;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZItem;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZSpawnArea;
import cc.lib.zombicide.ZSpell;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZWeapon;

public class UIZUser extends ZUser {

    @Override
    public ZPlayerName chooseCharacter(List<ZPlayerName> characters) {
        return UIZombicide.getInstance().pickCharacter("Pick character to play", characters);
    }

    @Override
    public ZMove chooseMove(ZPlayerName cur, List<ZMove> moves) {
        return UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose Move", ZMove.class, moves);
    }

    @Override
    public ZSkill chooseNewSkill(ZPlayerName cur, List<ZSkill> skillOptions) {
        return UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose New Skill", ZSkill.class, skillOptions);
    }

    @Override
    public ZEquipSlot chooseSlotToOrganize(ZPlayerName cur, List<ZEquipSlot> slots) {
        return UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose Slot to Organize", ZEquipSlot.class, slots);
    }

    @Override
    public ZEquipment chooseEquipment(ZPlayerName cur, List<ZEquipment> equipOptions) {
        return UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose Equipment to Organize", ZEquipment.class, equipOptions);
    }

    @Override
    public ZEquipSlot chooseSlotForEquip(ZPlayerName cur, List<ZEquipSlot> equipableSlots) {
        return UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose Slot to Equip Item", ZEquipSlot.class, equipableSlots);
    }

    @Override
    public Integer chooseZoneToWalk(ZPlayerName cur, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone(cur.name() + " Choose zone to Walk", zones);
    }

    @Override
    public ZDoor chooseDoorToToggle(ZPlayerName cur, List<ZDoor> doors) {
        return UIZombicide.getInstance().pickDoor(cur.name() + " Choose door to open or close", doors);
    }

    @Override
    public ZWeapon chooseWeaponSlot(ZPlayerName cur, List<ZWeapon> weapons) {
        return UIZombicide.getInstance().pickMenu(cur, cur.name() + " Choose weapon from slot", ZWeapon.class, weapons);
    }

    @Override
    public ZPlayerName chooseTradeCharacter(ZPlayerName cur, List<ZPlayerName> list) {
        return UIZombicide.getInstance().pickCharacter(cur.name() + " Choose Character for Trade", list);
    }

    @Override
    public Integer chooseZoneForAttack(ZPlayerName c, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone("Choose Zone to Attack", zones);
    }

    @Override
    public ZEquipment chooseItemToPickup(ZPlayerName cur, List<ZEquipment> list) {
        return UIZombicide.getInstance().pickMenu(cur, "Choose Menu to Pickup", ZEquipment.class, list);
    }

    @Override
    public ZEquipment chooseItemToDrop(ZPlayerName cur, List<ZEquipment> list) {
        return UIZombicide.getInstance().pickMenu(cur, "Choose Menu to Drop", ZEquipment.class, list);
    }

    @Override
    public ZItem chooseItemToThrow(ZPlayerName cur, List<ZItem> slots) {
        return UIZombicide.getInstance().pickMenu(cur,  "Choose Item to Throw", ZItem.class, slots);
    }

    @Override
    public Integer chooseZoneToThrowItem(ZPlayerName cur, ZItem toThrow, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone("Choose Zone to throw the " + toThrow, zones);
    }

    @Override
    public Integer chooseZoneToShove(ZPlayerName cur, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone("Choose Zone to shove zombies into", zones);
    }

    @Override
    public ZSpell chooseSpell(ZPlayerName cur, List<ZSpell> spells) {
        return UIZombicide.getInstance().pickMenu(cur, "Choose Spell", ZSpell.class, spells);
    }

    @Override
    public ZPlayerName chooseCharacterForSpell(ZPlayerName cur, ZSpell spell, List<ZPlayerName> targets) {
        return UIZombicide.getInstance().pickCharacter("Choose character to enchant with " + spell.getType(), targets);
    }

    @Override
    public ZPlayerName chooseCharacterToBequeathMove(ZPlayerName cur, List<ZPlayerName> targets) {
        return UIZombicide.getInstance().pickCharacter("Choose character to bequeath an extra action", targets);
    }

    @Override
    public Integer chooseZoneForBloodlust(ZPlayerName cur, List<Integer> list) {
        return UIZombicide.getInstance().pickZone("Choose Zone for Bloodlust", list);
    }

    @Override
    public Integer chooseSpawnAreaToRemove(ZPlayerName cur, List<ZSpawnArea> list) {
        return UIZombicide.getInstance().pickSpawn("Choose SPAWN Area to Remove", list);
    }

}
