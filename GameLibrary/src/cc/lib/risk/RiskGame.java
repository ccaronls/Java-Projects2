package cc.lib.risk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.GException;
import cc.lib.utils.Pair;
import cc.lib.utils.Reflector;
import cc.lib.utils.Table;

/**
 * Created by Chris Caron on 9/13/21.
 */
public class RiskGame extends Reflector<RiskGame> {

    static {
        addAllFields(RiskGame.class);
    }

    static final Logger log = LoggerFactory.getLogger(RiskGame.class);

    public final static int MIN_TROOPS_PER_TURN = 3;

    List<RiskPlayer> players = new ArrayList<>();
    RiskBoard board = new RiskBoard();
    int currentPlayer = 0;
    State state = State.INIT;

    public void clear() {
        players.clear();
        board.reset();
        currentPlayer = 0;
        state = State.INIT;
    }

    public final synchronized void runGame() {
        log.debug("runGame ENTER");

        log.verbose("Run game state: %s", state);

        if (players.size() < 2)
            throw new GException("Need at least 2 players");
        if (state != State.INIT && getWinner() != null) {
            state = State.GAME_OVER;
        }

        RiskPlayer cur = getCurrentPlayer();
        List<Integer> territories = board.getTerritories(cur.army);
        List<Action> actions = new ArrayList<>();
        List<Integer> stageable = null;

        switch (state) {
            case INIT: {
                // collect the territories
//                if (board.getNumCells() != 42)
  //                  throw new GException("Expecting 42 territories but found " + board.getNumCells());
                LinkedList<RiskCell> terr = new LinkedList<>();
                for (RiskCell cell : board.getCells()) {
                    if (cell.getRegion() == null)
                        throw new GException("Invalid board has a cell without a region: " + cell);
                    cell.setOccupier(null);
                    cell.setNumArmies(0);
                    terr.add(cell);
                }
                Utils.shuffle(terr);
                int numEach = terr.size() / players.size();
                int infantryEach = 0;
                if (players.size() == 2) {
                    // special case where we have a neutral player
                    numEach = terr.size()/3;
                }
                infantryEach = ((board.getNumCells()+10)/10) * 10 - 5 * players.size();
                for (RiskPlayer pl : players) {
                    pl.setArmiesToPlace(infantryEach);
                }
                if (players.size() > 2) {
                    state = State.CHOOSE_TERRITORIES;
                    break;
                }
                for (int i = 0; i < numEach; i++) {
                    for (RiskPlayer pl : players) {
                        RiskCell cell = terr.removeFirst();
                        cell.setOccupier(pl.army);
                        cell.setNumArmies(1);
                        pl.decrementArmy();
                        Utils.assertTrue(pl.getArmiesToPlace() > 0);
                    }
                }
                while (terr.size() > 0) {
                    RiskCell cell = terr.removeFirst();
                    cell.setOccupier(Army.NEUTRAL);
                    cell.setNumArmies(1);
                }
                state = State.PLACE_ARMY1;
                break;
            }

            case CHOOSE_TERRITORIES: {
                List<Integer> unclaimed = Utils.filter(Utils.getRangeIterator(0, board.getNumCells()-1), idx -> {
                    RiskCell cell = board.getCell(idx);
                    return cell.getOccupier() == null || cell.getNumArmies() == 0;
                });

                if (unclaimed.size() == 0) {
                    state = State.PLACE_ARMY;
                } else {
                    Integer picked = cur.pickTerritoryToClaim(this, unclaimed);
                    if (picked != null) {
                        Utils.assertTrue(unclaimed.contains(picked));
                        RiskCell cell = board.getCell(picked);
                        Utils.assertTrue(cell.occupier == null);
                        cell.setOccupier(cur.army);
                        cell.setNumArmies(1);
                        cur.decrementArmy();
                        nextPlayer();
                    }
                }

                break;
            }

            case PLACE_ARMY1:
            case PLACE_ARMY2:
            case PLACE_ARMY:
            case BEGIN_TURN_PLACE_ARMY: {
                if (cur.getArmiesToPlace() > 0) {
                    int startArmiesToPlace = cur.getInitialArmiesToPlace();
                    int remainingArmiesToPlace = startArmiesToPlace - cur.getArmiesToPlace();
                    Integer picked = cur.pickTerritoryForArmy(this, territories, remainingArmiesToPlace, startArmiesToPlace);
                    if (picked != null) {
                        Utils.assertTrue(territories.contains(picked));
                        onPlaceArmy(cur.getArmy(), picked);
                        RiskCell cell = board.getCell(picked);
                        cell.numArmies++;
                        cur.decrementArmy();
                        switch (state) {
                            case PLACE_ARMY1:
                                state = State.PLACE_ARMY2;
                                break;
                            case PLACE_ARMY2:
                                state = State.PLACE_NEUTRAL;
                                break;
                            case PLACE_ARMY:
                                nextPlayer();
                                break;
                            case BEGIN_TURN_PLACE_ARMY:
                                break;
                            default:
                                throw new GException("Unhandled case: " + state);
                        }
                    }
                } else if (state == State.BEGIN_TURN_PLACE_ARMY) {
                    state = State.CHOOSE_MOVE;
                } else {
                    if (Utils.count(players, player -> player.getArmiesToPlace() > 0) == 0) {
                        state = State.BEGIN_TURN;
                    } else {
                        nextPlayer();
                    }
                }
                break;
            }

            case PLACE_NEUTRAL: {
                territories = board.getTerritories(Army.NEUTRAL);
                Integer picked = cur.pickTerritoryForNeutralArmy(this, territories);
                if (picked != null) {
                    Utils.assertTrue(territories.contains(picked));
                    RiskCell cell = board.getCell(picked);
                    onPlaceArmy(Army.NEUTRAL, picked);
                    cell.numArmies++;
                    state = State.PLACE_ARMY1;
                    nextPlayer();
                }
                break;
            }

            case BEGIN_TURN: {
                if (territories.size() == 0) {
                    nextPlayer();
                } else {
                    onBeginTurn(cur.army);
                    int numArmies = computeTroopsPerTurn(cur.army);
                    cur.setArmiesToPlace(numArmies);
                    state = State.BEGIN_TURN_PLACE_ARMY;
                }
                break;
            }


            case CHOOSE_MOVE: {
                // See which territories can attack an adjacent

                stageable = Utils.filter(territories, idx -> {
                    RiskCell cell = board.getCell(idx);
                    if (cell.numArmies < 2)
                        return false;
                    for (int adj : board.getConnectedCells(cell)) {
                        if (board.getCell(adj).getOccupier() != cur.army) {
                            return true;
                        }
                    }
                    return false;
                });
                if (stageable.size() > 0) {
                    actions.add(Action.ATTACK);
                }
                for (int idx : territories) {
                    RiskCell cell = board.getCell(idx);
                    cell.movableTroops = cell.numArmies-1;
                }
            }

            case CHOOSE_MOVE_NO_ATTACK:

                // See which territories can move into an adjacent
                List<Integer> moveable = getMoveSourceOptions(territories, cur.getArmy());
                if (moveable.size() > 0) {
                    actions.add(Action.MOVE);
                }

                if (actions.size() > 0)
                    actions.add(Action.END);
                else {
                    state = State.BEGIN_TURN;
                    nextPlayer();
                    break;
                }
                onBeginMove();
                Action action = cur.pickAction(this, actions, cur.army + " Choose your Move");
                if (action != null) {
                    Utils.assertTrue(actions.contains(action));
                    switch (action) {
                        case ATTACK: {
                            Utils.assertTrue(stageable!=null);
                            Integer start = cur.pickTerritoryToAttackFrom(this, stageable);
                            if (start != null) {
                                Utils.assertTrue(stageable.contains(start));
                                onStartAttackTerritoryChosen(start);
                                RiskCell cell = board.getCell(start);
                                List<Integer> options = Utils.filter(board.getConnectedCells(cell), idx ->
                                    board.getCell(idx).getOccupier() != cur.army
                                );
                                if (options.size() == 0)
                                    throw new GException("Invalid stagable");
                                Integer end = cur.pickTerritoryToAttack(this, start, options);
                                if (end != null) {
                                    Utils.assertTrue(options.contains(end));
                                    onEndAttackTerritoryChosen(start, end);
                                    performAttack(start, end);
                                }
                            }
                            break;
                        }
                        case MOVE: {
                            Integer start = cur.pickTerritoryToMoveFrom(this, moveable);
                            if (start != null) {
                                Utils.assertTrue(moveable.contains(start));
                                onStartMoveTerritoryChosen(start);
                                RiskCell cell = board.getCell(start);
                                List<Integer> options = Utils.filter(board.getConnectedCells(cell), idx ->
                                        board.getCell(idx).getOccupier() == cur.army
                                );
                                if (options.size() == 0)
                                    throw new GException("Invalid movable");
                                while (cell.movableTroops > 0) {
                                    Integer end = cur.pickTerritoryToMoveTo(this, start, options);
                                    if (end == null) {
                                        break;
                                    }
                                    Utils.assertTrue(options.contains(end));
                                    onMoveTroops(start, end, 1);
                                    performMove(start, end);
                                    state = State.CHOOSE_MOVE_NO_ATTACK;
                                }
                            }

                            break;
                        }
                        case END:
                            state = State.BEGIN_TURN;
                            nextPlayer();
                            break;
                    }
                }

            break;

            case GAME_OVER:
                onGameOver(getWinner());
                state = State.DONE;
                break;

            case DONE:
                break;
        }

        log.debug("runGame EXIT");

    }

