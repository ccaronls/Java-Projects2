package cc.lib.zombicide;

import java.util.List;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;

@Keep
public enum ZWeaponType implements ZEquipmentType<ZWeapon> {
    // DAGGER get extra die roll when 2 handed with another melee weapon

    // MELEE
    DAGGER(false, true,false, true, false, true, new ZWeaponStat(ZAttackType.BLADE, 5, 0, 0, 1, 4, 1), new ZWeaponStat(ZAttackType.RANGED, 0, 1, 1, 1, 3, 2), null,  "Gain +1 die with another equipped melee weapon"),
    AXE(false, false,false, true, false, true, new ZWeaponStat(ZAttackType.BLADE,1, 0, 0, 1, 4, 1), null, null, null),
    HAMMER(false, false,false, false, false, true, new ZWeaponStat(ZAttackType.CRUSH,4, 0, 0, 1, 3, 2), null, null, null),
    SHORT_SWORD(false, false,false, true, false, true, new ZWeaponStat(ZAttackType.BLADE,4, 0, 0, 1, 4, 1), null, null, null),
    SWORD(false, false,false, true, false, true, new ZWeaponStat(ZAttackType.BLADE,4, 0, 0, 2, 4, 1), null, null, null),
    GREAT_SWORD(false, false,false, false, false, true, new ZWeaponStat(ZAttackType.BLADE,5, 0, 0, 5, 5, 1), null, null, null),
    // BOWS
    SHORT_BOW(false, true,false, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED,0, 0, 1, 1, 3, 1), null, null),
    LONG_BOW(false, true, false, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED,0, 1, 3, 1, 3, 1), null, null),
    // CROSSBOWS
    CROSSBOW(true, false, true, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED,0, 1, 2, 2, 4,2), null, null),
    REPEATING_CROSSBOW(true, false, true, true, false, false, null, new ZWeaponStat(ZAttackType.RANGED,0, 0, 1, 3, 5, 1), null, null),
    HAND_CROSSBOW(true, false, true, true, false, false, null, new ZWeaponStat(ZAttackType.RANGED,0, 0, 3, 2, 3, 1), null, "Auto reload at end of turn"),
    ORCISH_CROSSBOW(true, false, true, false, false, false, new ZWeaponStat(ZAttackType.CRUSH,0, 0, 0, 2, 3, 2), new ZWeaponStat(ZAttackType.RANGED,0, 1, 2, 2, 3, 2), null, null),
    HEAVY_CROSSBOW(true, false, true, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED,0, 1, 2, 2, 4, 3), null, null),

    // MAGIC
    DEATH_STRIKE(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.MENTAL_STRIKE,0, 0, 1, 1, 4, 2), null),
    // TODO: +1 damage on a die roll 6
    DISINTEGRATE(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.DISINTEGRATION,0, 0, 1, 3, 5, 1), null),
    EARTHQUAKE(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.EARTHQUAKE,0, 0, 1, 3, 4, 1), null),
    FIREBALL(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.FIRE,0, 0, 1, 3, 4, 1), null),
    MANA_BLAST(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.MENTAL_STRIKE,0, 0, 2, 1, 4, 1), null),
    INFERNO(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.FIRE,0 ,0 ,1, 4, 4, 2), null),
    LIGHTNING_BOLT(false, false, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.ELECTROCUTION,0 ,0 ,3, 1, 4, 1), null),

    // SPECIAL
    BREAK_IN(false, false, false, false, false, false, new ZWeaponStat(null, 1, 0, 0, 0, 0, 0), null, null, null),

    // WULFSBURG
    FLAMING_GREAT_SWORD(false, false, false, false, false, true, new ZWeaponStat(ZAttackType.BLADE, 5, 0, 0, 5, 5, 2), null, null, "Can ignite Dragon Fire at range 0-1"),
    VAMPIRE_CROSSBOW(true, false, true, false, false, true, null, new ZWeaponStat(ZAttackType.RANGED, 4, 1, 2, 2, 4, 3), null, "Heal 1 wound each time you kill a zombie."),
    AXE_OF_CARNAGE(false, false, false, false, false, true, new ZWeaponStat(ZAttackType.BLADE, 1, 0, 0, 4, 4, 2), null, null, "Add an additional success with each melee action reslved.??"), // TODO: Wha???
    DRAGON_FIRE_BLADE(false, false, false, false, false, true, new ZWeaponStat(ZAttackType.BLADE, 3, 0, 0, 2, 3,2), null, new ZWeaponStat(ZAttackType.DRAGON_FIRE, 0, 0, 1, 1, 1, 3), "Throw (Discard) at range 0-1 to create a dragon fire."),
    CHAOS_LONGBOW(false, true, false, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED, 0, 0, 3, 4, 4, 2), null, "4 or more hits on a ranged action causes dragon fire in the targeted zone."),
    BASTARD_SWORD(false, false, false, false, false, true, new ZWeaponStat(ZAttackType.BLADE, 4, 0, 0, 2, 4, 2), null, null, null),
    EARTHQUAKE_HAMMER(false, false, false, false, false, true, new ZWeaponStat(ZAttackType.CRUSH, 3, 0, 0, 3, 3, 2), null, null, "Roll 6: +1 die and +1 damage"),
    ;

    ZWeaponType(boolean usesBolts, boolean usesArrows, boolean needsReload, boolean canTwoHand, boolean attackIsNoisy, boolean openDoorsIsNoisy, ZWeaponStat meleeStats, ZWeaponStat rangedStats, ZWeaponStat magicStats, String specialInfo) {
        this.usesBolts = usesBolts;
        this.usesArrows = usesArrows;
        this.needsReload = needsReload;
        this.canTwoHand = canTwoHand;
        this.attackIsNoisy = attackIsNoisy;
        this.openDoorsIsNoisy = openDoorsIsNoisy;
        this.meleeStats = meleeStats;
        this.rangedStats = rangedStats;
        this.magicStats = magicStats;
        this.specialInfo = specialInfo;
    }

    final boolean usesBolts;
    final boolean usesArrows;
    final boolean needsReload;
    final boolean canTwoHand;
    final boolean attackIsNoisy;
    final boolean openDoorsIsNoisy;
    final ZWeaponStat meleeStats;
    final ZWeaponStat rangedStats;
    final ZWeaponStat magicStats;
    final String specialInfo;

    List<ZWeaponStat> getStats() {
        return Utils.filterItems(new Utils.Filter<ZWeaponStat>() {
            @Override
            public boolean keep(ZWeaponStat object) {
                return object != null;
            }
        }, Utils.toArray(meleeStats, rangedStats, magicStats));
    }

    @Override
    public ZWeapon create() {
        return new ZWeapon(this);
    }

    public boolean isFire() {
        switch (this) {
            case INFERNO:
            case FIREBALL:
                return true;
        }
        return false;
    }

}
