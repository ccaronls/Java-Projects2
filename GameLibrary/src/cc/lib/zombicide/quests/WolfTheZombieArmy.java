package cc.lib.zombicide.quests;

import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;

/**
 * Created by Chris Caron on 9/1/21.
 */
public class WolfTheZombieArmy extends ZQuest {

    public WolfTheZombieArmy() {
        super(ZQuests.Zombie_Court);
    }

    @Override
    public ZBoard loadBoard() {
        return null;
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[0];
    }

    @Override
    public void init(ZGame game) {

    }

    @Override
    public int getPercentComplete(ZGame game) {
        return 0;
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return null;
    }
}
