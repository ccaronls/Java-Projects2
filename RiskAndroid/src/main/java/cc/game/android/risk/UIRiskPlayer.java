package cc.game.android.risk;

import java.util.List;

import cc.lib.game.Utils;
import cc.lib.risk.Action;
import cc.lib.risk.Army;
import cc.lib.risk.RiskGame;
import cc.lib.risk.RiskPlayer;

/**
 * Created by Chris Caron on 9/14/21.
 */
public class UIRiskPlayer extends RiskPlayer {

    public UIRiskPlayer() {}

    public UIRiskPlayer(Army army) {
        super(army);
    }

    @Override
    public Integer pickTerritoryToClaim(RiskGame game, List<Integer> options) {
        return RiskActivity.instance.pickTerritory(options, getArmy() + " Pick a territory to claim");
    }

    public Integer pickTerritoryForArmy(RiskGame game, List<Integer> options, int remainingArmiesToPlace, int startArmiesToPlace) {
        return RiskActivity.instance.pickTerritory(options, String.format("%s Pick territory to place the %d%s of %d armies", getArmy(), remainingArmiesToPlace+1, Utils.getSuffix(remainingArmiesToPlace+1), startArmiesToPlace));
    }

    public Integer pickTerritoryForNeutralArmy(RiskGame game, List<Integer> options) {
        return RiskActivity.instance.pickTerritory(options, getArmy() + " Pick territory to place a Neutral Army");
    }

    @Override
    public Integer pickTerritoryToAttackFrom(RiskGame game, List<Integer> options) {
        return RiskActivity.instance.pickTerritory(options, getArmy() + " Pick territory from which to stage an attack");
    }

    @Override
    public Integer pickTerritoryToAttack(RiskGame game, int territoryAttackingFrom, List<Integer> options) {
        return RiskActivity.instance.pickTerritory(options, getArmy() + " Pick Territory to Attack");
    }

    @Override
    public Integer pickTerritoryToMoveFrom(RiskGame game, List<Integer> options) {
        return RiskActivity.instance.pickTerritory(options, getArmy() + " Pick territory from which to move a armys");
    }

    @Override
    public Integer pickTerritoryToMoveTo(RiskGame game, int territoryMovingFrom, List<Integer> options) {
        return RiskActivity.instance.pickTerritory(options, getArmy() + " Pick Territory to Move an Army to");
    }

    @Override
    public Action pickAction(RiskGame game, List<Action> actions, String msg) {
        return RiskActivity.instance.pickAction(actions, msg);
    }

}
