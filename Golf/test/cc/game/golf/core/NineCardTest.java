package cc.game.golf.core;

import cc.game.golf.core.Rules.GameType;
import junit.framework.TestCase;

public class NineCardTest extends TestCase {

    public void testSetMatching() {
        Rules rules = new Rules();
        rules.setGameType(GameType.NineCard);
        Card [][] cards = {
                { new Card(0, Rank.ACE, Suit.CLUBS, false), new Card(0, Rank.TWO, Suit.DIAMONDS, false), new Card(0, Rank.THREE, Suit.HEARTS, false) },
                { new Card(0, Rank.ACE, Suit.DIAMONDS, false), new Card(0, Rank.TWO, Suit.HEARTS, false), new Card(0, Rank.THREE, Suit.SPADES, false) },
                { new Card(0, Rank.ACE, Suit.HEARTS, false), new Card(0, Rank.TWO, Suit.SPADES, false), new Card(0, Rank.THREE, Suit.CLUBS, false) },
        };
        assertTrue(Player.getHandPoints(rules, cards) == 0);
    }
    
}
