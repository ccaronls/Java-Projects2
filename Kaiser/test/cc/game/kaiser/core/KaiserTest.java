package cc.game.kaiser.core;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import cc.game.kaiser.ai.PlayerBot;
import cc.lib.game.Utils;
import junit.framework.TestCase;

public class KaiserTest extends TestCase {

    @Override
    public void setUp() {
        Kaiser.DEBUG_ENABLED = true;
        System.out.println("Start Test: " + getName());
    }
    
    public void testSerializeTeam() throws Exception {
        Team team = new Team();
        team.bid = Bid.NO_BID;
        team.name = "scrubs";
        team.roundPoints = 10000;
        team.totalPoints = 98320982;
        
        //PrintWriter out = new PrintWriter(System.out);
        StringWriter out = new StringWriter();
        team.serialize(new PrintWriter(out));
        System.out.println(out.toString());
        
        Team t2 = new Team();
        t2.deserialize(out.toString());
        
        assertEquals(t2.totalPoints, team.totalPoints);
        assertEquals(t2.roundPoints, team.roundPoints);
        assertEquals(t2.name, team.name);
        assertEquals(t2, team);
    }
    
    public void testKaiser() throws Exception {
        boolean saved = false;
        Kaiser k = new Kaiser();
        k.setPlayer(0, new PlayerBot("Joe Blow"));
        k.setPlayer(1, new PlayerBot("Pat Robertson"));
        k.setPlayer(2, new PlayerBot("Bobbing for Apples"));
        k.setPlayer(3, new PlayerBot("Sam crow"));
        while (k.getState() != State.GAME_OVER) {
            if (k.getState() == State.NEW_ROUND && !saved && Math.random() * 10 < 2) {
                k.saveToFile(new File("resources/kaisertest.txt"));
                saved = true;
            }
            k.runGame();
        }
    }

    public void testRestoreGame() throws Exception {
        Kaiser.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = false;
        Kaiser k = new Kaiser();
        k.setPlayer(0, new PlayerBot("Joe Blow"));
        k.setPlayer(1, new PlayerBot("Pat Robertson"));
        k.setPlayer(2, new PlayerBot("Bobbing for Apples"));
        k.setPlayer(3, new PlayerBot("Sam crow"));
        while (k.getState() != State.GAME_OVER) {
            k.saveToFile(new File("resources/test.txt"));
            k = new Kaiser();
            k.loadFromFile(new File("resources/test.txt"));
            k.runGame();
            System.out.println("State after run: " + k.getState());
        }
    }

    public void testRestoreGame2() throws Exception {
        Kaiser k = new Kaiser();
        k.loadFromFile(new File("resources/savegame.txt"));
        Player p = k.getPlayer(0);
        assertEquals("Chris", p.getName());
        assertEquals(7, p.getNumCards());
        assertEquals(1, k.getTeam(1).getRoundPoints());
        assertEquals(10, k.getTeam(1).getTotalPoints());

        int cnt = 1000;
        while (cnt-- > 0 && k.getState() != State.GAME_OVER) {
            k.runGame();
        }
        assertTrue(k.getState() == State.GAME_OVER);
    }
    
    public void testSerialize() throws Exception {
        
        Kaiser k = new Kaiser();
        k.loadFromFile(new File("resources/kaisertest.txt"));
        while (k.getState() != State.GAME_OVER) {
            k.runGame();
        }
    }
    
    public void testGetTrickWinnerIndex() {

        Card aceClubs = new Card(Rank.ACE, Suit.CLUBS);
        Card kngDmnds = new Card(Rank.KING, Suit.DIAMONDS);
        Card qunHrts = new Card(Rank.QUEEN, Suit.HEARTS);
        Card jckSpds = new Card(Rank.JACK, Suit.SPADES);
        Card jckHrts = new Card(Rank.JACK, Suit.HEARTS);
        Card jckClubs = new Card(Rank.JACK, Suit.CLUBS);
        Card jckDmnds = new Card(Rank.JACK, Suit.DIAMONDS);
        Card qunDmnds = new Card(Rank.QUEEN, Suit.DIAMONDS);
        Card tenDmnds = new Card(Rank.TEN, Suit.DIAMONDS);
        
        // test when all the same, no trump
        doTestTrick(new Card[] { jckSpds, jckHrts, jckClubs, jckDmnds}, Suit.NOTRUMP, 0, jckSpds);
        doTestTrick(new Card[] { jckSpds, jckHrts, jckClubs, jckDmnds}, Suit.NOTRUMP, 1, jckHrts);
        doTestTrick(new Card[] { jckSpds, jckHrts, jckClubs, jckDmnds}, Suit.NOTRUMP, 2, jckClubs);
        doTestTrick(new Card[] { jckSpds, jckHrts, jckClubs, jckDmnds}, Suit.NOTRUMP, 3, jckDmnds);
        
        // test when all the same, has trump
        for (int i=0 ;i<4; i++) {
            doTestTrick(new Card[] { jckSpds, jckHrts, jckClubs, jckDmnds}, Suit.SPADES, i, jckSpds);
            doTestTrick(new Card[] { jckSpds, jckHrts, jckClubs, jckDmnds}, Suit.HEARTS, i, jckHrts);
            doTestTrick(new Card[] { jckSpds, jckHrts, jckClubs, jckDmnds}, Suit.CLUBS, i, jckClubs);
            doTestTrick(new Card[] { jckSpds, jckHrts, jckClubs, jckDmnds}, Suit.DIAMONDS, i, jckDmnds);
        }
        
        // test when all different, no trump
        doTestTrick(new Card[] { aceClubs, kngDmnds, qunHrts, jckSpds }, Suit.NOTRUMP, 0, aceClubs);
        doTestTrick(new Card[] { aceClubs, kngDmnds, qunHrts, jckSpds }, Suit.NOTRUMP, 1, kngDmnds);
        doTestTrick(new Card[] { aceClubs, kngDmnds, qunHrts, jckSpds }, Suit.NOTRUMP, 2, qunHrts);
        doTestTrick(new Card[] { aceClubs, kngDmnds, qunHrts, jckSpds }, Suit.NOTRUMP, 3, jckSpds);
        
        doTestTrick(new Card[] { aceClubs, qunHrts, jckSpds, jckHrts }, Suit.NOTRUMP, 0, aceClubs);
        doTestTrick(new Card[] { aceClubs, qunHrts, jckSpds, jckHrts }, Suit.NOTRUMP, 1, qunHrts);
        doTestTrick(new Card[] { aceClubs, qunHrts, jckSpds, jckHrts }, Suit.NOTRUMP, 2, jckSpds);
        doTestTrick(new Card[] { aceClubs, qunHrts, jckSpds, jckHrts }, Suit.NOTRUMP, 3, qunHrts);
        
        // test when all different, has trump
        Suit [] suits = new Suit []{ Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES };
        Card [] cards = new Card[] { aceClubs, kngDmnds, qunHrts, jckSpds };
        int index = 0;
        for (Suit trump: suits) {
        
            doTestTrick(cards, trump, 0, cards[index]);
            doTestTrick(cards, trump, 1, cards[index]);
            doTestTrick(cards, trump, 2, cards[index]);
            doTestTrick(cards, trump, 3, cards[index]);
            index++;
        }        
        
        // test all the same suit, no trump
        for (int i=0; i<4; i++)
            doTestTrick(new Card[] { kngDmnds, qunDmnds, jckDmnds, tenDmnds}, Suit.NOTRUMP, i, kngDmnds);

        for (int i=0; i<4; i++)
            doTestTrick(new Card[] { kngDmnds, qunDmnds, jckDmnds, tenDmnds}, Suit.DIAMONDS, i, kngDmnds);

        for (int i=0; i<4; i++)
            doTestTrick(new Card[] { kngDmnds, qunDmnds, jckDmnds, tenDmnds}, Suit.SPADES, i, kngDmnds);

    }
    
