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
import cc.lib.zombicide.ZUser;

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
    public ZMove chooseMove(ZGame zGame, ZCharacter cur, List<ZMove> moves) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose Move", moves);
    }

    @Override
    public ZSkill chooseNewSkill(ZGame game, ZCharacter cur, List<ZSkill> skillOptions) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose New Skill", skillOptions);
    }

    @Override
    public ZEquipSlot chooseSlotToOrganize(ZGame zGame, ZCharacter cur, List<ZEquipSlot> slots) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose Slot to Organize", slots);
    }

    @Override
    public ZEquipment chooseEquipment(ZGame zGame, ZCharacter cur, List<ZEquipment> equipOptions) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose Equipment to Organize", equipOptions);
    }

    @Override
    public ZEquipSlot chooseSlotForEquip(ZGame zGame, ZCharacter cur, List<ZEquipSlot> equipableSlots) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose Slot to Equip Item", equipableSlots);
    }

    @Override
    public Integer chooseZoneToWalk(ZGame zGame, ZCharacter cur, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone(cur.name() + " Choose zone to Walk", zones);
    }

    @Override
    public ZDoor chooseDoorToToggle(ZGame zGame, ZCharacter cur, List<ZDoor> doors) {
        return ZombicideApplet.instance.pickDoor(cur.name() + " Choose door to open or close", doors);
    }

    @Override
    public ZEquipSlot chooseWeaponSlot(ZGame zGame, ZCharacter cur, List<ZEquipSlot> weapons) {
        return ZombicideApplet.instance.pickMenu(cur.name() + " Choose weapon from slot", weapons);
    }

    @Override
    public ZCharacter chooseTradeCharacter(ZGame zGame, ZCharacter cur, List<ZCharacter> list) {
        return ZombicideApplet.instance.pickCharacter(cur.name() + " Choose Character for Trade", list);
    }

    @Override
    public Integer chooseZoneForAttack(ZGame zGame, ZCharacter c, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone("Choose Zone to Attack", zones);
    }

    @Override
    public ZEquipment chooseItemToPickup(ZGame zGame, ZCharacter cur, List<ZEquipment> list) {
        return ZombicideApplet.instance.pickMenu("Choose Menu to Pickup", list);
    }

    @Override
    public ZEquipment chooseItemToDrop(ZGame zGame, ZCharacter cur, List<ZEquipment> list) {
        return ZombicideApplet.instance.pickMenu("Choose Menu to Drop", list);
    }

    @Override
    public ZEquipSlot chooseItemToThrow(ZGame zGame, ZCharacter cur, List<ZEquipSlot> slots) {
        return ZombicideApplet.instance.pickMenu("Choose Item to Throw", slots);
    }

    @Override
    public Integer chooseZonetoThrowItem(ZGame zGame, ZCharacter cur, ZItem toThrow, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone("Choose Zone to throw the " + toThrow, zones);
    }
}