    protected void onPlaceArmy(Army army, int cellIdx) {
        log.debug("%s placing an army on %s", army, getBoard().getCell(cellIdx).getRegion());
    }

    protected void onGameOver(Army winner) {}

    protected void onBeginMove() {}

    protected void onBeginTurn(Army army) {}

    protected void onChooseMove(Army army) {}

    protected void onStartAttackTerritoryChosen(int cellIdx) {}

    protected void onEndAttackTerritoryChosen(int startIdx, int endIdx) {}

    protected void onStartMoveTerritoryChosen(int cellIdx) {}

    protected void onMoveTroops(int startIdx, int endIdx, int numTroops) {}

    List<Integer> getMoveSourceOptions(List<Integer> territories, Army army) {
        return Utils.filter(territories, idx -> {
            RiskCell cell = board.getCell(idx);
            if (cell.movableTroops < 1)
                return false;
            for (int adj : board.getConnectedCells(cell)) {
                if (board.getCell(adj).getOccupier() == army) {
                    return true;
                }
            }
            return false;
        });
    }

    List<Pair<Integer,Integer>> getMovePairOptions(List<Integer> territories, Army army) {
        List<Pair<Integer, Integer>> pairs = new ArrayList<>();
        for (int idx : territories) {
            RiskCell cell = getBoard().getCell(idx);
            if (cell.movableTroops > 0) {
                for (int adjIdx : board.getConnectedCells(cell)) {
                    RiskCell adj = board.getCell(adjIdx);
                    if (adj.getOccupier() == cell.getOccupier()) {
                        pairs.add(new Pair(idx, adjIdx));
                    }
                }
            }
        }
        return pairs;
    }

