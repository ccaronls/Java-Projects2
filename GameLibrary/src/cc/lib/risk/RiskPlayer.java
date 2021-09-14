package cc.lib.risk;

import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

/**
 * Created by Chris Caron on 9/13/21.
 */
public class RiskPlayer extends Reflector<RiskPlayer> {

    static {
        addAllFields(RiskPlayer.class);
    }

    Army army;
    int armiesToPlace;

    public RiskPlayer() {}

    public RiskPlayer(Army army) {
        this.army = army;
    }

    public Integer pickTerritory(List<Integer> options, String msg) {
        return Utils.randItem(options);
    }

    public Action pickAction(List<Action> actions) {
        if (actions.get(0) == Action.ATTACK)
            return Action.ATTACK;
        return Action.END;
    }

    public Integer pickNumberToAttack(List<Integer> options) {
        return options.get(options.size()-1);
    }
}
