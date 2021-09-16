package cc.lib.risk;

import junit.framework.TestCase;

import java.io.InputStream;

import cc.lib.game.Utils;
import cc.lib.utils.FileUtils;

/**
 * Created by Chris Caron on 9/13/21.
 */
public class RiskTest extends TestCase {

    public void testGame() throws Exception {
        Utils.setDebugEnabled();
        RiskGame game = new RiskGame();
        game.addPlayer(new RiskPlayer(Army.BLUE));
        game.addPlayer(new RiskPlayer(Army.GREEN));
        try (InputStream is = FileUtils.openFileOrResource("risk.board")) {
            game.getBoard().deserialize(is);
        }
        for (int i=0; i<1000 && !game.isGameOver(); i++) {
            game.runGame();
        }
        System.out.println(game.getSummary());
        System.out.println("Winner: " + game.getWinner());
    }


}
