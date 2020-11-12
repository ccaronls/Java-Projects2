package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

public abstract class ZUser {

    final List<ZCharacter> characters = new ArrayList<>();

    void prepareTurn() {
        for (ZCharacter c : characters)
            c.prepareTurn();
    }

    public abstract void showMessage(String s);

    public void addCharacter(ZCharacter c) {
        characters.add(c);
    }

    public abstract ZCharacter chooseCharacter(List<ZCharacter> options);

    public abstract ZMove chooseMove(ZGame zGame, ZCharacter cur, List<ZMove> options);

    public abstract ZSkill chooseNewSkill(ZGame game, ZCharacter character, List<ZSkill> skillOptions);

    public abstract Integer chooseZoneForBile(ZGame zGame, ZCharacter cur, List<Integer> accessable);

    public abstract ZEquipSlot chooseSlotToOrganize(ZGame zGame, ZCharacter cur, List<ZEquipSlot> slots);

    public abstract ZEquipment chooseEquipment(ZGame zGame, ZCharacter cur, List<ZEquipment> equipOptions);

    public abstract ZEquipSlot chooseSlotForEquip(ZGame zGame, ZCharacter cur, List<ZEquipSlot> equipableSlots);

    public abstract Integer chooseZoneToIgnite(ZGame zGame, ZCharacter cur, List<Integer> zones);

    public abstract Integer chooseZoneToWalk(ZGame zGame, ZCharacter cur, List<Integer> zones);

    public abstract ZDoor chooseDoorToToggle(ZGame zGame, ZCharacter cur, List<ZDoor> doors);

    public abstract ZEquipSlot chooseWeaponSlot(ZGame zGame, ZCharacter c, List<ZEquipSlot> weapons);

    public abstract ZCharacter chooseTradeCharacter(ZGame zGame, ZCharacter c, List<ZCharacter> list);

    public abstract Integer chooseZoneForAttack(ZGame zGame, ZCharacter c, List<Integer> zones);
}
