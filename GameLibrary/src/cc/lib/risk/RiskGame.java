package cc.lib.risk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    public void runGame() {

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
                if (board.getNumCells() != 42)
                    throw new GException("Expecting 42 territories but found " + board.getNumCells());
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
                switch (players.size()) {
                    case 2:
                        numEach = terr.size() / 3;
                        infantryEach = 40;
                        break;
                    case 3:
                        infantryEach = 35;
                        break;
                    case 4:
                        infantryEach = 30;
                        break;
                    case 5:
                        infantryEach = 25;
                        break;
                    case 6:
                        infantryEach = 20;
                        break;
                    default:
                        throw new GException("Invalid number of players: " + players.size());

                }
                for (RiskPlayer pl : players) {
                    pl.setArmiestoPlace(infantryEach);
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
                    Integer picked = cur.pickTerritory(unclaimed, cur.army + " Pick a territory to claim");
                    if (picked != null) {
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
                    Integer picked = cur.pickTerritory(territories,
                            String.format("%s Pick territory to place an %d of %d armies", cur.army, remainingArmiesToPlace, startArmiesToPlace));
                    if (picked != null) {
                        RiskCell cell = board.getCell(picked);
                        log.debug("%s placing an army on %s", cur.army, cell.region);
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
                Integer picked = cur.pickTerritory(territories, cur.army + " Pick territory to place a Neutral Army");
                if (picked != null) {
                    RiskCell cell = board.getCell(picked);
                    log.debug("%s placing a neutral army on %s", cur.army, cell.region);
                    cell.numArmies++;
                    state = State.PLACE_ARMY1;
                    nextPlayer();
                }
                break;
            }

            case BEGIN_TURN: {
                int numTerritoriesOccupied = board.getTerritories(getCurrentPlayer().army).size();
                onMessage(cur.army, "Occupies " + numTerritoriesOccupied + " territories");
                int numArmies = board.getTerritories(cur.army).size() / 3;
                // add any extras for holding complete continents
                log.debug("%s gets %d armies to position on the board", cur.army, numArmies);
                for (Region r : Region.values()) {
                    List<Integer> cells = board.getTerritories(r);
                    List<Pair<Integer, Army>> map = Utils.map(cells, cell -> new Pair(cell, board.getCell(cell).getOccupier()));
                    if (map.size() == 1) {
                        Army army = map.iterator().next().second;
                        if (army == cur.army) {
                            numArmies += r.extraArmies;
                            onMessage(cur.army, "Gets " + r.extraArmies + " extra armies for holding " + r.name());
                        }
                    }
                }
                cur.setArmiestoPlace(numArmies);
                state = State.BEGIN_TURN_PLACE_ARMY;
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
            }

            case CHOOSE_MOVE_NO_ATTACK:

                // See which territories can move into an adjacent
                List<Integer> moveable = Utils.filter(territories, idx -> {
                    RiskCell cell = board.getCell(idx);
                    if (cell.numArmies < 2)
                        return false;
                    for (int adj : board.getConnectedCells(cell)) {
                        if (board.getCell(adj).getOccupier() == cur.army) {
                            return true;
                        }
                    }
                    return false;
                });
                if (moveable.size() > 0) {
                    actions.add(Action.MOVE);
                }

                actions.add(Action.END);
                Action action = cur.pickAction(actions, cur.army + " Choose your Move");
                if (action != null) {
                    switch (action) {
                        case ATTACK: {
                            Utils.assertTrue(stageable!=null);
                            Integer start = cur.pickTerritory(stageable, cur.army + " Pick territory from which to stage an attack");
                            if (start != null) {
                                RiskCell cell = board.getCell(start);
                                List<Integer> options = Utils.filter(board.getConnectedCells(cell), idx ->
                                    board.getCell(idx).getOccupier() != cur.army
                                );
                                if (options.size() == 0)
                                    throw new GException("Invalid stagable");
                                Integer end = cur.pickTerritory(options, cur.army + " Pick Territory to Attack");
                                if (end != null) {
                                    performAttack(start, end);
                                }
                            }
                            break;
                        }
                        case MOVE: {
                            Integer start = cur.pickTerritory(moveable, cur.army + " Pick territory from which to move a armys");
                            if (start != null) {
                                RiskCell cell = board.getCell(start);
                                List<Integer> options = Utils.filter(board.getConnectedCells(cell), idx ->
                                        board.getCell(idx).getOccupier() == cur.army
                                );
                                if (options.size() == 0)
                                    throw new GException("Invalid movable");
                                while (cell.getNumArmies() > 1) {
                                    Integer end = cur.pickTerritory(options, cur.army + " Pick Territory to Move an Army to");
                                    if (end == null) {
                                        break;
                                    }
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
        }



    }

    private void performMove(int startIdx, int endIdx) {
        RiskCell start = board.getCell(startIdx);
        RiskCell end   = board.getCell(endIdx);

        onMoveArmy(getCurrentPlayer().army, startIdx, endIdx);
        start.numArmies--;
        end.numArmies++;

    }

    protected void onMoveArmy(Army army, int formIdx, int toIdx) {}

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

        Action numToAttack = null;
        if (options.size() == 1) {
            numToAttack = options.get(0);
        } else {
            numToAttack = getCurrentPlayer().pickAction(options, getCurrentPlayer().army + "Choose Number of Armies to attack");
        }
        if (numToAttack != null) {
            if (end.numArmies <= 0)
                throw new GException("End has no armies!");
            int [] attackingDice = new int[numToAttack.getArmies()];
            int [] defendingDice = new int[Math.min(2, end.numArmies)];
            boolean [] result = new boolean[Math.min(attackingDice.length, defendingDice.length)];
            rollDice(attackingDice);
            rollDice(defendingDice);
            int numToOccupy = attackingDice.length;
            for (int i=0; i<result.length; i++) {
                if (attackingDice[i] > defendingDice[i]) {
                    onDefenderLostArmy(end.occupier, endIdx);
                    end.numArmies--;
                    result[i] = true;
                } else {
                    onAttackerLostArmy(start.occupier, startIdx);
                    start.numArmies--;
                    numToOccupy--;
                }
            }
            onDiceRolled(start.getOccupier(), attackingDice, end.getOccupier(), defendingDice, result);
            if (end.numArmies < 0)
                throw new GException("Cannot have negative armies");
            if (end.numArmies == 0) {
                end.setOccupier(start.getOccupier());
                if (numToOccupy < 1)
                    throw new GException("Cannot occupy with less than 1 armies");
                end.setNumArmies(numToOccupy);
                onAtatckerGainedTerritory(end.occupier, endIdx);
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
            bResult.add(b ? "<" : ">");

        Table table = new Table()
                .addColumn(attacker.name(), aDice)
                .addColumn("", bResult)
                .addColumn(defender.name(), dDice);
        log.info("Result From Dice Roll\n" + table);
    }

    protected void onDefenderLostArmy(Army defender, int cellIdx) {
        log.info("%s Lost an Army from %s", defender, board.getCell(cellIdx).region);
    }

    protected void onAttackerLostArmy(Army attacker, int cellIdx) {
        log.info("%s: Lost an Army from %s", attacker, board.getCell(cellIdx).region);
    }

    protected void onAtatckerGainedTerritory(Army attacker, int cellIdx) {
        log.info("%s: Gained Territory for %s", attacker, board.getCell(cellIdx).region);
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


    RiskPlayer getWinner() {
        List<Pair<Army,Integer>> counts = Utils.map(Utils.getRangeIterator(0, board.getNumCells()-1), idx -> new Pair(((RiskCell)board.getCell(idx)).getOccupier(), idx));
        Map<Army, ?> map = Utils.toMap(counts);
        map.remove(Army.NEUTRAL);
        if (map.size() == 1) {
            return getPlayer(map.keySet().iterator().next());
        }
        return null;
    }

    RiskPlayer getPlayerOrNull(Army army) {
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

    public boolean isGameOver() {
        return state == State.GAME_OVER;
    }

    public Table getSummary() {
        List<String> header = new ArrayList<>();
        header.add("Army");
        for (Region r : Region.values()) {
            header.add(Utils.toPrettyString(r.name()));
        }
        Table table = new Table(header);
        for (Army army: Army.values()) {
            List<Integer> l = board.getTerritories(army);
            if (l.size() == 0)
                continue;
            List<String> row = new ArrayList<>();
            row.add(army.name());
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
