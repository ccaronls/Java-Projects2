package cc.lib.risk;

import java.util.ArrayList;
import java.util.List;

import cc.lib.board.BCell;
import cc.lib.game.Utils;

/**
 * Created by Chris Caron on 9/13/21.
 */
public class RiskCell extends BCell {

    static {
        addAllFields(RiskCell.class);
    }

    List<Integer> connectedCells = new ArrayList<>();
    Region region;
    Army occupier;
    int numArmies = 0;
    int movableTroops=0;

    void reset() {
        occupier = null;
        numArmies = 0;
    }

    public RiskCell() {}

    RiskCell(List<Integer> verts) {
        super(verts);
    }

    public List<Integer> getConnectedCells() {
        return connectedCells;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Army getOccupier() {
        return occupier;
    }

    public void setOccupier(Army occupier) {
        this.occupier = occupier;
    }

    public int getNumArmies() {
        return numArmies;
    }

    public void setNumArmies(int numArmies) {
        this.numArmies = numArmies;
    }

    public List<Integer> getAllConnectedCells() {
        return Utils.mergeLists(connectedCells, Utils.map(getAdjCells(), c -> c));
    }
}
