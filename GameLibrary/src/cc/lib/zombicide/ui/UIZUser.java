package cc.lib.zombicide.ui;

import java.util.List;

import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipSlot;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItem;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZSpell;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZWeapon;

public class UIZUser extends ZUser {
    @Override
    public void showMessage(String s) {
        UIZombicide.getInstance().addPlayerComponentMessage(s);
    }

    @Override
    public ZCharacter chooseCharacter(List<ZCharacter> characters) {
        return UIZombicide.getInstance().pickCharacter("Pick character to play", characters);
    }

    @Override
    public ZMove chooseMove(ZGame game, ZCharacter cur, List<ZMove> moves) {
        return UIZombicide.getInstance().pickMenu(cur.name() + " Choose Move", ZMove.class, moves);
    }

    @Override
    public ZSkill chooseNewSkill(ZGame game, ZCharacter cur, List<ZSkill> skillOptions) {
        return UIZombicide.getInstance().pickMenu(cur.name() + " Choose New Skill", ZSkill.class, skillOptions);
    }

    @Override
    public ZEquipSlot chooseSlotToOrganize(ZGame game, ZCharacter cur, List<ZEquipSlot> slots) {
        return UIZombicide.getInstance().pickMenu(cur.name() + " Choose Slot to Organize", ZEquipSlot.class, slots);
    }

    @Override
    public ZEquipment chooseEquipment(ZGame game, ZCharacter cur, List<ZEquipment> equipOptions) {
        return UIZombicide.getInstance().pickMenu(cur.name() + " Choose Equipment to Organize", ZEquipment.class, equipOptions);
    }

    @Override
    public ZEquipSlot chooseSlotForEquip(ZGame game, ZCharacter cur, List<ZEquipSlot> equipableSlots) {
        return UIZombicide.getInstance().pickMenu(cur.name() + " Choose Slot to Equip Item", ZEquipSlot.class, equipableSlots);
    }

    @Override
    public Integer chooseZoneToWalk(ZGame game, ZCharacter cur, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone(cur.name() + " Choose zone to Walk", zones);
    }

    @Override
    public ZDoor chooseDoorToToggle(ZGame game, ZCharacter cur, List<ZDoor> doors) {
        return UIZombicide.getInstance().pickDoor(cur.name() + " Choose door to open or close", doors);
    }

    @Override
    public ZWeapon chooseWeaponSlot(ZGame game, ZCharacter cur, List<ZWeapon> weapons) {
        return UIZombicide.getInstance().pickMenu(cur.name() + " Choose weapon from slot", ZWeapon.class, weapons);
    }

    @Override
    public ZCharacter chooseTradeCharacter(ZGame game, ZCharacter cur, List<ZCharacter> list) {
        return UIZombicide.getInstance().pickCharacter(cur.name() + " Choose Character for Trade", list);
    }

    @Override
    public Integer chooseZoneForAttack(ZGame game, ZCharacter c, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone("Choose Zone to Attack", zones);
    }

    @Override
    public ZEquipment chooseItemToPickup(ZGame game, ZCharacter cur, List<ZEquipment> list) {
        return UIZombicide.getInstance().pickMenu("Choose Menu to Pickup", ZEquipment.class, list);
    }

    @Override
    public ZEquipment chooseItemToDrop(ZGame game, ZCharacter cur, List<ZEquipment> list) {
        return UIZombicide.getInstance().pickMenu("Choose Menu to Drop", ZEquipment.class, list);
    }

    @Override
    public ZItem chooseItemToThrow(ZGame game, ZCharacter cur, List<ZItem> slots) {
        return UIZombicide.getInstance().pickMenu("Choose Item to Throw", ZItem.class, slots);
    }

    @Override
    public Integer chooseZonetoThrowItem(ZGame game, ZCharacter cur, ZItem toThrow, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone("Choose Zone to throw the " + toThrow, zones);
    }

    @Override
    public Integer chooseZoneToShove(ZGame game, ZCharacter cur, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone("Choose Zone to shove zombies into", zones);
    }

    @Override
    public ZSpell chooseSpell(ZGame game, ZCharacter cur, List<ZSpell> spells) {
        return UIZombicide.getInstance().pickMenu("Choose Spell", ZSpell.class, spells);
    }

    @Override
    public ZCharacter chooseCharacterForSpell(ZGame game, ZCharacter cur, ZSpell spell, List<ZCharacter> targets) {
        return UIZombicide.getInstance().pickCharacter("Choose character to enchant with " + spell.getType(), targets);
    }

    @Override
    public ZCharacter chooseCharacterToBequeathMove(ZGame game, ZCharacter cur, List<ZCharacter> targets) {
        return UIZombicide.getInstance().pickCharacter("Choose character to bequeath an extra action", targets);
    }

    @Override
    public Integer chooseZoneForBloodlust(ZGame zGame, ZCharacter cur, List<Integer> list) {
        return UIZombicide.getInstance().pickZone("Choose Zone for Bloodlust", list);
    }

    @Override
    public Integer chooseZoneToRemoveSpawn(ZGame game, ZCharacter cur, List<Integer> list) {
        return UIZombicide.getInstance().pickZone("Choose Zone to Remove SPAWN", list);
    }
}
