package cc.lib.risk;

/**
 * Created by Chris Caron on 9/13/21.
 */
public enum Region {
    NORTH_AMERICA(5),
    SOUTH_AMERICA(2),
    AFRICA(3),
    EUROPE(5),
    ASIA(7),
    AUSTRALIA(2);

    Region(int extra) {
        this.extraArmies = extra;
    }

    final int extraArmies; // amount extra per turn when player has whole of region
}
