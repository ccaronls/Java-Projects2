package cc.lib.zombicide;

import java.util.List;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;

@Keep
public enum ZWeaponType implements ZEquipmentType<ZWeapon> {
    // DAGGER get extra die roll when 2 handed with another weapon

    // MELEE
    DAGGER(false, false,false, true, false, true, new ZWeaponStat(ZAttackType.BLADE, 4, 0, 0, 1, 4, 1), null, null),
    AXE(false, false,false, true, false, true, new ZWeaponStat(ZAttackType.BLADE,1, 0, 0, 1, 4, 1), null, null),
    HAMMER(false, false,false, false, false, true, new ZWeaponStat(ZAttackType.CRUSH,4, 0, 0, 1, 3, 2), null, null),
    SHORT_SWORD(false, false,false, true, false, true, new ZWeaponStat(ZAttackType.BLADE,4, 0, 0, 1, 4, 1), null, null),
    SWORD(false, false,false, true, false, true, new ZWeaponStat(ZAttackType.BLADE,4, 0, 0, 2, 4, 1), null, null),
    GREAT_SWORD(false, false,false, false, false, true, new ZWeaponStat(ZAttackType.BLADE,5, 0, 0, 5, 5, 1), null, null),
    // BOWS
    SHORT_BOW(false, true,false, false, false, false, null, new ZWeaponStat(ZAttackType.ARROW,0, 0, 1, 1, 3, 1), null),
    LONG_BOW(false, true, false, false, false, false, null, new ZWeaponStat(ZAttackType.ARROW,0, 1, 3, 1, 3, 1), null),
    // CROSSBOWS
    CROSSBOW(true, false, true, false, false, false, null, new ZWeaponStat(ZAttackType.ARROW,0, 1, 2, 2, 4,2), null),
    REPEATING_CROSSBOW(true, false, true, true, false, false, null, new ZWeaponStat(ZAttackType.ARROW,0, 0, 1, 3, 5, 1), null),
    HAND_CROSSBOW(true, false, true, true, false, false, null, new ZWeaponStat(ZAttackType.ARROW,0, 0, 3, 2, 3, 1), null),
    ORCISH_CROSSBOW(true, false, true, false, false, false, new ZWeaponStat(ZAttackType.CRUSH,0, 0, 0, 2, 3, 2), new ZWeaponStat(ZAttackType.ARROW,0, 1, 2, 2, 3, 2), null),
    HEAVY_CROSSBOW(true, false, true, false, false, false, null, new ZWeaponStat(ZAttackType.ARROW,0, 1, 2, 2, 4, 3), null),

    // MAGIC
    DEATH_STRIKE(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.MENTAL_STRIKE,0, 0, 1, 1, 4, 2)),
    // TODO: +1 damagae on a die roll 6
    DISINTEGRATE(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.DISINTEGRATION,0, 0, 1, 3, 5, 1)),
    EARTHQUAKE(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.EARTHQUAKE,0, 0, 1, 3, 4, 1)),
    FIREBALL(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.FIRE,0, 0, 1, 3, 4, 1)),
    MANA_BLAST(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.MENTAL_STRIKE,0, 0, 2, 1, 4, 1)),
    INFERNO(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.FIRE,0 ,0 ,1, 4, 4, 2)),
    LIGHTNING_BOLT(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.ELECTROCUTION,0 ,0 ,3, 1, 4, 1)),

    // SPECIAL
    BREAK_IN(false, false, false, false, false, false, new ZWeaponStat(null, 1, 0, 0, 0, 0, 0), null, null),
    ;

    ZWeaponType(boolean usesBolts, boolean usesArrows, boolean needsReload, boolean canTwoHand, boolean attckIsNoisy, boolean openDoorsIsNoisy, ZWeaponStat meleeStats, ZWeaponStat rangedStats, ZWeaponStat magicStats) {
        this.usesBolts = usesBolts;
        this.usesArrows = usesArrows;
        this.needsReload = needsReload;
        this.canTwoHand = canTwoHand;
        this.attckIsNoisy = attckIsNoisy;
        this.openDoorsIsNoisy = openDoorsIsNoisy;
        this.meleeStats = meleeStats;
        this.rangedStats = rangedStats;
        this.magicStats = magicStats;
    }

    final boolean usesBolts;
    final boolean usesArrows;
    final boolean needsReload;
    final boolean canTwoHand;
    final boolean attckIsNoisy;
    final boolean openDoorsIsNoisy;
    final ZWeaponStat meleeStats;
    final ZWeaponStat rangedStats;
    final ZWeaponStat magicStats;

    List<ZWeaponStat> getStats() {
        return Utils.filterItems(new Utils.Filter<ZWeaponStat>() {
            @Override
            public boolean keep(ZWeaponStat object) {
                return object != null;
            }
        }, Utils.toArray(meleeStats, rangedStats, magicStats));
    }

    /*
    ZWeaponStat getStat(ZActionType type) {
        switch (type) {
            case MELEE:
                return meleeStats;
            case MAGIC:
                return magicStats;
            case RANGED_BOLTS:
            case RANGED_ARROWS:
                return rangedStats;
        }
        return null;
    }*/

    @Override
    public ZWeapon create() {
        return new ZWeapon(this);
    }

}
