package cc.lib.zombicide;

import java.util.List;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;

@Keep
public enum ZWeaponType implements ZEquipmentType<ZWeapon> {
    // DAGGER get extra die roll when 2 handed with another melee weapon

    // MELEE
    DAGGER(ZColor.BLUE, ZActionType.MELEE, false, true, false, true, new ZWeaponStat(ZAttackType.BLADE, 4, 0, 0, 1, 4, 1), new ZWeaponStat(ZAttackType.RANGED_THROW, 0, 1, 1, 1, 3, 2), null,  "Gain +1 die with another equipped melee weapon"),
    AXE(ZColor.BLUE, ZActionType.MELEE, false, true, false, true, new ZWeaponStat(ZAttackType.BLADE,1, 0, 0, 1, 4, 1), null, null, null),
    HAMMER(ZColor.BLUE, ZActionType.MELEE, false, false, false, true, new ZWeaponStat(ZAttackType.CRUSH,4, 0, 0, 1, 3, 2), null, null, null),
    SHORT_SWORD(ZColor.BLUE, ZActionType.MELEE, false, true, false, true, new ZWeaponStat(ZAttackType.BLADE,3, 0, 0, 1, 4, 1), null, null, null),
    SWORD(ZColor.BLUE, ZActionType.MELEE, false, true, false, true, new ZWeaponStat(ZAttackType.BLADE,3, 0, 0, 2, 4, 1), null, null, null),
    GREAT_SWORD(ZColor.BLUE, ZActionType.MELEE, false, false, false, true, new ZWeaponStat(ZAttackType.BLADE,4, 0, 0, 5, 5, 1), null, null, null),
    // BOWS
    SHORT_BOW(ZColor.BLUE, ZActionType.ARROWS, false, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED_ARROWS,0, 0, 1, 1, 3, 1), null, null),
    LONG_BOW(ZColor.BLUE, ZActionType.ARROWS, false, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED_ARROWS,0, 1, 3, 1, 3, 1), null, null),
    // CROSSBOWS
    CROSSBOW(ZColor.BLUE, ZActionType.BOLTS, true, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED_BOLTS,0, 1, 2, 2, 4,2), null, null),
    REPEATING_CROSSBOW(ZColor.BLUE, ZActionType.BOLTS, true, true, false, false, null, new ZWeaponStat(ZAttackType.RANGED_BOLTS,0, 0, 1, 3, 5, 1), null, null),
    HAND_CROSSBOW(ZColor.BLUE, ZActionType.BOLTS, true, true, false, false, null, new ZWeaponStat(ZAttackType.RANGED_BOLTS,0, 0, 3, 2, 3, 1), null, "Auto reload at end of turn"),
    ORCISH_CROSSBOW(ZColor.BLUE, ZActionType.BOLTS, true, false, false, false, new ZWeaponStat(ZAttackType.CRUSH,0, 0, 0, 2, 3, 2), new ZWeaponStat(ZAttackType.RANGED_BOLTS,0, 1, 2, 2, 3, 2), null, null),
    HEAVY_CROSSBOW(ZColor.BLUE, ZActionType.BOLTS, true, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED_BOLTS,0, 1, 2, 2, 4, 3), null, null),

    // MAGIC
    DEATH_STRIKE(ZColor.BLUE, ZActionType.MAGIC, false,true, true, false, null, null, new ZWeaponStat(ZAttackType.MENTAL_STRIKE,0, 0, 1, 1, 4, 2), null),
    // TODO: +1 damage on a die roll 6
    DISINTEGRATE(ZColor.BLUE, ZActionType.MAGIC, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.DISINTEGRATION,0, 0, 1, 3, 5, 1), null),
    EARTHQUAKE(ZColor.BLUE, ZActionType.MAGIC, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.EARTHQUAKE,0, 0, 1, 3, 4, 1), null),
    FIREBALL(ZColor.BLUE, ZActionType.MAGIC, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.FIRE,0, 0, 1, 3, 4, 1), null),
    MANA_BLAST(ZColor.BLUE, ZActionType.MAGIC, false, true, true, false, null, null, new ZWeaponStat(ZAttackType.MENTAL_STRIKE,0, 0, 2, 1, 4, 1), null),
    INFERNO(ZColor.BLUE, ZActionType.MAGIC, false, false, true, false, null, null, new ZWeaponStat(ZAttackType.FIRE,0 ,0 ,1, 4, 4, 2), null),
    LIGHTNING_BOLT(ZColor.BLUE, ZActionType.MAGIC, false,true, true, false, null, null, new ZWeaponStat(ZAttackType.ELECTROCUTION,0 ,0 ,3, 1, 4, 1), null),

    // SPECIAL
    BREAK_IN(ZColor.BLUE, ZActionType.OPEN_DOOR, false, false, false, false, new ZWeaponStat(null, 1, 0, 0, 0, 0, 0), null, null, null),

    // WULFSBURG
    FLAMING_GREAT_SWORD(ZColor.YELLOW, ZActionType.MELEE, false, false, false, true, new ZWeaponStat(ZAttackType.BLADE, 5, 0, 0, 5, 5, 2), null, null, "Can ignite Dragon Fire at range 0-1"),
    VAMPIRE_CROSSBOW(ZColor.YELLOW, ZActionType.BOLTS, true, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 4, 3), null, "Heal 1 wound each time you kill a zombie."),
    AXE_OF_CARNAGE(ZColor.ORANGE, ZActionType.MELEE, false, false, false, true, new ZWeaponStat(ZAttackType.BLADE, 1, 0, 0, 4, 4, 2), null, null, "Add an additional success with each melee action resolved.??"), // TODO: Wha???
    DRAGON_FIRE_BLADE(ZColor.ORANGE, ZActionType.MELEE, false, false, false, true, new ZWeaponStat(ZAttackType.BLADE, 3, 0, 0, 2, 3,2), null, new ZWeaponStat(ZAttackType.DRAGON_FIRE, 0, 0, 1, 1, 1, 3), "Throw (Discard) at range 0-1 to create a dragon fire."),
    CHAOS_LONGBOW(ZColor.ORANGE, ZActionType.ARROWS, false, false, false, false, null, new ZWeaponStat(ZAttackType.RANGED_ARROWS, 0, 0, 3, 4, 4, 2), null, "4 or more hits on a ranged action causes dragon fire in the targeted zone."),
    BASTARD_SWORD(ZColor.BLUE, ZActionType.MELEE, false, false, false, true, new ZWeaponStat(ZAttackType.BLADE, 4, 0, 0, 2, 4, 2), null, null, null),
    EARTHQUAKE_HAMMER(ZColor.ORANGE, ZActionType.MELEE, false, false, false, true, new ZWeaponStat(ZAttackType.CRUSH, 3, 0, 0, 3, 3, 2), null, null, "Roll 6: +1 die and +1 damage"),
    ;

    ZWeaponType(ZColor minColorToEquip, ZActionType actionType, boolean needsReload, boolean canTwoHand, boolean attackIsNoisy, boolean openDoorsIsNoisy, ZWeaponStat meleeStats, ZWeaponStat rangedStats, ZWeaponStat magicStats, String specialInfo) {
        this.minColorToEquip = minColorToEquip;
        this.actionType = actionType;
        this.needsReload = needsReload;
        this.canTwoHand = canTwoHand;
        this.attackIsNoisy = attackIsNoisy;
        this.openDoorsIsNoisy = openDoorsIsNoisy;
        this.meleeStats = meleeStats;
        this.rangedStats = rangedStats;
        this.magicStats = magicStats;
        this.specialInfo = specialInfo;
    }

    final ZColor minColorToEquip;
    final ZActionType actionType;
    final boolean needsReload;
    final boolean canTwoHand;
    final boolean attackIsNoisy;
    final boolean openDoorsIsNoisy;
    final ZWeaponStat meleeStats;
    final ZWeaponStat rangedStats;
    final ZWeaponStat magicStats;
    final String specialInfo;

    public List<ZWeaponStat> getStats() {
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

    @Override
    public ZActionType getActionType() {
        return actionType;
    }

    public boolean isFire() {
        switch (this) {
            case INFERNO:
            case FIREBALL:
                return true;
        }
        return false;
    }

    public ZSkill [] getSkills() {
        switch (this) {
            case VAMPIRE_CROSSBOW:
                return Utils.toArray(ZSkill.Roll_6_plus1_die_Combat, ZSkill.Roll_6_Plus1_Damage);
        }

        return null;
    }
}
