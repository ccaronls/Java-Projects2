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
        if (!isOpenDoorCapable())
            return 0;
        return 7-type.meleeStats.dieRollToOpenDoor + (type.openDoorsIsNoisy ? 0 : 1);
    }

    @Override
    public boolean isOpenDoorCapable() {
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
    public boolean isEquippable(ZCharacter c) {
        return c.getSkillLevel().getDifficultyColor().ordinal() >= type.minColorToEquip.ordinal();
    }

    public boolean isLoaded() {
        return !isEmpty;
    }

    @Override
    public ZWeaponType getType() {
        return type;
    }

    public void fireWeapon(ZGame game, ZCharacter cur, ZWeaponStat stat) {
        if (stat.getAttackType().needsReload())
            isEmpty = true;
        if (type == ZWeaponType.DAGGER) {
            cur.removeEquipment(this);
            game.putBackInSearchables(this);
        }
    }

    @Override
    public boolean isDualWieldCapable() {
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

    public boolean reload() {
        if (isEmpty) {
            isEmpty = false;
            return true;
        }
        return false;
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
                    doorInfo = String.format("%s %d%%", type.openDoorsIsNoisy ? "noisy" : "quiet", (7-stats.dieRollToOpenDoor)*100/6);
                } else {
                    doorInfo = "no";
                }
                cardLower.addColumnNoHeader(Arrays.asList(at.getLabel(),
                        String.format("%d %s", stats.damagePerHit, type.attackIsNoisy ? " loud" : " quiet"),
                        String.format("%d%% x %d", (7 - stats.dieRollToHit) * 100 / 6, stats.numDice),
                        stats.minRange == stats.maxRange ? String.valueOf(stats.minRange) : String.format("%d-%d", stats.minRange, stats.maxRange),
                        doorInfo,
                        stats.getAttackType().needsReload() ? String.format("yes (%s)", isEmpty ? "empty" : "loaded") : "no"
                ));
            }
        }

        Table card = new Table(String.format("%s%s %s", type.getLabel(), type.canTwoHand ? " (DW)" : "", type.minColorToEquip))
                .addRow(cardLower).setNoBorder();
        if (type.specialInfo != null) {
            card.addRow(Utils.wrapTextWithNewlines("*" + type.specialInfo, 32));
        }
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
                doorInfo = String.format("%s %d%%", type.openDoorsIsNoisy ? "noisy" : "quiet", (7-stats.dieRollToOpenDoor)*100/6);
            } else {
                doorInfo = "no";
            }
            cardLower.addColumnNoHeader(Arrays.asList(Utils.toPrettyString(stats.attackType.name()),
                    type.canTwoHand ? "yes" : "no",
                    String.format("%d %s", stats.damagePerHit, type.attackIsNoisy ? " loud" : " quiet"),
                    String.format("%d%% x %d", (7 - stats.dieRollToHit) * 100 / 6, stats.numDice),
                    stats.minRange == stats.maxRange ? String.valueOf(stats.minRange) : String.format("%d-%d", stats.minRange, stats.maxRange),
                    doorInfo,
                    stats.getAttackType().needsReload() ? String.format("yes (%s)", isEmpty ? "empty" : "loaded") : "no"
            ));
        }

        return cardLower.toString();
    }

    @Override
    public void onEndOfRound(ZGame game) {
        switch (type) {
            case HAND_CROSSBOW:
                if (!isLoaded()) {
                    game.addLogMessage(getLabel() + " auto reloaded");
                    reload();
                }
                break;
        }
    }

    public void onEnemyDestroyed(ZGame g, ZCharacter c, ZZombie z) {
        switch (type) {
            case VAMPIRE_CROSSBOW:
                if (c.heal(g,1)) {
                    g.onCharacterHealed(c, 1);
                }
                break;
        }
    }

}
