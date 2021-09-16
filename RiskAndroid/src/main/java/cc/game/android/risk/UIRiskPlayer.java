package cc.game.android.risk;

import java.util.List;

import cc.lib.risk.Action;
import cc.lib.risk.Army;
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
    public Integer pickTerritory(List<Integer> options, String msg) {
        return MainActivity.instance.pickTerritory(options, msg);
    }

    @Override
    public Action pickAction(List<Action> actions, String msg) {
        return MainActivity.instance.pickAction(actions, msg);
    }

}