    int computeTroopsPerTurn(Army army) {
        //int numTerritoriesOccupied = board.getTerritories(getCurrentPlayer().army).size();
        //onMessage(army, "Occupies " + numTerritoriesOccupied + " territories");
        int numArmies = board.getTerritories(army).size() / 3;
        // add any extras for holding complete continents
        //log.debug("%s gets %d armies to position on the board", army, numArmies);
        for (Region r : Region.values()) {
            List<Integer> cells = board.getTerritories(r);
            if (Utils.all(cells, c -> board.getCell(c).getOccupier() == army)) {
                numArmies += r.extraArmies;
//                onMessage(army, "Gets " + r.extraArmies + " extra armies for holding " + r.name());
            }
        }
        return Math.max(MIN_TROOPS_PER_TURN, numArmies);
    }

    private void performMove(int startIdx, int endIdx) {
        RiskCell start = board.getCell(startIdx);
        RiskCell end   = board.getCell(endIdx);
        start.movableTroops--;
        Utils.assertTrue(start.movableTroops >= 0);
        start.numArmies--;
        Utils.assertTrue(start.numArmies > 0);
        end.numArmies++;

    }

    private void performAttack(int startIdx, int endIdx) {
        RiskCell start = board.getCell(startIdx);
        RiskCell end   = board.getCell(endIdx);

        if (end.numArmies <= 0)
            throw new GException("End has no armies!");

        int maxAttacking = start.numArmies-1;
        List<Action> options = new ArrayList<>();
        options.add(Action.ONE_ARMY);
        if (maxAttacking > 1) {
            options.add(Action.TWO_ARMIES);
        }
        if (maxAttacking > 2) {
            options.add(Action.THREE_ARMIES);
        }
        options.add(Action.CANCEL);

        Action numToAttack = getCurrentPlayer().pickAction(this, options, getCurrentPlayer().army + " Choose Number of Armies to attack");
        if (numToAttack != null) {
            if (end.numArmies <= 0)
                throw new GException("End has no armies!");
            int numAttacking = numToAttack.getArmies();
            if (numAttacking <= 0)
                return;
            int [] attackingDice = new int[numAttacking];
            int [] defendingDice = new int[Math.min(2, end.numArmies)];
            boolean [] result = new boolean[Math.min(attackingDice.length, defendingDice.length)];
            rollDice(attackingDice);
            rollDice(defendingDice);
            int numToOccupy = attackingDice.length;
            for (int i=0; i<result.length; i++) {
                if (attackingDice[i] > defendingDice[i]) {
                    result[i] = true;
                } else {
                    numToOccupy--;
                }
            }
            onDiceRolled(start.getOccupier(), attackingDice, end.getOccupier(), defendingDice, result);
            int numStartArmiesLost=0;
            int numEndArmiesLost=0;
            for (int i=0; i<result.length; i++) {
                if (result[i]) {
                    numEndArmiesLost++;
                } else {
                    numStartArmiesLost++;
                }
            }
            start.numArmies -= numStartArmiesLost;
            end.numArmies -= numEndArmiesLost;

            Utils.assertTrue(start.numArmies > 0);
            Utils.assertTrue(end.numArmies >= 0);

            onArmiesDestroyed(startIdx, numStartArmiesLost, endIdx, numEndArmiesLost);

            if (end.numArmies < 0)
                throw new GException("Cannot have negative armies");
            if (end.numArmies == 0) {
                Utils.assertTrue(numToOccupy > 0);
                start.setNumArmies(start.getNumArmies() - numToOccupy);
                Utils.assertTrue(start.getNumArmies() > 0);
                onMoveTroops(startIdx, endIdx, numToOccupy);
                end.setOccupier(start.getOccupier());
                end.setNumArmies(numToOccupy);
                onAtatckerGainedTerritory(end.occupier, endIdx);
                if (getWinner() == null && Utils.all(board.getTerritories(end.region), idx -> board.getCell(idx).occupier == start.occupier)) {
                    onAttackerGainedRegion(end.occupier, end.region);
                }
            }
        }
    }

