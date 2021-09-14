package cc.lib.risk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.lib.board.BCell;
import cc.lib.board.CustomBoard;
import cc.lib.game.Utils;

/**
 * Created by Chris Caron on 9/13/21.
 */
public class RiskBoard extends CustomBoard {

    @Override
    protected BCell newCell(List<Integer> pts) {
        return new RiskCell(pts);
    }

    public List<Integer> getConnectedCells(RiskCell cell) {
        Set<Integer> all = new HashSet<>(cell.getConnectedCells());
        all.addAll(cell.getAdjCells());
        return new ArrayList<>(all);
    }

    public List<RiskCell> getAllTerritories() {
        return Utils.map(getCells(), cell -> (RiskCell)cell);
    }

    public List<Integer> getTerritories(Army army) {
        return Utils.filter(Utils.getRangeIterator(0, getNumCells()-1), idx->getRiskCell(idx).getOccupier()==army);
    }

    public List<Integer> getTerritories(Region region) {
        return Utils.filter(Utils.getRangeIterator(0, getNumCells()-1), idx->getRiskCell(idx).getRegion()==region);
    }

    public RiskCell getRiskCell(int idx) {
        return (RiskCell)getCell(idx);
    }
}
