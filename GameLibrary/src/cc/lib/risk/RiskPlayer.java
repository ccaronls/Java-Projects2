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
    private int armiesToPlace;
    private int initialArmiesToPlace;

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

    public Integer pickTerritory(RiskGame game, List<Integer> options, String msg) {
        return Utils.randItem(options);
    }

    public Action pickAction(RiskGame game, List<Action> actions, String msg) {
        return Utils.randItem(actions);
    }

}
