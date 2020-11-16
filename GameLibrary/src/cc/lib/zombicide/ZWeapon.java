package cc.lib.zombicide;

import java.util.Arrays;

import cc.lib.utils.Table;

public class ZWeapon extends ZEquipment<ZWeaponType> {

    static {
        addAllFields(ZWeapon.class);
    }

    final ZWeaponType type;
    boolean needsReload=false;

    public ZWeapon() {
        this(null);
    }

    ZWeapon(ZWeaponType type) {
        this.type = type;
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

    public boolean isNeedsReload() {
        return needsReload;
    }

    @Override
    public ZWeaponType getType() {
        return type;
    }


    @Override
    public String getCardString(ZCharacter c, ZGame game) {

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
                { "Doors", !canOpenDoor() ? "no" : (type.openDoorsIsNoisy ? "noisy" : "quiety") },
                { "Open %", type.meleeStats == null ? "(/)" : String.format("%d%%", (7-type.meleeStats.dieRollToOpenDoor)*100/6) }
        }, Table.NO_BORDER);
*/
        Table cardLower = new Table().setNoBorder();
        cardLower.addColumnNoHeader(Arrays.asList("",
                "Damage",
                "Hit %",
                "Range",
                "Doors",
                "Reloads"));

        for (ZActionType at : ZActionType.values()) {
            ZWeaponStat stats = c.getWeaponStat(this, at, game);
            if (stats != null) {
                String doorInfo = "";
                if (stats.dieRollToOpenDoor > 0) {
                    doorInfo = String.format("%s %d%%", type.openDoorsIsNoisy ? "noisy" : "quiet", (7-type.meleeStats.dieRollToOpenDoor)*100/6);
                } else {
                    doorInfo = "no";
                }
                cardLower.addColumnNoHeader(Arrays.asList(at.name(),
                        String.format("%d %s", stats.damagePerHit, type.attckIsNoisy ? " loud" : " quiet"),
                        String.format("%d%% x %d", (7 - stats.dieRollToHit) * 100 / 6, stats.numDice),
                        stats.minRange == stats.maxRange ? String.valueOf(stats.minRange) : String.format("%d-%d", stats.minRange, stats.maxRange),
                        doorInfo,
                        type.needsReload ? String.format("yes (%s)", needsReload ? "empty" : "loaded") : "no"
                ));
            }
        }

        Table card = new Table(new String [] { type.name() + (type.canTwoHand ? " (DW)" : "" ) },
            new Object [][] {
//                { cardUpper.toString() },
                { cardLower.toString() }
            }).setNoBorder();

        return card.toString();
    }
}
