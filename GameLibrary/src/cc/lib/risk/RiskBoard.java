package cc.lib.risk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.lib.board.BEdge;
import cc.lib.board.BVertex;
import cc.lib.board.CustomBoard;
import cc.lib.game.Utils;

/**
 * Created by Chris Caron on 9/13/21.
 */
public class RiskBoard extends CustomBoard<BVertex, BEdge, RiskCell> {

    int [][] distMatrix = null;

    @Override
    protected RiskCell newCell(List<Integer> pts) {
        return new RiskCell(pts);
    }

    public void reset() {
        for (RiskCell cell : getCells()) {
            cell.reset();
        }
    }

    public List<Integer> getConnectedCells(RiskCell cell) {
        Set<Integer> all = new HashSet<>(cell.getConnectedCells());
        all.addAll(Utils.map(cell.getAdjCells(), idx -> idx));
        return new ArrayList<>(all);
    }

    public List<RiskCell> getAllTerritories() {
        return Utils.map(getCells(), cell -> cell);
    }

    public List<Integer> getTerritories(Army army) {
        return Utils.filter(Utils.getRangeIterator(0, getNumCells()-1), idx->getCell(idx).getOccupier()==army);
    }

    public List<Integer> getTerritories(Region region) {
        return Utils.filter(Utils.getRangeIterator(0, getNumCells()-1), idx->getCell(idx).getRegion()==region);

    }

    public void moveArmies(int fromCellIdx, int toCellIdx, int numArmies) {
        RiskCell from = getCell(fromCellIdx);
        Utils.assertTrue(from.numArmies > numArmies);
        RiskCell to = getCell(toCellIdx);
        Utils.assertTrue(to.numArmies > 0);
        from.numArmies -= numArmies;
        to.numArmies += numArmies;
    }

    private int [][] computeFloydWarshallDistanceMatrix() {
        final int numV = getNumCells();
        final int INF = Integer.MAX_VALUE/2 - 1;
        int [][] dist = new int[numV][numV];
        for (int i=0; i<numV; i++) {
            for (int ii=0; ii<numV; ii++) {
                dist[i][ii] = INF;
            }
            dist[i][i] = 0;
        }

        for (int cellIdx = 0; cellIdx<getNumCells(); cellIdx++) {
            RiskCell cell = getCell(cellIdx);
            for (int adjIdx : cell.getAllConnectedCells()) {
                dist[cellIdx][adjIdx] = dist[adjIdx][cellIdx] = 1;
            }
        }

        for (int k=0; k<numV; k++) {
            for (int i=0; i<numV; i++) {
                for (int j=0; j<numV; j++) {
                    dist[i][j] = Math.min(dist[i][k] + dist[k][j], dist[i][j]);
                }
            }
        }
        return dist;
    }


    public int getDistance(int fromCellIdx, int toCellIdx) {
        if (distMatrix == null) {
            distMatrix = computeFloydWarshallDistanceMatrix();
        }
        return distMatrix[fromCellIdx][toCellIdx];
    }
}
