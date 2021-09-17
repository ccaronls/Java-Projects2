package cc.game.android.risk;

import java.util.List;

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
    public Integer pickTerritory(RiskGame game, List<Integer> options, String msg) {
        return RiskActivity.instance.pickTerritory(options, msg);
    }

    @Override
    public Action pickAction(RiskGame game, List<Action> actions, String msg) {
        return RiskActivity.instance.pickAction(actions, msg);
    }

}
