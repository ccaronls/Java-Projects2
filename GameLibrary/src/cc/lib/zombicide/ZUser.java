package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.Utils;

public abstract class ZUser {

    public final static GColor [] USER_COLORS = {
            GColor.YELLOW,
            GColor.RED,
            GColor.GREEN,
            GColor.ORANGE,
            GColor.BLUE,
            GColor.MAGENTA
    };

    public final static String [] USER_COLOR_NAMES = {
            "YELLOW", "RED", "GREEN", "ORANGE", "BLUE", "MAGENTA"
    };

    private GColor color = GColor.YELLOW;
    private final List<ZPlayerName> characters = new ArrayList<>();

    public GColor getColor() {
        return color;
    }

    public void setColor(GColor color) {
        this.color = color;
        for (ZPlayerName nm : characters) {
            nm.character.setColor(color);
        }
    }

    public List<ZPlayerName> getCharacters() {
        return new ArrayList<>(characters);
    }

    public void clearCharacters() {
        characters.clear();
    }

    public void addCharacter(ZPlayerName c) {
        Utils.assertFalse(characters.contains(c));
        characters.add(c);
        c.character.setColor(color);
    }

    public void removeCharacter(ZPlayerName name) {
        name.character.setColor(null);
        characters.remove(name);
    }

    public void setCharacters(List<ZPlayerName> chars) {
        characters.clear();
        for (ZPlayerName nm : chars) {
            addCharacter(nm);
        }
    }

    public abstract ZPlayerName chooseCharacter(List<ZPlayerName> options);

    public abstract ZMove chooseMove(ZPlayerName cur, List<ZMove> options);

    public abstract ZSkill chooseNewSkill(ZPlayerName character, List<ZSkill> skillOptions);

    public abstract ZEquipSlot chooseSlotToOrganize(ZPlayerName cur, List<ZEquipSlot> slots);

    public abstract ZEquipment chooseEquipment(ZPlayerName cur, List<ZEquipment> equipOptions);

    public abstract ZEquipSlot chooseSlotForEquip(ZPlayerName cur, List<ZEquipSlot> equipableSlots);

    public abstract Integer chooseZoneToWalk(ZPlayerName cur, List<Integer> zones);

    public abstract ZDoor chooseDoorToToggle(ZPlayerName cur, List<ZDoor> doors);

    public abstract ZWeapon chooseWeaponSlot(ZPlayerName c, List<ZWeapon> weapons);

    public abstract ZPlayerName chooseTradeCharacter(ZPlayerName c, List<ZPlayerName> list);

    public abstract Integer chooseZoneForAttack(ZPlayerName c, List<Integer> zones);

    public abstract ZEquipment chooseItemToPickup(ZPlayerName cur, List<ZEquipment> list);

    public abstract ZEquipment chooseItemToDrop(ZPlayerName cur, List<ZEquipment> list);

    public abstract ZItem chooseItemToThrow(ZPlayerName cur, List<ZItem> slots);

    public abstract Integer chooseZoneToThrowItem(ZPlayerName cur, ZItem toThrow, List<Integer> zones);

    public abstract Integer chooseZoneToShove(ZPlayerName cur, List<Integer> list);

    public abstract ZSpell chooseSpell(ZPlayerName cur, List<ZSpell> spells);

    public abstract ZPlayerName chooseCharacterForSpell(ZPlayerName cur, ZSpell spell, List<ZPlayerName> targets);

    public abstract ZPlayerName chooseCharacterToBequeathMove(ZPlayerName cur, List<ZPlayerName> list);

    public abstract Integer chooseZoneForBloodlust(ZPlayerName cur, List<Integer> list);

    public abstract Integer chooseZoneToRemoveSpawn(ZPlayerName cur, List<Integer> list);
}
