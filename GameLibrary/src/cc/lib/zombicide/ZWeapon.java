package cc.lib.zombicide;

import java.util.Arrays;

import cc.lib.game.Utils;
import cc.lib.utils.Table;

public class ZWeapon extends ZEquipment<ZWeaponType> {

    static {
        addAllFields(ZWeapon.class);
    }

    final ZWeaponType type;
    boolean isEmpty=false;

    public ZWeapon() {
        this(null);
    }

    ZWeapon(ZWeaponType type) {
        this.type = type;
    }

    int getOpenDoorValue() {
        if (!canOpenDoor())
            return 0;
        return 7-type.meleeStats.dieRollToOpenDoor + (type.openDoorsIsNoisy ? 0 : 1);
    }

    @Override
    public boolean canOpenDoor() {
        return type.meleeStats != null && type.meleeStats.dieRollToOpenDoor > 0;
    }

    @Override
    public ZEquipSlotType getSlotType() {
        return ZEquipSlotType.HAND;
    }

    @Override
    public boolean isMelee() {
        return type.meleeStats != null;
    }

    @Override
    public boolean isRanged() {
        return type.rangedStats != null;
    }

    @Override
    public boolean isMagic() {
        return type.magicStats != null;
    }

    @Override
    public boolean canEquip() {
        return true;
    }

    public boolean isLoaded() {
        return !isEmpty;
    }

    @Override
    public ZWeaponType getType() {
        return type;
    }

    public void fireWeapon() {
        if (type.needsReload)
            isEmpty = true;
    }

    @Override
    public boolean canDualWield() {
        return type.canTwoHand;
    }

    @Override
    public boolean isAttackNoisy() {
        return type.attackIsNoisy;
    }

    @Override
    public boolean isOpenDoorsNoisy() {
        return type.openDoorsIsNoisy;
    }

    public void reload() {
        isEmpty = false;
    }

    @Override
    public Table getCardInfo(ZCharacter c, ZGame game) {

        /*

        ORCISH CROSSBOW (DW)
        --------------------
        Doors |  no/quietly/noisy
        Open % |  1=6/6 2 = 5/6 3 = 4/6 4 = 3/6 2 = 5/6 6 = 1/6 ---> (7-n)*100/6 %
               | Melee      | Ranged
        Damage | 2 (loud)  | s (quiet)
        Hit %  | 50% x dice | 50% x dice
        Range  | 0      | 0-1 |
        Reload | no | yes |
         */

        /*
        Table cardUpper = new Table(new Object[][] {
                { "Doors", !canOpenDoor() ? "no" : (type.openDoorsIsNoisy ? "noisy" : "quietly") },
                { "Open %", type.meleeStats == null ? "(/)" : String.format("%d%%", (7-type.meleeStats.dieRollToOpenDoor)*100/6) }
        }, Table.NO_BORDER);
*/
        Table cardLower = new Table().setNoBorder();
        cardLower.addColumnNoHeader(Arrays.asList(
                "Type",
                "Damage",
                "Hit %",
                "Range",
                "Doors",
                "Reloads"));

        for (ZActionType at : ZActionType.values()) {
            ZWeaponStat stats = c.getWeaponStat(this, at, game, -1);
            if (stats != null) {
                String doorInfo = "";
                if (stats.dieRollToOpenDoor > 0) {
                    doorInfo = String.format("%s %d%%", type.openDoorsIsNoisy ? "noisy" : "quiet", (7-type.meleeStats.dieRollToOpenDoor)*100/6);
                } else {
                    doorInfo = "no";
                }
                cardLower.addColumnNoHeader(Arrays.asList(at.getLabel(),
                        String.format("%d %s", stats.damagePerHit, type.attackIsNoisy ? " loud" : " quiet"),
                        String.format("%d%% x %d", (7 - stats.dieRollToHit) * 100 / 6, stats.numDice),
                        stats.minRange == stats.maxRange ? String.valueOf(stats.minRange) : String.format("%d-%d", stats.minRange, stats.maxRange),
                        doorInfo,
                        type.needsReload ? String.format("yes (%s)", isEmpty ? "empty" : "loaded") : "no"
                ));
            }
        }

        Table card = new Table(new String [] { type.getLabel() + (type.canTwoHand ? " (DW)" : "" ) },
            new Object [][] {
//                { cardUpper.toString() },
                { cardLower }
            }).setNoBorder();

        return card;
    }

    @Override
    public String getTooltipText() {
        Table cardLower = new Table().setNoBorder();
        cardLower.addColumnNoHeader(Arrays.asList(
                "Attack Type",
                "Dual Wield",
                "Damage",
                "Hit %",
                "Range",
                "Doors",
                "Reloads"));

        for (ZWeaponStat stats : type.getStats()) {
            String doorInfo = "";
            if (stats.dieRollToOpenDoor > 0) {
                doorInfo = String.format("%s %d%%", type.openDoorsIsNoisy ? "noisy" : "quiet", (7-type.meleeStats.dieRollToOpenDoor)*100/6);
            } else {
                doorInfo = "no";
            }
            cardLower.addColumnNoHeader(Arrays.asList(Utils.toPrettyString(stats.attackType.name()),
                    type.canTwoHand ? "yes" : "no",
                    String.format("%d %s", stats.damagePerHit, type.attackIsNoisy ? " loud" : " quiet"),
                    String.format("%d%% x %d", (7 - stats.dieRollToHit) * 100 / 6, stats.numDice),
                    stats.minRange == stats.maxRange ? String.valueOf(stats.minRange) : String.format("%d-%d", stats.minRange, stats.maxRange),
                    doorInfo,
                    type.needsReload ? String.format("yes (%s)", isEmpty ? "empty" : "loaded") : "no"
            ));
        }

        return cardLower.toString();
    }
}
