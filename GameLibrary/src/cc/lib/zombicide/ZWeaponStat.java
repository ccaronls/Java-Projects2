package cc.lib.zombicide;

public class ZWeaponStat {

    int dieRollToOpenDoor; // 0 means cannot open. 1 means can open without dice
    int minRange;
    int maxRange;
    int numDice;
    int dieRollToHit;
    int damagePerHit;
    ZAttackType attackType;

    public ZWeaponStat() {
        this(null, 0, 0, 0, 0, 0, 0);
    }

    public ZWeaponStat(ZAttackType attackType, int dieRollToOpenDoor, int minRange, int maxRange, int numDice, int dieRollToHit, int damagePerHit) {
        this.attackType = attackType;
        this.dieRollToOpenDoor = dieRollToOpenDoor;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.numDice = numDice;
        this.dieRollToHit = dieRollToHit;
        this.damagePerHit = damagePerHit;
    }

    ZWeaponStat copy() {
        return new ZWeaponStat(attackType, dieRollToOpenDoor, minRange, maxRange, numDice, dieRollToHit, damagePerHit);
    }

    public int getDamagePerHit() {
        return damagePerHit;
    }

    public int getDieRollToOpenDoor() {
        return dieRollToOpenDoor;
    }

    public int getMinRange() {
        return minRange;
    }

    public int getMaxRange() {
        return maxRange;
    }

    public int getNumDice() {
        return numDice;
    }

    public int getDieRollToHit() {
        return dieRollToHit;
    }

    public ZAttackType getAttackType() {
        return attackType;
    }
}