    protected void onDiceRolled(Army attacker, int [] attackingDice, Army defender, int [] defendingDice, boolean [] result) {
        List<Integer> aDice = new ArrayList<>();
        for (int a : attackingDice)
            aDice.add(a);
        List<Integer> dDice = new ArrayList<>();
        for (int d : defendingDice)
            dDice.add(d);
        List<String> bResult = new ArrayList<>();
        for (boolean b : result)
            bResult.add(b ? "<--" : "-->");

        Table table = new Table()
                .addColumn(attacker.name(), aDice)
                .addColumn("", bResult)
                .addColumn(defender.name(), dDice);
        log.info("Result From Dice Roll\n" + table);
    }

    protected void onArmiesDestroyed(int attackerIdx, int attackersLost, int defenderIdx, int defendersLost) {
        RiskCell attacker = getBoard().getCell(attackerIdx);
        RiskCell defender = getBoard().getCell(defenderIdx);
        log.info("%s has lost %d armies and %s has lost %d armies", attacker.occupier, attackersLost, defender.occupier, defendersLost);
    }

    protected void onAtatckerGainedTerritory(Army attacker, int cellIdx) {
        log.info("%s: Gained Territory for %s", attacker, board.getCell(cellIdx).region);
    }

    protected void onAttackerGainedRegion(Army attacker, Region region) {
        log.info("%s: Gained Region %s for +%d troops", attacker, region, region.extraArmies);
    }

