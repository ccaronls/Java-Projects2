package cc.lib.risk;

import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Pair;
import cc.lib.utils.Reflector;

/**
 * Created by Chris Caron on 9/13/21.
 */
public class RiskPlayer extends Reflector<RiskPlayer> {

    static {
        addAllFields(RiskPlayer.class);
    }

    Army army;
    private int armiesToPlace;
    private int initialArmiesToPlace;
    @Omit
    private Pair<Integer, Integer> bestMove = null;

    public RiskPlayer() {}

    public RiskPlayer(Army army) {
        this.army = army;
    }

    public Army getArmy() {
        return army;
    }

    public int getArmiesToPlace() {
        return armiesToPlace;
    }

    public void setArmiesToPlace(int armies) {
        armiesToPlace = initialArmiesToPlace = armies;
    }

    public int getInitialArmiesToPlace() {
        return initialArmiesToPlace;
    }

    void decrementArmy() {
        armiesToPlace--;
    }

    public Integer pickTerritoryToClaim(RiskGame game, List<Integer> options) {
        //return Utils.randItem(options);
        return Utils.search(options, idx -> {
            RiskCell cell = game.getBoard().getCell(idx);
            return (long)cell.getRegion().extraArmies + cell.getAllConnectedCells().size();
        });
    }

    public Integer pickTerritoryForArmy(RiskGame game, List<Integer> options, int remaining, int start) {
        return Utils.search(options, idx -> {
            RiskCell cell = game.getBoard().getCell(idx);
            cell.numArmies ++;
            try {
                return evaluateBoard(game.getBoard(), army);
            } finally {
                cell.numArmies--;
            }
        });

        // we want at least 2 troops on a cell that is adjacent to an opponent

        // choose the territory that has the highest number of opponent armies adjacent

/*
        return Utils.search(options, idx -> {
            RiskCell cell = game.getBoard().getCell(idx);
            List<RiskCell> adj = game.getBoard().getAdjacentCells(cell);

            //int numArmies = Utils.sumInt(adj, c -> c.occupier == army ? c.numArmies : 0);
            //return Utils.sumInt(adj, c -> c.occupier == army ? 0 : c.numArmies) - numArmies;
        });*/
    }

    public Integer pickTerritoryForNeutralArmy(RiskGame game, List<Integer> options) {
        // place a neutral around opponents
        return Utils.randItem(options);
    }

    public Integer pickTerritoryToAttackFrom(RiskGame game, List<Integer> options) {
        // pick a territory from which we have many armies
        return Utils.search(options, integer -> game.getBoard().getCell(integer).getNumArmies());
    }

    public Integer pickTerritoryToAttack(RiskGame game, int attackingCellIdx, List<Integer> options) {
        RiskCell attackingCell = game.getBoard().getCell(attackingCellIdx);
        // pick cell with fewest number of troops compared to ours
        return Utils.search(options, integer -> attackingCell.getNumArmies() - game.getBoard().getCell(integer).getNumArmies());
    }

    public Integer pickTerritoryToMoveFrom(RiskGame game, List<Integer> options) {
        if (bestMove != null) {
            return bestMove.first;
        }
        return Utils.search(options, integer -> game.getBoard().getCell(integer).getNumArmies());
    }

    public Integer pickTerritoryToMoveTo(RiskGame game, int sourceCellIdx, List<Integer> options) {
        if (bestMove != null) {
            return bestMove.second;
        }
        return Utils.search(options, integer -> -game.getBoard().getCell(integer).getNumArmies());
    }

    public Action pickAction(RiskGame game, List<Action> actions, String msg) {
        Action best = null;
        for (Action a : actions) {
            switch (a) {
                case ATTACK: {
                    // if we have any cells with 3 or more that are adjacent to something then choose yes
                    if (0 < Utils.count(game.getBoard().getTerritories(getArmy()), idx -> {
                        RiskCell cell = game.board.getCell(idx);
                        if (cell.getNumArmies() < 3)
                            return false;
                        for (RiskCell adj : game.getBoard().getAdjacentCells(cell)) {
                            if (adj.getOccupier() != getArmy()) {
                                if (adj.getNumArmies() < cell.getNumArmies())
                                    return true;
                            }
                        }
                        return false;
                    })) {
                        return a;
                    }
                    break;
                }
                case MOVE: {
                    // compute all move pairs
                    RiskBoard copy = game.getBoard();
                    long value = evaluateBoard(copy, army);
                    bestMove = null;

                    copy.copyFrom(game.getBoard());
                    List<Integer> territories = game.getBoard().getTerritories(getArmy());
                    for (Pair<Integer, Integer> p : game.getMovePairOptions(territories, army)) {
                        copy.moveArmies(p.first, p.second, 1);
                        long v = evaluateBoard(copy, army);
                        if (v > value) {
                            bestMove = p;
                            value = v;
                        }
                        copy.moveArmies(p.second, p.first, 1);
                    }
                    if (bestMove != null) {
                        return a;
                    }
                    break;
                }

                case THREE_ARMIES:
                case TWO_ARMIES:
                case ONE_ARMY:
                    if (best == null || best.ordinal() < a.ordinal())
                        best = a;
                    break;
                case END:
                    if (bestMove == null)
                        return a;
                case CANCEL:
            }
        }

        return best == null ? Utils.randItem(actions) : best;
    }

    long evaluateBoard(RiskBoard b, Army army) {

        // things that are good:
        //   owning a continent
        //   having troops with low numbers surrounded by own troops with high numbers
        //   having more troops in territories adjacent to other armies
        //   we want to avoid having too many troops in one place so scale value logarithmically

        long score = 0;
        List<Integer> terr = b.getTerritories(army);

        for (int idx : terr) {
            RiskCell cell = b.getCell(idx);
            List<RiskCell> adj = b.getAdjacentCells(cell);
            int numOwnArmiesAdj = 0;
            int numEnemyArmiesAdj = 0;
            for (RiskCell c : adj) {
                if (c.occupier == cell.occupier) {
                    numOwnArmiesAdj += c.numArmies;
                } else {
                    score += cell.numArmies - c.numArmies;
                    numEnemyArmiesAdj += c.numArmies;
                }
            }
            if (numEnemyArmiesAdj == 0) {
                score -= cell.numArmies; // if we are surrounded by only friendlies then less is more
            }
        }

        return score;
    }


}
