package cc.lib.zombicide;

public enum ZWeapon implements ZEquipment {
    // DAGGER get extra die roll when 2 handed with another weapon
    DAGGER(true, false, true, new ZWeaponStat(4, 0, 0, 1, 4, 1), null, null),
    AXE(true, false, true, new ZWeaponStat(1, 0, 0, 1, 4, 1), null, null),
    HAMMER(false, false, true, new ZWeaponStat(4, 0, 0, 1, 3, 2), null, null),
    MANA_BLAST(true, true, false, null, null, new ZWeaponStat(0, 0, 2, 1, 4, 1)),
    SHORT_BOW(false, false, false, null, new ZWeaponStat(0, 0, 1, 1, 3, 1), null),
    SHORT_SWORD(true, false, true, new ZWeaponStat(4, 0, 0, 1, 4, 1), null, null),
    FIREBALL(true, true, false, null, null, new ZWeaponStat(0, 0, 1, 3, 4, 1)),
    INFERNO(true, true, false, null, null, new ZWeaponStat(0 ,0 ,1, 4, 4, 2)),
    CROSSBOW(false, false, false, null, new ZWeaponStat(0, 1, 2, 2, 4,2), null),
    ORCISH_CROSSBOW(false, false, false, new ZWeaponStat(0, 0, 0, 2, 3, 3), new ZWeaponStat(0, 1, 2, 2, 3, 2), null);

    ZWeapon(boolean canTwoHand, boolean attckIsNoisy, boolean openDoorsIsNoisy, ZWeaponStat meleeStats, ZWeaponStat rangedStats, ZWeaponStat magicStats) {
        this.canTwoHand = canTwoHand;
        this.attckIsNoisy = attckIsNoisy;
        this.openDoorsIsNoisy = openDoorsIsNoisy;
        this.meleeStats = meleeStats;
        this.rangedStats = rangedStats;
        this.magicStats = magicStats;
    }

    final boolean canTwoHand;
    final boolean attckIsNoisy;
    final boolean openDoorsIsNoisy;
    private final ZWeaponStat meleeStats;
    private final ZWeaponStat rangedStats;
    private final ZWeaponStat magicStats;

    @Override
    public ZEquipSlot getSlot() {
        return ZEquipSlot.HAND;
    }

    @Override
    public boolean canOpenDoor() {
        return meleeStats != null && meleeStats.dieRollToOpenDoor > 0;
    }

    public ZWeaponStat getMeleeStats(ZCharacter c) {
        if (meleeStats == null)
            return null;
        if (this == DAGGER && c.getWeapons().length == 2) {
            return meleeStats.getDualWeildingStats();
        } else if (c.isDualWeilding()) {
            return meleeStats.getDualWeildingStats();
        }
        return meleeStats;
    }

    public ZWeaponStat getRangedStats(ZCharacter c) {
        if (rangedStats == null)
            return null;
        if (c.isDualWeilding()) {
            return rangedStats.getDualWeildingStats();
        }
        return rangedStats;
    }

    public ZWeaponStat getMagicStats(ZCharacter c) {
        if (magicStats == null)
            return null;
        if (c.isDualWeilding()) {
            return magicStats.getDualWeildingStats();
        }
        return magicStats;
    }
}
