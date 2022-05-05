package cc.lib.zombicide.ui;

import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipSlot;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZEquipmentClass;
import cc.lib.zombicide.ZEquipmentType;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZSpawnArea;
import cc.lib.zombicide.ZSpell;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZWeapon;

public class UIZUser extends ZUser {

    <T> Integer indexOrNull(T item, List<T> options) {
        return item == null ? null : options.indexOf(item);
    }

    @Override
    public ZPlayerName chooseCharacter(List<? extends ZPlayerName> characters) {
        return UIZombicide.getInstance().pickCharacter("Pick character to play", (List)characters);
    }

    @Override
    public Integer chooseMove(ZPlayerName cur, List<ZMove> moves) {
        return indexOrNull(UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose Move", ZMove.class, moves), moves);
    }

    @Override
    public ZSkill chooseNewSkill(ZPlayerName cur, List<? extends ZSkill> list) {
        Table table = new Table(new Table.Model() {
            @Override
            public int getMaxCharsPerLine() {
                return 32;
            }
        });
        for (ZSkill t : list) {
            table.addColumn(Utils.toPrettyString(t.name()), t.getDescription());
        }
        UIZombicide.getInstance().boardRenderer.setOverlay(table);
        return UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose New Skill", ZSkill.class, list);
    }

    @Override
    public ZEquipSlot chooseSlotToOrganize(ZPlayerName cur, List<? extends ZEquipSlot> slots) {
        return UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose Slot to Organize", ZEquipSlot.class, slots);
    }

    @Override
    public Integer chooseEquipment(ZPlayerName cur, List<? extends ZEquipment<?>> list) {
        UIZombicide.getInstance().showEquipmentOverlay(cur, list);
        return indexOrNull(UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose Equipment to Organize", ZEquipment.class, list), (List)list);
    }

    @Override
    public ZEquipSlot chooseSlotForEquip(ZPlayerName cur, List<? extends ZEquipSlot> equipableSlots) {
        return UIZombicide.getInstance().pickMenu(cur,cur.name() + " Choose Slot to Equip Item", ZEquipSlot.class, equipableSlots);
    }

    @Override
    public Integer chooseZoneToWalk(ZPlayerName cur, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone(cur.name() + " Choose zone to Walk", zones);
    }

    @Override
    public Integer chooseDoorToToggle(ZPlayerName cur, List<ZDoor> doors) {
        return indexOrNull(UIZombicide.getInstance().pickDoor(cur.name() + " Choose door to open or close", doors), doors);
    }

    @Override
    public Integer chooseWeaponSlot(ZPlayerName cur, List<ZWeapon> weapons) {
        return indexOrNull(UIZombicide.getInstance().pickMenu(cur, cur.name() + " Choose weapon from slot", ZWeapon.class, weapons), weapons);
    }

    @Override
    public ZPlayerName chooseTradeCharacter(ZPlayerName cur, List<? extends ZPlayerName> list) {
        return UIZombicide.getInstance().pickCharacter(cur.name() + " Choose Character for Trade", (List)list);
    }

    @Override
    public Integer chooseZoneForAttack(ZPlayerName c, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone("Choose Zone to Attack", zones);
    }

    @Override
    public Integer chooseItemToPickup(ZPlayerName cur, List<? extends ZEquipment<?>> list) {
        UIZombicide.getInstance().showEquipmentOverlay(cur, list);
        return indexOrNull(UIZombicide.getInstance().pickMenu(cur, "Choose Item to Pickup", ZEquipment.class, list), (List)list);
    }

    @Override
    public Integer chooseItemToDrop(ZPlayerName cur, List<? extends ZEquipment<?>> list) {
        UIZombicide.getInstance().showEquipmentOverlay(cur, list);
        return indexOrNull(UIZombicide.getInstance().pickMenu(cur, "Choose Item to Drop", ZEquipment.class, list), (List)list);
    }

    @Override
    public Integer chooseEquipmentToThrow(ZPlayerName cur, List<? extends ZEquipment<?>> list) {
        UIZombicide.getInstance().showEquipmentOverlay(cur, list);
        return indexOrNull(UIZombicide.getInstance().pickMenu(cur,  "Choose Item to Throw", ZEquipment.class, list), (List)list);
    }

    @Override
    public Integer chooseZoneToThrowEquipment(ZPlayerName cur, ZEquipment<?> toThrow, List<Integer> zones) {
        UIZombicide.getInstance().showEquipmentOverlay(cur, Utils.toList(toThrow));
        return UIZombicide.getInstance().pickZone("Choose Zone to throw the " + toThrow, zones);
    }

    @Override
    public Integer chooseZoneToShove(ZPlayerName cur, List<Integer> zones) {
        return UIZombicide.getInstance().pickZone("Choose Zone to shove zombies into", zones);
    }

    @Override
    public ZSpell chooseSpell(ZPlayerName cur, List<ZSpell> spells) {
        UIZombicide.getInstance().showEquipmentOverlay(cur, spells);
        return UIZombicide.getInstance().pickMenu(cur, "Choose Spell", ZSpell.class, spells);
    }

    @Override
    public ZPlayerName chooseCharacterForSpell(ZPlayerName cur, ZSpell spell, List<? extends ZPlayerName> targets) {
        UIZombicide.getInstance().showEquipmentOverlay(cur, Utils.toList(spell));
        return UIZombicide.getInstance().pickCharacter("Choose character to enchant with " + spell.getType(), (List)targets);
    }

    @Override
    public ZPlayerName chooseCharacterToBequeathMove(ZPlayerName cur, List<? extends ZPlayerName> targets) {
        return UIZombicide.getInstance().pickCharacter("Choose character to bequeath an extra action", (List)targets);
    }

    @Override
    public Integer chooseZoneForBloodlust(ZPlayerName cur, List<Integer> list) {
        return UIZombicide.getInstance().pickZone("Choose Zone for Bloodlust", list);
    }

    @Override
    public Integer chooseSpawnAreaToRemove(ZPlayerName cur, List<ZSpawnArea> list) {
        return UIZombicide.getInstance().pickSpawn("Choose SPAWN Area to Remove", list);
    }

    @Override
    public Integer chooseZoneToIgnite(ZPlayerName playerName, List<Integer> ignitableZones) {
        return UIZombicide.getInstance().pickZone("Choose Zone to Ignite", ignitableZones);
    }

    @Override
    public ZEquipmentClass chooseEquipmentClass(ZPlayerName playerName, List<? extends ZEquipmentClass> classes) {
        return UIZombicide.getInstance().pickMenu(playerName, "Choose Equipment Class", ZEquipmentClass.class, classes);
    }

    @Override
    public ZEquipmentType chooseStartingEquipment(ZPlayerName playerName, List<? extends ZEquipmentType> list) {
        Table table = new Table().setNoBorder();
        for (ZEquipmentType t : list) {
            table.addColumnNoHeaderVarArg(t.create().getCardInfo(playerName.getCharacter(), UIZombicide.getInstance()));
        }

        UIZombicide.getInstance().boardRenderer.setOverlay(new Table().addColumn("Choose Starting Equipment", table));
        return UIZombicide.getInstance().pickMenu(playerName, "Choose Starting Equipment", ZEquipmentType.class, list);
    }
}
