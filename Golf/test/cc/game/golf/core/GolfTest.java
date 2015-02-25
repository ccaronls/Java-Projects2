package cc.game.golf.core;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import cc.game.golf.ai.PlayerBot;
import cc.game.golf.core.Rules.GameType;
import cc.game.golf.core.Rules.KnockerBonusType;
import cc.game.golf.core.Rules.KnockerPenaltyType;
import cc.game.golf.core.Rules.WildCard;
import junit.framework.TestCase;

public class GolfTest extends TestCase {

    public void test1() {
        Golf golf = new Golf() {

            @Override
            protected void message(String format, Object... params) {
                // TODO Auto-generated method stub
                //super.message(format, params);
            }
            
        };
        golf.addPlayer(new PlayerBot());
        golf.addPlayer(new PlayerBot());
        golf.addPlayer(new PlayerBot());
        long startTime = System.currentTimeMillis();
        Rules rules = new Rules(GameType.SixCard, 10,
                2,8,-5,-5,KnockerPenaltyType.None,KnockerBonusType.None, -20, WildCard.Dueces);
        for (int i=0; i<1000; i++) {
            golf.newGame(rules);
            while (golf.getState() != State.GAME_OVER) {
                golf.runGame();
            }
            System.out.print(".");
        }
        long endTime = System.currentTimeMillis();
        System.out.println("\nPlayed 1000 games in " + (endTime - startTime)/1000 + " seconds");
    }
    
    public void testRestore() throws Exception {
        Golf.DEBUG_ENABLED = true;
        Golf golf = new Golf();
        golf.addPlayer(new PlayerBot());
        golf.addPlayer(new PlayerBot());
        Rules rules = new Rules(GameType.NineCard, 18,
                1,2,-10,-5,KnockerPenaltyType.EqualToHighestHand,KnockerBonusType.MinusNumberOfPlayers, -20, WildCard.OneEyedJacks);
        golf.newGame(rules);
        for (int i=0; i<100; i++) {
            golf.runGame();
        }
        File file = new File("resources/testsave.txt");
        golf.saveGame(file);
        //golf.clear();
        //golf.newGame(new Rules());
        Golf golf2 = new Golf();
        golf2.loadGame(file);
        assertTrue(golf2.getNumPlayers() == 2);
        assertTrue(golf2.getState() == golf.getState());
        assertTrue(golf2.getDealer() == golf.getDealer());
        assertTrue(golf2.getCurrentPlayer() == golf.getCurrentPlayer());
        assertTrue(golf2.getNumDeckCards() == golf.getNumDeckCards());
        assertTrue(golf2.getTopOfDiscardPile().equals(golf.getTopOfDiscardPile()));
        assertTrue(golf2.getDeck().subList(0, golf2.getNumDeckCards()).equals(golf.getDeck().subList(0,  golf.getNumDeckCards())));
        assertTrue(golf2.getKnocker() == golf.getKnocker());
        for (int i=0; i<golf.getNumPlayers(); i++) {
            assertTrue(golf.getPlayer(i).getPoints() == golf2.getPlayer(i).getPoints());
            assertTrue(golf.getPlayer(i).getRow(0).equals(golf2.getPlayer(i).getRow(0)));
            assertTrue(golf.getPlayer(i).getRow(1).equals(golf2.getPlayer(i).getRow(1)));
        }
    }
    
}
