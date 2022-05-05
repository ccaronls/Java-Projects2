package cc.lib.zombicide;

import cc.lib.utils.Reflector;

public class ZWeaponStat extends Reflector<ZWeaponStat> {

    static {
        addAllFields(ZWeaponStat.class);
    }

    int dieRollToOpenDoor; // 0 means cannot open. 1 means can open without dice
    int minRange;
    int maxRange;
    int numDice;
    int dieRollToHit;
    int damagePerHit;
    ZAttackType attackType;
    ZActionType actionType;

    public ZWeaponStat() {
        this(null, null, 0, 0, 0, 0, 0, 0);
    }

    public ZWeaponStat(ZActionType actionType, ZAttackType attackType, int dieRollToOpenDoor, int minRange, int maxRange, int numDice, int dieRollToHit, int damagePerHit) {
        this.actionType = actionType;
        this.attackType = attackType;
        this.dieRollToOpenDoor = dieRollToOpenDoor;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.numDice = numDice;
        this.dieRollToHit = dieRollToHit;
        this.damagePerHit = damagePerHit;
    }

    ZWeaponStat copy() {
        return new ZWeaponStat(actionType, attackType, dieRollToOpenDoor, minRange, maxRange, numDice, dieRollToHit, damagePerHit);
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

    public ZActionType getActionType() { return actionType; }
}
