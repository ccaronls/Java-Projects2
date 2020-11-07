package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

/**
 * Zones are sets of adjacent cells that comprise rooms or streets separated by doors and walls
 */
public class ZZone {

    List<int []> cells = new ArrayList<>();

    int noiseLevel;
    boolean isSpawn;
    boolean searchable;

    public ZZone() {
    }

}
