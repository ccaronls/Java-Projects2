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

    private int color = 0;
    private String name = null;
    private final List<ZPlayerName> characters = new ArrayList<>();

    public GColor getColor() {
        return USER_COLORS[color];
    }

    public int getColorId() {
        return color;
    }

    public void setColor(int color) {
        Utils.assertTrue(color >= 0 && color < USER_COLORS.length);
        this.color = color;
        for (ZPlayerName nm : characters) {
            nm.character.setColor(USER_COLORS[color]);
        }
    }

    public String getName() {
        return name == null ? USER_COLOR_NAMES[color] : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ZPlayerName> getCharacters() {
        return new ArrayList<>(characters);
    }

    public void clearCharacters() {
        characters.clear();
    }

    public void addCharacter(ZPlayerName c) {
        characters.remove(c);
        characters.add(c);
        c.character.setColor(USER_COLORS[color]);
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

    public void setCharactersHidden(boolean hidden) {
        for (ZPlayerName nm : characters) {
            if (nm.getCharacter() != null)
                nm.getCharacter().setInvisible(hidden);
        }
    }

    public abstract ZPlayerName chooseCharacter(List<ZPlayerName> options);

    public abstract Integer chooseMove(ZPlayerName cur, List<ZMove> options);

    <T> T elemOrNull(Integer idx, List options) {
        if (idx == null)
            return null;
        return (T)options.get(idx);
    }

    final ZMove chooseMoveInternal(ZPlayerName cur, List<ZMove> options) {
        return elemOrNull(chooseMove(cur, options), options);
    }

    public abstract ZSkill chooseNewSkill(ZPlayerName character, List<ZSkill> skillOptions);

    public abstract ZEquipSlot chooseSlotToOrganize(ZPlayerName cur, List<ZEquipSlot> slots);

    public abstract Integer chooseEquipment(ZPlayerName cur, List<ZEquipment> equipOptions);

    final ZEquipment chooseEquipmentInternal(ZPlayerName cur, List<ZEquipment> equipOptions) {
        return elemOrNull(chooseEquipment(cur, equipOptions), equipOptions);
    }

    public abstract ZEquipSlot chooseSlotForEquip(ZPlayerName cur, List<ZEquipSlot> equipableSlots);

    public abstract Integer chooseZoneToWalk(ZPlayerName cur, List<Integer> zones);

    public abstract Integer chooseDoorToToggle(ZPlayerName cur, List<ZDoor> doors);

    final ZDoor chooseDoorToToggleInternal(ZPlayerName cur, List<ZDoor> doors) {
        return elemOrNull(chooseDoorToToggle(cur, doors), doors);
    }

    public abstract Integer chooseWeaponSlot(ZPlayerName c, List<ZWeapon> weapons);

    final ZWeapon chooseWeaponSlotInternal(ZPlayerName c, List<ZWeapon> weapons) {
        return elemOrNull(chooseWeaponSlot(c, weapons), weapons);
    }

    public abstract ZPlayerName chooseTradeCharacter(ZPlayerName c, List<ZPlayerName> list);

    public abstract Integer chooseZoneForAttack(ZPlayerName c, List<Integer> zones);

    public abstract Integer chooseItemToPickup(ZPlayerName cur, List<ZEquipment> list);

    final ZEquipment chooseItemToPickupInternal(ZPlayerName cur, List<ZEquipment> list) {
        return elemOrNull(chooseItemToPickup(cur, list), list);
    }

    public abstract Integer chooseItemToDrop(ZPlayerName cur, List<ZEquipment> list);

    final ZEquipment chooseItemToDropInternal(ZPlayerName cur, List<ZEquipment> list) {
        return elemOrNull(chooseItemToDrop(cur, list), list);
    }

    public abstract Integer chooseEquipmentToThrow(ZPlayerName cur, List<ZEquipment> slots);

    final ZEquipment chooseEquipmentToThrowInternal(ZPlayerName cur, List<ZEquipment> slots) {
        return elemOrNull(chooseEquipmentToThrow(cur, slots), slots);
    }

    public abstract Integer chooseZoneToThrowEquipment(ZPlayerName cur, ZEquipment toThrow, List<Integer> zones);

    public abstract Integer chooseZoneToShove(ZPlayerName cur, List<Integer> list);

    public abstract ZSpell chooseSpell(ZPlayerName cur, List<ZSpell> spells);

    public abstract ZPlayerName chooseCharacterForSpell(ZPlayerName cur, ZSpell spell, List<ZPlayerName> targets);

    public abstract ZPlayerName chooseCharacterToBequeathMove(ZPlayerName cur, List<ZPlayerName> list);

    public abstract Integer chooseZoneForBloodlust(ZPlayerName cur, List<Integer> list);

    public abstract Integer chooseSpawnAreaToRemove(ZPlayerName cur, List<ZSpawnArea> list);

    public abstract Integer chooseZoneToIgnite(ZPlayerName playerName, List<Integer> ignitableZones);
}
