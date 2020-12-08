package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

import cc.lib.utils.Reflector;

public abstract class ZUser extends Reflector<ZUser> {

    static {
        addAllFields(ZUser.class);
    }

    final List<ZPlayerName> characters = new ArrayList<>();

    public void clear() {
        characters.clear();
    }

    public abstract void showMessage(String s);

    public void addCharacter(ZPlayerName c) {
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

    public abstract Integer chooseZoneToShove(ZGame game, ZCharacter cur, List<Integer> list);

    public abstract ZSpell chooseSpell(ZGame game, ZCharacter cur, List<ZSpell> spells);

    public abstract ZCharacter chooseCharacterForSpell(ZGame game, ZCharacter cur, ZSpell spell, List<ZCharacter> targets);

    public abstract ZCharacter chooseCharacterToBequeathMove(ZGame game, ZCharacter cur, List<ZCharacter> list);

    public abstract Integer chooseZoneForBloodlust(ZGame zGame, ZCharacter cur, List<Integer> list);
}