    private void rollDice(int [] dice) {
        for (int i=0; i<dice.length; i++) {
            dice[i] = Utils.rand() % 6 + 1;
        }
        Arrays.sort(dice);
        Utils.reverse(dice);
    }

    protected void onMessage(Army army, String msg) {
        log.info("%s: %s", army, msg);
    }

    private void nextPlayer() {
        currentPlayer = (currentPlayer+1) % players.size();
    }

    public RiskPlayer getCurrentPlayer() {
        if (currentPlayer >= 0 && currentPlayer < players.size())
            return players.get(currentPlayer);
        return null;
    }

    public RiskPlayer getPlayer(int playerNum) {
        return players.get(playerNum);
    }

    public Army getWinner() {
        Army army = getBoard().getCell(0).getOccupier();
        if (army != null && Utils.all(Utils.getRangeIterator(1, getBoard().getNumCells()-1), idx -> {
            RiskCell cell = getBoard().getCell(idx);
            return cell.occupier == army || cell.occupier == Army.NEUTRAL;
        })) {
            return army;
        }
        return null;
    }

    public RiskPlayer getPlayerOrNull(Army army) {
        for (RiskPlayer rp : players) {
            if (rp.army == army)
                return rp;
        }
        return null;
    }

    RiskPlayer getPlayer(Army army) {
        RiskPlayer pl = getPlayerOrNull(army);
        if (pl != null)
            return pl;
        throw new GException("No player for army: " + army);
    }

    public int getNumPlayers() {
        return players.size();
    }

    public RiskBoard getBoard() {
        return board;
    }

    public void addPlayer(RiskPlayer player) {
        if (player.army == null)
            throw new NullPointerException("Player cannot have null army");
        if (getPlayerOrNull(player.army) != null)
            throw new GException("Player with army " + player.army + " already exists");
        players.add(player);
    }

    public boolean isDone() {
        return state == State.DONE;
    }

    public Table getSummary() {
        List<String> header = new ArrayList<>();
        header.add("Army");
        header.add("Troops");
        for (Region r : Region.values()) {
            header.add(Utils.toPrettyString(r.name()).replace(' ', '\n') + " +" + r.extraArmies);
        }
        Table table = new Table(header);
        List<Army> armies = Utils.map(players, p -> p.army);
        if (armies.size() < 3)
            armies.add(Army.NEUTRAL);
        for (Army army : armies) {
            List<Integer> l = board.getTerritories(army);
            List<String> row = new ArrayList<>();
            row.add(army.name());
            int troops = Utils.sumInt(board.getAllTerritories(), t -> t.getOccupier() == army ? t.getNumArmies() : 0);
            if (army == Army.NEUTRAL)
                row.add(String.valueOf(troops));
            else if (l.size() > 0)
                row.add(String.format("%d +%d", troops, computeTroopsPerTurn(army)));
            else
                row.add("-----");
            for (Region r : Region.values()) {
                List<Integer> l2 = board.getTerritories(r);
                int numOwned = Utils.count(l2, idx -> l.contains(idx));
                int percent = numOwned * 100 / l2.size();
                row.add(String.format("%d%%", percent));
            }
            table.addRowList(row);
        }
        return table;



        /*
        Table table = new Table("Army", "Num Armies", "Num Territories");
        for (RiskPlayer pl : players) {
            List<Integer> territories = board.getTerritories(pl.army);
            long numArmies = Utils.sum(territories, idx -> (long)board.getCell(idx).numArmies);
            table.addRow(pl.army.name(), numArmies, territories.size());
        }
        return table;
         */
    }
}
