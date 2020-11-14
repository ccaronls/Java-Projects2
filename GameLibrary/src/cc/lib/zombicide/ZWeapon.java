package cc.lib.zombicide;

public enum ZWeapon implements ZEquipment {

    // DAGGER get extra die roll when 2 handed with another weapon

    // MELEE
    DAGGER(false, true, false, true, new ZWeaponStat(4, 0, 0, 1, 4, 1), null, null),
    AXE(false, true, false, true, new ZWeaponStat(1, 0, 0, 1, 4, 1), null, null),
    HAMMER(false, false, false, true, new ZWeaponStat(4, 0, 0, 1, 3, 2), null, null),
    SHORT_SWORD(false, true, false, true, new ZWeaponStat(4, 0, 0, 1, 4, 1), null, null),
    SWORD(false, true, false, true, new ZWeaponStat(4, 0, 0, 2, 4, 1), null, null),
    GREAT_SWORD(false, false, false, true, new ZWeaponStat(5, 0, 0, 5, 5, 1), null, null),
    // BOWS
    SHORT_BOW(false, false, false, false, null, new ZWeaponStat(0, 0, 1, 1, 3, 1), null),
    LONG_BOW(false, false, false, false, null, new ZWeaponStat(0, 1, 3, 1, 3, 1), null),
    // CROSSBOWS
    CROSSBOW(true, false, false, false, null, new ZWeaponStat(0, 1, 2, 2, 4,2), null),
    REPEATING_CROSSBOW(true, true, false, false, null, new ZWeaponStat(0, 0, 1, 3, 5, 1), null),
    HAND_CROSSBOW(true, true, false, false, null, new ZWeaponStat(0, 0, 3, 2, 3, 1), null),
    ORCISH_CROSSBOW(true, false, false, false, new ZWeaponStat(0, 0, 0, 2, 3, 3), new ZWeaponStat(0, 1, 2, 2, 3, 2), null),

    // MAGIC
    DEATH_STRIKE(false, true, true, false, null, null, new ZWeaponStat(0, 0, 1, 1, 4, 2)),
    // TODO: +1 damagae on a die roll 6
    DISINTEGRATE(false, true, true, false, null, null, new ZWeaponStat(0, 0, 1, 3, 5, 1)),
    EARTHQUAKE(false, true, true, false, null, null, new ZWeaponStat(0, 0, 1, 3, 4, 1)),
    FIREBALL(false, true, true, false, null, null, new ZWeaponStat(0, 0, 1, 3, 4, 1)),
    MANA_BLAST(false, true, true, false, null, null, new ZWeaponStat(0, 0, 2, 1, 4, 1)),
    INFERNO(false, true, true, false, null, null, new ZWeaponStat(0 ,0 ,1, 4, 4, 2)),
    LIGHTNING_BOLT(false, true, true, false, null, null, new ZWeaponStat(0 ,0 ,3, 1, 4, 1)),

    // SPECIAL
    BREAK_IN(false, false, false, false, new ZWeaponStat(1, 0, 0, 0, 0, 0), null, null),
    ;


    ZWeapon(boolean needsReload, boolean canTwoHand, boolean attckIsNoisy, boolean openDoorsIsNoisy, ZWeaponStat meleeStats, ZWeaponStat rangedStats, ZWeaponStat magicStats) {
        this.needsReload = needsReload;
        this.canTwoHand = canTwoHand;
        this.attckIsNoisy = attckIsNoisy;
        this.openDoorsIsNoisy = openDoorsIsNoisy;
        this.meleeStats = meleeStats;
        this.rangedStats = rangedStats;
        this.magicStats = magicStats;
    }

    final boolean needsReload;
    final boolean canTwoHand;
    final boolean attckIsNoisy;
    final boolean openDoorsIsNoisy;
    final ZWeaponStat meleeStats;
    final ZWeaponStat rangedStats;
    final ZWeaponStat magicStats;

    @Override
    public boolean canOpenDoor() {
        return meleeStats != null && meleeStats.dieRollToOpenDoor > 0;
    }

    @Override
    public ZEquipSlotType getSlotType() {
        return ZEquipSlotType.HAND;
    }

    @Override
    public boolean isMelee() {
        return meleeStats != null;
    }

    @Override
    public boolean isRanged() {
        return rangedStats != null;
    }

    @Override
    public boolean isMagic() {
        return magicStats != null;
    }

    @Override
    public boolean canEquip() {
        return true;
    }
}
