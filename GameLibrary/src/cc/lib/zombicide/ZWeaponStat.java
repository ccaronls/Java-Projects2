package cc.lib.zombicide;

public class ZWeaponStat {

    int dieRollToOpenDoor; // 0 means cannot open. 1 means can open without dice
    int minRange;
    int maxRange;
    int numDice;
    int dieRollToHit;
    int damagePerHit;

    public ZWeaponStat() {
        this(0, 0, 0, 0, 0, 0);
    }

    public ZWeaponStat(int dieRollToOpenDoor, int minRange, int maxRange, int numDice, int dieRollToHit, int damagePerHit) {
        this.dieRollToOpenDoor = dieRollToOpenDoor;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.numDice = numDice;
        this.dieRollToHit = dieRollToHit;
        this.damagePerHit = damagePerHit;
    }

    ZWeaponStat copy() {
        return new ZWeaponStat(dieRollToOpenDoor, minRange, maxRange, numDice, dieRollToHit, damagePerHit);
    }

    //public ZWeaponStat getDualWeildingStats() {
    //    return new ZWeaponStat(dieRollToOpenDoor, minRange, maxRange, numDice*2, dieRollToHit, damagePerHit);
   // }
}
