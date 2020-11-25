package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

public abstract class ZUser {

    final List<ZCharacter> characters = new ArrayList<>();

    public abstract void showMessage(String s);

    public void addCharacter(ZCharacter c) {
        characters.add(c);
    }

    public abstract ZCharacter chooseCharacter(List<ZCharacter> options);

    public abstract ZMove chooseMove(ZGame game, ZCharacter cur, List<ZMove> options);

    public abstract ZSkill chooseNewSkill(ZGame game, ZCharacter character, List<ZSkill> skillOptions);

    public abstract ZEquipSlot chooseSlotToOrganize(ZGame game, ZCharacter cur, List<ZEquipSlot> slots);

    public abstract ZEquipment chooseEquipment(ZGame game, ZCharacter cur, List<ZEquipment> equipOptions);

    public abstract ZEquipSlot chooseSlotForEquip(ZGame game, ZCharacter cur, List<ZEquipSlot> equipableSlots);

    public abstract Integer chooseZoneToWalk(ZGame game, ZCharacter cur, List<Integer> zones);

    public abstract ZDoor chooseDoorToToggle(ZGame game, ZCharacter cur, List<ZDoor> doors);

    public abstract ZWeapon chooseWeaponSlot(ZGame game, ZCharacter c, List<ZWeapon> weapons);

    public abstract ZCharacter chooseTradeCharacter(ZGame game, ZCharacter c, List<ZCharacter> list);

    public abstract Integer chooseZoneForAttack(ZGame game, ZCharacter c, List<Integer> zones);

    public abstract ZEquipment chooseItemToPickup(ZGame game, ZCharacter cur, List<ZEquipment> list);

    public abstract ZEquipment chooseItemToDrop(ZGame game, ZCharacter cur, List<ZEquipment> list);

    public abstract ZItem chooseItemToThrow(ZGame game, ZCharacter cur, List<ZItem> slots);

    public abstract Integer chooseZonetoThrowItem(ZGame game, ZCharacter cur, ZItem toThrow, List<Integer> zones);

    public abstract Integer chooseZoneToShove(ZGame zGame, ZCharacter cur, List<Integer> list);
}
