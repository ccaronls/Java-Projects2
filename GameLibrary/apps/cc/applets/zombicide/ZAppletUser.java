package cc.applets.zombicide;

import java.util.List;

import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipSlot;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZGame;
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
    public Integer chooseZoneForBile(ZGame zGame, ZCharacter cur, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone("Pick a zone to place the Dragon Bile", zones);
    }

    @Override
    public ZMove chooseMove(ZGame zGame, ZCharacter cur, List<ZMove> moves) {
        return ZombicideApplet.instance.pickMenu("Choose Move", moves);
    }

    @Override
    public ZSkill chooseNewSkill(ZGame game, ZCharacter character, List<ZSkill> skillOptions) {
        return ZombicideApplet.instance.pickMenu("Choose New Skill", skillOptions);
    }

    @Override
    public ZEquipSlot chooseSlotToOrganize(ZGame zGame, ZCharacter cur, List<ZEquipSlot> slots) {
        return ZombicideApplet.instance.pickMenu("Choose Slot to Organize", slots);
    }

    @Override
    public ZEquipment chooseEquipment(ZGame zGame, ZCharacter cur, List<ZEquipment> equipOptions) {
        return ZombicideApplet.instance.pickMenu("Choose Equipment to Organize", equipOptions);
    }

    @Override
    public ZEquipSlot chooseSlotForEquip(ZGame zGame, ZCharacter cur, List<ZEquipSlot> equipableSlots) {
        return ZombicideApplet.instance.pickMenu("Choose Slot to Equip Item", equipableSlots);
    }

    @Override
    public Integer chooseZoneToIgnite(ZGame zGame, ZCharacter cur, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone("Choose zone to Ignite", zones);
    }

    @Override
    public Integer chooseZoneToWalk(ZGame zGame, ZCharacter cur, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone("Choose zone to Walk", zones);
    }

    @Override
    public ZDoor chooseDoorToToggle(ZGame zGame, ZCharacter cur, List<ZDoor> doors) {
        return ZombicideApplet.instance.pickDoor("Choose door to open or close", doors);
    }

    @Override
    public ZEquipSlot chooseWeaponSlot(ZGame zGame, ZCharacter c, List<ZEquipSlot> weapons) {
        return ZombicideApplet.instance.pickMenu("Choose weapon from slot", weapons);
    }

    @Override
    public ZCharacter chooseTradeCharacter(ZGame zGame, ZCharacter c, List<ZCharacter> list) {
        return ZombicideApplet.instance.pickCharacter("Choose Character for Trade", list);
    }

    @Override
    public Integer chooseZoneForAttack(ZGame zGame, ZCharacter c, List<Integer> zones) {
        return ZombicideApplet.instance.pickZone("Choose Zone to Attack", zones);
    }
}