    void doTestTrick(Card [] trick, Suit trump, int startIndex, Card expected) {
        int index = Kaiser.getTrickWinnerIndex(trump, trick, startIndex);
        assertEquals(expected, trick[index]);
    }
 /*   
    public void testParseNextLineElement() {
        String line = "PLAYER:0 cc.game.kaiser.swing.SwingPlayerUser \"Chris\"";
        
        assertEquals("PLAYER", Kaiser.parseNextLineElement(line, 0));
        assertEquals("0", Kaiser.parseNextLineElement(line, 1));
        assertEquals("cc.game.kaiser.swing.SwingPlayerUser", Kaiser.parseNextLineElement(line, 2));
        assertEquals("Chris", Kaiser.parseNextLineElement(line, 3));
      
    }
   */ 
    public void test_getBestTrumpOptions() {
        
        Kaiser k = new Kaiser();
        for (int i=0; i<4; i++) {
            Card [] hand = new Card[] { k.getCard(Rank.ACE, Suit.values()[i]) };
        
            Suit [] suits = Kaiser.getBestTrumpOptions(hand);
            System.out.println(Arrays.asList(suits));
        
            assertEquals(Suit.values()[i], suits[0]);
        }

        {
            Card [] hand = new Card[] { 
                    new Card(Rank.EIGHT, Suit.DIAMONDS), 
                    new Card(Rank.NINE, Suit.DIAMONDS), 
                    new Card(Rank.TEN, Suit.DIAMONDS), 
                    new Card(Rank.EIGHT, Suit.HEARTS), 
                    new Card(Rank.EIGHT, Suit.HEARTS), 
                    new Card(Rank.EIGHT, Suit.CLUBS) 
            };
    
            Suit [] suits = Kaiser.getBestTrumpOptions(hand);
            assertEquals(Suit.DIAMONDS, suits[0]);
            assertEquals(Suit.HEARTS, suits[1]);
            assertEquals(Suit.CLUBS, suits[2]);
        }


    }
    
    public void test_computeBidOptions() {
        
        Card [] hand = new Card[] { 
                new Card(Rank.EIGHT, Suit.DIAMONDS), 
                new Card(Rank.NINE, Suit.DIAMONDS), 
                new Card(Rank.TEN, Suit.CLUBS), 
                new Card(Rank.EIGHT, Suit.HEARTS), 
                new Card(Rank.ACE, Suit.CLUBS), 
                new Card(Rank.QUEEN, Suit.DIAMONDS), 
                new Card(Rank.TEN, Suit.SPADES), 
                new Card(Rank.FIVE, Suit.HEARTS), 
        };
        
        Bid [] options;
        //Bid [] options = Kaiser.computeBidOptions(null, false, hand);
        //System.out.println(Arrays.asList(options));
        //options = Kaiser.computeBidOptions(null, true, hand);
        //System.out.println(Arrays.asList(options));
        options = Kaiser.computeBidOptions(new Bid(10, Suit.HEARTS), false, hand);
        System.out.println(Arrays.asList(options));
        options = Kaiser.computeBidOptions(new Bid(10, Suit.HEARTS), true, hand);
        System.out.println(Arrays.asList(options));
        options = Kaiser.computeBidOptions(new Bid(10, Suit.NOTRUMP), false, hand);
        System.out.println(Arrays.asList(options));
        options = Kaiser.computeBidOptions(new Bid(10, Suit.NOTRUMP), true, hand);
        System.out.println(Arrays.asList(options));
    }
}
