package cc.lib.risk;

import junit.framework.TestCase;

import java.io.File;
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
        game.addPlayer(new RiskPlayer(Army.MAGENTA));

        try (InputStream is = FileUtils.openFileOrResource("risk.board")) {
            game.getBoard().deserialize(is);
        }
        for (int i=0; i<10000 && !game.isGameOver(); i++) {
            try {
                game.runGame();
            } catch (Exception e) {
                e.printStackTrace();
                game.getBoard().saveToFile(new File("risk_failed_game.board"));
                throw e;
            }
            System.out.println(game.getSummary());
        }
        System.out.println(game.getSummary());
        System.out.println("Winner: " + game.getWinner());
    }


}
