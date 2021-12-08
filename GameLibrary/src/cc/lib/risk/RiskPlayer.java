package cc.lib.risk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Pair;
import cc.lib.utils.Reflector;

/**
 * Created by Chris Caron on 9/13/21.
 */
public class RiskPlayer extends Reflector<RiskPlayer> {

    static final Logger log = LoggerFactory.getLogger(RiskPlayer.class);

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
        int choice = Utils.search(options, idx -> {
            RiskCell cell = game.getBoard().getCell(idx);
            cell.numArmies ++;
            try {
                long value = evaluateBoard(game.getBoard(), army);
                log.debug("pickTerritoryForArmy %s idx(%d) value(%d)", army, idx, value);
                return value;
            } finally {
                cell.numArmies--;
            }
        });

        log.debug("pickTerritoryForArmy: choose cell: %d", choice);
        return choice;

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
                        for (int adjIdx : cell.getAllConnectedCells()) {
                            RiskCell adj = game.getBoard().getCell(adjIdx);
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
                    log.debug("chooseMove: %-10s value(%d)", "NONE", value);

                    //copy.copyFrom(game.getBoard());
                    List<Integer> territories = game.getBoard().getTerritories(getArmy());
                    for (Pair<Integer, Integer> p : game.getMovePairOptions(territories, army)) {
                        copy.moveArmies(p.first, p.second, 1);
                        long v = evaluateBoard(copy, army);
                        log.debug("chooseMove: %-10s value(%d)", p.first+"->"+p.second, v);
                        if (v > value) {
                            bestMove = p;
                            value = v;
                        }
                        copy.moveArmies(p.second, p.first, 1);
                    }
                    log.debug("chooseMove: best=%s", bestMove);
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

        double score = 0;
        List<Integer> terr = b.getTerritories(army);
        Set<Integer> enemies = new HashSet<>();

        double numAdj = 0;
        double adjDelta = 0;
        for (int idx : terr) {

            RiskCell cell = b.getCell(idx);
            for (int adjIdx : cell.getAllConnectedCells()) {
                RiskCell adj = b.getCell(adjIdx);
                if (adj.occupier != army) {
                    numAdj++;
                    adjDelta = cell.numArmies - adj.numArmies;
                    enemies.add(adjIdx);
                }
            }
        }

        // make the distance from enemies to our territories a minimum
        if (enemies.size() > 0) {
            /*
            GRectangle bounds = b.getBounds();
            double maxDist = bounds.getTopLeft().subEq(bounds.getBottomRight()).mag();
            double distance = 0;
            for (int idx : terr) {
                RiskCell cell= b.getCell(idx);
                Vector2D v = new Vector2D(cell);

                for (RiskCell enemy : enemies) {
                    double distNorm = v.sub(enemy).mag() / maxDist;
                    double distNormInv = 1000.0 / distNorm;
                    double delta = cell.numArmies - enemy.numArmies;
                    if (delta > 1) {
                        delta = 1 + Math.log(delta);
                    }
                    //double logDelta = Math.log(delta);
                    distance += distNormInv * delta;
                }
            }*/

            double distance = 0;
            for (int idx : terr) {
                RiskCell cell = b.getCell(idx);
                for (int adjIdx : enemies) {
                    RiskCell enemy = b.getCell(adjIdx);
                    double dist = b.getDistance(idx, adjIdx);
                    double delta = cell.numArmies - enemy.numArmies;
                    if (delta > 1) {
                        //delta = 1 + Math.log(delta);
                    }
                    if (dist > 0)
                        distance += delta / dist;
                }
            }

            score += distance;
        }

        if (numAdj > 0) {
            double factor = Math.log(adjDelta / numAdj);
            score += factor;
        }

        return Math.round(score)*100+ Utils.rand()*100;
    }


    long evaluateBoard2(RiskBoard b, Army army) {

        // things that are good:
        //   owning a continent
        //   having troops with low numbers surrounded by own troops with high numbers
        //   having more troops in territories adjacent to other armies
        //   we want to avoid having too many troops in one place so scale value logarithmically
        //   we want to minimize the delta between our armies and our opponents
        //   we want to maximize the number of cells we have advantage over

        long score = 0;
        List<Integer> terr = b.getTerritories(army);

        int numEnemiesAdjacent = 0;
        float adjacentEnemiesDelta = 0;
        for (int idx : terr) {
            RiskCell cell = b.getCell(idx);
            score += cell.getRegion().extraArmies + cell.getAllConnectedCells().size();

            int numOwnArmiesAdj = 0;
            int numEnemyArmiesAdj = 0;
            for (int adjIdx : cell.getAllConnectedCells()) {
                RiskCell c = b.getCell(adjIdx);
                if (c.occupier == cell.occupier) {
                    numOwnArmiesAdj += c.numArmies;
                } else {
                    numEnemiesAdjacent++;
                    score += (cell.numArmies - c.numArmies) / 2;
                    adjacentEnemiesDelta += cell.numArmies - c.numArmies;
                    numEnemyArmiesAdj += c.numArmies;
                }
            }
            if (numEnemyArmiesAdj == 0) {
                score -= cell.numArmies; // if we are surrounded by only friendlies then less is more
            }
        }

        if (numEnemiesAdjacent > 0) {
            score += 5 * Math.round(adjacentEnemiesDelta / (float)numEnemiesAdjacent);
        }

        return score;//*100 + Utils.rand()%100;
    }


}
