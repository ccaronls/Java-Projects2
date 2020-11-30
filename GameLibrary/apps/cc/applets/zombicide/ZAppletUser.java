package cc.applets.zombicide;

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

public class ZAppletUser extends ZUser {
    @Override
    public void showMessage(String s) {
        ZombicideApplet.instance.charComp.addMessage(s);
    }

    @Override
    public ZCharacter chooseCharacter(List<ZCharacter> characters) {
        return ZombicideApplet.instance.pickCharacter("Pick character to play", characters);
    }

    @Override
    public ZMove chooseMove(ZGame game, ZCharacter cur, List<ZMove> moves) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose Move", ZMove.class, moves);
    }

    @Override
    public ZSkill chooseNewSkill(ZGame game, ZCharacter cur, List<ZSkill> skillOptions) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose New Skill", ZSkill.class, skillOptions);
    }

    @Override
    public ZEquipSlot chooseSlotToOrganize(ZGame game, ZCharacter cur, List<ZEquipSlot> slots) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose Slot to Organize", ZEquipSlot.class, slots);
    }

    @Override
    public ZEquipment chooseEquipment(ZGame game, ZCharacter cur, List<ZEquipment> equipOptions) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose Equipment to Organize", ZEquipment.class, equipOptions);
    }

    @Override
    public ZEquipSlot chooseSlotForEquip(ZGame game, ZCharacter cur, List<ZEquipSlot> equipableSlots) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose Slot to Equip Item", ZEquipSlot.class, equipableSlots);
    }

    @Override
    public Integer chooseZoneToWalk(ZGame game, ZCharacter cur, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone(cur.name() + " Choose zone to Walk", zones);
    }

    @Override
    public ZDoor chooseDoorToToggle(ZGame game, ZCharacter cur, List<ZDoor> doors) {
        return ZombicideApplet.instance.pickDoor(cur.name() + " Choose door to open or close", doors);
    }

    @Override
    public ZWeapon chooseWeaponSlot(ZGame game, ZCharacter cur, List<ZWeapon> weapons) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose weapon from slot", ZWeapon.class, weapons);
    }

    @Override
    public ZCharacter chooseTradeCharacter(ZGame game, ZCharacter cur, List<ZCharacter> list) {
        return ZombicideApplet.instance.pickCharacter(cur.name() + " Choose Character for Trade", list);
    }

    @Override
    public Integer chooseZoneForAttack(ZGame game, ZCharacter c, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone("Choose Zone to Attack", zones);
    }

    @Override
    public ZEquipment chooseItemToPickup(ZGame game, ZCharacter cur, List<ZEquipment> list) {
        return ZombicideApplet.instance.pickMenu("Choose Menu to Pickup", ZEquipment.class, list);
    }

    @Override
    public ZEquipment chooseItemToDrop(ZGame game, ZCharacter cur, List<ZEquipment> list) {
        return ZombicideApplet.instance.pickMenu("Choose Menu to Drop", ZEquipment.class, list);
    }

    @Override
    public ZItem chooseItemToThrow(ZGame game, ZCharacter cur, List<ZItem> slots) {
        return ZombicideApplet.instance.pickMenu("Choose Item to Throw", ZItem.class, slots);
    }

    @Override
    public Integer chooseZonetoThrowItem(ZGame game, ZCharacter cur, ZItem toThrow, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone("Choose Zone to throw the " + toThrow, zones);
    }

    @Override
    public Integer chooseZoneToShove(ZGame game, ZCharacter cur, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone("Choose Zone to shove zombies into", zones);
    }

    @Override
    public ZSpell chooseSpell(ZGame game, ZCharacter cur, List<ZSpell> spells) {
        return ZombicideApplet.instance.pickMenu("Choose Spell", ZSpell.class, spells);
    }

    @Override
    public ZCharacter chooseCharacterForSpell(ZGame game, ZCharacter cur, ZSpell spell, List<ZCharacter> targets) {
        return ZombicideApplet.instance.pickCharacter("Choose character to enchant with " + spell.getType(), targets);
    }

    @Override
    public ZCharacter chooseCharacterToBequeathMove(ZGame game, ZCharacter cur, List<ZCharacter> list) {
        return null;
    }
}
